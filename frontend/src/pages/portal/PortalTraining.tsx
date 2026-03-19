import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import api from '../../api/client'
import type { ApiResponse, MemberDto, TrainingPlanDto, TrainingSessionDto, TrainingGoalDto } from '../../types'

export default function PortalTraining() {
  const [tab, setTab] = useState<'plans' | 'sessions' | 'goals'>('plans')
  const [selectedPlan, setSelectedPlan] = useState<TrainingPlanDto | null>(null)

  const { data: profileRes } = useQuery({
    queryKey: ['portal-profile'],
    queryFn: () => api.get<ApiResponse<MemberDto>>('/portal/profile').then(r => r.data),
  })
  const memberId = profileRes?.data?.id

  const { data: plansRes } = useQuery({
    queryKey: ['portal-plans', memberId],
    queryFn: () => api.get<ApiResponse<TrainingPlanDto[]>>(`/training/plans/member/${memberId}`).then(r => r.data),
    enabled: !!memberId,
  })
  const plans = plansRes?.data ?? []

  const { data: sessionsRes } = useQuery({
    queryKey: ['portal-sessions', memberId],
    queryFn: () => api.get<ApiResponse<TrainingSessionDto[]>>(`/training/sessions/member/${memberId}`).then(r => r.data),
    enabled: !!memberId && tab === 'sessions',
  })
  const sessions = sessionsRes?.data ?? []

  const { data: goalsRes } = useQuery({
    queryKey: ['portal-goals', memberId],
    queryFn: () => api.get<ApiResponse<TrainingGoalDto[]>>(`/training/goals/member/${memberId}`).then(r => r.data),
    enabled: !!memberId && tab === 'goals',
  })
  const goals = goalsRes?.data ?? []

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
            className={`pb-2 px-1 text-sm font-medium ${
              tab === t.key ? 'border-b-2 border-emerald-600 text-emerald-600' : 'text-gray-500'
            }`}>
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'plans' && (
        <div>
          {plans.length === 0 ? (
            <p className="text-gray-500">No training plans assigned yet.</p>
          ) : (
            <div className="space-y-4">
              {plans.map(p => (
                <div key={p.id} className="bg-white rounded-lg shadow p-5">
                  <div className="flex items-center justify-between mb-2">
                    <h3 className="text-lg font-semibold">{p.name}</h3>
                    <span className={`text-xs px-2 py-1 rounded ${
                      p.status === 'PUBLISHED' ? 'bg-green-100 text-green-700' :
                      p.status === 'DRAFT' ? 'bg-yellow-100 text-yellow-700' :
                      'bg-gray-100 text-gray-500'
                    }`}>
                      {p.status}
                    </span>
                  </div>
                  {p.description && <p className="text-sm text-gray-500 mb-2">{p.description}</p>}
                  <div className="flex gap-4 text-sm text-gray-600 mb-3">
                    {p.category && <span>Category: {p.category}</span>}
                    {p.estimatedDurationMinutes && <span>{p.estimatedDurationMinutes} min</span>}
                    {p.difficultyLevel && <span>{p.difficultyLevel}</span>}
                    <span>{p.exerciseCount} exercises</span>
                  </div>
                  <button
                    onClick={() => setSelectedPlan(selectedPlan?.id === p.id ? null : p)}
                    className="text-sm text-emerald-600 hover:text-emerald-800"
                  >
                    {selectedPlan?.id === p.id ? 'Hide Exercises' : 'View Exercises'}
                  </button>

                  {selectedPlan?.id === p.id && selectedPlan.exercises && (
                    <div className="mt-3 border-t pt-3 space-y-2">
                      {selectedPlan.exercises.map((ex, i) => (
                        <div key={ex.id} className="flex items-center gap-3 text-sm">
                          <span className="text-gray-400 w-6">{i + 1}.</span>
                          <div className="flex-1">
                            <span className="font-medium">{ex.exerciseName}</span>
                            {ex.primaryMuscleGroup && (
                              <span className="text-gray-400 ml-2">({ex.primaryMuscleGroup})</span>
                            )}
                          </div>
                          <span className="text-gray-600">
                            {ex.sets}x{ex.reps}
                            {ex.weight ? ` @ ${ex.weight}kg` : ''}
                          </span>
                          {ex.restSeconds && (
                            <span className="text-gray-400">{ex.restSeconds}s rest</span>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {tab === 'sessions' && (
        <div className="space-y-3">
          {sessions.length === 0 ? (
            <p className="text-gray-500">No training sessions recorded.</p>
          ) : (
            sessions.map(s => (
              <div key={s.id} className="bg-white rounded-lg shadow p-4">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="font-medium">{s.planName ?? 'Free Session'}</p>
                    <p className="text-sm text-gray-500">
                      {new Date(s.startedAt).toLocaleString()}
                      {s.durationMinutes ? ` (${s.durationMinutes} min)` : ''}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    {s.rating && (
                      <span className="text-sm text-yellow-500">
                        {'★'.repeat(s.rating)}{'☆'.repeat(5 - s.rating)}
                      </span>
                    )}
                    <span className={`text-xs px-2 py-1 rounded ${
                      s.finishedAt ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
                    }`}>
                      {s.finishedAt ? 'Completed' : 'In Progress'}
                    </span>
                  </div>
                </div>
                {s.notes && <p className="text-sm text-gray-500 mt-2">{s.notes}</p>}
              </div>
            ))
          )}
        </div>
      )}

      {tab === 'goals' && (
        <div className="space-y-4">
          {goals.length === 0 ? (
            <p className="text-gray-500">No training goals set.</p>
          ) : (
            goals.map(g => (
              <div key={g.id} className="bg-white rounded-lg shadow p-4">
                <div className="flex items-center justify-between mb-2">
                  <h3 className="font-semibold">{g.title}</h3>
                  <span className={`text-xs px-2 py-1 rounded ${
                    g.status === 'ACTIVE' ? 'bg-blue-100 text-blue-700' :
                    g.status === 'ACHIEVED' ? 'bg-green-100 text-green-700' :
                    'bg-gray-100 text-gray-500'
                  }`}>
                    {g.status}
                  </span>
                </div>
                {g.description && <p className="text-sm text-gray-500 mb-2">{g.description}</p>}
                {g.targetValue != null && (
                  <div className="mt-2">
                    <div className="flex justify-between text-xs text-gray-500 mb-1">
                      <span>{g.currentValue ?? 0} {g.unit}</span>
                      <span>{g.targetValue} {g.unit}</span>
                    </div>
                    <div className="w-full bg-gray-200 rounded-full h-2">
                      <div
                        className="bg-emerald-500 h-2 rounded-full"
                        style={{ width: `${Math.min((g.progressPercent ?? 0), 100)}%` }}
                      />
                    </div>
                  </div>
                )}
                {g.targetDate && (
                  <p className="text-xs text-gray-400 mt-2">Target: {g.targetDate}</p>
                )}
              </div>
            ))
          )}
        </div>
      )}
    </div>
  )
}
