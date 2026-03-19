import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../../api/client'
import type { ApiResponse, ClassScheduleDto, ClassBookingDto, MemberDto } from '../../types'

/** Format date as YYYY-MM-DD in LOCAL timezone (not UTC) */
function localDateStr(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** Get the day-of-week index (0=Mon..6=Sun) for a UTC timestamp, in local time */
function localDayIndex(isoString: string): number {
  const d = new Date(isoString)
  const day = d.getDay() // 0=Sun..6=Sat
  return day === 0 ? 6 : day - 1 // convert to 0=Mon..6=Sun
}

function getMonday(date: Date): Date {
  const d = new Date(date)
  d.setHours(12, 0, 0, 0) // noon local avoids UTC date-shift at midnight
  const day = d.getDay()
  const diff = d.getDate() - day + (day === 0 ? -6 : 1)
  d.setDate(diff)
  return d
}

export default function PortalClasses() {
  const qc = useQueryClient()
  const [tab, setTab] = useState<'schedule' | 'bookings'>('schedule')
  const [weekOffset, setWeekOffset] = useState(0)

  const { data: profileRes } = useQuery({
    queryKey: ['portal-profile'],
    queryFn: () => api.get<ApiResponse<MemberDto>>('/portal/profile').then(r => r.data),
  })
  const memberId = profileRes?.data?.id

  // Weekly schedule with navigation
  const baseMonday = getMonday(new Date())
  const weekStart = new Date(baseMonday)
  weekStart.setDate(weekStart.getDate() + weekOffset * 7)
  const weekEnd = new Date(weekStart)
  weekEnd.setDate(weekEnd.getDate() + 6)

  const weekLabel = `${weekStart.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })} — ${weekEnd.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}`
  const isCurrentWeek = weekOffset === 0

  const { data: scheduleRes, isLoading: schedulesLoading } = useQuery({
    queryKey: ['portal-schedule', weekStart.toISOString()],
    queryFn: () =>
      api.get<ApiResponse<ClassScheduleDto[]>>('/booking/schedules/weekly', {
        params: { weekStart: weekStart.toISOString() },
      }).then(r => r.data),
  })
  const rawSchedules = scheduleRes?.data
  const schedules: ClassScheduleDto[] = Array.isArray(rawSchedules)
    ? rawSchedules
    : (rawSchedules as any)?.content ?? []

  // Group schedules by local day index (0=Mon..6=Sun)
  const schedulesByDay: Record<number, ClassScheduleDto[]> = {}
  for (const s of schedules) {
    if (s.cancelled) continue
    const idx = localDayIndex(s.startTime)
    if (!schedulesByDay[idx]) schedulesByDay[idx] = []
    schedulesByDay[idx].push(s)
  }

  // My bookings
  const { data: bookingsRes } = useQuery({
    queryKey: ['portal-bookings', memberId],
    queryFn: () =>
      api.get<ApiResponse<ClassBookingDto[]>>(`/booking/bookings/member/${memberId}`).then(r => r.data),
    enabled: !!memberId,
  })
  const rawBookings = bookingsRes?.data
  const bookings: ClassBookingDto[] = Array.isArray(rawBookings)
    ? rawBookings
    : (rawBookings as any)?.content ?? []

  const bookMutation = useMutation({
    mutationFn: (scheduleId: string) =>
      api.post('/booking/bookings', { scheduleId, memberId }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['portal-bookings'] })
      qc.invalidateQueries({ queryKey: ['portal-schedule'] })
    },
  })

  const cancelBookingMutation = useMutation({
    mutationFn: (bookingId: string) =>
      api.post(`/booking/bookings/${bookingId}/cancel`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['portal-bookings'] })
      qc.invalidateQueries({ queryKey: ['portal-schedule'] })
    },
  })

  const bookedScheduleIds = new Set(
    bookings.filter(b => b.status === 'CONFIRMED').map(b => b.scheduleId)
  )

  const dayNames = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
  const now = new Date()
  const todayStr = localDateStr(now)

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Classes</h1>

      <div className="flex space-x-4 mb-6 border-b">
        <button
          onClick={() => setTab('schedule')}
          className={`pb-2 px-1 text-sm font-medium ${
            tab === 'schedule' ? 'border-b-2 border-brand-600 text-brand-600' : 'text-gray-500'
          }`}
        >
          Weekly Schedule
        </button>
        <button
          onClick={() => setTab('bookings')}
          className={`pb-2 px-1 text-sm font-medium ${
            tab === 'bookings' ? 'border-b-2 border-brand-600 text-brand-600' : 'text-gray-500'
          }`}
        >
          My Bookings ({bookings.filter(b => b.status === 'CONFIRMED').length})
        </button>
      </div>

      {tab === 'schedule' && (
        <div>
          {/* Week navigation */}
          <div className="flex items-center justify-between mb-4">
            <button
              onClick={() => setWeekOffset(w => w - 1)}
              className="px-3 py-1.5 text-sm border rounded-lg hover:bg-gray-50"
            >
              &larr; Previous
            </button>
            <div className="text-center">
              <p className="text-sm font-semibold text-gray-700">{weekLabel}</p>
              {!isCurrentWeek && (
                <button
                  onClick={() => setWeekOffset(0)}
                  className="text-xs text-brand-600 hover:text-brand-700"
                >
                  Back to this week
                </button>
              )}
            </div>
            <button
              onClick={() => setWeekOffset(w => w + 1)}
              className="px-3 py-1.5 text-sm border rounded-lg hover:bg-gray-50"
            >
              Next &rarr;
            </button>
          </div>

          {schedulesLoading ? (
            <p className="text-sm text-gray-400 text-center py-8">Loading schedule...</p>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-7 gap-2">
              {dayNames.map((day, i) => {
                const dayDate = new Date(weekStart)
                dayDate.setDate(dayDate.getDate() + i)
                const dayDateStr = localDateStr(dayDate)
                const daySchedules = schedulesByDay[i] ?? []
                const isToday = dayDateStr === todayStr
                const isPast = dayDateStr < todayStr

                return (
                  <div key={day} className={`bg-white rounded-lg shadow p-3 ${isToday ? 'ring-2 ring-brand-400' : ''} ${isPast ? 'opacity-60' : ''}`}>
                    <h3 className={`font-semibold text-sm mb-1 text-center ${isToday ? 'text-brand-600' : 'text-gray-700'}`}>
                      {day}
                    </h3>
                    <p className="text-xs text-gray-400 text-center mb-3">{dayDate.toLocaleDateString()}</p>
                    {daySchedules.length === 0 ? (
                      <p className="text-xs text-gray-400 text-center">No classes</p>
                    ) : (
                      <div className="space-y-2">
                        {daySchedules.map(s => {
                          const isBooked = bookedScheduleIds.has(s.id)
                          const isFull = s.bookedCount >= s.capacity
                          const classTime = new Date(s.startTime)
                          const isPastClass = classTime < now

                          return (
                            <div key={s.id}
                              className={`p-2 rounded text-xs border ${
                                isBooked ? 'border-brand-300 bg-brand-50' : 'border-gray-200'
                              }`}
                            >
                              <p className="font-semibold" style={{ color: s.categoryColor || '#374151' }}>
                                {s.className}
                              </p>
                              <p className="text-gray-500">
                                {classTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                              </p>
                              <p className="text-gray-500">{s.trainerName}</p>
                              <p className={`${isFull ? 'text-red-500' : 'text-gray-500'}`}>
                                {s.bookedCount}/{s.capacity}
                                {s.waitlistCount > 0 ? ` (+${s.waitlistCount} waitlist)` : ''}
                              </p>
                              {isPastClass ? (
                                <span className="text-gray-400 text-[10px]">Past</span>
                              ) : isBooked ? (
                                <span className="text-brand-600 font-medium">Booked</span>
                              ) : (
                                <button
                                  onClick={() => bookMutation.mutate(s.id)}
                                  disabled={bookMutation.isPending}
                                  className={`mt-1 w-full py-1 rounded text-white text-xs ${
                                    isFull ? 'bg-orange-500 hover:bg-orange-600' : 'bg-brand-600 hover:bg-brand-700'
                                  }`}
                                >
                                  {bookMutation.isPending ? '...' : isFull ? 'Join Waitlist' : 'Book'}
                                </button>
                              )}
                            </div>
                          )
                        })}
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          )}
        </div>
      )}

      {tab === 'bookings' && (
        <div className="space-y-3">
          {bookings.length === 0 ? (
            <p className="text-gray-500">No bookings yet. Go to the schedule to book a class!</p>
          ) : (
            bookings.map(b => (
              <div key={b.id} className="bg-white rounded-lg shadow p-4 flex items-center justify-between">
                <div>
                  <p className="font-medium">{b.className}</p>
                  <p className="text-sm text-gray-500">
                    {b.classStartTime ? new Date(b.classStartTime).toLocaleString() : ''}
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <span className={`text-xs px-2 py-1 rounded ${
                    b.status === 'CONFIRMED' ? 'bg-green-100 text-green-700' :
                    b.status === 'ATTENDED' ? 'bg-blue-100 text-blue-700' :
                    b.status === 'CANCELLED' ? 'bg-gray-100 text-gray-500' :
                    b.status === 'NO_SHOW' ? 'bg-red-100 text-red-700' :
                    'bg-gray-100 text-gray-600'
                  }`}>
                    {b.status}
                  </span>
                  {b.status === 'CONFIRMED' && (
                    <button
                      onClick={() => cancelBookingMutation.mutate(b.id)}
                      className="text-xs text-red-600 hover:text-red-800"
                    >
                      Cancel
                    </button>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  )
}
