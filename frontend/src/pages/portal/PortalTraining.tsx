import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import api from '../../api/client'
import type { ApiResponse, MemberDto, TrainingPlanDto, TrainingPlanExerciseDto, TrainingSessionDto, TrainingGoalDto } from '../../types'

const muscleColor: Record<string, string> = {
  CHEST: '#ef4444', BACK: '#3b82f6', SHOULDERS: '#f59e0b', BICEPS: '#8b5cf6', TRICEPS: '#a855f7',
  QUADRICEPS: '#10b981', HAMSTRINGS: '#14b8a6', GLUTES: '#ec4899', ABS: '#f97316', CALVES: '#06b6d4',
  LATS: '#2563eb', TRAPS: '#6366f1', OBLIQUES: '#f97316', LOWER_BACK: '#0ea5e9',
  FULL_BODY: '#6b7280', CARDIO: '#22c55e', FOREARMS: '#d946ef', HIP_FLEXORS: '#f43f5e',
  ADDUCTORS: '#e11d48', ABDUCTORS: '#be185d',
}

const muscleEmoji: Record<string, string> = {
  CHEST: '🫁', BACK: '🔙', SHOULDERS: '💪', BICEPS: '💪', TRICEPS: '💪',
  QUADRICEPS: '🦵', HAMSTRINGS: '🦵', GLUTES: '🍑', ABS: '🎯', CALVES: '🦶',
  LATS: '🔙', TRAPS: '🔺', FULL_BODY: '🏋️', CARDIO: '❤️', OBLIQUES: '🎯',
}

export default function PortalTraining() {
  const [tab, setTab] = useState<'plans' | 'sessions' | 'goals'>('plans')

  const { data: profileRes } = useQuery({
    queryKey: ['portal-profile'],
    queryFn: () => api.get<ApiResponse<MemberDto>>('/portal/profile').then(r => r.data),
  })
  const memberId = profileRes?.data?.id

  const tabs = [
    { key: 'plans' as const, label: 'My Plans' },
    { key: 'sessions' as const, label: 'Sessions' },
    { key: 'goals' as const, label: 'Goals' },
  ]

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Training</h1>
      <div className="flex space-x-4 mb-6 border-b">
        {tabs.map(t => (
          <button key={t.key} onClick={() => setTab(t.key)}
            className={`pb-2 px-1 text-sm font-medium ${tab === t.key ? 'border-b-2 border-emerald-600 text-emerald-600' : 'text-gray-500'}`}>
            {t.label}
          </button>
        ))}
      </div>
      {tab === 'plans' && <PlansTab memberId={memberId} />}
      {tab === 'sessions' && <SessionsTab memberId={memberId} />}
      {tab === 'goals' && <GoalsTab memberId={memberId} />}
    </div>
  )
}

// ===================== PLANS TAB =====================

function PlansTab({ memberId }: { memberId?: string }) {
  const [expandedPlanId, setExpandedPlanId] = useState<string | null>(null)
  const [exerciseDetail, setExerciseDetail] = useState<TrainingPlanExerciseDto | null>(null)

  const { data: plansRes } = useQuery({
    queryKey: ['portal-plans', memberId],
    queryFn: () => api.get<ApiResponse<TrainingPlanDto[]>>(`/training/plans/member/${memberId}`).then(r => r.data),
    enabled: !!memberId,
  })
  const plans = Array.isArray(plansRes?.data) ? plansRes!.data.filter(p => p.status === 'PUBLISHED') : []

  // Fetch full plan detail when expanded
  const { data: planDetailRes } = useQuery({
    queryKey: ['portal-plan-detail', expandedPlanId],
    queryFn: () => api.get<ApiResponse<TrainingPlanDto>>(`/training/plans/${expandedPlanId}`).then(r => r.data),
    enabled: !!expandedPlanId,
  })
  const planDetail = planDetailRes?.data

  if (!memberId) return <p className="text-gray-400">Loading...</p>

  return (
    <div>
      {plans.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-4xl mb-3">🏋️</p>
          <p className="text-gray-500">No training plans assigned yet.</p>
          <p className="text-gray-400 text-sm mt-1">Ask your trainer to create a plan for you.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {plans.map(p => {
            const isExpanded = expandedPlanId === p.id
            const detail = isExpanded ? planDetail : null
            const exercises = detail?.exercises ?? []

            return (
              <div key={p.id} className="bg-white rounded-xl shadow-sm border overflow-hidden">
                {/* Plan header */}
                <div className="p-5 cursor-pointer hover:bg-gray-50" onClick={() => setExpandedPlanId(isExpanded ? null : p.id)}>
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="text-lg font-semibold text-gray-900">{p.name}</h3>
                      <div className="flex items-center gap-3 mt-1 text-sm text-gray-500">
                        {p.category && <span className="bg-gray-100 px-2 py-0.5 rounded">{p.category}</span>}
                        {p.difficultyLevel && <span>{p.difficultyLevel}</span>}
                        {p.estimatedDurationMinutes && <span>~{p.estimatedDurationMinutes} min</span>}
                        <span>{p.exerciseCount} exercises</span>
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      {p.trainerName && <span className="text-xs text-gray-400">Trainer: {p.trainerName}</span>}
                      <span className={`text-lg transition-transform ${isExpanded ? 'rotate-180' : ''}`}>▾</span>
                    </div>
                  </div>
                  {p.description && <p className="text-sm text-gray-600 mt-2">{p.description}</p>}
                </div>

                {/* Exercises list */}
                {isExpanded && (
                  <div className="border-t bg-gray-50">
                    {exercises.length === 0 ? (
                      <p className="p-5 text-gray-400 text-sm text-center">Loading exercises...</p>
                    ) : (
                      <div className="divide-y divide-gray-200">
                        {exercises.map((ex, i) => (
                          <div key={ex.id} className="p-4 hover:bg-white transition-colors cursor-pointer"
                            onClick={() => setExerciseDetail(ex)}>
                            <div className="flex items-start gap-4">
                              {/* Thumbnail */}
                              <div className="flex-shrink-0">
                                <div className="relative">
                                  <div className="absolute -top-1 -left-1 w-6 h-6 rounded-full flex items-center justify-center text-white text-xs font-bold z-10"
                                    style={{ backgroundColor: muscleColor[ex.primaryMuscleGroup || ''] || '#6b7280' }}>
                                    {i + 1}
                                  </div>
                                  {ex.exerciseThumbnailUrl ? (
                                    <img src={ex.exerciseThumbnailUrl} alt={ex.exerciseName}
                                      className="w-24 h-16 object-cover rounded-lg border" />
                                  ) : (
                                    <div className="w-24 h-16 rounded-lg border bg-gray-100 flex items-center justify-center text-2xl"
                                      style={{ backgroundColor: (muscleColor[ex.primaryMuscleGroup || ''] || '#6b7280') + '15' }}>
                                      {muscleEmoji[ex.primaryMuscleGroup || ''] || '🏋️'}
                                    </div>
                                  )}
                                </div>
                              </div>

                              {/* Exercise info */}
                              <div className="flex-1 min-w-0">
                                <div className="flex items-center gap-2">
                                  <h4 className="font-semibold text-gray-900">{ex.exerciseName}</h4>
                                  <span className="text-xs px-2 py-0.5 rounded-full text-white"
                                    style={{ backgroundColor: muscleColor[ex.primaryMuscleGroup || ''] || '#6b7280' }}>
                                    {(ex.primaryMuscleGroup || '').replace(/_/g, ' ')}
                                  </span>
                                </div>

                                {/* Compact set/rep display */}
                                <div className="flex items-center gap-4 mt-2 text-sm">
                                  <div className="flex items-center gap-1.5">
                                    <span className="text-gray-500">Sets</span>
                                    <span className="font-bold text-gray-900 bg-gray-100 px-2 py-0.5 rounded">{ex.sets}</span>
                                  </div>
                                  <div className="flex items-center gap-1.5">
                                    <span className="text-gray-500">Reps</span>
                                    <span className="font-bold text-gray-900 bg-gray-100 px-2 py-0.5 rounded">{ex.reps}</span>
                                  </div>
                                  {ex.weight && (
                                    <div className="flex items-center gap-1.5">
                                      <span className="text-gray-500">Weight</span>
                                      <span className="font-bold text-gray-900 bg-gray-100 px-2 py-0.5 rounded">{ex.weight} kg</span>
                                    </div>
                                  )}
                                  {ex.restSeconds && (
                                    <div className="flex items-center gap-1.5">
                                      <span className="text-gray-500">Rest</span>
                                      <span className="font-bold text-gray-900 bg-gray-100 px-2 py-0.5 rounded">{ex.restSeconds}s</span>
                                    </div>
                                  )}
                                </div>

                                {ex.trainerComment && (
                                  <p className="mt-2 text-xs text-emerald-700 italic">"{ex.trainerComment}"</p>
                                )}
                              </div>

                              {/* Tap hint */}
                              <div className="flex-shrink-0 text-gray-300 self-center text-sm">›</div>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}

      {/* Exercise detail modal */}
      {exerciseDetail && <ExerciseDetailModal exercise={exerciseDetail} onClose={() => setExerciseDetail(null)} />}
    </div>
  )
}

function ExerciseDetailModal({ exercise, onClose }: { exercise: TrainingPlanExerciseDto; onClose: () => void }) {
  const { data: exRes } = useQuery({
    queryKey: ['exercise-detail', exercise.exerciseId],
    queryFn: () => api.get(`/training/exercises/${exercise.exerciseId}`).then(r => r.data),
  })
  const ex = exRes?.data as any

  // Extract YouTube video ID from URL
  const youtubeId = ex?.videoUrl?.match(/(?:v=|\/)([\w-]{11})/)?.[1]

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4" onClick={onClose}>
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
        {/* Video or thumbnail header */}
        <div className="relative bg-black rounded-t-2xl overflow-hidden">
          {youtubeId ? (
            <div className="aspect-video">
              <iframe
                src={`https://www.youtube.com/embed/${youtubeId}?rel=0`}
                className="w-full h-full"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
                title={exercise.exerciseName}
              />
            </div>
          ) : ex?.thumbnailUrl ? (
            <img src={ex.thumbnailUrl} alt={exercise.exerciseName} className="w-full h-48 object-cover" />
          ) : (
            <div className="h-32 flex items-center justify-center text-6xl"
              style={{ backgroundColor: (muscleColor[ex?.primaryMuscleGroup] || '#6b7280') + '20' }}>
              {muscleEmoji[ex?.primaryMuscleGroup] || '🏋️'}
            </div>
          )}
          <button onClick={onClose} className="absolute top-3 right-3 w-8 h-8 bg-black/50 rounded-full flex items-center justify-center text-white hover:bg-black/70 text-lg">&times;</button>
        </div>

        <div className="p-6">
          <h2 className="text-xl font-bold text-gray-900">{exercise.exerciseName}</h2>

          {/* Tags */}
          <div className="flex flex-wrap gap-2 mt-3">
            {ex?.primaryMuscleGroup && (
              <span className="text-xs px-3 py-1 rounded-full text-white font-medium"
                style={{ backgroundColor: muscleColor[ex.primaryMuscleGroup] || '#6b7280' }}>
                Primary: {ex.primaryMuscleGroup.replace(/_/g, ' ')}
              </span>
            )}
            {ex?.secondaryMuscleGroup && (
              <span className="text-xs px-3 py-1 rounded-full bg-gray-200 text-gray-700">
                Secondary: {ex.secondaryMuscleGroup.replace(/_/g, ' ')}
              </span>
            )}
            {ex?.exerciseType && (
              <span className="text-xs px-3 py-1 rounded-full bg-blue-100 text-blue-700">
                {ex.exerciseType.replace(/_/g, ' ')}
              </span>
            )}
            {ex?.difficultyLevel && (
              <span className={`text-xs px-3 py-1 rounded-full ${
                ex.difficultyLevel === 'BEGINNER' ? 'bg-green-100 text-green-700' :
                ex.difficultyLevel === 'INTERMEDIATE' ? 'bg-yellow-100 text-yellow-700' :
                'bg-red-100 text-red-700'
              }`}>{ex.difficultyLevel}</span>
            )}
          </div>

          {/* Equipment */}
          {ex?.equipment && (
            <div className="mt-4 bg-gray-50 rounded-lg px-4 py-3 flex items-center gap-2 text-sm">
              <span className="text-lg">🔧</span>
              <span><span className="font-medium text-gray-700">Equipment:</span> {ex.equipment}</span>
            </div>
          )}

          {/* Execution */}
          {ex?.description && (
            <div className="mt-5">
              <h3 className="text-sm font-semibold text-gray-800 uppercase tracking-wide mb-2 flex items-center gap-2">
                <span className="w-1 h-4 rounded-full bg-blue-500"></span> Execution
              </h3>
              <p className="text-sm text-gray-700 leading-relaxed pl-3">{ex.description}</p>
            </div>
          )}

          {/* Tips */}
          {ex?.postureNotes && (
            <div className="mt-4">
              <h3 className="text-sm font-semibold text-gray-800 uppercase tracking-wide mb-2 flex items-center gap-2">
                <span className="w-1 h-4 rounded-full bg-amber-500"></span> Tips & Form
              </h3>
              <p className="text-sm text-gray-700 leading-relaxed pl-3">{ex.postureNotes}</p>
            </div>
          )}

          {/* Your prescription */}
          <div className="mt-6 bg-gray-50 rounded-xl p-4">
            <h3 className="text-sm font-semibold text-gray-800 uppercase tracking-wide mb-3 flex items-center gap-2">
              <span className="w-1 h-4 rounded-full bg-emerald-500"></span> Your Prescription
            </h3>
            <div className="grid grid-cols-4 gap-3 text-center">
              <div className="bg-white rounded-lg p-3 shadow-sm border">
                <p className="text-2xl font-bold text-gray-900">{exercise.sets}</p>
                <p className="text-xs text-gray-500 uppercase">Sets</p>
              </div>
              <div className="bg-white rounded-lg p-3 shadow-sm border">
                <p className="text-2xl font-bold text-gray-900">{exercise.reps}</p>
                <p className="text-xs text-gray-500 uppercase">Reps</p>
              </div>
              <div className="bg-white rounded-lg p-3 shadow-sm border">
                <p className="text-2xl font-bold text-gray-900">{exercise.weight ?? '—'}</p>
                <p className="text-xs text-gray-500 uppercase">kg</p>
              </div>
              <div className="bg-white rounded-lg p-3 shadow-sm border">
                <p className="text-2xl font-bold text-gray-900">{exercise.restSeconds ?? '—'}</p>
                <p className="text-xs text-gray-500 uppercase">Rest (s)</p>
              </div>
            </div>
          </div>

          {exercise.trainerComment && (
            <div className="mt-4 bg-emerald-50 border border-emerald-200 rounded-xl p-4">
              <h3 className="text-sm font-semibold text-emerald-800 mb-1 flex items-center gap-2">
                <span className="text-base">💬</span> Trainer Note
              </h3>
              <p className="text-sm text-emerald-700 italic">"{exercise.trainerComment}"</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

// ===================== SESSIONS TAB =====================

function SessionsTab({ memberId }: { memberId?: string }) {
  const { data: sessionsRes } = useQuery({
    queryKey: ['portal-sessions', memberId],
    queryFn: () => api.get<ApiResponse<TrainingSessionDto[]>>(`/training/sessions/member/${memberId}`).then(r => r.data),
    enabled: !!memberId,
  })
  const sessions = Array.isArray(sessionsRes?.data) ? sessionsRes!.data : []

  return (
    <div className="space-y-3">
      {sessions.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-4xl mb-3">📋</p>
          <p className="text-gray-500">No training sessions recorded.</p>
        </div>
      ) : sessions.map(s => (
        <div key={s.id} className="bg-white rounded-lg shadow-sm border p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="font-medium">{s.planName ?? 'Free Session'}</p>
              <p className="text-sm text-gray-500">{new Date(s.startedAt).toLocaleString()}{s.durationMinutes ? ` (${s.durationMinutes} min)` : ''}</p>
            </div>
            <div className="flex items-center gap-2">
              {s.rating && <span className="text-sm text-yellow-500">{'★'.repeat(s.rating)}{'☆'.repeat(5 - s.rating)}</span>}
              <span className={`text-xs px-2 py-1 rounded ${s.finishedAt ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'}`}>
                {s.finishedAt ? 'Completed' : 'In Progress'}
              </span>
            </div>
          </div>
          {s.notes && <p className="text-sm text-gray-500 mt-2">{s.notes}</p>}
        </div>
      ))}
    </div>
  )
}

// ===================== GOALS TAB =====================

function GoalsTab({ memberId }: { memberId?: string }) {
  const { data: goalsRes } = useQuery({
    queryKey: ['portal-goals', memberId],
    queryFn: () => api.get<ApiResponse<TrainingGoalDto[]>>(`/training/goals/member/${memberId}`).then(r => r.data),
    enabled: !!memberId,
  })
  const goals = Array.isArray(goalsRes?.data) ? goalsRes!.data : []

  return (
    <div className="space-y-4">
      {goals.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-4xl mb-3">🎯</p>
          <p className="text-gray-500">No training goals set.</p>
          <p className="text-gray-400 text-sm mt-1">Ask your trainer to set goals for you.</p>
        </div>
      ) : goals.map(g => (
        <div key={g.id} className="bg-white rounded-lg shadow-sm border p-4">
          <div className="flex items-center justify-between mb-2">
            <h3 className="font-semibold">{g.title}</h3>
            <span className={`text-xs px-2 py-1 rounded ${
              g.status === 'ACTIVE' ? 'bg-blue-100 text-blue-700' :
              g.status === 'ACHIEVED' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
            }`}>{g.status}</span>
          </div>
          {g.description && <p className="text-sm text-gray-500 mb-2">{g.description}</p>}
          {g.targetValue != null && (
            <div className="mt-2">
              <div className="flex justify-between text-xs text-gray-500 mb-1">
                <span>{g.currentValue ?? 0} {g.unit}</span>
                <span>{g.targetValue} {g.unit}</span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-2.5">
                <div className="bg-emerald-500 h-2.5 rounded-full transition-all" style={{ width: `${Math.min((g.progressPercent ?? 0), 100)}%` }} />
              </div>
              <p className="text-xs text-gray-400 mt-1">{(g.progressPercent ?? 0).toFixed(0)}% complete</p>
            </div>
          )}
          {g.targetDate && <p className="text-xs text-gray-400 mt-2">Target: {g.targetDate}</p>}
        </div>
      ))}
    </div>
  )
}
