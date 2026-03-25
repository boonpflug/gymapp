import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import api from '../api/client'
import type {
  ApiResponse,
  MachineDto,
  MachineMaintenanceLogDto,
  MachineSensorSessionDto,
  StrengthMeasurementDto,
  MemberProgressDto,
  MachineStatus,
  MaintenanceType,
  MeasurementType,
} from '../types'

type Tab = 'inventory' | 'sensor' | 'progress' | 'maintenance'

export default function MachinesPage() {
  const { t } = useTranslation()
  const [activeTab, setActiveTab] = useState<Tab>('inventory')

  const tabs: { key: Tab; label: string }[] = [
    { key: 'inventory', label: t('machines.inventory') },
    { key: 'sensor', label: t('machines.sensorData') },
    { key: 'progress', label: t('machines.strengthProgress') },
    { key: 'maintenance', label: t('machines.maintenance') },
  ]

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">{t('machines.title')}</h1>
      <div className="border-b border-gray-200 mb-6">
        <nav className="flex space-x-8">
          {tabs.map((t) => (
            <button
              key={t.key}
              onClick={() => setActiveTab(t.key)}
              className={`py-3 px-1 border-b-2 text-sm font-medium ${
                activeTab === t.key
                  ? 'border-indigo-500 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {t.label}
            </button>
          ))}
        </nav>
      </div>
      {activeTab === 'inventory' && <InventoryTab />}
      {activeTab === 'sensor' && <SensorDataTab />}
      {activeTab === 'progress' && <StrengthProgressTab />}
      {activeTab === 'maintenance' && <MaintenanceTab />}
    </div>
  )
}

// ── Helpers ──────────────────────────────────────

const machineStatusColors: Record<MachineStatus, string> = {
  ACTIVE: 'bg-green-100 text-green-700',
  MAINTENANCE: 'bg-amber-100 text-amber-700',
  OUT_OF_ORDER: 'bg-red-100 text-red-700',
  DECOMMISSIONED: 'bg-gray-100 text-gray-500',
}

const maintenanceTypeColors: Record<MaintenanceType, string> = {
  ROUTINE: 'bg-blue-100 text-blue-700',
  REPAIR: 'bg-red-100 text-red-700',
  CALIBRATION: 'bg-purple-100 text-purple-700',
  FIRMWARE_UPDATE: 'bg-teal-100 text-teal-700',
}

const measurementTypeColors: Record<MeasurementType, string> = {
  ISOMETRIC: 'bg-blue-100 text-blue-700',
  DYNAMIC: 'bg-purple-100 text-purple-700',
  RANGE_OF_MOTION: 'bg-teal-100 text-teal-700',
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString([], { year: 'numeric', month: 'short', day: 'numeric' })
}

function formatDateTime(iso: string) {
  return new Date(iso).toLocaleString([], { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function StatusBadge({ status }: { status: MachineStatus }) {
  return (
    <span className={`text-xs px-2 py-0.5 rounded font-medium ${machineStatusColors[status]}`}>
      {status.replace(/_/g, ' ')}
    </span>
  )
}

function MaintenanceTypeBadge({ type }: { type: MaintenanceType }) {
  return (
    <span className={`text-xs px-2 py-0.5 rounded font-medium ${maintenanceTypeColors[type]}`}>
      {type.replace(/_/g, ' ')}
    </span>
  )
}

function MeasurementTypeBadge({ type }: { type: MeasurementType }) {
  return (
    <span className={`text-xs px-2 py-0.5 rounded font-medium ${measurementTypeColors[type]}`}>
      {type.replace(/_/g, ' ')}
    </span>
  )
}

// ── Inventory Tab ──────────────────────────────────────

function InventoryTab() {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const [statusFilter, setStatusFilter] = useState<MachineStatus | ''>('')
  const [facilityFilter, setFacilityFilter] = useState('')
  const [showRegister, setShowRegister] = useState(false)
  const [statusDropdown, setStatusDropdown] = useState<string | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['machines', statusFilter, facilityFilter],
    queryFn: () =>
      api.get<ApiResponse<MachineDto[]>>('/machines', {
        params: {
          ...(statusFilter && { status: statusFilter }),
          ...(facilityFilter && { facilityId: facilityFilter }),
        },
      }).then((r) => r.data),
  })

  const updateStatus = useMutation({
    mutationFn: ({ id, status }: { id: string; status: MachineStatus }) =>
      api.patch(`/machines/${id}/status`, { status }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['machines'] }),
  })

  const machines = data?.data ?? []

  return (
    <div>
      {/* Filters */}
      <div className="flex items-center gap-4 mb-6">
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as MachineStatus | '')}
          className="border rounded-lg px-3 py-2 text-sm"
        >
          <option value="">{t('machines.allStatuses')}</option>
          <option value="ACTIVE">{t('machines.active')}</option>
          <option value="MAINTENANCE">{t('machines.maintenanceStatus')}</option>
          <option value="OUT_OF_ORDER">{t('machines.outOfOrder')}</option>
          <option value="DECOMMISSIONED">{t('machines.decommissioned')}</option>
        </select>
        <input
          placeholder={t('machines.filterByFacility')}
          value={facilityFilter}
          onChange={(e) => setFacilityFilter(e.target.value)}
          className="border rounded-lg px-3 py-2 text-sm w-56"
        />
        <button
          onClick={() => setShowRegister(true)}
          className="ml-auto bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700"
        >
          + {t('machines.registerMachine')}
        </button>
      </div>

      {isLoading ? (
        <p className="text-gray-500 text-sm">{t('machines.loadingMachines')}</p>
      ) : machines.length === 0 ? (
        <p className="text-gray-400 text-sm">{t('machines.noMachines')}</p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {machines.map((m) => (
            <div key={m.id} className="bg-white rounded-xl border p-4 shadow-sm relative">
              <div className="flex items-start gap-3">
                {m.imageUrl ? (
                  <img src={m.imageUrl} alt={m.name} className="w-16 h-16 rounded-lg object-cover flex-shrink-0" />
                ) : (
                  <div className="w-16 h-16 rounded-lg bg-indigo-50 flex items-center justify-center flex-shrink-0">
                    <svg className="w-8 h-8 text-indigo-300" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={1.5}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                  </div>
                )}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="bg-indigo-100 text-indigo-700 text-xs font-bold px-2 py-0.5 rounded">{m.code}</span>
                    <StatusBadge status={m.status} />
                    {m.isComputerAssisted && (
                      <span className="bg-blue-100 text-blue-700 text-xs px-2 py-0.5 rounded font-medium">Biofeedback</span>
                    )}
                  </div>
                  <h3 className="text-sm font-semibold text-gray-900 truncate">{m.name}</h3>
                  {m.fullName && <p className="text-xs text-gray-500 truncate">{m.fullName}</p>}
                  <div className="flex flex-wrap gap-x-3 gap-y-1 mt-2 text-xs text-gray-500">
                    {m.category && <span>{t('machines.category')}: {m.category}</span>}
                    {m.series && <span>{t('machines.series')}: {m.series}</span>}
                    {m.serialNumber && <span>S/N: {m.serialNumber}</span>}
                    {m.facilityId && <span>{t('machines.facility')}: {m.facilityId.slice(0, 8)}...</span>}
                  </div>
                </div>
              </div>

              {/* Status dropdown */}
              <div className="mt-3 relative">
                <button
                  onClick={() => setStatusDropdown(statusDropdown === m.id ? null : m.id)}
                  className="text-xs text-indigo-600 hover:text-indigo-800 font-medium"
                >
                  {t('machines.changeStatus')}
                </button>
                {statusDropdown === m.id && (
                  <div className="absolute top-6 left-0 bg-white border rounded-lg shadow-lg z-10 py-1">
                    {(['ACTIVE', 'MAINTENANCE', 'OUT_OF_ORDER', 'DECOMMISSIONED'] as MachineStatus[]).map((s) => (
                      <button
                        key={s}
                        onClick={() => {
                          updateStatus.mutate({ id: m.id, status: s })
                          setStatusDropdown(null)
                        }}
                        className="block w-full text-left px-4 py-1.5 text-xs hover:bg-gray-50"
                      >
                        {s.replace(/_/g, ' ')}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {showRegister && <RegisterMachineModal onClose={() => setShowRegister(false)} />}
    </div>
  )
}

// ── Register Machine Modal ──────────────────────────────────

function RegisterMachineModal({ onClose }: { onClose: () => void }) {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const [form, setForm] = useState({
    code: '', name: '', fullName: '', series: '', category: '', facilityId: '',
    serialNumber: '', model: '', firmwareVersion: '', installationDate: '',
    isComputerAssisted: false, imageUrl: '', notes: '',
  })

  const create = useMutation({
    mutationFn: () => api.post('/machines', form),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['machines'] }); onClose() },
  })

  const set = (k: string, v: string | boolean) => setForm((f) => ({ ...f, [k]: v }))

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/30 backdrop-blur-sm" onClick={onClose} />
      <div className="relative bg-white rounded-xl shadow-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">{t('machines.registerMachine')}</h2>
        <div className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <input placeholder="Code *" value={form.code} onChange={(e) => set('code', e.target.value)} className="border rounded-lg px-3 py-2 text-sm" />
            <input placeholder="Name *" value={form.name} onChange={(e) => set('name', e.target.value)} className="border rounded-lg px-3 py-2 text-sm" />
          </div>
          <input placeholder="Full Name" value={form.fullName} onChange={(e) => set('fullName', e.target.value)} className="border rounded-lg px-3 py-2 text-sm w-full" />
          <div className="grid grid-cols-2 gap-3">
            <input placeholder="Series" value={form.series} onChange={(e) => set('series', e.target.value)} className="border rounded-lg px-3 py-2 text-sm" />
            <input placeholder="Category" value={form.category} onChange={(e) => set('category', e.target.value)} className="border rounded-lg px-3 py-2 text-sm" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <input placeholder="Facility ID" value={form.facilityId} onChange={(e) => set('facilityId', e.target.value)} className="border rounded-lg px-3 py-2 text-sm" />
            <input placeholder="Serial Number" value={form.serialNumber} onChange={(e) => set('serialNumber', e.target.value)} className="border rounded-lg px-3 py-2 text-sm" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <input placeholder="Model" value={form.model} onChange={(e) => set('model', e.target.value)} className="border rounded-lg px-3 py-2 text-sm" />
            <input placeholder="Firmware Version" value={form.firmwareVersion} onChange={(e) => set('firmwareVersion', e.target.value)} className="border rounded-lg px-3 py-2 text-sm" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs text-gray-500 mb-1">Installation Date</label>
              <input type="date" value={form.installationDate} onChange={(e) => set('installationDate', e.target.value)} className="border rounded-lg px-3 py-2 text-sm w-full" />
            </div>
            <div className="flex items-center gap-2 pt-5">
              <input type="checkbox" checked={form.isComputerAssisted} onChange={(e) => set('isComputerAssisted', e.target.checked)} className="rounded" />
              <label className="text-sm text-gray-700">{t('machines.computerAssisted')}</label>
            </div>
          </div>
          <input placeholder="Image URL" value={form.imageUrl} onChange={(e) => set('imageUrl', e.target.value)} className="border rounded-lg px-3 py-2 text-sm w-full" />
          <textarea placeholder="Notes" value={form.notes} onChange={(e) => set('notes', e.target.value)} rows={2} className="border rounded-lg px-3 py-2 text-sm w-full" />
        </div>
        <div className="flex justify-end gap-3 mt-5">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">{t('common.cancel')}</button>
          <button onClick={() => create.mutate()} disabled={!form.code || !form.name} className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700 disabled:opacity-50">
            Register
          </button>
        </div>
      </div>
    </div>
  )
}

// ── Sensor Data Tab ──────────────────────────────────────

function SensorDataTab() {
  const { t } = useTranslation()
  const [memberFilter, setMemberFilter] = useState('')
  const [machineFilter, setMachineFilter] = useState('')
  const [showRecord, setShowRecord] = useState(false)
  const [viewSession, setViewSession] = useState<MachineSensorSessionDto | null>(null)

  const { data: machinesData } = useQuery({
    queryKey: ['machines'],
    queryFn: () => api.get<ApiResponse<MachineDto[]>>('/machines').then((r) => r.data),
  })

  const { data, isLoading } = useQuery({
    queryKey: ['sensor-sessions', memberFilter, machineFilter],
    queryFn: () =>
      api.get<ApiResponse<MachineSensorSessionDto[]>>('/machines/sensor-sessions', {
        params: {
          ...(memberFilter && { memberId: memberFilter }),
          ...(machineFilter && { machineId: machineFilter }),
        },
      }).then((r) => r.data),
  })

  const machines = machinesData?.data ?? []
  const sessions = data?.data ?? []

  return (
    <div>
      <div className="flex items-center gap-4 mb-6">
        <input
          placeholder={t('machines.memberIdFilter')}
          value={memberFilter}
          onChange={(e) => setMemberFilter(e.target.value)}
          className="border rounded-lg px-3 py-2 text-sm w-56"
        />
        <select
          value={machineFilter}
          onChange={(e) => setMachineFilter(e.target.value)}
          className="border rounded-lg px-3 py-2 text-sm"
        >
          <option value="">{t('machines.allMachines')}</option>
          {machines.map((m) => (
            <option key={m.id} value={m.id}>{m.code} - {m.name}</option>
          ))}
        </select>
        <button
          onClick={() => setShowRecord(true)}
          className="ml-auto bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700"
        >
          {t('machines.recordSession')}
        </button>
      </div>

      {isLoading ? (
        <p className="text-gray-500 text-sm">{t('machines.loadingSessions')}</p>
      ) : sessions.length === 0 ? (
        <p className="text-gray-400 text-sm">{t('machines.noSessions')}</p>
      ) : (
        <div className="bg-white rounded-xl border overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-gray-600">
              <tr>
                <th className="text-left px-4 py-3 font-medium">{t('machines.machine')}</th>
                <th className="text-left px-4 py-3 font-medium">{t('machines.member')}</th>
                <th className="text-left px-4 py-3 font-medium">{t('machines.started')}</th>
                <th className="text-left px-4 py-3 font-medium">{t('machines.duration')}</th>
                <th className="text-left px-4 py-3 font-medium">{t('common.actions')}</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {sessions.map((s) => (
                <tr key={s.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3">
                    <span className="bg-indigo-100 text-indigo-700 text-xs font-bold px-1.5 py-0.5 rounded mr-1">{s.machineCode}</span>
                    {s.machineName}
                  </td>
                  <td className="px-4 py-3 text-gray-600">{s.memberName || s.memberId.slice(0, 8)}</td>
                  <td className="px-4 py-3 text-gray-600">{formatDateTime(s.startedAt)}</td>
                  <td className="px-4 py-3 text-gray-600">
                    {s.durationSeconds ? `${Math.floor(s.durationSeconds / 60)}m ${s.durationSeconds % 60}s` : '-'}
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => setViewSession(s)}
                      className="text-indigo-600 hover:text-indigo-800 text-xs font-medium"
                    >
                      {t('machines.viewData')}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showRecord && <RecordSessionModal machines={machines} onClose={() => setShowRecord(false)} />}

      {viewSession && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="absolute inset-0 bg-black/30 backdrop-blur-sm" onClick={() => setViewSession(null)} />
          <div className="relative bg-white rounded-xl shadow-2xl w-full max-w-lg max-h-[80vh] overflow-y-auto p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-2">{t('machines.sessionDetails')}</h2>
            <p className="text-sm text-gray-500 mb-1">{viewSession.machineCode} - {viewSession.machineName}</p>
            <p className="text-sm text-gray-500 mb-4">Member: {viewSession.memberName || viewSession.memberId}</p>
            <div className="bg-gray-50 rounded-lg p-4">
              <h3 className="text-xs font-medium text-gray-600 mb-2">{t('machines.rawSensorData')}</h3>
              <pre className="text-xs text-gray-800 whitespace-pre-wrap break-all font-mono">
                {viewSession.sensorData ? JSON.stringify(JSON.parse(viewSession.sensorData), null, 2) : t('machines.noSensorData')}
              </pre>
            </div>
            <div className="flex justify-end mt-4">
              <button onClick={() => setViewSession(null)} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">{t('common.close')}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

// ── Record Session Modal ──────────────────────────────────

function RecordSessionModal({ machines, onClose }: { machines: MachineDto[]; onClose: () => void }) {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const [form, setForm] = useState({
    machineId: '', memberId: '', startedAt: '', endedAt: '', sensorData: '',
  })

  const create = useMutation({
    mutationFn: () => api.post('/machines/sensor-sessions', form),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['sensor-sessions'] }); onClose() },
  })

  const set = (k: string, v: string) => setForm((f) => ({ ...f, [k]: v }))

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/30 backdrop-blur-sm" onClick={onClose} />
      <div className="relative bg-white rounded-xl shadow-2xl w-full max-w-lg p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">{t('machines.recordSensorSession')}</h2>
        <div className="space-y-3">
          <select value={form.machineId} onChange={(e) => set('machineId', e.target.value)} className="border rounded-lg px-3 py-2 text-sm w-full">
            <option value="">{t('machines.selectMachine')}</option>
            {machines.map((m) => <option key={m.id} value={m.id}>{m.code} - {m.name}</option>)}
          </select>
          <input placeholder="Member ID *" value={form.memberId} onChange={(e) => set('memberId', e.target.value)} className="border rounded-lg px-3 py-2 text-sm w-full" />
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs text-gray-500 mb-1">Start Time</label>
              <input type="datetime-local" value={form.startedAt} onChange={(e) => set('startedAt', e.target.value)} className="border rounded-lg px-3 py-2 text-sm w-full" />
            </div>
            <div>
              <label className="block text-xs text-gray-500 mb-1">End Time</label>
              <input type="datetime-local" value={form.endedAt} onChange={(e) => set('endedAt', e.target.value)} className="border rounded-lg px-3 py-2 text-sm w-full" />
            </div>
          </div>
          <div>
            <label className="block text-xs text-gray-500 mb-1">Sensor Data (JSON)</label>
            <textarea value={form.sensorData} onChange={(e) => set('sensorData', e.target.value)} rows={5} placeholder='{"force": [120, 135, ...], "angle": [0, 15, ...]}' className="border rounded-lg px-3 py-2 text-sm w-full font-mono" />
          </div>
        </div>
        <div className="flex justify-end gap-3 mt-5">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">{t('common.cancel')}</button>
          <button onClick={() => create.mutate()} disabled={!form.machineId || !form.memberId} className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700 disabled:opacity-50">
            Record
          </button>
        </div>
      </div>
    </div>
  )
}

// ── Strength Progress Tab ──────────────────────────────────────

function StrengthProgressTab() {
  const { t } = useTranslation()
  const [memberId, setMemberId] = useState('')
  const [machineId, setMachineId] = useState('')
  const [measurementType, setMeasurementType] = useState<MeasurementType | ''>('')

  const { data: machinesData } = useQuery({
    queryKey: ['machines'],
    queryFn: () => api.get<ApiResponse<MachineDto[]>>('/machines').then((r) => r.data),
  })

  const machines = machinesData?.data ?? []

  const { data: progressData, isLoading, refetch } = useQuery({
    queryKey: ['member-progress', memberId, machineId, measurementType],
    queryFn: () =>
      api.get<ApiResponse<MemberProgressDto>>(`/machines/measurements/${memberId}/progress`, {
        params: {
          ...(machineId && { machineId }),
          ...(measurementType && { type: measurementType }),
        },
      }).then((r) => r.data),
    enabled: false,
  })

  const progress = progressData?.data

  const handleFetch = () => {
    if (memberId) refetch()
  }

  // SVG line chart for peak force over time
  const renderChart = (measurements: StrengthMeasurementDto[]) => {
    const points = measurements
      .filter((m) => m.peakForceNewtons != null)
      .map((m) => ({ date: new Date(m.measuredAt).getTime(), force: m.peakForceNewtons! }))
      .sort((a, b) => a.date - b.date)

    if (points.length < 2) return null

    const minForce = Math.min(...points.map((p) => p.force)) * 0.9
    const maxForce = Math.max(...points.map((p) => p.force)) * 1.1
    const minDate = points[0].date
    const maxDate = points[points.length - 1].date
    const w = 600
    const h = 200
    const pad = 40

    const toX = (d: number) => pad + ((d - minDate) / (maxDate - minDate || 1)) * (w - pad * 2)
    const toY = (f: number) => h - pad - ((f - minForce) / (maxForce - minForce || 1)) * (h - pad * 2)

    const pathD = points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${toX(p.date)} ${toY(p.force)}`).join(' ')

    return (
      <div className="bg-white rounded-xl border p-4 mb-4">
        <h3 className="text-sm font-semibold text-gray-900 mb-3">{t('machines.peakForceOverTime')}</h3>
        <svg viewBox={`0 0 ${w} ${h}`} className="w-full" style={{ maxHeight: 220 }}>
          {/* Grid lines */}
          {[0, 0.25, 0.5, 0.75, 1].map((pct) => {
            const y = h - pad - pct * (h - pad * 2)
            const val = Math.round(minForce + pct * (maxForce - minForce))
            return (
              <g key={pct}>
                <line x1={pad} y1={y} x2={w - pad} y2={y} stroke="#e5e7eb" strokeWidth={1} />
                <text x={pad - 5} y={y + 4} textAnchor="end" className="fill-gray-400" fontSize={10}>{val}N</text>
              </g>
            )
          })}
          {/* Line */}
          <path d={pathD} fill="none" stroke="#6366f1" strokeWidth={2.5} strokeLinecap="round" strokeLinejoin="round" />
          {/* Points */}
          {points.map((p, i) => (
            <circle key={i} cx={toX(p.date)} cy={toY(p.force)} r={4} fill="#6366f1" stroke="white" strokeWidth={2} />
          ))}
        </svg>
      </div>
    )
  }

  return (
    <div>
      <div className="flex items-center gap-4 mb-6">
        <input
          placeholder={t('machines.memberIdRequired')}
          value={memberId}
          onChange={(e) => setMemberId(e.target.value)}
          className="border rounded-lg px-3 py-2 text-sm w-56"
        />
        <select value={machineId} onChange={(e) => setMachineId(e.target.value)} className="border rounded-lg px-3 py-2 text-sm">
          <option value="">{t('machines.allMachines')}</option>
          {machines.map((m) => <option key={m.id} value={m.id}>{m.code} - {m.name}</option>)}
        </select>
        <select value={measurementType} onChange={(e) => setMeasurementType(e.target.value as MeasurementType | '')} className="border rounded-lg px-3 py-2 text-sm">
          <option value="">{t('machines.allTypes')}</option>
          <option value="ISOMETRIC">{t('machines.isometric')}</option>
          <option value="DYNAMIC">{t('machines.dynamic')}</option>
          <option value="RANGE_OF_MOTION">{t('machines.rangeOfMotion')}</option>
        </select>
        <button
          onClick={handleFetch}
          disabled={!memberId}
          className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700 disabled:opacity-50"
        >
          {t('machines.fetchProgress')}
        </button>
      </div>

      {isLoading && <p className="text-gray-500 text-sm">{t('machines.loadingProgress')}</p>}

      {progress && (
        <div>
          {/* Summary card */}
          <div className="bg-white rounded-xl border p-4 mb-4">
            <div className="flex items-center gap-6">
              <div>
                <p className="text-xs text-gray-500">{t('machines.member')}</p>
                <p className="text-sm font-semibold text-gray-900">{progress.memberName}</p>
              </div>
              <div>
                <p className="text-xs text-gray-500">{t('machines.machine')}</p>
                <p className="text-sm font-semibold text-gray-900">{progress.machineCode} - {progress.machineName}</p>
              </div>
              <div>
                <p className="text-xs text-gray-500">{t('machines.initialPeakForce')}</p>
                <p className="text-sm font-semibold text-gray-900">{progress.initialPeakForce ? `${progress.initialPeakForce} N` : '-'}</p>
              </div>
              <div>
                <p className="text-xs text-gray-500">{t('machines.latestPeakForce')}</p>
                <p className="text-sm font-semibold text-gray-900">{progress.latestPeakForce ? `${progress.latestPeakForce} N` : '-'}</p>
              </div>
              <div>
                <p className="text-xs text-gray-500">{t('machines.improvement')}</p>
                <p className={`text-sm font-bold ${progress.improvementPercent >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                  {progress.improvementPercent >= 0 ? '+' : ''}{progress.improvementPercent.toFixed(1)}%
                </p>
              </div>
            </div>
          </div>

          {/* Chart */}
          {progress.measurements && renderChart(progress.measurements)}

          {/* Measurements table */}
          {progress.measurements && progress.measurements.length > 0 && (
            <div className="bg-white rounded-xl border overflow-hidden">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 text-gray-600">
                  <tr>
                    <th className="text-left px-4 py-3 font-medium">{t('machines.measurementDate')}</th>
                    <th className="text-left px-4 py-3 font-medium">{t('machines.measurementType')}</th>
                    <th className="text-left px-4 py-3 font-medium">{t('machines.peakForce')}</th>
                    <th className="text-left px-4 py-3 font-medium">{t('machines.avgForce')}</th>
                    <th className="text-left px-4 py-3 font-medium">{t('machines.rom')}</th>
                    <th className="text-left px-4 py-3 font-medium">{t('machines.tut')}</th>
                    <th className="text-left px-4 py-3 font-medium">{t('machines.reps')}</th>
                    <th className="text-left px-4 py-3 font-medium">{t('machines.setNumber')}</th>
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {[...progress.measurements]
                    .sort((a, b) => new Date(a.measuredAt).getTime() - new Date(b.measuredAt).getTime())
                    .map((m) => (
                      <tr key={m.id} className="hover:bg-gray-50">
                        <td className="px-4 py-3 text-gray-600">{formatDate(m.measuredAt)}</td>
                        <td className="px-4 py-3"><MeasurementTypeBadge type={m.measurementType} /></td>
                        <td className="px-4 py-3 text-gray-900 font-medium">{m.peakForceNewtons ? `${m.peakForceNewtons} N` : '-'}</td>
                        <td className="px-4 py-3 text-gray-600">{m.avgForceNewtons ? `${m.avgForceNewtons} N` : '-'}</td>
                        <td className="px-4 py-3 text-gray-600">{m.rangeOfMotionDegrees ? `${m.rangeOfMotionDegrees}°` : '-'}</td>
                        <td className="px-4 py-3 text-gray-600">{m.timeUnderTensionSeconds ? `${m.timeUnderTensionSeconds}s` : '-'}</td>
                        <td className="px-4 py-3 text-gray-600">{m.repetitions ?? '-'}</td>
                        <td className="px-4 py-3 text-gray-600">{m.setNumber ?? '-'}</td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

// ── Maintenance Tab ──────────────────────────────────────

function MaintenanceTab() {
  const { t } = useTranslation()
  const [machineFilter, setMachineFilter] = useState('')
  const [showLog, setShowLog] = useState(false)

  const { data: machinesData } = useQuery({
    queryKey: ['machines'],
    queryFn: () => api.get<ApiResponse<MachineDto[]>>('/machines').then((r) => r.data),
  })

  const machines = machinesData?.data ?? []

  const { data, isLoading } = useQuery({
    queryKey: ['maintenance-logs', machineFilter],
    queryFn: () =>
      api.get<ApiResponse<MachineMaintenanceLogDto[]>>('/machines/maintenance', {
        params: { ...(machineFilter && { machineId: machineFilter }) },
      }).then((r) => r.data),
  })

  const logs = data?.data ?? []

  // Upcoming maintenance alerts
  const now = new Date()
  const sevenDaysFromNow = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000)
  const upcomingMachines = machines.filter((m) => {
    if (!m.nextMaintenanceDate) return false
    const d = new Date(m.nextMaintenanceDate)
    return d >= now && d <= sevenDaysFromNow
  })

  return (
    <div>
      {/* Upcoming maintenance alerts */}
      {upcomingMachines.length > 0 && (
        <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 mb-6">
          <h3 className="text-sm font-semibold text-amber-800 mb-2">{t('machines.upcomingMaintenance')}</h3>
          <div className="space-y-1">
            {upcomingMachines.map((m) => (
              <div key={m.id} className="flex items-center gap-2 text-sm">
                <span className="bg-amber-100 text-amber-700 text-xs font-bold px-1.5 py-0.5 rounded">{m.code}</span>
                <span className="text-amber-900">{m.name}</span>
                <span className="text-amber-600 ml-auto">{formatDate(m.nextMaintenanceDate!)}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="flex items-center gap-4 mb-6">
        <select
          value={machineFilter}
          onChange={(e) => setMachineFilter(e.target.value)}
          className="border rounded-lg px-3 py-2 text-sm"
        >
          <option value="">{t('machines.allMachines')}</option>
          {machines.map((m) => (
            <option key={m.id} value={m.id}>{m.code} - {m.name}</option>
          ))}
        </select>
        <button
          onClick={() => setShowLog(true)}
          className="ml-auto bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700"
        >
          {t('machines.logMaintenance')}
        </button>
      </div>

      {isLoading ? (
        <p className="text-gray-500 text-sm">{t('machines.loadingMaintenanceLogs')}</p>
      ) : logs.length === 0 ? (
        <p className="text-gray-400 text-sm">{t('machines.noMaintenanceLogs')}</p>
      ) : (
        <div className="bg-white rounded-xl border overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-gray-600">
              <tr>
                <th className="text-left px-4 py-3 font-medium">{t('machines.machine')}</th>
                <th className="text-left px-4 py-3 font-medium">{t('machines.maintenanceType')}</th>
                <th className="text-left px-4 py-3 font-medium">{t('common.description')}</th>
                <th className="text-left px-4 py-3 font-medium">{t('machines.performedBy')}</th>
                <th className="text-left px-4 py-3 font-medium">{t('common.date')}</th>
                <th className="text-left px-4 py-3 font-medium">{t('machines.nextDueDate')}</th>
                <th className="text-left px-4 py-3 font-medium">{t('machines.cost')}</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {logs.map((log) => (
                <tr key={log.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-gray-900">{log.machineName || log.machineId.slice(0, 8)}</td>
                  <td className="px-4 py-3"><MaintenanceTypeBadge type={log.maintenanceType} /></td>
                  <td className="px-4 py-3 text-gray-600 max-w-[200px] truncate">{log.description || '-'}</td>
                  <td className="px-4 py-3 text-gray-600">{log.performedBy || '-'}</td>
                  <td className="px-4 py-3 text-gray-600">{formatDate(log.performedAt)}</td>
                  <td className="px-4 py-3 text-gray-600">{log.nextDueDate ? formatDate(log.nextDueDate) : '-'}</td>
                  <td className="px-4 py-3 text-gray-600">{log.cost != null ? `${log.cost.toFixed(2)}` : '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showLog && <LogMaintenanceModal machines={machines} onClose={() => setShowLog(false)} />}
    </div>
  )
}

// ── Log Maintenance Modal ──────────────────────────────────

function LogMaintenanceModal({ machines, onClose }: { machines: MachineDto[]; onClose: () => void }) {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const [form, setForm] = useState({
    machineId: '', maintenanceType: 'ROUTINE' as MaintenanceType, description: '',
    performedBy: '', performedAt: '', nextDueDate: '', cost: '', notes: '',
  })

  const create = useMutation({
    mutationFn: () =>
      api.post('/machines/maintenance', {
        ...form,
        cost: form.cost ? parseFloat(form.cost) : undefined,
      }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['maintenance-logs'] }); onClose() },
  })

  const set = (k: string, v: string) => setForm((f) => ({ ...f, [k]: v }))

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/30 backdrop-blur-sm" onClick={onClose} />
      <div className="relative bg-white rounded-xl shadow-2xl w-full max-w-lg p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">{t('machines.logMaintenanceTitle')}</h2>
        <div className="space-y-3">
          <select value={form.machineId} onChange={(e) => set('machineId', e.target.value)} className="border rounded-lg px-3 py-2 text-sm w-full">
            <option value="">{t('machines.selectMachineRequired')}</option>
            {machines.map((m) => <option key={m.id} value={m.id}>{m.code} - {m.name}</option>)}
          </select>
          <select value={form.maintenanceType} onChange={(e) => set('maintenanceType', e.target.value)} className="border rounded-lg px-3 py-2 text-sm w-full">
            <option value="ROUTINE">{t('machines.routine')}</option>
            <option value="REPAIR">{t('machines.repair')}</option>
            <option value="CALIBRATION">{t('machines.calibration')}</option>
            <option value="FIRMWARE_UPDATE">{t('machines.firmwareUpdate')}</option>
          </select>
          <input placeholder="Description" value={form.description} onChange={(e) => set('description', e.target.value)} className="border rounded-lg px-3 py-2 text-sm w-full" />
          <input placeholder="Performed By" value={form.performedBy} onChange={(e) => set('performedBy', e.target.value)} className="border rounded-lg px-3 py-2 text-sm w-full" />
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs text-gray-500 mb-1">Performed Date *</label>
              <input type="date" value={form.performedAt} onChange={(e) => set('performedAt', e.target.value)} className="border rounded-lg px-3 py-2 text-sm w-full" />
            </div>
            <div>
              <label className="block text-xs text-gray-500 mb-1">Next Due Date</label>
              <input type="date" value={form.nextDueDate} onChange={(e) => set('nextDueDate', e.target.value)} className="border rounded-lg px-3 py-2 text-sm w-full" />
            </div>
          </div>
          <input type="number" step="0.01" placeholder="Cost" value={form.cost} onChange={(e) => set('cost', e.target.value)} className="border rounded-lg px-3 py-2 text-sm w-full" />
          <textarea placeholder="Notes" value={form.notes} onChange={(e) => set('notes', e.target.value)} rows={2} className="border rounded-lg px-3 py-2 text-sm w-full" />
        </div>
        <div className="flex justify-end gap-3 mt-5">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">{t('common.cancel')}</button>
          <button onClick={() => create.mutate()} disabled={!form.machineId || !form.performedAt} className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700 disabled:opacity-50">
            Log
          </button>
        </div>
      </div>
    </div>
  )
}
