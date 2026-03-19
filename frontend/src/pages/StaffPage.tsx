import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../api/client'
import type {
  ApiResponse,
  EmployeeDto,
  ShiftDto,
  TimeEntryDto,
  EmploymentType,
  ShiftStatus,
} from '../types'

const EMPLOYMENT_TYPES: EmploymentType[] = ['FULL_TIME', 'PART_TIME', 'FREELANCE', 'INTERN']

type Tab = 'employees' | 'shifts' | 'time'

export default function StaffPage() {
  const [activeTab, setActiveTab] = useState<Tab>('employees')

  const tabs: { key: Tab; label: string }[] = [
    { key: 'employees', label: 'Employees' },
    { key: 'shifts', label: 'Shift Schedule' },
    { key: 'time', label: 'Time Tracking' },
  ]

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Staff</h1>
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

      {activeTab === 'employees' && <EmployeeList />}
      {activeTab === 'shifts' && <ShiftSchedule />}
      {activeTab === 'time' && <TimeTracking />}
    </div>
  )
}

// ===================== EMPLOYEES =====================

function EmployeeList() {
  const queryClient = useQueryClient()
  const [showCreate, setShowCreate] = useState(false)

  const { data, isLoading } = useQuery({
    queryKey: ['employees'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<EmployeeDto[]>>('/staff/employees?size=50')
      return res.data
    },
  })

  const createMutation = useMutation({
    mutationFn: (emp: Record<string, unknown>) => api.post('/staff/employees', emp),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['employees'] })
      setShowCreate(false)
    },
  })

  const deactivateMutation = useMutation({
    mutationFn: (id: string) => api.post(`/staff/employees/${id}/deactivate`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['employees'] }),
  })

  const employees = data?.data ?? []

  return (
    <div>
      <div className="flex justify-end mb-4">
        <button
          onClick={() => setShowCreate(true)}
          className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-indigo-700"
        >
          Add Employee
        </button>
      </div>

      {isLoading ? (
        <p className="text-gray-500">Loading employees...</p>
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">Name</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">Role</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">Type</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">Position</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">Contact</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {employees.map((emp) => (
                <tr key={emp.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm font-medium">
                    {emp.firstName} {emp.lastName}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600">{emp.role || '—'}</td>
                  <td className="px-4 py-3">
                    <span className={`text-xs px-2 py-0.5 rounded ${
                      emp.employmentType === 'FULL_TIME' ? 'bg-green-100 text-green-700' :
                      emp.employmentType === 'PART_TIME' ? 'bg-blue-100 text-blue-700' :
                      emp.employmentType === 'FREELANCE' ? 'bg-purple-100 text-purple-700' :
                      'bg-gray-100 text-gray-600'
                    }`}>
                      {emp.employmentType.replace(/_/g, ' ')}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600">{emp.position || '—'}</td>
                  <td className="px-4 py-3 text-sm text-gray-500">{emp.email || emp.phone || '—'}</td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => deactivateMutation.mutate(emp.id)}
                      className="text-xs text-red-500 hover:text-red-700"
                    >
                      Deactivate
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showCreate && (
        <CreateEmployeeModal
          onClose={() => setShowCreate(false)}
          onSubmit={(data) => createMutation.mutate(data)}
          isLoading={createMutation.isPending}
        />
      )}
    </div>
  )
}

function CreateEmployeeModal({
  onClose,
  onSubmit,
  isLoading,
}: {
  onClose: () => void
  onSubmit: (data: Record<string, unknown>) => void
  isLoading: boolean
}) {
  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    role: '',
    employmentType: 'FULL_TIME' as EmploymentType,
    position: '',
    hourlyRate: '',
    competencies: '',
  })

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
        <h2 className="text-lg font-bold mb-4">Add Employee</h2>
        <div className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <input placeholder="First name *" value={form.firstName}
              onChange={(e) => setForm({ ...form, firstName: e.target.value })}
              className="border rounded px-3 py-2 text-sm" />
            <input placeholder="Last name *" value={form.lastName}
              onChange={(e) => setForm({ ...form, lastName: e.target.value })}
              className="border rounded px-3 py-2 text-sm" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <input placeholder="Email" value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              className="border rounded px-3 py-2 text-sm" />
            <input placeholder="Phone" value={form.phone}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
              className="border rounded px-3 py-2 text-sm" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <select value={form.employmentType}
              onChange={(e) => setForm({ ...form, employmentType: e.target.value as EmploymentType })}
              className="border rounded px-3 py-2 text-sm">
              {EMPLOYMENT_TYPES.map((t) => (
                <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>
              ))}
            </select>
            <input placeholder="Role (e.g., TRAINER)" value={form.role}
              onChange={(e) => setForm({ ...form, role: e.target.value })}
              className="border rounded px-3 py-2 text-sm" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <input placeholder="Position" value={form.position}
              onChange={(e) => setForm({ ...form, position: e.target.value })}
              className="border rounded px-3 py-2 text-sm" />
            <input type="number" placeholder="Hourly rate" value={form.hourlyRate}
              onChange={(e) => setForm({ ...form, hourlyRate: e.target.value })}
              className="border rounded px-3 py-2 text-sm" />
          </div>
          <textarea placeholder="Competencies" value={form.competencies}
            onChange={(e) => setForm({ ...form, competencies: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm" rows={2} />
        </div>
        <div className="flex justify-end gap-3 mt-6">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600">Cancel</button>
          <button
            onClick={() => onSubmit({
              firstName: form.firstName, lastName: form.lastName,
              email: form.email || undefined, phone: form.phone || undefined,
              role: form.role || undefined, employmentType: form.employmentType,
              position: form.position || undefined,
              hourlyRate: form.hourlyRate ? parseFloat(form.hourlyRate) : undefined,
              competencies: form.competencies || undefined,
            })}
            disabled={!form.firstName || !form.lastName || isLoading}
            className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-indigo-700 disabled:opacity-50"
          >
            {isLoading ? 'Creating...' : 'Add Employee'}
          </button>
        </div>
      </div>
    </div>
  )
}

// ===================== SHIFTS =====================

function ShiftSchedule() {
  const queryClient = useQueryClient()
  const [showCreate, setShowCreate] = useState(false)

  const { data, isLoading } = useQuery({
    queryKey: ['weekly-shifts'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<ShiftDto[]>>('/staff/shifts/weekly')
      return res.data
    },
  })

  const createMutation = useMutation({
    mutationFn: (shift: Record<string, unknown>) => api.post('/staff/shifts', shift),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['weekly-shifts'] })
      setShowCreate(false)
    },
  })

  const cancelMutation = useMutation({
    mutationFn: (id: string) => api.post(`/staff/shifts/${id}/cancel`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['weekly-shifts'] }),
  })

  const shifts = data?.data ?? []

  const statusColor = (status: ShiftStatus) => {
    const colors: Record<ShiftStatus, string> = {
      SCHEDULED: 'bg-blue-100 text-blue-700',
      CONFIRMED: 'bg-indigo-100 text-indigo-700',
      IN_PROGRESS: 'bg-yellow-100 text-yellow-700',
      COMPLETED: 'bg-green-100 text-green-700',
      CANCELLED: 'bg-gray-100 text-gray-500',
      NO_SHOW: 'bg-red-100 text-red-700',
    }
    return colors[status]
  }

  return (
    <div>
      <div className="flex justify-end mb-4">
        <button onClick={() => setShowCreate(true)}
          className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-indigo-700">
          Add Shift
        </button>
      </div>

      {isLoading ? (
        <p className="text-gray-500">Loading schedule...</p>
      ) : shifts.length === 0 ? (
        <p className="text-gray-400">No shifts scheduled this week.</p>
      ) : (
        <div className="space-y-3">
          {shifts.map((shift) => (
            <div key={shift.id} className="bg-white rounded-lg shadow p-4 flex items-center justify-between">
              <div>
                <p className="font-medium text-sm">{shift.employeeName || 'Unknown'}</p>
                <p className="text-xs text-gray-500 mt-1">
                  {new Date(shift.startTime).toLocaleString()} — {new Date(shift.endTime).toLocaleTimeString()}
                  <span className="ml-2">({shift.durationMinutes} min)</span>
                </p>
                {shift.notes && <p className="text-xs text-gray-400 mt-1">{shift.notes}</p>}
              </div>
              <div className="flex items-center gap-2">
                <span className={`text-xs px-2 py-0.5 rounded ${statusColor(shift.status)}`}>
                  {shift.status.replace(/_/g, ' ')}
                </span>
                {shift.status === 'SCHEDULED' && (
                  <button onClick={() => cancelMutation.mutate(shift.id)}
                    className="text-xs text-red-500 hover:text-red-700">Cancel</button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {showCreate && (
        <CreateShiftModal
          onClose={() => setShowCreate(false)}
          onSubmit={(data) => createMutation.mutate(data)}
          isLoading={createMutation.isPending}
        />
      )}
    </div>
  )
}

function CreateShiftModal({
  onClose, onSubmit, isLoading,
}: { onClose: () => void; onSubmit: (data: Record<string, unknown>) => void; isLoading: boolean }) {
  const { data: employees } = useQuery({
    queryKey: ['employees-for-shift'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<EmployeeDto[]>>('/staff/employees?size=100')
      return res.data
    },
  })

  const [form, setForm] = useState({ employeeId: '', startTime: '', endTime: '', notes: '' })

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
        <h2 className="text-lg font-bold mb-4">Add Shift</h2>
        <div className="space-y-3">
          <select value={form.employeeId}
            onChange={(e) => setForm({ ...form, employeeId: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm">
            <option value="">Select employee *</option>
            {(employees?.data ?? []).map((e) => (
              <option key={e.id} value={e.id}>{e.firstName} {e.lastName}</option>
            ))}
          </select>
          <div className="grid grid-cols-2 gap-3">
            <input type="datetime-local" value={form.startTime}
              onChange={(e) => setForm({ ...form, startTime: e.target.value })}
              className="border rounded px-3 py-2 text-sm" />
            <input type="datetime-local" value={form.endTime}
              onChange={(e) => setForm({ ...form, endTime: e.target.value })}
              className="border rounded px-3 py-2 text-sm" />
          </div>
          <input placeholder="Notes" value={form.notes}
            onChange={(e) => setForm({ ...form, notes: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm" />
        </div>
        <div className="flex justify-end gap-3 mt-6">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600">Cancel</button>
          <button
            onClick={() => onSubmit({
              employeeId: form.employeeId,
              startTime: new Date(form.startTime).toISOString(),
              endTime: new Date(form.endTime).toISOString(),
              notes: form.notes || undefined,
            })}
            disabled={!form.employeeId || !form.startTime || !form.endTime || isLoading}
            className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-indigo-700 disabled:opacity-50"
          >
            {isLoading ? 'Creating...' : 'Add Shift'}
          </button>
        </div>
      </div>
    </div>
  )
}

// ===================== TIME TRACKING =====================

function TimeTracking() {
  const queryClient = useQueryClient()
  const [selectedEmployee, setSelectedEmployee] = useState('')

  const { data: employees } = useQuery({
    queryKey: ['employees-for-time'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<EmployeeDto[]>>('/staff/employees?size=100')
      return res.data
    },
  })

  const { data: activeEntry } = useQuery({
    queryKey: ['active-time-entry', selectedEmployee],
    queryFn: async () => {
      if (!selectedEmployee) return null
      const res = await api.get<ApiResponse<TimeEntryDto>>(`/staff/time/active/${selectedEmployee}`)
      return res.data.data
    },
    enabled: !!selectedEmployee,
  })

  const clockInMutation = useMutation({
    mutationFn: (data: Record<string, unknown>) => api.post('/staff/time/clock-in', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['active-time-entry'] })
    },
  })

  const clockOutMutation = useMutation({
    mutationFn: ({ entryId, data }: { entryId: string; data: Record<string, unknown> }) =>
      api.post(`/staff/time/${entryId}/clock-out`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['active-time-entry'] })
    },
  })

  return (
    <div>
      <div className="mb-6">
        <select value={selectedEmployee}
          onChange={(e) => setSelectedEmployee(e.target.value)}
          className="border rounded-lg px-3 py-2 text-sm w-80">
          <option value="">Select employee...</option>
          {(employees?.data ?? []).map((e) => (
            <option key={e.id} value={e.id}>{e.firstName} {e.lastName}</option>
          ))}
        </select>
      </div>

      {selectedEmployee && (
        <div className="bg-white rounded-lg shadow p-6 max-w-md">
          {activeEntry ? (
            <div>
              <div className="flex items-center gap-2 mb-4">
                <div className="w-3 h-3 rounded-full bg-green-500 animate-pulse" />
                <span className="text-sm font-medium text-green-700">Clocked In</span>
              </div>
              <p className="text-sm text-gray-600 mb-1">
                Since: {new Date(activeEntry.clockIn).toLocaleString()}
              </p>
              <p className="text-xs text-gray-400 mb-4">
                Duration: {Math.round((Date.now() - new Date(activeEntry.clockIn).getTime()) / 60000)} min
              </p>
              <div className="flex gap-3">
                <button
                  onClick={() => clockOutMutation.mutate({
                    entryId: activeEntry.id,
                    data: { breakMinutes: 0 },
                  })}
                  className="bg-red-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-red-700"
                >
                  Clock Out
                </button>
                <button
                  onClick={() => clockOutMutation.mutate({
                    entryId: activeEntry.id,
                    data: { breakMinutes: 30 },
                  })}
                  className="bg-orange-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-orange-700"
                >
                  Clock Out (30m break)
                </button>
              </div>
            </div>
          ) : (
            <div>
              <p className="text-sm text-gray-500 mb-4">Not clocked in.</p>
              <button
                onClick={() => clockInMutation.mutate({ employeeId: selectedEmployee })}
                disabled={clockInMutation.isPending}
                className="bg-green-600 text-white px-6 py-2 rounded-lg text-sm hover:bg-green-700 disabled:opacity-50"
              >
                {clockInMutation.isPending ? 'Clocking in...' : 'Clock In'}
              </button>
            </div>
          )}
        </div>
      )}

      {!selectedEmployee && (
        <p className="text-gray-400">Select an employee to manage time tracking.</p>
      )}
    </div>
  )
}
