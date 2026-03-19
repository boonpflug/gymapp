import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../api/client'
import type {
  ApiResponse,
  ExerciseDto,
  TrainingPlanDto,
  TrainingGoalDto,
  ExerciseType,
  MuscleGroup,
  TrainingPlanStatus,
  GoalType,
  MemberDto,
} from '../types'

const MUSCLE_GROUPS: MuscleGroup[] = [
  'CHEST', 'BACK', 'SHOULDERS', 'BICEPS', 'TRICEPS', 'FOREARMS',
  'ABS', 'OBLIQUES', 'LOWER_BACK', 'QUADRICEPS', 'HAMSTRINGS',
  'GLUTES', 'CALVES', 'TRAPS', 'LATS', 'FULL_BODY', 'CARDIO',
]
const EXERCISE_TYPES: ExerciseType[] = [
  'STRENGTH', 'CARDIO', 'FLEXIBILITY', 'BALANCE', 'BODYWEIGHT',
  'MACHINE', 'FREE_WEIGHT', 'CABLE', 'RESISTANCE_BAND', 'FUNCTIONAL', 'STRETCHING',
]
const GOAL_TYPES: GoalType[] = [
  'WEIGHT_LOSS', 'MUSCLE_GAIN', 'STRENGTH', 'ENDURANCE',
  'FLEXIBILITY', 'GENERAL_FITNESS', 'CUSTOM',
]

type Tab = 'exercises' | 'plans' | 'templates' | 'catalog' | 'goals'

export default function TrainingPage() {
  const [activeTab, setActiveTab] = useState<Tab>('exercises')
  const tabs: { key: Tab; label: string }[] = [
    { key: 'exercises', label: 'Exercise Library' },
    { key: 'plans', label: 'Training Plans' },
    { key: 'templates', label: 'Templates' },
    { key: 'catalog', label: 'Catalog' },
    { key: 'goals', label: 'Goals' },
  ]

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Training</h1>
      <div className="border-b border-gray-200 mb-6">
        <nav className="flex space-x-8">
          {tabs.map((tab) => (
            <button key={tab.key} onClick={() => setActiveTab(tab.key)}
              className={`pb-3 px-1 text-sm font-medium border-b-2 ${activeTab === tab.key ? 'border-brand-500 text-brand-600' : 'border-transparent text-gray-500 hover:text-gray-700'}`}>
              {tab.label}
            </button>
          ))}
        </nav>
      </div>
      {activeTab === 'exercises' && <ExerciseLibrary />}
      {activeTab === 'plans' && <TrainingPlans />}
      {activeTab === 'templates' && <TemplatesTab />}
      {activeTab === 'catalog' && <PlanListView queryKey="training-catalog" url="/training/plans/catalog?size=50" emptyMsg="No catalog plans. Create a plan with Catalog checked and publish it." label="Catalog" />}
      {activeTab === 'goals' && <GoalsList />}
    </div>
  )
}

// ===================== EXERCISE LIBRARY =====================

function ExerciseLibrary() {
  const qc = useQueryClient()
  const [search, setSearch] = useState('')
  const [muscleFilter, setMuscleFilter] = useState('')
  const [typeFilter, setTypeFilter] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editExercise, setEditExercise] = useState<ExerciseDto | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['exercises', search, muscleFilter, typeFilter],
    queryFn: () => {
      const params: Record<string, string> = { size: '50' }
      if (search) params.name = search
      if (muscleFilter) params.muscleGroup = muscleFilter
      if (typeFilter) params.exerciseType = typeFilter
      return api.get<ApiResponse<ExerciseDto[]>>('/training/exercises', { params }).then(r => r.data)
    },
  })
  const exercises = Array.isArray(data?.data) ? data!.data : []

  const saveMutation = useMutation({
    mutationFn: ({ id, data }: { id?: string; data: Record<string, unknown> }) =>
      id ? api.post(`/training/exercises/${id}`, data) : api.post('/training/exercises', data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['exercises'] }); setShowModal(false); setEditExercise(null) },
  })

  const openCreate = () => { setEditExercise(null); setShowModal(true) }
  const openEdit = (ex: ExerciseDto) => { setEditExercise(ex); setShowModal(true) }

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <div className="flex gap-3 flex-1">
          <input type="text" placeholder="Search exercises..." value={search} onChange={e => setSearch(e.target.value)} className="border rounded-lg px-3 py-2 text-sm w-64" />
          <select value={muscleFilter} onChange={e => setMuscleFilter(e.target.value)} className="border rounded-lg px-3 py-2 text-sm">
            <option value="">All Muscles</option>
            {MUSCLE_GROUPS.map(mg => <option key={mg} value={mg}>{mg.replace(/_/g, ' ')}</option>)}
          </select>
          <select value={typeFilter} onChange={e => setTypeFilter(e.target.value)} className="border rounded-lg px-3 py-2 text-sm">
            <option value="">All Types</option>
            {EXERCISE_TYPES.map(t => <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>)}
          </select>
        </div>
        <button onClick={openCreate} className="bg-brand-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-brand-700">Add Exercise</button>
      </div>

      {isLoading ? <p className="text-gray-500">Loading...</p> : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {exercises.map(ex => (
            <div key={ex.id} onClick={() => openEdit(ex)} className="bg-white rounded-lg shadow p-4 cursor-pointer hover:shadow-md">
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="font-medium text-gray-900">{ex.name}</h3>
                  <p className="text-xs text-gray-500 mt-1">{ex.exerciseType.replace(/_/g, ' ')} &middot; {ex.primaryMuscleGroup.replace(/_/g, ' ')}</p>
                </div>
                {ex.equipment && <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded">{ex.equipment}</span>}
              </div>
              {ex.description && <p className="text-sm text-gray-600 mt-2 line-clamp-2">{ex.description}</p>}
              {ex.difficultyLevel && (
                <span className={`text-xs mt-2 inline-block px-2 py-0.5 rounded ${ex.difficultyLevel === 'BEGINNER' ? 'bg-green-100 text-green-700' : ex.difficultyLevel === 'INTERMEDIATE' ? 'bg-yellow-100 text-yellow-700' : 'bg-red-100 text-red-700'}`}>
                  {ex.difficultyLevel}
                </span>
              )}
            </div>
          ))}
        </div>
      )}

      {showModal && (
        <ExerciseModal
          initial={editExercise}
          onClose={() => { setShowModal(false); setEditExercise(null) }}
          onSubmit={data => saveMutation.mutate({ id: editExercise?.id, data })}
          isLoading={saveMutation.isPending}
        />
      )}
    </div>
  )
}

function ExerciseModal({ initial, onClose, onSubmit, isLoading }: {
  initial: ExerciseDto | null; onClose: () => void
  onSubmit: (data: Record<string, unknown>) => void; isLoading: boolean
}) {
  const [form, setForm] = useState({
    name: initial?.name ?? '', description: initial?.description ?? '',
    exerciseType: (initial?.exerciseType ?? 'STRENGTH') as ExerciseType,
    primaryMuscleGroup: (initial?.primaryMuscleGroup ?? 'CHEST') as MuscleGroup,
    secondaryMuscleGroup: initial?.secondaryMuscleGroup ?? '',
    equipment: initial?.equipment ?? '', videoUrl: initial?.videoUrl ?? '',
    executionTips: initial?.executionTips ?? '', postureNotes: initial?.postureNotes ?? '',
    difficultyLevel: initial?.difficultyLevel ?? 'BEGINNER',
  })
  const [suggestTerm, setSuggestTerm] = useState('')
  const [showSuggestions, setShowSuggestions] = useState(false)
  const [appliedSuggestion, setAppliedSuggestion] = useState<string | null>(null)

  // Fuzzy suggest query — fires as user types exercise name (only for new exercises)
  const { data: suggestRes } = useQuery({
    queryKey: ['exercise-suggest', suggestTerm],
    queryFn: () => api.get<ApiResponse<ExerciseDto[]>>('/training/exercises/suggest', { params: { name: suggestTerm } }).then(r => r.data),
    enabled: !initial && suggestTerm.length >= 3 && showSuggestions,
  })
  const suggestions: ExerciseDto[] = Array.isArray(suggestRes?.data) ? suggestRes!.data : []

  const handleNameChange = (value: string) => {
    setForm({ ...form, name: value })
    setSuggestTerm(value)
    setShowSuggestions(true)
    setAppliedSuggestion(null)
  }

  const applySuggestion = (ex: ExerciseDto) => {
    setForm({
      name: form.name, // keep the name the user typed
      description: ex.description || form.description,
      exerciseType: ex.exerciseType || form.exerciseType,
      primaryMuscleGroup: ex.primaryMuscleGroup || form.primaryMuscleGroup,
      secondaryMuscleGroup: ex.secondaryMuscleGroup || form.secondaryMuscleGroup,
      equipment: ex.equipment || form.equipment,
      videoUrl: ex.videoUrl || form.videoUrl,
      executionTips: ex.executionTips || form.executionTips,
      postureNotes: ex.postureNotes || form.postureNotes,
      difficultyLevel: ex.difficultyLevel || form.difficultyLevel,
    })
    setShowSuggestions(false)
    setAppliedSuggestion(ex.name)
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto p-6">
        <h2 className="text-lg font-bold mb-4">{initial ? 'Edit Exercise' : 'Add Exercise'}</h2>
        <div className="space-y-3">
          {/* Name field with smart suggest */}
          <div className="relative">
            <input placeholder="Exercise name *" value={form.name}
              onChange={e => handleNameChange(e.target.value)}
              onFocus={() => form.name.length >= 3 && setShowSuggestions(true)}
              className="w-full border rounded px-3 py-2 text-sm" />
            {!initial && showSuggestions && suggestions.length > 0 && (
              <div className="absolute z-20 w-full bg-white border border-brand-200 rounded-lg shadow-lg mt-1 overflow-hidden">
                <div className="px-3 py-1.5 bg-brand-50 text-xs text-brand-600 font-medium">
                  Auto-fill from similar exercises
                </div>
                {suggestions.map(s => (
                  <button key={s.id} onClick={() => applySuggestion(s)}
                    className="w-full text-left px-3 py-2.5 text-sm hover:bg-brand-50 flex items-center gap-3 border-t border-gray-100">
                    {s.thumbnailUrl ? (
                      <img src={s.thumbnailUrl} alt="" className="w-10 h-7 object-cover rounded flex-shrink-0" />
                    ) : (
                      <div className="w-10 h-7 bg-gray-100 rounded flex-shrink-0 flex items-center justify-center text-xs">🏋️</div>
                    )}
                    <div className="flex-1 min-w-0">
                      <p className="font-medium truncate">{s.name}</p>
                      <p className="text-xs text-gray-400 truncate">
                        {s.primaryMuscleGroup?.replace(/_/g, ' ')} &middot; {s.exerciseType?.replace(/_/g, ' ')}
                        {s.equipment && <> &middot; {s.equipment}</>}
                      </p>
                    </div>
                    <span className="text-xs text-brand-500 flex-shrink-0">Fill</span>
                  </button>
                ))}
                <button onClick={() => setShowSuggestions(false)}
                  className="w-full text-center py-1.5 text-xs text-gray-400 hover:text-gray-600 border-t">
                  Dismiss
                </button>
              </div>
            )}
          </div>

          {/* Applied suggestion banner */}
          {appliedSuggestion && (
            <div className="bg-green-50 border border-green-200 rounded-lg px-3 py-2 text-xs text-green-700 flex items-center justify-between">
              <span>Auto-filled from: <strong>{appliedSuggestion}</strong></span>
              <button onClick={() => setAppliedSuggestion(null)} className="text-green-500 hover:text-green-700">&times;</button>
            </div>
          )}

          <textarea placeholder="Description" value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} className="w-full border rounded px-3 py-2 text-sm" rows={2} />
          <div className="grid grid-cols-2 gap-3">
            <select value={form.exerciseType} onChange={e => setForm({ ...form, exerciseType: e.target.value as ExerciseType })} className="border rounded px-3 py-2 text-sm">
              {EXERCISE_TYPES.map(t => <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>)}
            </select>
            <select value={form.primaryMuscleGroup} onChange={e => setForm({ ...form, primaryMuscleGroup: e.target.value as MuscleGroup })} className="border rounded px-3 py-2 text-sm">
              {MUSCLE_GROUPS.map(mg => <option key={mg} value={mg}>{mg.replace(/_/g, ' ')}</option>)}
            </select>
          </div>
          <select value={form.secondaryMuscleGroup} onChange={e => setForm({ ...form, secondaryMuscleGroup: e.target.value })} className="w-full border rounded px-3 py-2 text-sm">
            <option value="">No secondary muscle group</option>
            {MUSCLE_GROUPS.map(mg => <option key={mg} value={mg}>{mg.replace(/_/g, ' ')}</option>)}
          </select>
          <input placeholder="Equipment" value={form.equipment} onChange={e => setForm({ ...form, equipment: e.target.value })} className="w-full border rounded px-3 py-2 text-sm" />
          <input placeholder="Video URL" value={form.videoUrl} onChange={e => setForm({ ...form, videoUrl: e.target.value })} className="w-full border rounded px-3 py-2 text-sm" />
          <textarea placeholder="Execution tips" value={form.executionTips} onChange={e => setForm({ ...form, executionTips: e.target.value })} className="w-full border rounded px-3 py-2 text-sm" rows={2} />
          <textarea placeholder="Posture notes" value={form.postureNotes} onChange={e => setForm({ ...form, postureNotes: e.target.value })} className="w-full border rounded px-3 py-2 text-sm" rows={2} />
          <select value={form.difficultyLevel} onChange={e => setForm({ ...form, difficultyLevel: e.target.value })} className="w-full border rounded px-3 py-2 text-sm">
            <option value="BEGINNER">Beginner</option><option value="INTERMEDIATE">Intermediate</option><option value="ADVANCED">Advanced</option>
          </select>
        </div>
        <div className="flex justify-end gap-3 mt-6">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600">Cancel</button>
          <button onClick={() => onSubmit({ ...form, secondaryMuscleGroup: form.secondaryMuscleGroup || undefined })}
            disabled={!form.name || isLoading} className="bg-brand-600 text-white px-4 py-2 rounded-lg text-sm disabled:opacity-50">
            {isLoading ? 'Saving...' : initial ? 'Update' : 'Create'}
          </button>
        </div>
      </div>
    </div>
  )
}

// ===================== TRAINING PLANS (Member Plans) =====================

function TrainingPlans() {
  const qc = useQueryClient()
  const [showCreate, setShowCreate] = useState(false)
  const [selectedPlan, setSelectedPlan] = useState<string | null>(null)
  const [memberSearch, setMemberSearch] = useState('')
  const [selectedMember, setSelectedMember] = useState<MemberDto | null>(null)
  const [showAssignTemplate, setShowAssignTemplate] = useState(false)

  const { data: membersRes } = useQuery({
    queryKey: ['members-plan-search', memberSearch],
    queryFn: () => api.get<ApiResponse<MemberDto[]>>('/members', { params: { name: memberSearch, size: 5 } }).then(r => r.data),
    enabled: memberSearch.length >= 2 && !selectedMember,
  })

  const { data, isLoading } = useQuery({
    queryKey: ['member-plans', selectedMember?.id],
    queryFn: () => api.get<ApiResponse<TrainingPlanDto[]>>(`/training/plans/member/${selectedMember!.id}`).then(r => r.data),
    enabled: !!selectedMember,
  })

  const { data: templatesRes } = useQuery({
    queryKey: ['templates-for-assign'],
    queryFn: () => api.get<ApiResponse<TrainingPlanDto[]>>('/training/plans/templates?size=50').then(r => r.data),
    enabled: showAssignTemplate,
  })

  const publishMut = useMutation({
    mutationFn: (id: string) => api.post(`/training/plans/${id}/publish`),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['member-plans'] }); setSelectedPlan(null) },
  })
  const archiveMut = useMutation({
    mutationFn: (id: string) => api.post(`/training/plans/${id}/archive`),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['member-plans'] }); setSelectedPlan(null) },
  })
  const assignMut = useMutation({
    mutationFn: (templateId: string) => api.post(`/training/plans/from-template/${templateId}`, null, { params: { memberId: selectedMember!.id } }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['member-plans'] }); setShowAssignTemplate(false) },
  })

  const plans = Array.isArray(data?.data) ? data!.data : []
  const templates = Array.isArray(templatesRes?.data) ? templatesRes!.data : []

  return (
    <div>
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6 text-sm text-blue-800">
        <strong>How it works:</strong> Search for a member below to view & manage their training plans. You can create a custom plan from scratch or assign an existing template. Plans start as <em>Draft</em> — publish them when ready so the member can see them.
      </div>

      {/* Member search */}
      <div className="mb-6">
        <label className="block text-sm font-medium text-gray-700 mb-1">Select member</label>
        <div className="relative max-w-md">
          <input type="text" placeholder="Search member by name..." value={memberSearch}
            onChange={e => { setMemberSearch(e.target.value); if (selectedMember) setSelectedMember(null) }}
            className="w-full border rounded-lg px-4 py-2.5 text-sm focus:ring-2 focus:ring-brand-500" />
          {!selectedMember && (membersRes?.data ?? []).length > 0 && (
            <div className="absolute z-10 w-full bg-white border rounded-lg shadow-lg mt-1 max-h-48 overflow-y-auto">
              {(membersRes?.data ?? []).map(m => (
                <button key={m.id} onClick={() => { setSelectedMember(m); setMemberSearch(m.firstName + ' ' + m.lastName) }}
                  className="w-full text-left px-4 py-3 text-sm hover:bg-brand-50 border-b last:border-0">
                  <span className="font-medium">{m.firstName} {m.lastName}</span>
                  <span className="text-gray-400 ml-2">#{m.memberNumber}</span>
                </button>
              ))}
            </div>
          )}
        </div>
        {selectedMember && (
          <div className="flex items-center gap-2 mt-2">
            <span className="bg-brand-100 text-brand-700 px-3 py-1 rounded-full text-sm font-medium">
              {selectedMember.firstName} {selectedMember.lastName}
            </span>
            <button onClick={() => { setSelectedMember(null); setMemberSearch('') }} className="text-xs text-gray-500 hover:text-red-500">Clear</button>
          </div>
        )}
      </div>

      {!selectedMember ? (
        <p className="text-gray-400 text-center py-8">Search and select a member above to manage their training plans.</p>
      ) : (
        <>
          {/* Actions */}
          <div className="flex gap-3 mb-4">
            <button onClick={() => setShowCreate(true)} className="bg-brand-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-brand-700">
              Create Custom Plan
            </button>
            <button onClick={() => setShowAssignTemplate(true)} className="bg-purple-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-purple-700">
              Assign from Template
            </button>
          </div>

          {/* Member's plans */}
          {isLoading ? <p className="text-gray-500">Loading plans...</p> : plans.length === 0 ? (
            <p className="text-gray-400">No plans yet for {selectedMember.firstName}. Create one or assign a template above.</p>
          ) : (
            <div className="space-y-3">
              {plans.map(plan => (
                <div key={plan.id} onClick={() => setSelectedPlan(plan.id)}
                  className="bg-white rounded-lg shadow p-4 cursor-pointer hover:shadow-md transition-shadow">
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="font-medium">{plan.name}</h3>
                      <p className="text-xs text-gray-500 mt-1">
                        {plan.exerciseCount} exercises
                        {plan.trainerName && <> &middot; Trainer: {plan.trainerName}</>}
                        {plan.category && <> &middot; {plan.category}</>}
                      </p>
                    </div>
                    <div className="flex items-center gap-2">
                      <PlanStatusBadge status={plan.status} />
                    </div>
                  </div>
                  {plan.description && <p className="text-sm text-gray-600 mt-2 line-clamp-1">{plan.description}</p>}
                  {plan.status === 'DRAFT' && (
                    <p className="text-xs text-amber-600 mt-2">Draft — member can't see this yet. Click to publish.</p>
                  )}
                </div>
              ))}
            </div>
          )}

          {/* Assign template modal */}
          {showAssignTemplate && (
            <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
              <div className="bg-white rounded-xl shadow-xl w-full max-w-lg max-h-[80vh] flex flex-col">
                <div className="px-6 py-4 border-b">
                  <h2 className="text-lg font-bold">Assign Template to {selectedMember.firstName}</h2>
                  <p className="text-sm text-gray-500 mt-1">This creates a copy of the template as a personal plan for the member.</p>
                </div>
                <div className="flex-1 overflow-y-auto p-4 space-y-2">
                  {templates.length === 0 ? (
                    <p className="text-gray-400 text-center py-8">No templates available. Create a plan with "Save as Template" checked first.</p>
                  ) : templates.map(t => (
                    <div key={t.id} className="border rounded-lg p-4 hover:border-purple-300 hover:bg-purple-50 cursor-pointer transition-colors"
                      onClick={() => assignMut.mutate(t.id)}>
                      <div className="flex items-center justify-between">
                        <div>
                          <h3 className="font-medium">{t.name}</h3>
                          <p className="text-xs text-gray-500 mt-1">
                            {t.exerciseCount} exercises
                            {t.difficultyLevel && <> &middot; {t.difficultyLevel}</>}
                            {t.category && <> &middot; {t.category}</>}
                          </p>
                        </div>
                        <span className="text-purple-600 text-sm font-medium">Assign</span>
                      </div>
                      {t.description && <p className="text-sm text-gray-500 mt-1 line-clamp-1">{t.description}</p>}
                    </div>
                  ))}
                </div>
                <div className="px-6 py-3 border-t bg-gray-50 flex justify-end">
                  <button onClick={() => setShowAssignTemplate(false)} className="px-4 py-2 text-sm text-gray-600">Cancel</button>
                </div>
              </div>
            </div>
          )}
        </>
      )}

      {showCreate && <CreatePlanModal onClose={() => setShowCreate(false)} onCreated={() => { qc.invalidateQueries({ queryKey: ['member-plans'] }); setShowCreate(false) }} defaultMemberId={selectedMember?.id} />}
      {selectedPlan && <PlanDetailModal planId={selectedPlan} onClose={() => setSelectedPlan(null)} onPublish={id => publishMut.mutate(id)} onArchive={id => archiveMut.mutate(id)} />}
    </div>
  )
}

function PlanStatusBadge({ status }: { status: TrainingPlanStatus }) {
  const c = { DRAFT: 'bg-yellow-100 text-yellow-700', PUBLISHED: 'bg-green-100 text-green-700', ARCHIVED: 'bg-gray-100 text-gray-600' }
  return <span className={`text-xs px-2 py-0.5 rounded ${c[status]}`}>{status}</span>
}

function CreatePlanModal({ onClose, onCreated, defaultMemberId, defaultTemplate }: { onClose: () => void; onCreated: () => void; defaultMemberId?: string; defaultTemplate?: boolean }) {
  const [form, setForm] = useState({
    name: '', description: '', memberId: defaultMemberId ?? '',
    template: defaultTemplate ?? false, catalog: false, category: '',
    estimatedDurationMinutes: '', difficultyLevel: 'BEGINNER',
  })
  const [planExercises, setPlanExercises] = useState<{ exerciseId: string; exerciseName: string; muscleGroup: string; exerciseType: string; sets: number; reps: number; weight: string; restSeconds: string; trainerComment: string }[]>([])
  const [exSearch, setExSearch] = useState('')
  const [exMuscleFilter, setExMuscleFilter] = useState('')
  const [step, setStep] = useState<'details' | 'exercises'>('details')

  // Browse all exercises (not just search) — loads immediately
  const { data: browseRes, isLoading: browsing } = useQuery({
    queryKey: ['ex-browse', exSearch, exMuscleFilter],
    queryFn: () => {
      const params: Record<string, string> = { size: '30' }
      if (exSearch) params.name = exSearch
      if (exMuscleFilter) params.muscleGroup = exMuscleFilter
      return api.get<ApiResponse<ExerciseDto[]>>('/training/exercises', { params }).then(r => r.data)
    },
  })
  const availableExercises: ExerciseDto[] = Array.isArray(browseRes?.data) ? browseRes!.data : []

  const createMut = useMutation({
    mutationFn: (data: Record<string, unknown>) => api.post('/training/plans', data),
    onSuccess: onCreated,
    onError: (err: any) => alert(err?.response?.data?.errors?.[0]?.message || 'Failed to create plan'),
  })

  const addExercise = (ex: ExerciseDto) => {
    if (planExercises.some(e => e.exerciseId === ex.id)) return
    setPlanExercises([...planExercises, {
      exerciseId: ex.id, exerciseName: ex.name,
      muscleGroup: ex.primaryMuscleGroup, exerciseType: ex.exerciseType,
      sets: 3, reps: 10, weight: '', restSeconds: '60', trainerComment: '',
    }])
  }
  const removeExercise = (i: number) => setPlanExercises(planExercises.filter((_, idx) => idx !== i))
  const moveExercise = (i: number, d: -1 | 1) => {
    const ni = i + d; if (ni < 0 || ni >= planExercises.length) return
    const u = [...planExercises]; [u[i], u[ni]] = [u[ni], u[i]]; setPlanExercises(u)
  }
  const updateExField = (i: number, field: string, value: string | number) => {
    const u = [...planExercises]; u[i] = { ...u[i], [field]: value }; setPlanExercises(u)
  }

  const handleSubmit = () => {
    createMut.mutate({
      name: form.name,
      description: form.description || undefined,
      memberId: form.memberId || undefined,
      template: form.template, catalog: form.catalog,
      category: form.category || undefined,
      estimatedDurationMinutes: form.estimatedDurationMinutes ? parseInt(form.estimatedDurationMinutes) : undefined,
      difficultyLevel: form.difficultyLevel,
      exercises: planExercises.map((ex, i) => ({
        exerciseId: ex.exerciseId, sortOrder: i + 1, sets: ex.sets, reps: ex.reps,
        weight: ex.weight ? parseFloat(ex.weight) : undefined,
        restSeconds: ex.restSeconds ? parseInt(ex.restSeconds) : undefined,
        trainerComment: ex.trainerComment || undefined,
      })),
    })
  }

  const addedIds = new Set(planExercises.map(e => e.exerciseId))
  const muscleColor: Record<string, string> = {
    CHEST: '#ef4444', BACK: '#3b82f6', SHOULDERS: '#f59e0b', BICEPS: '#8b5cf6', TRICEPS: '#a855f7',
    QUADRICEPS: '#10b981', HAMSTRINGS: '#14b8a6', GLUTES: '#ec4899', ABS: '#f97316', CALVES: '#06b6d4',
    LATS: '#2563eb', TRAPS: '#6366f1', FULL_BODY: '#6b7280', CARDIO: '#22c55e',
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-5xl max-h-[92vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b">
          <div>
            <h2 className="text-lg font-bold">Create Training Plan</h2>
            <p className="text-xs text-gray-500">{step === 'details' ? 'Step 1: Plan details' : 'Step 2: Add exercises'}</p>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-2xl leading-none">&times;</button>
        </div>

        {step === 'details' && (
          <div className="flex-1 overflow-y-auto p-6">
            <div className="max-w-lg mx-auto space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Plan Name *</label>
                <input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} className="w-full border rounded-lg px-4 py-2.5 text-sm focus:ring-2 focus:ring-brand-500 focus:border-brand-500" placeholder="e.g., Full Body Strength" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <textarea value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} className="w-full border rounded-lg px-4 py-2.5 text-sm focus:ring-2 focus:ring-brand-500" rows={3} placeholder="What this plan focuses on..." />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Difficulty</label>
                  <select value={form.difficultyLevel} onChange={e => setForm({ ...form, difficultyLevel: e.target.value })} className="w-full border rounded-lg px-4 py-2.5 text-sm">
                    <option value="BEGINNER">Beginner</option><option value="INTERMEDIATE">Intermediate</option><option value="ADVANCED">Advanced</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
                  <input value={form.category} onChange={e => setForm({ ...form, category: e.target.value })} className="w-full border rounded-lg px-4 py-2.5 text-sm" placeholder="e.g., Strength, Cardio" />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Est. Duration (min)</label>
                  <input type="number" value={form.estimatedDurationMinutes} onChange={e => setForm({ ...form, estimatedDurationMinutes: e.target.value })} className="w-full border rounded-lg px-4 py-2.5 text-sm" placeholder="45" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Member ID</label>
                  <input value={form.memberId} onChange={e => setForm({ ...form, memberId: e.target.value })} className="w-full border rounded-lg px-4 py-2.5 text-sm" placeholder="Leave empty for template" />
                </div>
              </div>
              <div className="flex gap-6 pt-2">
                <label className="flex items-center gap-2 text-sm cursor-pointer">
                  <input type="checkbox" checked={form.template} onChange={e => setForm({ ...form, template: e.target.checked })} className="rounded text-brand-600" />
                  <span>Save as Template</span>
                </label>
                <label className="flex items-center gap-2 text-sm cursor-pointer">
                  <input type="checkbox" checked={form.catalog} onChange={e => setForm({ ...form, catalog: e.target.checked })} className="rounded text-brand-600" />
                  <span>Show in Catalog</span>
                </label>
              </div>
            </div>
          </div>
        )}

        {step === 'exercises' && (
          <div className="flex-1 flex overflow-hidden">
            {/* Left: Exercise browser */}
            <div className="w-1/2 border-r flex flex-col">
              <div className="p-4 border-b bg-gray-50 space-y-2">
                <input value={exSearch} onChange={e => setExSearch(e.target.value)}
                  placeholder="Search exercises..." className="w-full border rounded-lg px-3 py-2 text-sm" />
                <select value={exMuscleFilter} onChange={e => setExMuscleFilter(e.target.value)} className="w-full border rounded-lg px-3 py-2 text-sm">
                  <option value="">All muscle groups</option>
                  {MUSCLE_GROUPS.map(mg => <option key={mg} value={mg}>{mg.replace(/_/g, ' ')}</option>)}
                </select>
              </div>
              <div className="flex-1 overflow-y-auto p-3 space-y-2">
                {browsing ? <p className="text-gray-400 text-sm text-center py-4">Loading...</p> :
                 availableExercises.length === 0 ? <p className="text-gray-400 text-sm text-center py-4">No exercises found</p> :
                 availableExercises.map(ex => {
                  const added = addedIds.has(ex.id)
                  return (
                    <div key={ex.id}
                      onClick={() => !added && addExercise(ex)}
                      className={`rounded-lg border p-3 transition-all ${
                        added ? 'border-green-300 bg-green-50 opacity-60' : 'border-gray-200 hover:border-brand-300 hover:shadow-sm cursor-pointer'
                      }`}>
                      <div className="flex items-center justify-between">
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium text-gray-900 truncate">{ex.name}</p>
                          <div className="flex items-center gap-2 mt-1">
                            <span className="text-xs px-1.5 py-0.5 rounded text-white" style={{ backgroundColor: muscleColor[ex.primaryMuscleGroup] || '#6b7280' }}>
                              {ex.primaryMuscleGroup.replace(/_/g, ' ')}
                            </span>
                            <span className="text-xs text-gray-400">{ex.exerciseType.replace(/_/g, ' ')}</span>
                          </div>
                        </div>
                        {added ? (
                          <span className="text-xs text-green-600 font-medium ml-2">Added</span>
                        ) : (
                          <button className="ml-2 w-7 h-7 rounded-full bg-brand-100 text-brand-600 flex items-center justify-center text-lg hover:bg-brand-200">+</button>
                        )}
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>

            {/* Right: Plan exercises */}
            <div className="w-1/2 flex flex-col">
              <div className="p-4 border-b bg-gray-50">
                <h3 className="text-sm font-semibold text-gray-700">Plan Exercises ({planExercises.length})</h3>
              </div>
              <div className="flex-1 overflow-y-auto p-3">
                {planExercises.length === 0 ? (
                  <div className="flex flex-col items-center justify-center h-full text-gray-400">
                    <p className="text-4xl mb-3">+</p>
                    <p className="text-sm">Click exercises on the left to add them</p>
                  </div>
                ) : (
                  <div className="space-y-3">
                    {planExercises.map((ex, i) => (
                      <div key={i} className="bg-white border border-gray-200 rounded-lg p-3 shadow-sm">
                        <div className="flex items-start justify-between mb-2">
                          <div className="flex items-center gap-2">
                            <div className="flex flex-col">
                              <button onClick={() => moveExercise(i, -1)} disabled={i === 0}
                                className="text-gray-300 hover:text-gray-600 disabled:opacity-20 text-xs leading-tight">&#9650;</button>
                              <button onClick={() => moveExercise(i, 1)} disabled={i === planExercises.length - 1}
                                className="text-gray-300 hover:text-gray-600 disabled:opacity-20 text-xs leading-tight">&#9660;</button>
                            </div>
                            <div>
                              <span className="text-xs bg-gray-100 text-gray-500 w-5 h-5 rounded-full inline-flex items-center justify-center mr-1">{i + 1}</span>
                              <span className="text-sm font-medium">{ex.exerciseName}</span>
                              <span className="text-xs ml-2 px-1.5 py-0.5 rounded text-white" style={{ backgroundColor: muscleColor[ex.muscleGroup] || '#6b7280' }}>
                                {ex.muscleGroup.replace(/_/g, ' ')}
                              </span>
                            </div>
                          </div>
                          <button onClick={() => removeExercise(i)} className="text-gray-300 hover:text-red-500 text-lg leading-none">&times;</button>
                        </div>
                        <div className="grid grid-cols-4 gap-2">
                          <div>
                            <label className="text-xs text-gray-500 block">Sets</label>
                            <input type="number" min="1" value={ex.sets} onChange={e => updateExField(i, 'sets', parseInt(e.target.value) || 1)}
                              className="w-full border rounded px-2 py-1.5 text-sm text-center font-medium" />
                          </div>
                          <div>
                            <label className="text-xs text-gray-500 block">Reps</label>
                            <input type="number" min="1" value={ex.reps} onChange={e => updateExField(i, 'reps', parseInt(e.target.value) || 1)}
                              className="w-full border rounded px-2 py-1.5 text-sm text-center font-medium" />
                          </div>
                          <div>
                            <label className="text-xs text-gray-500 block">Weight (kg)</label>
                            <input value={ex.weight} onChange={e => updateExField(i, 'weight', e.target.value)}
                              className="w-full border rounded px-2 py-1.5 text-sm text-center" placeholder="—" />
                          </div>
                          <div>
                            <label className="text-xs text-gray-500 block">Rest (s)</label>
                            <input value={ex.restSeconds} onChange={e => updateExField(i, 'restSeconds', e.target.value)}
                              className="w-full border rounded px-2 py-1.5 text-sm text-center" placeholder="60" />
                          </div>
                        </div>
                        <input value={ex.trainerComment} onChange={e => updateExField(i, 'trainerComment', e.target.value)}
                          className="w-full border rounded px-2 py-1.5 text-xs mt-2 text-gray-600" placeholder="Trainer note (optional)..." />
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Footer */}
        <div className="flex items-center justify-between px-6 py-4 border-t bg-gray-50">
          <div className="text-sm text-gray-500">
            {step === 'exercises' && `${planExercises.length} exercise${planExercises.length !== 1 ? 's' : ''} added`}
          </div>
          <div className="flex gap-3">
            {step === 'exercises' && (
              <button onClick={() => setStep('details')} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">Back</button>
            )}
            <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">Cancel</button>
            {step === 'details' ? (
              <button onClick={() => setStep('exercises')} disabled={!form.name}
                className="bg-brand-600 text-white px-5 py-2 rounded-lg text-sm hover:bg-brand-700 disabled:opacity-50">
                Next: Add Exercises
              </button>
            ) : (
              <button onClick={handleSubmit} disabled={!form.name || createMut.isPending}
                className="bg-green-600 text-white px-5 py-2 rounded-lg text-sm hover:bg-green-700 disabled:opacity-50">
                {createMut.isPending ? 'Creating...' : `Create Plan (${planExercises.length} exercises)`}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

function PlanDetailModal({ planId, onClose, onPublish, onArchive }: { planId: string; onClose: () => void; onPublish: (id: string) => void; onArchive: (id: string) => void }) {
  const { data, isLoading } = useQuery({
    queryKey: ['training-plan', planId],
    queryFn: () => api.get<ApiResponse<TrainingPlanDto>>(`/training/plans/${planId}`).then(r => r.data.data),
  })

  if (isLoading || !data) return <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50"><div className="bg-white rounded-xl p-6">Loading...</div></div>
  const plan = data

  const statusInfo: Record<string, { label: string; desc: string; color: string }> = {
    DRAFT: { label: 'Draft', desc: 'This plan is not yet visible to the member. Publish it when ready.', color: 'bg-amber-50 border-amber-200 text-amber-800' },
    PUBLISHED: { label: 'Published', desc: 'The member can see and use this plan.', color: 'bg-green-50 border-green-200 text-green-800' },
    ARCHIVED: { label: 'Archived', desc: 'This plan is no longer active.', color: 'bg-gray-50 border-gray-200 text-gray-600' },
  }
  const si = statusInfo[plan.status] ?? statusInfo.DRAFT

  const muscleColor: Record<string, string> = {
    CHEST: '#ef4444', BACK: '#3b82f6', SHOULDERS: '#f59e0b', BICEPS: '#8b5cf6', TRICEPS: '#a855f7',
    QUADRICEPS: '#10b981', HAMSTRINGS: '#14b8a6', GLUTES: '#ec4899', ABS: '#f97316', CALVES: '#06b6d4',
    LATS: '#2563eb', TRAPS: '#6366f1', FULL_BODY: '#6b7280', CARDIO: '#22c55e',
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div className="px-6 py-4 border-b flex justify-between items-start">
          <div>
            <h2 className="text-lg font-bold">{plan.name}</h2>
            <p className="text-sm text-gray-500 mt-0.5">
              {plan.memberName && <>{plan.memberName} &middot; </>}
              {plan.trainerName && <>Trainer: {plan.trainerName} &middot; </>}
              {plan.exerciseCount} exercises
            </p>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-2xl leading-none">&times;</button>
        </div>

        <div className="p-6">
          {/* Status banner */}
          <div className={`rounded-lg border p-3 mb-4 text-sm ${si.color}`}>
            <span className="font-medium">{si.label}</span> &mdash; {si.desc}
          </div>

          {plan.description && <p className="text-sm text-gray-700 mb-4">{plan.description}</p>}

          <div className="flex flex-wrap gap-2 mb-5">
            {plan.category && <span className="text-xs bg-gray-100 text-gray-600 px-2.5 py-1 rounded-full">{plan.category}</span>}
            {plan.difficultyLevel && <span className="text-xs bg-blue-100 text-blue-700 px-2.5 py-1 rounded-full">{plan.difficultyLevel}</span>}
            {plan.estimatedDurationMinutes && <span className="text-xs bg-gray-100 text-gray-600 px-2.5 py-1 rounded-full">~{plan.estimatedDurationMinutes} min</span>}
            {plan.template && <span className="text-xs bg-purple-100 text-purple-700 px-2.5 py-1 rounded-full">Template</span>}
            {plan.catalog && <span className="text-xs bg-green-100 text-green-700 px-2.5 py-1 rounded-full">Catalog</span>}
          </div>

          <h3 className="text-sm font-semibold mb-3">Exercises</h3>
          <div className="space-y-2 mb-6">
            {plan.exercises?.map((ex, i) => (
              <div key={ex.id} className="bg-gray-50 rounded-lg p-3 flex items-center gap-3">
                <span className="text-xs bg-white text-gray-500 w-6 h-6 rounded-full flex items-center justify-center font-medium border">{i + 1}</span>
                <div className="flex-1">
                  <p className="text-sm font-medium">{ex.exerciseName}</p>
                  <p className="text-xs text-gray-500 mt-0.5">
                    <strong>{ex.sets}</strong> sets &times; <strong>{ex.reps}</strong> reps
                    {ex.weight ? <> @ <strong>{ex.weight}kg</strong></> : ''}
                    {ex.restSeconds ? <> &middot; {ex.restSeconds}s rest</> : ''}
                  </p>
                  {ex.trainerComment && <p className="text-xs text-brand-600 mt-1 italic">"{ex.trainerComment}"</p>}
                </div>
                {ex.primaryMuscleGroup && (
                  <span className="text-xs px-2 py-0.5 rounded text-white" style={{ backgroundColor: muscleColor[ex.primaryMuscleGroup] || '#6b7280' }}>
                    {ex.primaryMuscleGroup.replace(/_/g, ' ')}
                  </span>
                )}
              </div>
            ))}
            {(!plan.exercises || plan.exercises.length === 0) && (
              <p className="text-gray-400 text-sm text-center py-4">No exercises in this plan.</p>
            )}
          </div>
        </div>

        {/* Actions footer */}
        <div className="px-6 py-4 border-t bg-gray-50 flex justify-end gap-3">
          {plan.status === 'DRAFT' && (
            <button onClick={() => onPublish(plan.id)} className="bg-green-600 text-white px-5 py-2 rounded-lg text-sm hover:bg-green-700">
              Publish (make visible to member)
            </button>
          )}
          {plan.status === 'PUBLISHED' && (
            <button onClick={() => onArchive(plan.id)} className="bg-gray-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-gray-700">
              Archive
            </button>
          )}
          {plan.status === 'DRAFT' && (
            <button onClick={() => onArchive(plan.id)} className="text-gray-500 px-4 py-2 text-sm hover:text-gray-700">
              Discard
            </button>
          )}
        </div>
      </div>
    </div>
  )
}

// ===================== TEMPLATES TAB =====================

function TemplatesTab() {
  const qc = useQueryClient()
  const [showCreate, setShowCreate] = useState(false)

  const { data, isLoading } = useQuery({
    queryKey: ['training-templates'],
    queryFn: () => api.get<ApiResponse<TrainingPlanDto[]>>('/training/plans/templates?size=50').then(r => r.data),
  })
  const templates = Array.isArray(data?.data) ? data!.data : []

  return (
    <div>
      <div className="bg-purple-50 border border-purple-200 rounded-lg p-4 mb-6 text-sm text-purple-800">
        <strong>Templates</strong> are reusable plan blueprints. Create a template once, then assign copies to individual members from the <em>Training Plans</em> tab. The original template stays unchanged.
      </div>
      <div className="flex justify-end mb-4">
        <button onClick={() => setShowCreate(true)} className="bg-purple-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-purple-700">Create Template</button>
      </div>
      {isLoading ? <p className="text-gray-500">Loading...</p> : templates.length === 0 ? (
        <p className="text-gray-400 text-center py-8">No templates yet. Click "Create Template" to build your first reusable plan.</p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {templates.map(t => (
            <div key={t.id} className="bg-white rounded-lg shadow p-5 border-l-4 border-purple-500">
              <h3 className="font-semibold">{t.name}</h3>
              <p className="text-xs text-gray-500 mt-1">
                {t.exerciseCount} exercises
                {t.difficultyLevel && <> &middot; {t.difficultyLevel}</>}
                {t.category && <> &middot; {t.category}</>}
              </p>
              {t.description && <p className="text-sm text-gray-600 mt-2 line-clamp-2">{t.description}</p>}
              <p className="text-xs text-gray-400 mt-3">To assign to a member, go to Training Plans tab and click "Assign from Template".</p>
            </div>
          ))}
        </div>
      )}
      {showCreate && <CreatePlanModal onClose={() => setShowCreate(false)} onCreated={() => { qc.invalidateQueries({ queryKey: ['training-templates'] }); setShowCreate(false) }} defaultTemplate={true} />}
    </div>
  )
}

// ===================== CATALOG & GENERIC LIST =====================

function PlanListView({ queryKey, url, emptyMsg, label }: { queryKey: string; url: string; emptyMsg: string; label: string }) {
  const { data, isLoading } = useQuery({
    queryKey: [queryKey],
    queryFn: () => api.get<ApiResponse<TrainingPlanDto[]>>(url).then(r => r.data),
  })
  const plans = Array.isArray(data?.data) ? data!.data : []

  return (
    <div>
      <p className="text-sm text-gray-500 mb-4">{label} — reusable plans for members.</p>
      {isLoading ? <p className="text-gray-500">Loading...</p> : plans.length === 0 ? <p className="text-gray-400">{emptyMsg}</p> : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {plans.map(p => (
            <div key={p.id} className="bg-white rounded-lg shadow p-4">
              <h3 className="font-medium">{p.name}</h3>
              <p className="text-xs text-gray-500 mt-1">{p.exerciseCount} exercises{p.category && <> &middot; {p.category}</>}</p>
              {p.description && <p className="text-sm text-gray-600 mt-2 line-clamp-2">{p.description}</p>}
              {p.difficultyLevel && (
                <span className={`text-xs mt-2 inline-block px-2 py-0.5 rounded ${p.difficultyLevel === 'BEGINNER' ? 'bg-green-100 text-green-700' : p.difficultyLevel === 'INTERMEDIATE' ? 'bg-yellow-100 text-yellow-700' : 'bg-red-100 text-red-700'}`}>{p.difficultyLevel}</span>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

// ===================== GOALS =====================

function GoalsList() {
  const qc = useQueryClient()
  const [memberSearch, setMemberSearch] = useState('')
  const [selectedMember, setSelectedMember] = useState<MemberDto | null>(null)
  const [showCreate, setShowCreate] = useState(false)

  const { data: membersRes } = useQuery({
    queryKey: ['members-goal-search', memberSearch],
    queryFn: () => api.get<ApiResponse<MemberDto[]>>('/members', { params: { name: memberSearch, size: 5 } }).then(r => r.data),
    enabled: memberSearch.length >= 2 && !selectedMember,
  })

  const { data, isLoading } = useQuery({
    queryKey: ['training-goals', selectedMember?.id],
    queryFn: () => api.get<ApiResponse<TrainingGoalDto[]>>(`/training/goals/member/${selectedMember!.id}`).then(r => r.data),
    enabled: !!selectedMember,
  })

  const updateProgressMut = useMutation({
    mutationFn: ({ goalId, currentValue }: { goalId: string; currentValue: number }) => api.post(`/training/goals/${goalId}/progress`, { currentValue }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['training-goals'] }),
  })
  const abandonMut = useMutation({
    mutationFn: (goalId: string) => api.post(`/training/goals/${goalId}/abandon`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['training-goals'] }),
  })

  const goals = Array.isArray(data?.data) ? data!.data : []

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <div className="relative w-80">
          <input type="text" placeholder="Search member..." value={memberSearch}
            onChange={e => { setMemberSearch(e.target.value); if (selectedMember) setSelectedMember(null) }}
            className="border rounded-lg px-3 py-2 text-sm w-full" />
          {!selectedMember && (membersRes?.data ?? []).length > 0 && (
            <div className="absolute z-10 w-full bg-white border rounded shadow mt-1 max-h-40 overflow-y-auto">
              {(membersRes?.data ?? []).map(m => (
                <button key={m.id} onClick={() => { setSelectedMember(m); setMemberSearch(m.firstName + ' ' + m.lastName) }}
                  className="w-full text-left px-3 py-2 text-sm hover:bg-gray-50">{m.firstName} {m.lastName}</button>
              ))}
            </div>
          )}
          {selectedMember && <button onClick={() => { setSelectedMember(null); setMemberSearch('') }} className="text-xs text-gray-500 underline mt-1">Clear</button>}
        </div>
        <button onClick={() => setShowCreate(true)} className="bg-brand-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-brand-700">Create Goal</button>
      </div>

      {!selectedMember ? <p className="text-gray-400">Search a member to view their goals.</p> : isLoading ? <p className="text-gray-500">Loading...</p> : goals.length === 0 ? <p className="text-gray-400">No goals for this member.</p> : (
        <div className="space-y-3">
          {goals.map(g => (
            <div key={g.id} className="bg-white rounded-lg shadow p-4">
              <div className="flex items-center justify-between mb-2">
                <div>
                  <h3 className="font-medium">{g.title}</h3>
                  <p className="text-xs text-gray-500">{g.goalType.replace(/_/g, ' ')}{g.targetDate && <> &middot; Target: {g.targetDate}</>}</p>
                </div>
                <span className={`text-xs px-2 py-0.5 rounded ${g.status === 'ACTIVE' ? 'bg-blue-100 text-blue-700' : g.status === 'ACHIEVED' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>{g.status}</span>
              </div>
              {g.targetValue != null && (
                <div className="mt-2">
                  <div className="flex justify-between text-xs text-gray-500 mb-1"><span>{g.currentValue ?? 0} {g.unit}</span><span>{g.targetValue} {g.unit}</span></div>
                  <div className="w-full bg-gray-200 rounded-full h-2"><div className="bg-brand-600 h-2 rounded-full" style={{ width: `${Math.min(g.progressPercent ?? 0, 100)}%` }} /></div>
                </div>
              )}
              {g.status === 'ACTIVE' && (
                <div className="flex gap-2 mt-3">
                  <input type="number" placeholder="New value" className="border rounded px-2 py-1 text-xs w-24"
                    onKeyDown={e => { if (e.key === 'Enter') { const v = parseFloat((e.target as HTMLInputElement).value); if (!isNaN(v)) { updateProgressMut.mutate({ goalId: g.id, currentValue: v }); (e.target as HTMLInputElement).value = '' } } }} />
                  <button onClick={() => abandonMut.mutate(g.id)} className="text-xs text-red-500">Abandon</button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {showCreate && <CreateGoalModal onClose={() => setShowCreate(false)} onCreated={() => { qc.invalidateQueries({ queryKey: ['training-goals'] }); setShowCreate(false) }} defaultMemberId={selectedMember?.id} />}
    </div>
  )
}

function CreateGoalModal({ onClose, onCreated, defaultMemberId }: { onClose: () => void; onCreated: () => void; defaultMemberId?: string }) {
  const [form, setForm] = useState({ memberId: defaultMemberId ?? '', goalType: 'GENERAL_FITNESS' as GoalType, title: '', description: '', targetValue: '', currentValue: '', unit: '', targetDate: '' })
  const createMut = useMutation({ mutationFn: (d: Record<string, unknown>) => api.post('/training/goals', d), onSuccess: onCreated })

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
        <h2 className="text-lg font-bold mb-4">Create Training Goal</h2>
        <div className="space-y-3">
          <input placeholder="Member ID *" value={form.memberId} onChange={e => setForm({ ...form, memberId: e.target.value })} className="w-full border rounded px-3 py-2 text-sm" />
          <input placeholder="Goal title *" value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} className="w-full border rounded px-3 py-2 text-sm" />
          <select value={form.goalType} onChange={e => setForm({ ...form, goalType: e.target.value as GoalType })} className="w-full border rounded px-3 py-2 text-sm">
            {GOAL_TYPES.map(g => <option key={g} value={g}>{g.replace(/_/g, ' ')}</option>)}
          </select>
          <textarea placeholder="Description" value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} className="w-full border rounded px-3 py-2 text-sm" rows={2} />
          <div className="grid grid-cols-3 gap-3">
            <input type="number" placeholder="Target" value={form.targetValue} onChange={e => setForm({ ...form, targetValue: e.target.value })} className="border rounded px-3 py-2 text-sm" />
            <input type="number" placeholder="Current" value={form.currentValue} onChange={e => setForm({ ...form, currentValue: e.target.value })} className="border rounded px-3 py-2 text-sm" />
            <input placeholder="Unit" value={form.unit} onChange={e => setForm({ ...form, unit: e.target.value })} className="border rounded px-3 py-2 text-sm" />
          </div>
          <input type="date" value={form.targetDate} onChange={e => setForm({ ...form, targetDate: e.target.value })} className="w-full border rounded px-3 py-2 text-sm" />
        </div>
        <div className="flex justify-end gap-3 mt-6">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600">Cancel</button>
          <button onClick={() => createMut.mutate({ memberId: form.memberId, goalType: form.goalType, title: form.title, description: form.description || undefined, targetValue: form.targetValue ? parseFloat(form.targetValue) : undefined, currentValue: form.currentValue ? parseFloat(form.currentValue) : undefined, unit: form.unit || undefined, targetDate: form.targetDate || undefined })}
            disabled={!form.memberId || !form.title || createMut.isPending} className="bg-brand-600 text-white px-4 py-2 rounded-lg text-sm disabled:opacity-50">
            {createMut.isPending ? 'Creating...' : 'Create Goal'}
          </button>
        </div>
      </div>
    </div>
  )
}
