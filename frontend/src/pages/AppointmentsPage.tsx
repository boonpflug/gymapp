import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import api from '../api/client'
import type {
  ApiResponse,
  PageMeta,
  AppointmentDto,
  AppointmentTypeDto,
  AppointmentStatus,
  AnamneseFormDto,
  AnamneseSubmissionDto,
  DayAgendaDto,
  QuestionType,
} from '../types'

type Tab = 'agenda' | 'appointments' | 'types' | 'anamnese'

export default function AppointmentsPage() {
  const { t } = useTranslation()
  const [activeTab, setActiveTab] = useState<Tab>('agenda')

  const tabs: { key: Tab; label: string }[] = [
    { key: 'agenda', label: t('appointments.agenda') },
    { key: 'appointments', label: t('appointments.appointments') },
    { key: 'types', label: t('appointments.types') },
    { key: 'anamnese', label: t('appointments.anamnese') },
  ]

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">{t('appointments.title')}</h1>
      <div className="border-b border-gray-200 mb-6">
        <nav className="flex space-x-8">
          {tabs.map((t) => (
            <button
              key={t.key}
              onClick={() => setActiveTab(t.key)}
              className={`py-3 px-1 border-b-2 text-sm font-medium ${
                activeTab === t.key
                  ? 'border-teal-500 text-teal-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {t.label}
            </button>
          ))}
        </nav>
      </div>
      {activeTab === 'agenda' && <AgendaTab />}
      {activeTab === 'appointments' && <AppointmentsTab />}
      {activeTab === 'types' && <TypesTab />}
      {activeTab === 'anamnese' && <AnamneseTab />}
    </div>
  )
}

// ── Status helpers ──────────────────────────────────────

const statusColors: Record<AppointmentStatus, string> = {
  SCHEDULED: 'bg-blue-100 text-blue-700',
  CONFIRMED: 'bg-indigo-100 text-indigo-700',
  IN_PROGRESS: 'bg-yellow-100 text-yellow-700',
  COMPLETED: 'bg-green-100 text-green-700',
  CANCELLED: 'bg-red-100 text-red-700',
  NO_SHOW: 'bg-gray-100 text-gray-500',
}

function StatusBadge({ status }: { status: AppointmentStatus }) {
  return (
    <span className={`text-xs px-2 py-0.5 rounded font-medium ${statusColors[status]}`}>
      {status.replace(/_/g, ' ')}
    </span>
  )
}

function formatTime(iso: string) {
  return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString([], { year: 'numeric', month: 'short', day: 'numeric' })
}

function todayString() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

// ── Agenda Tab ──────────────────────────────────────────

function AgendaTab() {
  const { t } = useTranslation()
  const [staffId, setStaffId] = useState('')
  const [date, setDate] = useState(todayString())
  const qc = useQueryClient()

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ['agenda', staffId, date],
    queryFn: () =>
      api
        .get<ApiResponse<DayAgendaDto>>(`/appointments/staff/${staffId}/agenda`, { params: { date } })
        .then((r) => r.data),
    enabled: !!staffId,
  })

  const agenda = data?.data

  const statusMutation = useMutation({
    mutationFn: ({ id, action, reason }: { id: string; action: string; reason?: string }) =>
      api.put(`/appointments/${id}/${action}`, reason ? { cancellationReason: reason } : {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['agenda'] }),
  })

  function handleAction(id: string, action: string) {
    if (action === 'cancel') {
      const reason = window.prompt('Cancellation reason:')
      if (reason === null) return
      statusMutation.mutate({ id, action, reason })
    } else {
      statusMutation.mutate({ id, action })
    }
  }

  const nextActions: Record<AppointmentStatus, { label: string; action: string; color: string }[]> = {
    SCHEDULED: [
      { label: t('appointments.confirmAction'), action: 'confirm', color: 'text-indigo-600 hover:text-indigo-800' },
      { label: t('appointments.cancelAction'), action: 'cancel', color: 'text-red-600 hover:text-red-800' },
    ],
    CONFIRMED: [
      { label: t('appointments.startAction'), action: 'start', color: 'text-yellow-600 hover:text-yellow-800' },
      { label: t('appointments.cancelAction'), action: 'cancel', color: 'text-red-600 hover:text-red-800' },
    ],
    IN_PROGRESS: [
      { label: t('appointments.completeAction'), action: 'complete', color: 'text-green-600 hover:text-green-800' },
      { label: t('appointments.noShowAction'), action: 'no-show', color: 'text-gray-600 hover:text-gray-800' },
    ],
    COMPLETED: [],
    CANCELLED: [],
    NO_SHOW: [],
  }

  return (
    <div>
      <div className="flex flex-wrap items-end gap-4 mb-6">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">{t('appointments.staffId')}</label>
          <input
            className="border rounded-lg px-3 py-2 text-sm w-64"
            placeholder={t('appointments.enterStaffId')}
            value={staffId}
            onChange={(e) => setStaffId(e.target.value)}
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">{t('common.date')}</label>
          <input
            type="date"
            className="border rounded-lg px-3 py-2 text-sm"
            value={date}
            onChange={(e) => setDate(e.target.value)}
          />
        </div>
        {isFetching && <span className="text-xs text-gray-400">{t('common.loading')}</span>}
      </div>

      {!staffId ? (
        <div className="bg-white rounded-lg shadow p-12 text-center">
          <svg className="w-12 h-12 mx-auto text-gray-300 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={1.2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          <p className="text-gray-400 text-sm">{t('appointments.enterStaffIdPrompt')}</p>
        </div>
      ) : isLoading ? (
        <p className="text-gray-500 text-sm">{t('appointments.loadingAgenda')}</p>
      ) : !agenda ? (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <p className="text-gray-400 text-sm">{t('appointments.noAgendaData')}</p>
        </div>
      ) : (
        <div>
          <div className="flex items-center gap-4 mb-4">
            <h3 className="text-sm font-semibold text-gray-900">{agenda.staffName}</h3>
            <span className="text-xs text-gray-500">
              {agenda.bookedSlots} / {agenda.totalSlots} {t('appointments.slotsBooked')}
            </span>
            <div className="flex-1 h-2 bg-gray-100 rounded-full overflow-hidden max-w-xs">
              <div
                className="h-full bg-teal-500 rounded-full transition-all"
                style={{ width: `${agenda.totalSlots > 0 ? (agenda.bookedSlots / agenda.totalSlots) * 100 : 0}%` }}
              />
            </div>
          </div>

          {agenda.appointments.length === 0 ? (
            <div className="bg-white rounded-lg shadow p-8 text-center">
              <p className="text-gray-400 text-sm">{t('appointments.noAppointmentsDay')}</p>
            </div>
          ) : (
            <div className="space-y-2">
              {[...agenda.appointments]
                .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime())
                .map((apt) => (
                  <div
                    key={apt.id}
                    className="bg-white rounded-lg shadow p-4 flex items-center gap-4 border-l-4"
                    style={{ borderLeftColor: apt.status === 'CANCELLED' ? '#ef4444' : '#14b8a6' }}
                  >
                    <div className="text-center min-w-[80px]">
                      <p className="text-sm font-semibold text-gray-900">{formatTime(apt.startTime)}</p>
                      <p className="text-xs text-gray-400">{formatTime(apt.endTime)}</p>
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900 truncate">{apt.memberName}</p>
                      <p className="text-xs text-gray-500">{apt.appointmentTypeName}</p>
                      {apt.notes && <p className="text-xs text-gray-400 mt-0.5 truncate">{apt.notes}</p>}
                    </div>
                    <StatusBadge status={apt.status} />
                    <div className="flex gap-2">
                      {nextActions[apt.status]?.map((a) => (
                        <button
                          key={a.action}
                          onClick={() => handleAction(apt.id, a.action)}
                          disabled={statusMutation.isPending}
                          className={`text-xs font-medium ${a.color}`}
                        >
                          {a.label}
                        </button>
                      ))}
                    </div>
                  </div>
                ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

// ── Appointments Tab ────────────────────────────────────

function AppointmentsTab() {
  const { t } = useTranslation()
  const [page, setPage] = useState(0)
  const [filterMode, setFilterMode] = useState<'staff' | 'member'>('staff')
  const [filterId, setFilterId] = useState('')
  const [statusFilter, setStatusFilter] = useState<AppointmentStatus | ''>('')
  const [showCreate, setShowCreate] = useState(false)
  const qc = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['appointments', filterMode, filterId, statusFilter, page],
    queryFn: () =>
      api
        .get<ApiResponse<AppointmentDto[]>>(`/appointments/${filterMode}/${filterId}`, {
          params: { page, size: 20, ...(statusFilter ? { status: statusFilter } : {}) },
        })
        .then((r) => r.data),
    enabled: !!filterId,
  })

  const appointments = data?.data ?? []
  const meta = data?.meta as PageMeta | undefined

  const statusMutation = useMutation({
    mutationFn: ({ id, action, reason }: { id: string; action: string; reason?: string }) =>
      api.put(`/appointments/${id}/${action}`, reason ? { cancellationReason: reason } : {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['appointments'] }),
  })

  function handleAction(id: string, action: string) {
    if (action === 'cancel') {
      const reason = window.prompt('Cancellation reason:')
      if (reason === null) return
      statusMutation.mutate({ id, action, reason })
    } else {
      statusMutation.mutate({ id, action })
    }
  }

  return (
    <div>
      <div className="flex flex-wrap items-end gap-4 mb-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">{t('appointments.filterBy')}</label>
          <select
            className="border rounded-lg px-3 py-2 text-sm"
            value={filterMode}
            onChange={(e) => { setFilterMode(e.target.value as 'staff' | 'member'); setFilterId(''); setPage(0) }}
          >
            <option value="staff">{t('appointments.staffFilter')}</option>
            <option value="member">{t('appointments.memberFilter')}</option>
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">{filterMode === 'staff' ? t('appointments.staffFilter') : t('appointments.memberFilter')} ID</label>
          <input
            className="border rounded-lg px-3 py-2 text-sm w-64"
            placeholder={t('appointments.enterId', { mode: filterMode })}
            value={filterId}
            onChange={(e) => { setFilterId(e.target.value); setPage(0) }}
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">{t('common.status')}</label>
          <select
            className="border rounded-lg px-3 py-2 text-sm"
            value={statusFilter}
            onChange={(e) => { setStatusFilter(e.target.value as AppointmentStatus | ''); setPage(0) }}
          >
            <option value="">{t('common.all')}</option>
            <option value="SCHEDULED">{t('appointments.scheduled')}</option>
            <option value="CONFIRMED">{t('appointments.confirmed')}</option>
            <option value="IN_PROGRESS">{t('appointments.inProgress')}</option>
            <option value="COMPLETED">{t('appointments.completed')}</option>
            <option value="CANCELLED">{t('appointments.cancelled')}</option>
            <option value="NO_SHOW">{t('appointments.noShow')}</option>
          </select>
        </div>
        <button
          onClick={() => setShowCreate(true)}
          className="bg-teal-600 text-white px-4 py-2 rounded-lg hover:bg-teal-700 text-sm ml-auto"
        >
          {t('appointments.newAppointment')}
        </button>
      </div>

      {!filterId ? (
        <div className="bg-white rounded-lg shadow p-12 text-center">
          <p className="text-gray-400 text-sm">{t('appointments.enterIdToView')}</p>
        </div>
      ) : isLoading ? (
        <p className="text-gray-500 text-sm">{t('common.loading')}</p>
      ) : appointments.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <p className="text-gray-400 text-sm">{t('appointments.noAppointmentsFound')}</p>
        </div>
      ) : (
        <>
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('appointments.dateTime')}</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('appointments.memberFilter')}</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('appointments.staffFilter')}</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('appointments.type')}</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('common.status')}</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('common.actions')}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {appointments.map((apt) => (
                  <tr key={apt.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm text-gray-900">
                      <div>{formatDate(apt.startTime)}</div>
                      <div className="text-xs text-gray-500">{formatTime(apt.startTime)} - {formatTime(apt.endTime)}</div>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-900">{apt.memberName}</td>
                    <td className="px-4 py-3 text-sm text-gray-600">{apt.staffName}</td>
                    <td className="px-4 py-3 text-sm text-gray-600">{apt.appointmentTypeName}</td>
                    <td className="px-4 py-3"><StatusBadge status={apt.status} /></td>
                    <td className="px-4 py-3">
                      <div className="flex gap-2">
                        {apt.status === 'SCHEDULED' && (
                          <>
                            <button onClick={() => handleAction(apt.id, 'confirm')} className="text-xs font-medium text-indigo-600 hover:text-indigo-800">{t('appointments.confirmAction')}</button>
                            <button onClick={() => handleAction(apt.id, 'cancel')} className="text-xs font-medium text-red-600 hover:text-red-800">{t('appointments.cancelAction')}</button>
                          </>
                        )}
                        {apt.status === 'CONFIRMED' && (
                          <>
                            <button onClick={() => handleAction(apt.id, 'start')} className="text-xs font-medium text-yellow-600 hover:text-yellow-800">{t('appointments.startAction')}</button>
                            <button onClick={() => handleAction(apt.id, 'cancel')} className="text-xs font-medium text-red-600 hover:text-red-800">{t('appointments.cancelAction')}</button>
                          </>
                        )}
                        {apt.status === 'IN_PROGRESS' && (
                          <>
                            <button onClick={() => handleAction(apt.id, 'complete')} className="text-xs font-medium text-green-600 hover:text-green-800">{t('appointments.completeAction')}</button>
                            <button onClick={() => handleAction(apt.id, 'no-show')} className="text-xs font-medium text-gray-600 hover:text-gray-800">{t('appointments.noShowAction')}</button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {meta && meta.totalPages > 1 && (
            <div className="flex items-center justify-between mt-4">
              <p className="text-sm text-gray-500">
                Page {meta.page + 1} of {meta.totalPages} ({meta.totalElements} total)
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-3 py-1 border rounded text-sm disabled:opacity-50"
                >
                  {t('common.previous')}
                </button>
                <button
                  onClick={() => setPage((p) => p + 1)}
                  disabled={page >= (meta.totalPages ?? 1) - 1}
                  className="px-3 py-1 border rounded text-sm disabled:opacity-50"
                >
                  {t('common.next')}
                </button>
              </div>
            </div>
          )}
        </>
      )}

      {showCreate && <CreateAppointmentModal onClose={() => setShowCreate(false)} />}
    </div>
  )
}

// ── Create Appointment Modal ────────────────────────────

function CreateAppointmentModal({ onClose }: { onClose: () => void }) {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const [form, setForm] = useState({
    memberId: '',
    staffId: '',
    appointmentTypeId: '',
    startTime: '',
    notes: '',
  })

  const { data: typesData } = useQuery({
    queryKey: ['appointment-types-all'],
    queryFn: () =>
      api.get<ApiResponse<AppointmentTypeDto[]>>('/appointments/types', { params: { size: 100 } }).then((r) => r.data),
  })
  const types = typesData?.data ?? []

  const createMutation = useMutation({
    mutationFn: (data: typeof form) =>
      api.post<ApiResponse<AppointmentDto>>('/appointments', {
        ...data,
        notes: data.notes || null,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['appointments'] })
      qc.invalidateQueries({ queryKey: ['agenda'] })
      onClose()
    },
  })

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 overflow-auto">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg p-6 m-4 max-h-[90vh] overflow-y-auto">
        <h2 className="text-lg font-semibold mb-4">{t('appointments.createAppointment')}</h2>
        <form
          onSubmit={(e) => {
            e.preventDefault()
            createMutation.mutate(form)
          }}
          className="space-y-4"
        >
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('appointments.memberIdRequired')}</label>
              <input
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.memberId}
                onChange={(e) => setForm({ ...form, memberId: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('appointments.staffIdRequired')}</label>
              <input
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.staffId}
                onChange={(e) => setForm({ ...form, staffId: e.target.value })}
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t('appointments.appointmentTypeRequired')}</label>
            <select
              className="w-full border rounded-lg px-3 py-2 text-sm"
              value={form.appointmentTypeId}
              onChange={(e) => setForm({ ...form, appointmentTypeId: e.target.value })}
              required
            >
              <option value="">{t('appointments.selectType')}</option>
              {types.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name} ({t.durationMinutes} min)
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t('appointments.startDateTime')}</label>
            <input
              type="datetime-local"
              className="w-full border rounded-lg px-3 py-2 text-sm"
              value={form.startTime}
              onChange={(e) => setForm({ ...form, startTime: e.target.value })}
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t('common.notes')}</label>
            <textarea
              className="w-full border rounded-lg px-3 py-2 text-sm"
              rows={3}
              value={form.notes}
              onChange={(e) => setForm({ ...form, notes: e.target.value })}
            />
          </div>

          {createMutation.isError && (
            <p className="text-red-600 text-sm">{t('appointments.failedCreateAppointment')}</p>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 border rounded-lg text-sm text-gray-600 hover:bg-gray-50">
              {t('common.cancel')}
            </button>
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="px-4 py-2 bg-teal-600 text-white rounded-lg text-sm hover:bg-teal-700 disabled:opacity-50"
            >
              {createMutation.isPending ? t('common.creating') : t('appointments.createAppointment')}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

// ── Types Tab ───────────────────────────────────────────

function TypesTab() {
  const { t } = useTranslation()
  const [showCreate, setShowCreate] = useState(false)
  const qc = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['appointment-types'],
    queryFn: () =>
      api.get<ApiResponse<AppointmentTypeDto[]>>('/appointments/types', { params: { size: 100 } }).then((r) => r.data),
  })

  const types = data?.data ?? []

  const deactivateMutation = useMutation({
    mutationFn: (id: string) => api.put(`/appointments/types/${id}/deactivate`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['appointment-types'] }),
  })

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <p className="text-sm text-gray-500">{types.length !== 1 ? t('appointments.typesCountPlural', { count: types.length }) : t('appointments.typesCount', { count: types.length })}</p>
        <button
          onClick={() => setShowCreate(true)}
          className="bg-teal-600 text-white px-4 py-2 rounded-lg hover:bg-teal-700 text-sm"
        >
          {t('appointments.newType')}
        </button>
      </div>

      {isLoading ? (
        <p className="text-gray-500 text-sm">{t('common.loading')}</p>
      ) : types.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <p className="text-gray-400 text-sm">{t('appointments.noTypesYet')}</p>
          <button onClick={() => setShowCreate(true)} className="mt-2 text-teal-600 text-sm font-medium hover:text-teal-700">
            {t('appointments.createFirstType')}
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {types.map((tp) => (
            <div key={tp.id} className="bg-white rounded-lg shadow p-4 flex flex-col">
              <div className="flex items-start justify-between mb-2">
                <div className="flex items-center gap-2">
                  {tp.color && (
                    <span className="w-3 h-3 rounded-full flex-shrink-0" style={{ backgroundColor: tp.color }} />
                  )}
                  <h4 className="text-sm font-semibold text-gray-900">{tp.name}</h4>
                </div>
                <span className={`text-xs px-2 py-0.5 rounded ${tp.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                  {tp.active ? t('common.active') : t('common.inactive')}
                </span>
              </div>
              {tp.description && <p className="text-xs text-gray-500 mb-2">{tp.description}</p>}
              <div className="flex flex-wrap gap-2 mb-3">
                <span className="bg-teal-100 text-teal-700 px-2 py-0.5 rounded text-xs font-medium">
                  {tp.durationMinutes} min
                </span>
                {tp.requiresTrainer && (
                  <span className="bg-indigo-100 text-indigo-700 px-2 py-0.5 rounded text-xs font-medium">
                    {t('appointments.trainerRequired')}
                  </span>
                )}
              </div>
              <div className="flex items-center justify-end text-xs mt-auto pt-2 border-t">
                {tp.active && (
                  <button
                    onClick={() => deactivateMutation.mutate(tp.id)}
                    disabled={deactivateMutation.isPending}
                    className="text-red-600 hover:text-red-800 font-medium"
                  >
                    {t('common.deactivate')}
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {showCreate && <CreateTypeModal onClose={() => setShowCreate(false)} />}
    </div>
  )
}

// ── Create Type Modal ───────────────────────────────────

function CreateTypeModal({ onClose }: { onClose: () => void }) {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const [form, setForm] = useState({
    name: '',
    description: '',
    durationMinutes: 30,
    color: '#14b8a6',
    requiresTrainer: true,
  })

  const createMutation = useMutation({
    mutationFn: (data: typeof form) =>
      api.post<ApiResponse<AppointmentTypeDto>>('/appointments/types', {
        ...data,
        description: data.description || null,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['appointment-types'] })
      onClose()
    },
  })

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 overflow-auto">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg p-6 m-4 max-h-[90vh] overflow-y-auto">
        <h2 className="text-lg font-semibold mb-4">{t('appointments.createType')}</h2>
        <form
          onSubmit={(e) => {
            e.preventDefault()
            createMutation.mutate(form)
          }}
          className="space-y-4"
        >
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t('loyalty.nameRequired')}</label>
            <input
              className="w-full border rounded-lg px-3 py-2 text-sm"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t('common.description')}</label>
            <input
              className="w-full border rounded-lg px-3 py-2 text-sm"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('appointments.durationMinutes')}</label>
              <input
                type="number"
                min={5}
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.durationMinutes}
                onChange={(e) => setForm({ ...form, durationMinutes: Number(e.target.value) })}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('loyalty.color')}</label>
              <div className="flex items-center gap-2">
                <input
                  type="color"
                  className="w-10 h-10 rounded border cursor-pointer"
                  value={form.color}
                  onChange={(e) => setForm({ ...form, color: e.target.value })}
                />
                <span className="text-xs text-gray-500">{form.color}</span>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="requiresTrainer"
              checked={form.requiresTrainer}
              onChange={(e) => setForm({ ...form, requiresTrainer: e.target.checked })}
              className="rounded border-gray-300 text-teal-600 focus:ring-teal-500"
            />
            <label htmlFor="requiresTrainer" className="text-sm text-gray-700">{t('appointments.requiresTrainer')}</label>
          </div>

          {createMutation.isError && (
            <p className="text-red-600 text-sm">{t('appointments.failedCreateType')}</p>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 border rounded-lg text-sm text-gray-600 hover:bg-gray-50">
              {t('common.cancel')}
            </button>
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="px-4 py-2 bg-teal-600 text-white rounded-lg text-sm hover:bg-teal-700 disabled:opacity-50"
            >
              {createMutation.isPending ? t('common.creating') : t('appointments.createType')}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

// ── Anamnese Tab ────────────────────────────────────────

type AnamneseSubTab = 'forms' | 'submissions'

function AnamneseTab() {
  const { t } = useTranslation()
  const [subTab, setSubTab] = useState<AnamneseSubTab>('forms')

  return (
    <div>
      <div className="flex gap-2 mb-4">
        <button
          onClick={() => setSubTab('forms')}
          className={`px-4 py-1.5 rounded-lg text-sm font-medium transition-colors ${
            subTab === 'forms' ? 'bg-teal-100 text-teal-700' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
          }`}
        >
          {t('appointments.forms')}
        </button>
        <button
          onClick={() => setSubTab('submissions')}
          className={`px-4 py-1.5 rounded-lg text-sm font-medium transition-colors ${
            subTab === 'submissions' ? 'bg-teal-100 text-teal-700' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
          }`}
        >
          {t('appointments.submissions')}
        </button>
      </div>
      {subTab === 'forms' && <AnamneseFormsPanel />}
      {subTab === 'submissions' && <AnamneseSubmissionsPanel />}
    </div>
  )
}

// ── Anamnese Forms Panel ────────────────────────────────

function AnamneseFormsPanel() {
  const { t } = useTranslation()
  const [showCreate, setShowCreate] = useState(false)
  const [viewForm, setViewForm] = useState<AnamneseFormDto | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['anamnese-forms'],
    queryFn: () =>
      api.get<ApiResponse<AnamneseFormDto[]>>('/appointments/anamnese/forms', { params: { size: 100 } }).then((r) => r.data),
  })

  const forms = data?.data ?? []

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <p className="text-sm text-gray-500">{forms.length !== 1 ? t('appointments.formsCountPlural', { count: forms.length }) : t('appointments.formsCount', { count: forms.length })}</p>
        <button
          onClick={() => setShowCreate(true)}
          className="bg-teal-600 text-white px-4 py-2 rounded-lg hover:bg-teal-700 text-sm"
        >
          {t('appointments.newForm')}
        </button>
      </div>

      {isLoading ? (
        <p className="text-gray-500 text-sm">{t('common.loading')}</p>
      ) : forms.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <p className="text-gray-400 text-sm">{t('appointments.noFormsYet')}</p>
          <button onClick={() => setShowCreate(true)} className="mt-2 text-teal-600 text-sm font-medium hover:text-teal-700">
            {t('appointments.createFirstForm')}
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {forms.map((f) => (
            <div key={f.id} className="bg-white rounded-lg shadow p-4 flex flex-col">
              <div className="flex items-start justify-between mb-2">
                <h4 className="text-sm font-semibold text-gray-900">{f.name}</h4>
                <span className={`text-xs px-2 py-0.5 rounded ${f.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                  {f.active ? t('common.active') : t('common.inactive')}
                </span>
              </div>
              {f.description && <p className="text-xs text-gray-500 mb-2">{f.description}</p>}
              <div className="flex flex-wrap gap-2 mb-3">
                <span className="bg-teal-100 text-teal-700 px-2 py-0.5 rounded text-xs font-medium">
                  v{f.version}
                </span>
                <span className="bg-gray-100 text-gray-600 px-2 py-0.5 rounded text-xs">
                  {f.questions?.length ?? 0} question{(f.questions?.length ?? 0) !== 1 ? 's' : ''}
                </span>
              </div>
              <div className="flex items-center justify-end text-xs mt-auto pt-2 border-t">
                <button
                  onClick={() => setViewForm(f)}
                  className="text-teal-600 hover:text-teal-800 font-medium"
                >
                  {t('appointments.viewDetails')}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {showCreate && <CreateFormModal onClose={() => setShowCreate(false)} />}
      {viewForm && <ViewFormModal form={viewForm} onClose={() => setViewForm(null)} />}
    </div>
  )
}

// ── View Form Modal ─────────────────────────────────────

function ViewFormModal({ form, onClose }: { form: AnamneseFormDto; onClose: () => void }) {
  const { t } = useTranslation()
  const questions = [...(form.questions ?? [])].sort((a, b) => a.sortOrder - b.sortOrder)
  const sections = Array.from(new Set(questions.map((q) => q.section || 'General')))

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 overflow-auto">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl p-6 m-4 max-h-[90vh] overflow-y-auto">
        <div className="flex items-start justify-between mb-4">
          <div>
            <h2 className="text-lg font-semibold">{form.name}</h2>
            {form.description && <p className="text-sm text-gray-500 mt-0.5">{form.description}</p>}
            <div className="flex gap-2 mt-2">
              <span className="bg-teal-100 text-teal-700 px-2 py-0.5 rounded text-xs font-medium">v{form.version}</span>
              <span className={`text-xs px-2 py-0.5 rounded ${form.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                {form.active ? t('common.active') : t('common.inactive')}
              </span>
            </div>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {questions.length === 0 ? (
          <p className="text-gray-400 text-sm">{t('appointments.noQuestions')}</p>
        ) : (
          <div className="space-y-4">
            {sections.map((section) => (
              <div key={section}>
                <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">{section}</h3>
                <div className="space-y-2">
                  {questions
                    .filter((q) => (q.section || 'General') === section)
                    .map((q, i) => (
                      <div key={q.id} className="bg-gray-50 rounded-lg p-3">
                        <div className="flex items-start justify-between">
                          <p className="text-sm text-gray-900">
                            <span className="text-gray-400 mr-2">{i + 1}.</span>
                            {q.questionText}
                            {q.required && <span className="text-red-500 ml-1">*</span>}
                          </p>
                          <span className="bg-gray-200 text-gray-600 px-2 py-0.5 rounded text-xs flex-shrink-0 ml-2">
                            {q.questionType.replace(/_/g, ' ')}
                          </span>
                        </div>
                        {q.options && (
                          <p className="text-xs text-gray-500 mt-1">Options: {q.options}</p>
                        )}
                      </div>
                    ))}
                </div>
              </div>
            ))}
          </div>
        )}

        <div className="flex justify-end mt-6">
          <button onClick={onClose} className="px-4 py-2 border rounded-lg text-sm text-gray-600 hover:bg-gray-50">
            {t('common.close')}
          </button>
        </div>
      </div>
    </div>
  )
}

// ── Create Form Modal ───────────────────────────────────

interface QuestionDraft {
  questionText: string
  questionType: QuestionType
  options: string
  required: boolean
  sortOrder: number
  section: string
}

function CreateFormModal({ onClose }: { onClose: () => void }) {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [questions, setQuestions] = useState<QuestionDraft[]>([])

  function addQuestion() {
    setQuestions([
      ...questions,
      { questionText: '', questionType: 'TEXT', options: '', required: false, sortOrder: questions.length + 1, section: '' },
    ])
  }

  function updateQuestion(idx: number, patch: Partial<QuestionDraft>) {
    setQuestions(questions.map((q, i) => (i === idx ? { ...q, ...patch } : q)))
  }

  function removeQuestion(idx: number) {
    setQuestions(questions.filter((_, i) => i !== idx).map((q, i) => ({ ...q, sortOrder: i + 1 })))
  }

  const createMutation = useMutation({
    mutationFn: () =>
      api.post<ApiResponse<AnamneseFormDto>>('/appointments/anamnese/forms', {
        name,
        description: description || null,
        questions: questions.map((q) => ({
          ...q,
          options: q.options || null,
          section: q.section || null,
        })),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['anamnese-forms'] })
      onClose()
    },
  })

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 overflow-auto">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl p-6 m-4 max-h-[90vh] overflow-y-auto">
        <h2 className="text-lg font-semibold mb-4">{t('appointments.createAnamneseForm')}</h2>
        <form
          onSubmit={(e) => {
            e.preventDefault()
            createMutation.mutate()
          }}
          className="space-y-4"
        >
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('appointments.formNameRequired')}</label>
              <input
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('common.description')}</label>
              <input
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>
          </div>

          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="text-sm font-medium text-gray-700">{t('appointments.questions')}</label>
              <button
                type="button"
                onClick={addQuestion}
                className="text-teal-600 text-sm font-medium hover:text-teal-700"
              >
                {t('appointments.addQuestion')}
              </button>
            </div>

            {questions.length === 0 ? (
              <div className="bg-gray-50 rounded-lg p-6 text-center">
                <p className="text-gray-400 text-sm">{t('appointments.noQuestionsAdded')}</p>
                <button type="button" onClick={addQuestion} className="mt-1 text-teal-600 text-sm font-medium hover:text-teal-700">
                  {t('appointments.addFirstQuestion')}
                </button>
              </div>
            ) : (
              <div className="space-y-3">
                {questions.map((q, idx) => (
                  <div key={idx} className="bg-gray-50 rounded-lg p-3 border border-gray-200">
                    <div className="flex items-start gap-2 mb-2">
                      <span className="text-xs text-gray-400 mt-2 font-medium">#{idx + 1}</span>
                      <div className="flex-1 space-y-2">
                        <input
                          className="w-full border rounded-lg px-3 py-1.5 text-sm"
                          placeholder={t('appointments.questionTextPlaceholder')}
                          value={q.questionText}
                          onChange={(e) => updateQuestion(idx, { questionText: e.target.value })}
                          required
                        />
                        <div className="grid grid-cols-3 gap-2">
                          <select
                            className="border rounded-lg px-2 py-1.5 text-sm"
                            value={q.questionType}
                            onChange={(e) => updateQuestion(idx, { questionType: e.target.value as QuestionType })}
                          >
                            <option value="TEXT">{t('appointments.questionTypes.TEXT')}</option>
                            <option value="NUMBER">{t('appointments.questionTypes.NUMBER')}</option>
                            <option value="SINGLE_CHOICE">{t('appointments.questionTypes.SINGLE_CHOICE')}</option>
                            <option value="MULTI_CHOICE">{t('appointments.questionTypes.MULTI_CHOICE')}</option>
                            <option value="SCALE">{t('appointments.questionTypes.SCALE')}</option>
                            <option value="DATE">{t('appointments.questionTypes.DATE')}</option>
                            <option value="BOOLEAN">{t('appointments.questionTypes.BOOLEAN')}</option>
                          </select>
                          <input
                            className="border rounded-lg px-2 py-1.5 text-sm"
                            placeholder={t('appointments.sectionPlaceholder')}
                            value={q.section}
                            onChange={(e) => updateQuestion(idx, { section: e.target.value })}
                          />
                          <div className="flex items-center gap-2">
                            <input
                              type="checkbox"
                              checked={q.required}
                              onChange={(e) => updateQuestion(idx, { required: e.target.checked })}
                              className="rounded border-gray-300 text-teal-600 focus:ring-teal-500"
                            />
                            <label className="text-xs text-gray-600">{t('common.required')}</label>
                          </div>
                        </div>
                        {(q.questionType === 'SINGLE_CHOICE' || q.questionType === 'MULTI_CHOICE') && (
                          <input
                            className="w-full border rounded-lg px-2 py-1.5 text-sm"
                            placeholder={t('appointments.optionsPlaceholder')}
                            value={q.options}
                            onChange={(e) => updateQuestion(idx, { options: e.target.value })}
                          />
                        )}
                      </div>
                      <button
                        type="button"
                        onClick={() => removeQuestion(idx)}
                        className="text-red-400 hover:text-red-600 mt-1"
                      >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={2}>
                          <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {createMutation.isError && (
            <p className="text-red-600 text-sm">{t('appointments.failedCreateForm')}</p>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 border rounded-lg text-sm text-gray-600 hover:bg-gray-50">
              {t('common.cancel')}
            </button>
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="px-4 py-2 bg-teal-600 text-white rounded-lg text-sm hover:bg-teal-700 disabled:opacity-50"
            >
              {createMutation.isPending ? t('common.creating') : t('appointments.createForm')}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

// ── Anamnese Submissions Panel ──────────────────────────

function AnamneseSubmissionsPanel() {
  const { t } = useTranslation()
  const [page, setPage] = useState(0)
  const [viewSubmission, setViewSubmission] = useState<AnamneseSubmissionDto | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['anamnese-submissions', page],
    queryFn: () =>
      api
        .get<ApiResponse<AnamneseSubmissionDto[]>>('/appointments/anamnese/submissions', { params: { page, size: 20 } })
        .then((r) => r.data),
  })

  const submissions = data?.data ?? []
  const meta = data?.meta as PageMeta | undefined

  return (
    <div>
      {isLoading ? (
        <p className="text-gray-500 text-sm">{t('common.loading')}</p>
      ) : submissions.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <p className="text-gray-400 text-sm">{t('appointments.noSubmissionsYet')}</p>
        </div>
      ) : (
        <>
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('appointments.memberFilter')}</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('appointments.forms')}</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('appointments.submittedBy')}</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('common.date')}</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('appointments.answers')}</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('common.actions')}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {submissions.map((s) => (
                  <tr key={s.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm text-gray-900">{s.memberName || s.memberId}</td>
                    <td className="px-4 py-3 text-sm text-gray-600">{s.formName || s.formId}</td>
                    <td className="px-4 py-3 text-sm text-gray-600">{s.submittedByName || s.submittedBy || '-'}</td>
                    <td className="px-4 py-3 text-sm text-gray-600">{formatDate(s.submittedAt)}</td>
                    <td className="px-4 py-3 text-sm text-gray-500">{s.answers?.length ?? 0}</td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => setViewSubmission(s)}
                        className="text-xs font-medium text-teal-600 hover:text-teal-800"
                      >
                        {t('common.view')}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {meta && meta.totalPages > 1 && (
            <div className="flex items-center justify-between mt-4">
              <p className="text-sm text-gray-500">
                Page {meta.page + 1} of {meta.totalPages} ({meta.totalElements} total)
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-3 py-1 border rounded text-sm disabled:opacity-50"
                >
                  {t('common.previous')}
                </button>
                <button
                  onClick={() => setPage((p) => p + 1)}
                  disabled={page >= (meta.totalPages ?? 1) - 1}
                  className="px-3 py-1 border rounded text-sm disabled:opacity-50"
                >
                  {t('common.next')}
                </button>
              </div>
            </div>
          )}
        </>
      )}

      {viewSubmission && <ViewSubmissionModal submission={viewSubmission} onClose={() => setViewSubmission(null)} />}
    </div>
  )
}

// ── View Submission Modal ───────────────────────────────

function ViewSubmissionModal({ submission, onClose }: { submission: AnamneseSubmissionDto; onClose: () => void }) {
  const { t } = useTranslation()
  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 overflow-auto">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl p-6 m-4 max-h-[90vh] overflow-y-auto">
        <div className="flex items-start justify-between mb-4">
          <div>
            <h2 className="text-lg font-semibold">{t('appointments.submissionDetails')}</h2>
            <p className="text-sm text-gray-500 mt-0.5">
              {submission.memberName || submission.memberId} &mdash; {submission.formName || submission.formId}
            </p>
            <p className="text-xs text-gray-400 mt-1">
              Submitted: {formatDate(submission.submittedAt)} at {formatTime(submission.submittedAt)}
              {submission.submittedByName && ` by ${submission.submittedByName}`}
            </p>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {submission.notes && (
          <div className="bg-yellow-50 rounded-lg p-3 mb-4">
            <p className="text-xs font-medium text-yellow-800">Notes</p>
            <p className="text-sm text-yellow-900 mt-0.5">{submission.notes}</p>
          </div>
        )}

        {(!submission.answers || submission.answers.length === 0) ? (
          <p className="text-gray-400 text-sm">{t('appointments.noAnswers')}</p>
        ) : (
          <div className="space-y-3">
            {submission.answers.map((a, i) => (
              <div key={a.id} className="bg-gray-50 rounded-lg p-3">
                <p className="text-xs font-medium text-gray-500 mb-1">
                  <span className="text-gray-400 mr-1">{i + 1}.</span>
                  {a.questionText || a.questionId}
                </p>
                <p className="text-sm text-gray-900">
                  {a.answerText ?? (a.answerNumber != null ? String(a.answerNumber) : (a.answerBoolean != null ? (a.answerBoolean ? 'Yes' : 'No') : '-'))}
                </p>
              </div>
            ))}
          </div>
        )}

        <div className="flex justify-end mt-6">
          <button onClick={onClose} className="px-4 py-2 border rounded-lg text-sm text-gray-600 hover:bg-gray-50">
            {t('common.close')}
          </button>
        </div>
      </div>
    </div>
  )
}
