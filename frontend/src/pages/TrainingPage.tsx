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
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`pb-3 px-1 text-sm font-medium border-b-2 ${
                activeTab === tab.key
                  ? 'border-indigo-500 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {activeTab === 'exercises' && <ExerciseLibrary />}
      {activeTab === 'plans' && <TrainingPlans />}
      {activeTab === 'templates' && <TemplateList />}
      {activeTab === 'catalog' && <CatalogList />}
      {activeTab === 'goals' && <GoalsList />}
    </div>
  )
}

// ===================== EXERCISE LIBRARY =====================

function ExerciseLibrary() {
  const queryClient = useQueryClient()
  const [search, setSearch] = useState('')
  const [muscleFilter, setMuscleFilter] = useState<string>('')
  const [typeFilter, setTypeFilter] = useState<string>('')
  const [showCreate, setShowCreate] = useState(false)
  const [selectedExercise, setSelectedExercise] = useState<ExerciseDto | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['exercises', search, muscleFilter, typeFilter],
    queryFn: async () => {
      const params = new URLSearchParams()
      if (search) params.set('name', search)
      if (muscleFilter) params.set('muscleGroup', muscleFilter)
      if (typeFilter) params.set('exerciseType', typeFilter)
      params.set('size', '50')
      const res = await api.get<ApiResponse<ExerciseDto[]>>(`/training/exercises?${params}`)
      return res.data
    },
  })

  const createMutation = useMutation({
    mutationFn: (exercise: Record<string, unknown>) =>
      api.post('/training/exercises', exercise),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['exercises'] })
      setShowCreate(false)
    },
  })

  const exercises = data?.data ?? []

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <div className="flex gap-3 flex-1">
          <input
            type="text"
            placeholder="Search exercises..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="border rounded-lg px-3 py-2 text-sm w-64"
          />
          <select
            value={muscleFilter}
            onChange={(e) => setMuscleFilter(e.target.value)}
            className="border rounded-lg px-3 py-2 text-sm"
          >
            <option value="">All Muscle Groups</option>
            {MUSCLE_GROUPS.map((mg) => (
              <option key={mg} value={mg}>{mg.replace(/_/g, ' ')}</option>
            ))}
          </select>
          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            className="border rounded-lg px-3 py-2 text-sm"
          >
            <option value="">All Types</option>
            {EXERCISE_TYPES.map((t) => (
              <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>
            ))}
          </select>
        </div>
        <button
          onClick={() => setShowCreate(true)}
          className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-indigo-700"
        >
          Add Exercise
        </button>
      </div>

      {isLoading ? (
        <p className="text-gray-500">Loading exercises...</p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {exercises.map((ex) => (
            <div
              key={ex.id}
              onClick={() => setSelectedExercise(ex)}
              className="bg-white rounded-lg shadow p-4 cursor-pointer hover:shadow-md transition-shadow"
            >
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="font-medium text-gray-900">{ex.name}</h3>
                  <p className="text-xs text-gray-500 mt-1">
                    {ex.exerciseType.replace(/_/g, ' ')} &middot; {ex.primaryMuscleGroup.replace(/_/g, ' ')}
                  </p>
                </div>
                {ex.equipment && (
                  <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded">
                    {ex.equipment}
                  </span>
                )}
              </div>
              {ex.description && (
                <p className="text-sm text-gray-600 mt-2 line-clamp-2">{ex.description}</p>
              )}
              {ex.difficultyLevel && (
                <span className={`text-xs mt-2 inline-block px-2 py-0.5 rounded ${
                  ex.difficultyLevel === 'BEGINNER' ? 'bg-green-100 text-green-700' :
                  ex.difficultyLevel === 'INTERMEDIATE' ? 'bg-yellow-100 text-yellow-700' :
                  'bg-red-100 text-red-700'
                }`}>
                  {ex.difficultyLevel}
                </span>
              )}
            </div>
          ))}
        </div>
      )}

      {showCreate && (
        <CreateExerciseModal
          onClose={() => setShowCreate(false)}
          onSubmit={(data) => createMutation.mutate(data)}
          isLoading={createMutation.isPending}
        />
      )}

      {selectedExercise && (
        <ExerciseDetailModal
          exercise={selectedExercise}
          onClose={() => setSelectedExercise(null)}
        />
      )}
    </div>
  )
}

function CreateExerciseModal({
  onClose,
  onSubmit,
  isLoading,
}: {
  onClose: () => void
  onSubmit: (data: Record<string, unknown>) => void
  isLoading: boolean
}) {
  const [form, setForm] = useState({
    name: '',
    description: '',
    exerciseType: 'STRENGTH' as ExerciseType,
    primaryMuscleGroup: 'CHEST' as MuscleGroup,
    secondaryMuscleGroup: '' as string,
    equipment: '',
    videoUrl: '',
    executionTips: '',
    postureNotes: '',
    difficultyLevel: 'BEGINNER',
  })

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto p-6">
        <h2 className="text-lg font-bold mb-4">Add Exercise</h2>
        <div className="space-y-3">
          <input
            placeholder="Exercise name *"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
          />
          <textarea
            placeholder="Description"
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
            rows={2}
          />
          <div className="grid grid-cols-2 gap-3">
            <select
              value={form.exerciseType}
              onChange={(e) => setForm({ ...form, exerciseType: e.target.value as ExerciseType })}
              className="border rounded px-3 py-2 text-sm"
            >
              {EXERCISE_TYPES.map((t) => (
                <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>
              ))}
            </select>
            <select
              value={form.primaryMuscleGroup}
              onChange={(e) => setForm({ ...form, primaryMuscleGroup: e.target.value as MuscleGroup })}
              className="border rounded px-3 py-2 text-sm"
            >
              {MUSCLE_GROUPS.map((mg) => (
                <option key={mg} value={mg}>{mg.replace(/_/g, ' ')}</option>
              ))}
            </select>
          </div>
          <select
            value={form.secondaryMuscleGroup}
            onChange={(e) => setForm({ ...form, secondaryMuscleGroup: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
          >
            <option value="">No secondary muscle group</option>
            {MUSCLE_GROUPS.map((mg) => (
              <option key={mg} value={mg}>{mg.replace(/_/g, ' ')}</option>
            ))}
          </select>
          <input
            placeholder="Equipment (e.g., Barbell, Dumbbell)"
            value={form.equipment}
            onChange={(e) => setForm({ ...form, equipment: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
          />
          <input
            placeholder="Video URL"
            value={form.videoUrl}
            onChange={(e) => setForm({ ...form, videoUrl: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
          />
          <textarea
            placeholder="Execution tips"
            value={form.executionTips}
            onChange={(e) => setForm({ ...form, executionTips: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
            rows={2}
          />
          <textarea
            placeholder="Posture notes"
            value={form.postureNotes}
            onChange={(e) => setForm({ ...form, postureNotes: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
            rows={2}
          />
          <select
            value={form.difficultyLevel}
            onChange={(e) => setForm({ ...form, difficultyLevel: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
          >
            <option value="BEGINNER">Beginner</option>
            <option value="INTERMEDIATE">Intermediate</option>
            <option value="ADVANCED">Advanced</option>
          </select>
        </div>
        <div className="flex justify-end gap-3 mt-6">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">
            Cancel
          </button>
          <button
            onClick={() => onSubmit({
              ...form,
              secondaryMuscleGroup: form.secondaryMuscleGroup || undefined,
            })}
            disabled={!form.name || isLoading}
            className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-indigo-700 disabled:opacity-50"
          >
            {isLoading ? 'Creating...' : 'Create Exercise'}
          </button>
        </div>
      </div>
    </div>
  )
}

function ExerciseDetailModal({
  exercise,
  onClose,
}: {
  exercise: ExerciseDto
  onClose: () => void
}) {
  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto p-6">
        <div className="flex justify-between items-start mb-4">
          <h2 className="text-lg font-bold">{exercise.name}</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl">&times;</button>
        </div>
        <div className="space-y-3">
          <div className="flex gap-2">
            <span className="text-xs bg-indigo-100 text-indigo-700 px-2 py-0.5 rounded">
              {exercise.exerciseType.replace(/_/g, ' ')}
            </span>
            <span className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded">
              {exercise.primaryMuscleGroup.replace(/_/g, ' ')}
            </span>
            {exercise.secondaryMuscleGroup && (
              <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded">
                {exercise.secondaryMuscleGroup.replace(/_/g, ' ')}
              </span>
            )}
          </div>
          {exercise.description && <p className="text-sm text-gray-700">{exercise.description}</p>}
          {exercise.equipment && (
            <p className="text-sm"><span className="font-medium">Equipment:</span> {exercise.equipment}</p>
          )}
          {exercise.executionTips && (
            <div>
              <h4 className="text-sm font-medium text-gray-900">Execution Tips</h4>
              <p className="text-sm text-gray-600 mt-1">{exercise.executionTips}</p>
            </div>
          )}
          {exercise.postureNotes && (
            <div>
              <h4 className="text-sm font-medium text-gray-900">Posture Notes</h4>
              <p className="text-sm text-gray-600 mt-1">{exercise.postureNotes}</p>
            </div>
          )}
          {exercise.videoUrl && (
            <a
              href={exercise.videoUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-sm text-indigo-600 hover:underline block"
            >
              Watch Video
            </a>
          )}
        </div>
      </div>
    </div>
  )
}

// ===================== TRAINING PLANS =====================

function TrainingPlans() {
  const queryClient = useQueryClient()
  const [showCreate, setShowCreate] = useState(false)
  const [selectedPlan, setSelectedPlan] = useState<TrainingPlanDto | null>(null)
  const [memberIdFilter, setMemberIdFilter] = useState('')

  const { data, isLoading } = useQuery({
    queryKey: ['training-plans', memberIdFilter],
    queryFn: async () => {
      const url = memberIdFilter
        ? `/training/plans/member/${memberIdFilter}`
        : '/training/plans/templates?size=50'
      const res = await api.get<ApiResponse<TrainingPlanDto[]>>(url)
      return res.data
    },
  })

  const publishMutation = useMutation({
    mutationFn: (planId: string) => api.post(`/training/plans/${planId}/publish`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['training-plans'] })
      setSelectedPlan(null)
    },
  })

  const archiveMutation = useMutation({
    mutationFn: (planId: string) => api.post(`/training/plans/${planId}/archive`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['training-plans'] })
      setSelectedPlan(null)
    },
  })

  const plans = data?.data ?? []

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <input
          type="text"
          placeholder="Filter by member ID..."
          value={memberIdFilter}
          onChange={(e) => setMemberIdFilter(e.target.value)}
          className="border rounded-lg px-3 py-2 text-sm w-80"
        />
        <button
          onClick={() => setShowCreate(true)}
          className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-indigo-700"
        >
          Create Plan
        </button>
      </div>

      {isLoading ? (
        <p className="text-gray-500">Loading plans...</p>
      ) : plans.length === 0 ? (
        <p className="text-gray-500">No training plans found. Create one or enter a member ID to filter.</p>
      ) : (
        <div className="space-y-3">
          {plans.map((plan) => (
            <div
              key={plan.id}
              onClick={() => setSelectedPlan(plan)}
              className="bg-white rounded-lg shadow p-4 cursor-pointer hover:shadow-md transition-shadow"
            >
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="font-medium text-gray-900">{plan.name}</h3>
                  <p className="text-xs text-gray-500 mt-1">
                    {plan.exerciseCount} exercises
                    {plan.memberName && <> &middot; {plan.memberName}</>}
                    {plan.trainerName && <> &middot; Trainer: {plan.trainerName}</>}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <StatusBadge status={plan.status} />
                  {plan.template && (
                    <span className="text-xs bg-purple-100 text-purple-700 px-2 py-0.5 rounded">Template</span>
                  )}
                  {plan.catalog && (
                    <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded">Catalog</span>
                  )}
                </div>
              </div>
              {plan.description && (
                <p className="text-sm text-gray-600 mt-2 line-clamp-1">{plan.description}</p>
              )}
            </div>
          ))}
        </div>
      )}

      {showCreate && (
        <CreatePlanModal
          onClose={() => setShowCreate(false)}
          onCreated={() => {
            queryClient.invalidateQueries({ queryKey: ['training-plans'] })
            setShowCreate(false)
          }}
        />
      )}

      {selectedPlan && (
        <PlanDetailModal
          planId={selectedPlan.id}
          onClose={() => setSelectedPlan(null)}
          onPublish={(id) => publishMutation.mutate(id)}
          onArchive={(id) => archiveMutation.mutate(id)}
        />
      )}
    </div>
  )
}

function StatusBadge({ status }: { status: TrainingPlanStatus }) {
  const colors = {
    DRAFT: 'bg-yellow-100 text-yellow-700',
    PUBLISHED: 'bg-green-100 text-green-700',
    ARCHIVED: 'bg-gray-100 text-gray-600',
  }
  return (
    <span className={`text-xs px-2 py-0.5 rounded ${colors[status]}`}>
      {status}
    </span>
  )
}

function CreatePlanModal({
  onClose,
  onCreated,
}: {
  onClose: () => void
  onCreated: () => void
}) {
  const [form, setForm] = useState({
    name: '',
    description: '',
    memberId: '',
    template: false,
    catalog: false,
    category: '',
    estimatedDurationMinutes: '',
    difficultyLevel: 'BEGINNER',
  })
  const [exercises, setExercises] = useState<Array<{
    exerciseId: string
    exerciseName: string
    sets: number
    reps: number
    weight: string
    restSeconds: string
    trainerComment: string
  }>>([])
  const [exerciseSearch, setExerciseSearch] = useState('')

  const { data: searchResults } = useQuery({
    queryKey: ['exercise-search', exerciseSearch],
    queryFn: async () => {
      if (!exerciseSearch) return { data: [] }
      const res = await api.get<ApiResponse<ExerciseDto[]>>(
        `/training/exercises?name=${encodeURIComponent(exerciseSearch)}&size=10`
      )
      return res.data
    },
    enabled: exerciseSearch.length > 1,
  })

  const createMutation = useMutation({
    mutationFn: (data: Record<string, unknown>) => api.post('/training/plans', data),
    onSuccess: onCreated,
  })

  const handleSubmit = () => {
    createMutation.mutate({
      name: form.name,
      description: form.description || undefined,
      memberId: form.memberId || undefined,
      template: form.template,
      catalog: form.catalog,
      category: form.category || undefined,
      estimatedDurationMinutes: form.estimatedDurationMinutes ? parseInt(form.estimatedDurationMinutes) : undefined,
      difficultyLevel: form.difficultyLevel,
      exercises: exercises.map((ex, i) => ({
        exerciseId: ex.exerciseId,
        sortOrder: i,
        sets: ex.sets,
        reps: ex.reps,
        weight: ex.weight ? parseFloat(ex.weight) : undefined,
        restSeconds: ex.restSeconds ? parseInt(ex.restSeconds) : undefined,
        trainerComment: ex.trainerComment || undefined,
      })),
    })
  }

  const addExercise = (ex: ExerciseDto) => {
    setExercises([...exercises, {
      exerciseId: ex.id,
      exerciseName: ex.name,
      sets: 3,
      reps: 10,
      weight: '',
      restSeconds: '60',
      trainerComment: '',
    }])
    setExerciseSearch('')
  }

  const removeExercise = (index: number) => {
    setExercises(exercises.filter((_, i) => i !== index))
  }

  const moveExercise = (index: number, direction: -1 | 1) => {
    const newIndex = index + direction
    if (newIndex < 0 || newIndex >= exercises.length) return
    const updated = [...exercises]
    ;[updated[index], updated[newIndex]] = [updated[newIndex], updated[index]]
    setExercises(updated)
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto p-6">
        <h2 className="text-lg font-bold mb-4">Create Training Plan</h2>
        <div className="space-y-3">
          <input
            placeholder="Plan name *"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
          />
          <textarea
            placeholder="Description"
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
            rows={2}
          />
          <div className="grid grid-cols-2 gap-3">
            <input
              placeholder="Member ID (optional)"
              value={form.memberId}
              onChange={(e) => setForm({ ...form, memberId: e.target.value })}
              className="border rounded px-3 py-2 text-sm"
            />
            <input
              placeholder="Category"
              value={form.category}
              onChange={(e) => setForm({ ...form, category: e.target.value })}
              className="border rounded px-3 py-2 text-sm"
            />
          </div>
          <div className="grid grid-cols-3 gap-3">
            <select
              value={form.difficultyLevel}
              onChange={(e) => setForm({ ...form, difficultyLevel: e.target.value })}
              className="border rounded px-3 py-2 text-sm"
            >
              <option value="BEGINNER">Beginner</option>
              <option value="INTERMEDIATE">Intermediate</option>
              <option value="ADVANCED">Advanced</option>
            </select>
            <input
              type="number"
              placeholder="Duration (min)"
              value={form.estimatedDurationMinutes}
              onChange={(e) => setForm({ ...form, estimatedDurationMinutes: e.target.value })}
              className="border rounded px-3 py-2 text-sm"
            />
            <div className="flex items-center gap-4">
              <label className="flex items-center gap-1 text-sm">
                <input
                  type="checkbox"
                  checked={form.template}
                  onChange={(e) => setForm({ ...form, template: e.target.checked })}
                />
                Template
              </label>
              <label className="flex items-center gap-1 text-sm">
                <input
                  type="checkbox"
                  checked={form.catalog}
                  onChange={(e) => setForm({ ...form, catalog: e.target.checked })}
                />
                Catalog
              </label>
            </div>
          </div>

          <div className="border-t pt-4 mt-4">
            <h3 className="text-sm font-medium mb-2">Exercises</h3>
            <div className="relative mb-3">
              <input
                placeholder="Search exercises to add..."
                value={exerciseSearch}
                onChange={(e) => setExerciseSearch(e.target.value)}
                className="w-full border rounded px-3 py-2 text-sm"
              />
              {searchResults?.data && searchResults.data.length > 0 && exerciseSearch && (
                <div className="absolute z-10 w-full bg-white border rounded shadow-lg mt-1 max-h-48 overflow-y-auto">
                  {searchResults.data.map((ex) => (
                    <button
                      key={ex.id}
                      onClick={() => addExercise(ex)}
                      className="w-full text-left px-3 py-2 text-sm hover:bg-gray-50 flex justify-between"
                    >
                      <span>{ex.name}</span>
                      <span className="text-xs text-gray-400">{ex.primaryMuscleGroup.replace(/_/g, ' ')}</span>
                    </button>
                  ))}
                </div>
              )}
            </div>

            <div className="space-y-2">
              {exercises.map((ex, i) => (
                <div key={i} className="bg-gray-50 rounded p-3 flex items-center gap-3">
                  <div className="flex flex-col gap-1">
                    <button
                      onClick={() => moveExercise(i, -1)}
                      disabled={i === 0}
                      className="text-xs text-gray-400 hover:text-gray-600 disabled:opacity-30"
                    >
                      ▲
                    </button>
                    <button
                      onClick={() => moveExercise(i, 1)}
                      disabled={i === exercises.length - 1}
                      className="text-xs text-gray-400 hover:text-gray-600 disabled:opacity-30"
                    >
                      ▼
                    </button>
                  </div>
                  <div className="flex-1">
                    <p className="text-sm font-medium">{ex.exerciseName}</p>
                    <div className="flex gap-2 mt-1">
                      <input
                        type="number"
                        value={ex.sets}
                        onChange={(e) => {
                          const updated = [...exercises]
                          updated[i] = { ...updated[i], sets: parseInt(e.target.value) || 1 }
                          setExercises(updated)
                        }}
                        className="w-16 border rounded px-2 py-1 text-xs"
                        placeholder="Sets"
                      />
                      <span className="text-xs text-gray-400 self-center">x</span>
                      <input
                        type="number"
                        value={ex.reps}
                        onChange={(e) => {
                          const updated = [...exercises]
                          updated[i] = { ...updated[i], reps: parseInt(e.target.value) || 1 }
                          setExercises(updated)
                        }}
                        className="w-16 border rounded px-2 py-1 text-xs"
                        placeholder="Reps"
                      />
                      <input
                        value={ex.weight}
                        onChange={(e) => {
                          const updated = [...exercises]
                          updated[i] = { ...updated[i], weight: e.target.value }
                          setExercises(updated)
                        }}
                        className="w-20 border rounded px-2 py-1 text-xs"
                        placeholder="Weight (kg)"
                      />
                      <input
                        value={ex.restSeconds}
                        onChange={(e) => {
                          const updated = [...exercises]
                          updated[i] = { ...updated[i], restSeconds: e.target.value }
                          setExercises(updated)
                        }}
                        className="w-20 border rounded px-2 py-1 text-xs"
                        placeholder="Rest (s)"
                      />
                    </div>
                    <input
                      value={ex.trainerComment}
                      onChange={(e) => {
                        const updated = [...exercises]
                        updated[i] = { ...updated[i], trainerComment: e.target.value }
                        setExercises(updated)
                      }}
                      className="w-full border rounded px-2 py-1 text-xs mt-1"
                      placeholder="Trainer comment..."
                    />
                  </div>
                  <button
                    onClick={() => removeExercise(i)}
                    className="text-red-400 hover:text-red-600 text-lg"
                  >
                    &times;
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="flex justify-end gap-3 mt-6">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600">Cancel</button>
          <button
            onClick={handleSubmit}
            disabled={!form.name || createMutation.isPending}
            className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-indigo-700 disabled:opacity-50"
          >
            {createMutation.isPending ? 'Creating...' : 'Create Plan (Draft)'}
          </button>
        </div>
      </div>
    </div>
  )
}

function PlanDetailModal({
  planId,
  onClose,
  onPublish,
  onArchive,
}: {
  planId: string
  onClose: () => void
  onPublish: (id: string) => void
  onArchive: (id: string) => void
}) {
  const { data, isLoading } = useQuery({
    queryKey: ['training-plan', planId],
    queryFn: async () => {
      const res = await api.get<ApiResponse<TrainingPlanDto>>(`/training/plans/${planId}`)
      return res.data.data
    },
  })

  if (isLoading || !data) {
    return (
      <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
        <div className="bg-white rounded-xl p-6">Loading...</div>
      </div>
    )
  }

  const plan = data

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto p-6">
        <div className="flex justify-between items-start mb-4">
          <div>
            <h2 className="text-lg font-bold">{plan.name}</h2>
            <p className="text-sm text-gray-500">
              {plan.memberName && <>Member: {plan.memberName} &middot; </>}
              {plan.trainerName && <>Trainer: {plan.trainerName} &middot; </>}
              <StatusBadge status={plan.status} />
            </p>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl">&times;</button>
        </div>

        {plan.description && <p className="text-sm text-gray-700 mb-4">{plan.description}</p>}

        <div className="flex gap-2 mb-4">
          {plan.category && (
            <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded">{plan.category}</span>
          )}
          {plan.difficultyLevel && (
            <span className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded">{plan.difficultyLevel}</span>
          )}
          {plan.estimatedDurationMinutes && (
            <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded">
              ~{plan.estimatedDurationMinutes} min
            </span>
          )}
        </div>

        <h3 className="text-sm font-medium mb-2">Exercises ({plan.exercises?.length ?? 0})</h3>
        <div className="space-y-2 mb-4">
          {plan.exercises?.map((ex, i) => (
            <div key={ex.id} className="bg-gray-50 rounded p-3 flex items-center gap-3">
              <span className="text-xs text-gray-400 w-6">{i + 1}</span>
              <div className="flex-1">
                <p className="text-sm font-medium">{ex.exerciseName}</p>
                <p className="text-xs text-gray-500">
                  {ex.sets} sets x {ex.reps} reps
                  {ex.weight ? ` @ ${ex.weight} kg` : ''}
                  {ex.restSeconds ? ` | ${ex.restSeconds}s rest` : ''}
                </p>
                {ex.trainerComment && (
                  <p className="text-xs text-indigo-600 mt-1">{ex.trainerComment}</p>
                )}
              </div>
              {ex.primaryMuscleGroup && (
                <span className="text-xs bg-blue-50 text-blue-600 px-2 py-0.5 rounded">
                  {ex.primaryMuscleGroup.replace(/_/g, ' ')}
                </span>
              )}
            </div>
          ))}
        </div>

        <div className="flex justify-end gap-2 border-t pt-4">
          {plan.status === 'DRAFT' && (
            <button
              onClick={() => onPublish(plan.id)}
              className="bg-green-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-green-700"
            >
              Publish
            </button>
          )}
          {plan.status !== 'ARCHIVED' && (
            <button
              onClick={() => onArchive(plan.id)}
              className="bg-gray-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-gray-700"
            >
              Archive
            </button>
          )}
        </div>
      </div>
    </div>
  )
}

// ===================== TEMPLATE & CATALOG LISTS =====================

function TemplateList() {
  const { data, isLoading } = useQuery({
    queryKey: ['training-templates'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<TrainingPlanDto[]>>('/training/plans/templates?size=50')
      return res.data
    },
  })

  const templates = data?.data ?? []

  return (
    <div>
      <p className="text-sm text-gray-500 mb-4">Reusable templates that can be applied to any member.</p>
      {isLoading ? (
        <p className="text-gray-500">Loading templates...</p>
      ) : templates.length === 0 ? (
        <p className="text-gray-400">No templates yet. Create a plan with the "Template" option checked.</p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {templates.map((t) => (
            <div key={t.id} className="bg-white rounded-lg shadow p-4">
              <h3 className="font-medium">{t.name}</h3>
              <p className="text-xs text-gray-500 mt-1">
                {t.exerciseCount} exercises
                {t.category && <> &middot; {t.category}</>}
                {t.difficultyLevel && <> &middot; {t.difficultyLevel}</>}
              </p>
              {t.description && <p className="text-sm text-gray-600 mt-2 line-clamp-2">{t.description}</p>}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

function CatalogList() {
  const { data, isLoading } = useQuery({
    queryKey: ['training-catalog'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<TrainingPlanDto[]>>('/training/plans/catalog?size=50')
      return res.data
    },
  })

  const catalogPlans = data?.data ?? []

  return (
    <div>
      <p className="text-sm text-gray-500 mb-4">Published plans available for members to self-select.</p>
      {isLoading ? (
        <p className="text-gray-500">Loading catalog...</p>
      ) : catalogPlans.length === 0 ? (
        <p className="text-gray-400">No catalog plans yet. Create a plan with the "Catalog" option checked and publish it.</p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {catalogPlans.map((p) => (
            <div key={p.id} className="bg-white rounded-lg shadow p-4">
              <h3 className="font-medium">{p.name}</h3>
              <p className="text-xs text-gray-500 mt-1">
                {p.exerciseCount} exercises
                {p.estimatedDurationMinutes && <> &middot; ~{p.estimatedDurationMinutes} min</>}
              </p>
              {p.description && <p className="text-sm text-gray-600 mt-2 line-clamp-2">{p.description}</p>}
              {p.difficultyLevel && (
                <span className={`text-xs mt-2 inline-block px-2 py-0.5 rounded ${
                  p.difficultyLevel === 'BEGINNER' ? 'bg-green-100 text-green-700' :
                  p.difficultyLevel === 'INTERMEDIATE' ? 'bg-yellow-100 text-yellow-700' :
                  'bg-red-100 text-red-700'
                }`}>
                  {p.difficultyLevel}
                </span>
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
  const queryClient = useQueryClient()
  const [memberIdFilter, setMemberIdFilter] = useState('')
  const [showCreate, setShowCreate] = useState(false)

  const { data, isLoading } = useQuery({
    queryKey: ['training-goals', memberIdFilter],
    queryFn: async () => {
      if (!memberIdFilter) return { data: [] as TrainingGoalDto[] }
      const res = await api.get<ApiResponse<TrainingGoalDto[]>>(
        `/training/goals/member/${memberIdFilter}`
      )
      return res.data
    },
    enabled: !!memberIdFilter,
  })

  const updateProgressMutation = useMutation({
    mutationFn: ({ goalId, currentValue }: { goalId: string; currentValue: number }) =>
      api.post(`/training/goals/${goalId}/progress`, { currentValue }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['training-goals'] }),
  })

  const abandonMutation = useMutation({
    mutationFn: (goalId: string) => api.post(`/training/goals/${goalId}/abandon`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['training-goals'] }),
  })

  const goals = data?.data ?? []

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <input
          type="text"
          placeholder="Enter member ID to view goals..."
          value={memberIdFilter}
          onChange={(e) => setMemberIdFilter(e.target.value)}
          className="border rounded-lg px-3 py-2 text-sm w-80"
        />
        <button
          onClick={() => setShowCreate(true)}
          className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-indigo-700"
        >
          Create Goal
        </button>
      </div>

      {!memberIdFilter ? (
        <p className="text-gray-400">Enter a member ID to view their training goals.</p>
      ) : isLoading ? (
        <p className="text-gray-500">Loading goals...</p>
      ) : goals.length === 0 ? (
        <p className="text-gray-400">No goals found for this member.</p>
      ) : (
        <div className="space-y-3">
          {goals.map((goal) => (
            <div key={goal.id} className="bg-white rounded-lg shadow p-4">
              <div className="flex items-center justify-between mb-2">
                <div>
                  <h3 className="font-medium">{goal.title}</h3>
                  <p className="text-xs text-gray-500">
                    {goal.goalType.replace(/_/g, ' ')}
                    {goal.targetDate && <> &middot; Target: {goal.targetDate}</>}
                  </p>
                </div>
                <span className={`text-xs px-2 py-0.5 rounded ${
                  goal.status === 'ACTIVE' ? 'bg-blue-100 text-blue-700' :
                  goal.status === 'ACHIEVED' ? 'bg-green-100 text-green-700' :
                  'bg-gray-100 text-gray-600'
                }`}>
                  {goal.status}
                </span>
              </div>

              {goal.targetValue != null && (
                <div className="mt-2">
                  <div className="flex justify-between text-xs text-gray-500 mb-1">
                    <span>{goal.currentValue ?? 0} {goal.unit}</span>
                    <span>{goal.targetValue} {goal.unit}</span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div
                      className="bg-indigo-600 h-2 rounded-full transition-all"
                      style={{ width: `${Math.min(goal.progressPercent ?? 0, 100)}%` }}
                    />
                  </div>
                  <p className="text-xs text-gray-400 mt-1">
                    {(goal.progressPercent ?? 0).toFixed(1)}% complete
                  </p>
                </div>
              )}

              {goal.status === 'ACTIVE' && (
                <div className="flex gap-2 mt-3">
                  <input
                    type="number"
                    placeholder="New value"
                    className="border rounded px-2 py-1 text-xs w-24"
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        const value = parseFloat((e.target as HTMLInputElement).value)
                        if (!isNaN(value)) {
                          updateProgressMutation.mutate({ goalId: goal.id, currentValue: value })
                          ;(e.target as HTMLInputElement).value = ''
                        }
                      }
                    }}
                  />
                  <button
                    onClick={() => abandonMutation.mutate(goal.id)}
                    className="text-xs text-red-500 hover:text-red-700"
                  >
                    Abandon
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {showCreate && (
        <CreateGoalModal
          onClose={() => setShowCreate(false)}
          onCreated={() => {
            queryClient.invalidateQueries({ queryKey: ['training-goals'] })
            setShowCreate(false)
          }}
        />
      )}
    </div>
  )
}

function CreateGoalModal({
  onClose,
  onCreated,
}: {
  onClose: () => void
  onCreated: () => void
}) {
  const [form, setForm] = useState({
    memberId: '',
    goalType: 'GENERAL_FITNESS' as GoalType,
    title: '',
    description: '',
    targetValue: '',
    currentValue: '',
    unit: '',
    targetDate: '',
  })

  const createMutation = useMutation({
    mutationFn: (data: Record<string, unknown>) => api.post('/training/goals', data),
    onSuccess: onCreated,
  })

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
        <h2 className="text-lg font-bold mb-4">Create Training Goal</h2>
        <div className="space-y-3">
          <input
            placeholder="Member ID *"
            value={form.memberId}
            onChange={(e) => setForm({ ...form, memberId: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
          />
          <input
            placeholder="Goal title *"
            value={form.title}
            onChange={(e) => setForm({ ...form, title: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
          />
          <select
            value={form.goalType}
            onChange={(e) => setForm({ ...form, goalType: e.target.value as GoalType })}
            className="w-full border rounded px-3 py-2 text-sm"
          >
            {GOAL_TYPES.map((g) => (
              <option key={g} value={g}>{g.replace(/_/g, ' ')}</option>
            ))}
          </select>
          <textarea
            placeholder="Description"
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
            rows={2}
          />
          <div className="grid grid-cols-3 gap-3">
            <input
              type="number"
              placeholder="Target"
              value={form.targetValue}
              onChange={(e) => setForm({ ...form, targetValue: e.target.value })}
              className="border rounded px-3 py-2 text-sm"
            />
            <input
              type="number"
              placeholder="Current"
              value={form.currentValue}
              onChange={(e) => setForm({ ...form, currentValue: e.target.value })}
              className="border rounded px-3 py-2 text-sm"
            />
            <input
              placeholder="Unit (kg, reps...)"
              value={form.unit}
              onChange={(e) => setForm({ ...form, unit: e.target.value })}
              className="border rounded px-3 py-2 text-sm"
            />
          </div>
          <input
            type="date"
            value={form.targetDate}
            onChange={(e) => setForm({ ...form, targetDate: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
          />
        </div>
        <div className="flex justify-end gap-3 mt-6">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600">Cancel</button>
          <button
            onClick={() => createMutation.mutate({
              memberId: form.memberId,
              goalType: form.goalType,
              title: form.title,
              description: form.description || undefined,
              targetValue: form.targetValue ? parseFloat(form.targetValue) : undefined,
              currentValue: form.currentValue ? parseFloat(form.currentValue) : undefined,
              unit: form.unit || undefined,
              targetDate: form.targetDate || undefined,
            })}
            disabled={!form.memberId || !form.title || createMutation.isPending}
            className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-indigo-700 disabled:opacity-50"
          >
            {createMutation.isPending ? 'Creating...' : 'Create Goal'}
          </button>
        </div>
      </div>
    </div>
  )
}
