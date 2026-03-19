import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../api/client'
import type {
  ApiResponse,
  ClassCategoryDto,
  ClassDefinitionDto,
  ClassScheduleDto,
  ClassBookingDto,
  WaitlistEntryDto,
} from '../types'

export default function ClassesPage() {
  const [tab, setTab] = useState<'schedule' | 'classes' | 'categories'>('schedule')
  const [selectedSchedule, setSelectedSchedule] = useState<ClassScheduleDto | null>(null)
  const [showCreateClass, setShowCreateClass] = useState(false)
  const [showCreateCategory, setShowCreateCategory] = useState(false)
  const [showCreateSchedule, setShowCreateSchedule] = useState(false)
  const queryClient = useQueryClient()

  // Current week range
  const weekStart = useMemo(() => {
    const now = new Date()
    const day = now.getDay()
    const diff = now.getDate() - day + (day === 0 ? -6 : 1) // Monday
    const monday = new Date(now.setDate(diff))
    monday.setHours(0, 0, 0, 0)
    return monday.toISOString()
  }, [])

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Classes & Booking</h1>
        <div className="flex gap-2">
          {tab === 'schedule' && (
            <button
              onClick={() => setShowCreateSchedule(true)}
              className="bg-brand-600 text-white px-4 py-2 rounded-md text-sm hover:bg-brand-700"
            >
              + Schedule Class
            </button>
          )}
          {tab === 'classes' && (
            <button
              onClick={() => setShowCreateClass(true)}
              className="bg-brand-600 text-white px-4 py-2 rounded-md text-sm hover:bg-brand-700"
            >
              + New Class
            </button>
          )}
          {tab === 'categories' && (
            <button
              onClick={() => setShowCreateCategory(true)}
              className="bg-brand-600 text-white px-4 py-2 rounded-md text-sm hover:bg-brand-700"
            >
              + New Category
            </button>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b mb-6">
        {(['schedule', 'classes', 'categories'] as const).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`px-4 py-2 text-sm font-medium border-b-2 ${
              tab === t
                ? 'border-brand-600 text-brand-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            {t === 'schedule' ? 'Weekly Schedule' : t === 'classes' ? 'Class Definitions' : 'Categories'}
          </button>
        ))}
      </div>

      {tab === 'schedule' && (
        <ScheduleTab
          weekStart={weekStart}
          selectedSchedule={selectedSchedule}
          onSelect={setSelectedSchedule}
        />
      )}
      {tab === 'classes' && <ClassDefinitionsTab />}
      {tab === 'categories' && <CategoriesTab />}

      {/* Modals */}
      {showCreateCategory && (
        <CreateCategoryModal
          onClose={() => setShowCreateCategory(false)}
          onCreated={() => {
            setShowCreateCategory(false)
            queryClient.invalidateQueries({ queryKey: ['class-categories'] })
          }}
        />
      )}
      {showCreateClass && (
        <CreateClassModal
          onClose={() => setShowCreateClass(false)}
          onCreated={() => {
            setShowCreateClass(false)
            queryClient.invalidateQueries({ queryKey: ['class-definitions'] })
          }}
        />
      )}
      {showCreateSchedule && (
        <CreateScheduleModal
          onClose={() => setShowCreateSchedule(false)}
          onCreated={() => {
            setShowCreateSchedule(false)
            queryClient.invalidateQueries({ queryKey: ['class-schedule'] })
          }}
        />
      )}
    </div>
  )
}

// ============ SCHEDULE TAB ============

function ScheduleTab({
  weekStart,
  selectedSchedule,
  onSelect,
}: {
  weekStart: string
  selectedSchedule: ClassScheduleDto | null
  onSelect: (s: ClassScheduleDto | null) => void
}) {
  const { data: schedules, isLoading } = useQuery({
    queryKey: ['class-schedule', weekStart],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<ClassScheduleDto[]>>('/booking/schedules/weekly', {
        params: { weekStart },
      })
      return data.data ?? []
    },
  })

  const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

  const schedulesByDay = useMemo(() => {
    if (!schedules) return {}
    const grouped: Record<string, ClassScheduleDto[]> = {}
    for (const s of schedules) {
      const dayIdx = new Date(s.startTime).getDay()
      const dayName = days[dayIdx === 0 ? 6 : dayIdx - 1]
      if (!grouped[dayName]) grouped[dayName] = []
      grouped[dayName].push(s)
    }
    return grouped
  }, [schedules])

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div className="lg:col-span-2">
        {isLoading ? (
          <div className="text-center text-gray-500 py-8">Loading schedule...</div>
        ) : (
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <div className="grid grid-cols-7 border-b">
              {days.map((day) => (
                <div key={day} className="px-2 py-3 text-center text-xs font-semibold text-gray-500 uppercase">
                  {day}
                </div>
              ))}
            </div>
            <div className="grid grid-cols-7 min-h-[400px]">
              {days.map((day) => (
                <div key={day} className="border-r last:border-r-0 p-1 space-y-1">
                  {(schedulesByDay[day] ?? []).map((s) => (
                    <button
                      key={s.id}
                      onClick={() => onSelect(s)}
                      className={`w-full text-left p-2 rounded text-xs ${
                        selectedSchedule?.id === s.id
                          ? 'bg-brand-100 border border-brand-300'
                          : 'bg-gray-50 hover:bg-gray-100'
                      }`}
                    >
                      <div className="font-medium text-gray-900 truncate">{s.className}</div>
                      <div className="text-gray-500">
                        {new Date(s.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </div>
                      <div className="flex items-center gap-1 mt-1">
                        {s.categoryColor && (
                          <span
                            className="inline-block w-2 h-2 rounded-full"
                            style={{ backgroundColor: s.categoryColor }}
                          />
                        )}
                        <span className="text-gray-400">
                          {s.bookedCount}/{s.capacity}
                        </span>
                        {s.waitlistCount > 0 && (
                          <span className="text-orange-500">+{s.waitlistCount} wl</span>
                        )}
                      </div>
                    </button>
                  ))}
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Schedule detail panel */}
      <div>
        {selectedSchedule ? (
          <ScheduleDetail schedule={selectedSchedule} onClose={() => onSelect(null)} />
        ) : (
          <div className="bg-white rounded-lg shadow p-6 text-center text-gray-400 text-sm">
            Select a class from the schedule to view details
          </div>
        )}
      </div>
    </div>
  )
}

function ScheduleDetail({ schedule, onClose }: { schedule: ClassScheduleDto; onClose: () => void }) {
  const queryClient = useQueryClient()

  const { data: bookings } = useQuery({
    queryKey: ['schedule-bookings', schedule.id],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<ClassBookingDto[]>>(
        `/booking/bookings/schedule/${schedule.id}`,
      )
      return data.data ?? []
    },
  })

  const { data: waitlist } = useQuery({
    queryKey: ['schedule-waitlist', schedule.id],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<WaitlistEntryDto[]>>(
        `/booking/bookings/schedule/${schedule.id}/waitlist`,
      )
      return data.data ?? []
    },
  })

  const cancelScheduleMutation = useMutation({
    mutationFn: async () => {
      await api.post(`/booking/schedules/${schedule.id}/cancel`, { reason: 'Cancelled by staff' })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['class-schedule'] })
      onClose()
    },
  })

  const attendanceMutation = useMutation({
    mutationFn: async ({ bookingId, status }: { bookingId: string; status: string }) => {
      await api.post('/booking/bookings/attendance', { bookingId, status })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schedule-bookings', schedule.id] })
    },
  })

  return (
    <div className="bg-white rounded-lg shadow">
      <div className="px-4 py-3 border-b flex justify-between items-center">
        <div>
          <h3 className="font-semibold text-gray-800">{schedule.className}</h3>
          <p className="text-xs text-gray-500">
            {new Date(schedule.startTime).toLocaleString()} - {new Date(schedule.endTime).toLocaleTimeString()}
          </p>
        </div>
        <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-lg">&times;</button>
      </div>

      <div className="p-4 space-y-3 text-sm">
        <div className="flex justify-between">
          <span className="text-gray-500">Room</span>
          <span className="text-gray-800">{schedule.room || '—'}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-gray-500">Trainer</span>
          <span className="text-gray-800">{schedule.trainerName || '—'}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-gray-500">Capacity</span>
          <span className="text-gray-800">
            {schedule.bookedCount} / {schedule.capacity}
          </span>
        </div>
        {schedule.virtualLink && (
          <div className="flex justify-between">
            <span className="text-gray-500">Virtual</span>
            <a href={schedule.virtualLink} target="_blank" rel="noreferrer" className="text-brand-600 hover:underline">
              Join Link
            </a>
          </div>
        )}

        {!schedule.cancelled && (
          <button
            onClick={() => cancelScheduleMutation.mutate()}
            disabled={cancelScheduleMutation.isPending}
            className="w-full mt-2 px-3 py-1.5 text-xs text-red-600 border border-red-300 rounded hover:bg-red-50"
          >
            Cancel This Class
          </button>
        )}
        {schedule.cancelled && (
          <div className="mt-2 p-2 bg-red-50 text-red-700 rounded text-xs text-center">Cancelled</div>
        )}
      </div>

      {/* Bookings */}
      <div className="border-t">
        <div className="px-4 py-2 bg-gray-50 text-xs font-semibold text-gray-600">
          Bookings ({bookings?.length ?? 0})
        </div>
        <div className="divide-y max-h-48 overflow-auto">
          {bookings?.length === 0 && (
            <div className="px-4 py-3 text-xs text-gray-400 text-center">No bookings yet</div>
          )}
          {bookings?.map((b) => (
            <div key={b.id} className="px-4 py-2 flex items-center justify-between">
              <div>
                <span className="text-sm text-gray-800">{b.memberName || b.guestName}</span>
                {b.guestEmail && <span className="text-xs text-gray-400 ml-2">(guest)</span>}
              </div>
              <div className="flex items-center gap-1">
                {b.status === 'CONFIRMED' && (
                  <>
                    <button
                      onClick={() => attendanceMutation.mutate({ bookingId: b.id, status: 'ATTENDED' })}
                      className="px-2 py-0.5 text-xs bg-green-100 text-green-700 rounded hover:bg-green-200"
                    >
                      Present
                    </button>
                    <button
                      onClick={() => attendanceMutation.mutate({ bookingId: b.id, status: 'NO_SHOW' })}
                      className="px-2 py-0.5 text-xs bg-red-100 text-red-700 rounded hover:bg-red-200"
                    >
                      No Show
                    </button>
                  </>
                )}
                {b.status === 'ATTENDED' && (
                  <span className="px-2 py-0.5 text-xs bg-green-100 text-green-800 rounded-full">Attended</span>
                )}
                {b.status === 'NO_SHOW' && (
                  <span className="px-2 py-0.5 text-xs bg-red-100 text-red-800 rounded-full">No Show</span>
                )}
                {b.status === 'CANCELLED' && (
                  <span className="px-2 py-0.5 text-xs bg-gray-100 text-gray-600 rounded-full">Cancelled</span>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Waitlist */}
      {(waitlist?.length ?? 0) > 0 && (
        <div className="border-t">
          <div className="px-4 py-2 bg-orange-50 text-xs font-semibold text-orange-700">
            Waitlist ({waitlist?.length})
          </div>
          <div className="divide-y max-h-32 overflow-auto">
            {waitlist?.map((w) => (
              <div key={w.id} className="px-4 py-2 flex justify-between text-sm">
                <span>
                  #{w.position} — {w.memberName}
                </span>
                <span className="text-xs text-gray-400">{new Date(w.joinedAt).toLocaleTimeString()}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

// ============ CLASS DEFINITIONS TAB ============

function ClassDefinitionsTab() {
  const { data, isLoading } = useQuery({
    queryKey: ['class-definitions'],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<{ content: ClassDefinitionDto[] }>>('/booking/classes', {
        params: { size: 50 },
      })
      return data.data?.content ?? []
    },
  })

  return (
    <div className="bg-white rounded-lg shadow overflow-hidden">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Category</th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Duration</th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Capacity</th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Room</th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Trial</th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Waitlist</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-200">
          {isLoading ? (
            <tr>
              <td colSpan={7} className="px-6 py-8 text-center text-gray-500">Loading...</td>
            </tr>
          ) : data?.length === 0 ? (
            <tr>
              <td colSpan={7} className="px-6 py-8 text-center text-gray-500">No classes defined yet</td>
            </tr>
          ) : (
            data?.map((cls) => (
              <tr key={cls.id} className="hover:bg-gray-50">
                <td className="px-6 py-4">
                  <div className="text-sm font-medium text-gray-900">{cls.name}</div>
                  {cls.description && (
                    <div className="text-xs text-gray-500 truncate max-w-xs">{cls.description}</div>
                  )}
                </td>
                <td className="px-6 py-4 text-sm text-gray-500">{cls.categoryName || '—'}</td>
                <td className="px-6 py-4 text-sm text-gray-500">{cls.durationMinutes} min</td>
                <td className="px-6 py-4 text-sm text-gray-500">{cls.capacity}</td>
                <td className="px-6 py-4 text-sm text-gray-500">{cls.room || '—'}</td>
                <td className="px-6 py-4">
                  <span
                    className={`px-2 py-0.5 text-xs rounded-full ${
                      cls.allowTrial ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-500'
                    }`}
                  >
                    {cls.allowTrial ? 'Yes' : 'No'}
                  </span>
                </td>
                <td className="px-6 py-4">
                  <span
                    className={`px-2 py-0.5 text-xs rounded-full ${
                      cls.allowWaitlist ? 'bg-blue-100 text-blue-800' : 'bg-gray-100 text-gray-500'
                    }`}
                  >
                    {cls.allowWaitlist ? 'Yes' : 'No'}
                  </span>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  )
}

// ============ CATEGORIES TAB ============

function CategoriesTab() {
  const queryClient = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['class-categories'],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<ClassCategoryDto[]>>('/booking/categories/all')
      return data.data ?? []
    },
  })

  const deactivateMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/booking/categories/${id}`)
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['class-categories'] }),
  })

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      {isLoading && <div className="text-gray-500 col-span-full text-center py-8">Loading...</div>}
      {data?.length === 0 && (
        <div className="text-gray-500 col-span-full text-center py-8">No categories yet</div>
      )}
      {data?.map((cat) => (
        <div key={cat.id} className="bg-white rounded-lg shadow p-4">
          <div className="flex items-center gap-2 mb-2">
            {cat.color && (
              <span className="w-4 h-4 rounded-full" style={{ backgroundColor: cat.color }} />
            )}
            <h3 className="font-semibold text-gray-800">{cat.name}</h3>
            {!cat.active && (
              <span className="px-2 py-0.5 text-xs bg-gray-100 text-gray-500 rounded-full">Inactive</span>
            )}
          </div>
          {cat.description && <p className="text-sm text-gray-500 mb-3">{cat.description}</p>}
          {cat.active && (
            <button
              onClick={() => deactivateMutation.mutate(cat.id)}
              className="text-xs text-red-500 hover:text-red-700"
            >
              Deactivate
            </button>
          )}
        </div>
      ))}
    </div>
  )
}

// ============ CREATE CATEGORY MODAL ============

function CreateCategoryModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [color, setColor] = useState('#6366f1')

  const mutation = useMutation({
    mutationFn: async () => {
      await api.post('/booking/categories', { name, description, color })
    },
    onSuccess: onCreated,
  })

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md p-6">
        <h2 className="text-lg font-semibold mb-4">New Category</h2>
        <div className="space-y-3">
          <input
            type="text"
            placeholder="Category name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full px-3 py-2 border rounded-md text-sm"
          />
          <textarea
            placeholder="Description (optional)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="w-full px-3 py-2 border rounded-md text-sm"
            rows={2}
          />
          <div className="flex items-center gap-2">
            <label className="text-sm text-gray-600">Color</label>
            <input type="color" value={color} onChange={(e) => setColor(e.target.value)} className="w-8 h-8" />
          </div>
        </div>
        <div className="flex justify-end gap-2 mt-4">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">
            Cancel
          </button>
          <button
            onClick={() => mutation.mutate()}
            disabled={!name || mutation.isPending}
            className="px-4 py-2 text-sm bg-brand-600 text-white rounded-md hover:bg-brand-700 disabled:opacity-50"
          >
            {mutation.isPending ? 'Creating...' : 'Create'}
          </button>
        </div>
        {mutation.isError && (
          <p className="mt-2 text-xs text-red-600">Failed to create category. Please try again.</p>
        )}
      </div>
    </div>
  )
}

// ============ CREATE CLASS MODAL ============

function CreateClassModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [form, setForm] = useState({
    name: '',
    description: '',
    categoryId: '',
    room: '',
    capacity: 20,
    durationMinutes: 60,
    virtualLink: '',
    allowWaitlist: true,
    bookingCutoffMinutes: 60,
    cancellationCutoffMinutes: 120,
    allowTrial: false,
  })

  const { data: categories } = useQuery({
    queryKey: ['class-categories'],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<ClassCategoryDto[]>>('/booking/categories')
      return data.data ?? []
    },
  })

  const mutation = useMutation({
    mutationFn: async () => {
      const payload = {
        ...form,
        categoryId: form.categoryId || null,
      }
      await api.post('/booking/classes', payload)
    },
    onSuccess: onCreated,
  })

  const update = (field: string, value: unknown) => setForm((prev) => ({ ...prev, [field]: value }))

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-lg p-6 max-h-[90vh] overflow-auto">
        <h2 className="text-lg font-semibold mb-4">New Class</h2>
        <div className="space-y-3">
          <input
            type="text"
            placeholder="Class name *"
            value={form.name}
            onChange={(e) => update('name', e.target.value)}
            className="w-full px-3 py-2 border rounded-md text-sm"
          />
          <textarea
            placeholder="Description"
            value={form.description}
            onChange={(e) => update('description', e.target.value)}
            className="w-full px-3 py-2 border rounded-md text-sm"
            rows={2}
          />
          <select
            value={form.categoryId}
            onChange={(e) => update('categoryId', e.target.value)}
            className="w-full px-3 py-2 border rounded-md text-sm"
          >
            <option value="">No category</option>
            {categories?.map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-gray-500">Capacity *</label>
              <input
                type="number"
                min={1}
                value={form.capacity}
                onChange={(e) => update('capacity', parseInt(e.target.value) || 1)}
                className="w-full px-3 py-2 border rounded-md text-sm"
              />
            </div>
            <div>
              <label className="text-xs text-gray-500">Duration (min) *</label>
              <input
                type="number"
                min={1}
                value={form.durationMinutes}
                onChange={(e) => update('durationMinutes', parseInt(e.target.value) || 1)}
                className="w-full px-3 py-2 border rounded-md text-sm"
              />
            </div>
          </div>
          <input
            type="text"
            placeholder="Room"
            value={form.room}
            onChange={(e) => update('room', e.target.value)}
            className="w-full px-3 py-2 border rounded-md text-sm"
          />
          <input
            type="url"
            placeholder="Virtual meeting link (Zoom, etc.)"
            value={form.virtualLink}
            onChange={(e) => update('virtualLink', e.target.value)}
            className="w-full px-3 py-2 border rounded-md text-sm"
          />
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-gray-500">Booking cutoff (min before)</label>
              <input
                type="number"
                min={0}
                value={form.bookingCutoffMinutes}
                onChange={(e) => update('bookingCutoffMinutes', parseInt(e.target.value) || 0)}
                className="w-full px-3 py-2 border rounded-md text-sm"
              />
            </div>
            <div>
              <label className="text-xs text-gray-500">Cancel cutoff (min before)</label>
              <input
                type="number"
                min={0}
                value={form.cancellationCutoffMinutes}
                onChange={(e) => update('cancellationCutoffMinutes', parseInt(e.target.value) || 0)}
                className="w-full px-3 py-2 border rounded-md text-sm"
              />
            </div>
          </div>
          <div className="flex gap-4">
            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={form.allowWaitlist}
                onChange={(e) => update('allowWaitlist', e.target.checked)}
              />
              Allow waitlist
            </label>
            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={form.allowTrial}
                onChange={(e) => update('allowTrial', e.target.checked)}
              />
              Allow trial/guest
            </label>
          </div>
        </div>
        <div className="flex justify-end gap-2 mt-4">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">Cancel</button>
          <button
            onClick={() => mutation.mutate()}
            disabled={!form.name || mutation.isPending}
            className="px-4 py-2 text-sm bg-brand-600 text-white rounded-md hover:bg-brand-700 disabled:opacity-50"
          >
            {mutation.isPending ? 'Creating...' : 'Create'}
          </button>
        </div>
        {mutation.isError && (
          <p className="mt-2 text-xs text-red-600">Failed to create class. Please try again.</p>
        )}
      </div>
    </div>
  )
}

// ============ CREATE SCHEDULE MODAL ============

function CreateScheduleModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [classId, setClassId] = useState('')
  const [startDate, setStartDate] = useState('')
  const [startTime, setStartTime] = useState('09:00')
  const [recurrenceRule, setRecurrenceRule] = useState('NONE')
  const [recurrenceWeeks, setRecurrenceWeeks] = useState(4)

  const { data: classes } = useQuery({
    queryKey: ['class-definitions'],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<{ content: ClassDefinitionDto[] }>>('/booking/classes', {
        params: { size: 100 },
      })
      return data.data?.content ?? []
    },
  })

  const mutation = useMutation({
    mutationFn: async () => {
      const startDateTime = new Date(`${startDate}T${startTime}`).toISOString()
      await api.post('/booking/schedules', {
        classId,
        startTime: startDateTime,
        recurrenceRule: recurrenceRule !== 'NONE' ? recurrenceRule : null,
        recurrenceWeeks: recurrenceRule !== 'NONE' ? recurrenceWeeks : 1,
      })
    },
    onSuccess: onCreated,
  })

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md p-6">
        <h2 className="text-lg font-semibold mb-4">Schedule a Class</h2>
        <div className="space-y-3">
          <select
            value={classId}
            onChange={(e) => setClassId(e.target.value)}
            className="w-full px-3 py-2 border rounded-md text-sm"
          >
            <option value="">Select a class *</option>
            {classes?.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name} ({c.durationMinutes}min, cap {c.capacity})
              </option>
            ))}
          </select>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-gray-500">Date *</label>
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="w-full px-3 py-2 border rounded-md text-sm"
              />
            </div>
            <div>
              <label className="text-xs text-gray-500">Time *</label>
              <input
                type="time"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                className="w-full px-3 py-2 border rounded-md text-sm"
              />
            </div>
          </div>
          <div>
            <label className="text-xs text-gray-500">Recurrence</label>
            <select
              value={recurrenceRule}
              onChange={(e) => setRecurrenceRule(e.target.value)}
              className="w-full px-3 py-2 border rounded-md text-sm"
            >
              <option value="NONE">One-time</option>
              <option value="DAILY">Daily</option>
              <option value="WEEKLY">Weekly</option>
              <option value="BIWEEKLY">Every 2 weeks</option>
              <option value="MONTHLY">Monthly</option>
            </select>
          </div>
          {recurrenceRule !== 'NONE' && (
            <div>
              <label className="text-xs text-gray-500">Generate for how many occurrences?</label>
              <input
                type="number"
                min={1}
                max={52}
                value={recurrenceWeeks}
                onChange={(e) => setRecurrenceWeeks(parseInt(e.target.value) || 1)}
                className="w-full px-3 py-2 border rounded-md text-sm"
              />
            </div>
          )}
        </div>
        <div className="flex justify-end gap-2 mt-4">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">Cancel</button>
          <button
            onClick={() => mutation.mutate()}
            disabled={!classId || !startDate || mutation.isPending}
            className="px-4 py-2 text-sm bg-brand-600 text-white rounded-md hover:bg-brand-700 disabled:opacity-50"
          >
            {mutation.isPending ? 'Scheduling...' : 'Schedule'}
          </button>
        </div>
        {mutation.isError && (
          <p className="mt-2 text-xs text-red-600">Failed to schedule class. Please try again.</p>
        )}
      </div>
    </div>
  )
}
