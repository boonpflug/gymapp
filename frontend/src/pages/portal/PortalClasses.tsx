import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../../api/client'
import type { ApiResponse, ClassScheduleDto, ClassBookingDto, MemberDto } from '../../types'

export default function PortalClasses() {
  const qc = useQueryClient()
  const [tab, setTab] = useState<'schedule' | 'bookings'>('schedule')

  const { data: profileRes } = useQuery({
    queryKey: ['portal-profile'],
    queryFn: () => api.get<ApiResponse<MemberDto>>('/portal/profile').then(r => r.data),
  })
  const memberId = profileRes?.data?.id

  // Weekly schedule
  const weekStart = new Date()
  weekStart.setHours(0, 0, 0, 0)
  weekStart.setDate(weekStart.getDate() - weekStart.getDay() + 1)

  const { data: scheduleRes } = useQuery({
    queryKey: ['portal-schedule', weekStart.toISOString()],
    queryFn: () =>
      api.get<ApiResponse<ClassScheduleDto[]>>('/booking/schedules/weekly', {
        params: { weekStart: weekStart.toISOString() },
      }).then(r => r.data),
  })
  const schedules = scheduleRes?.data ?? []

  // My bookings
  const { data: bookingsRes } = useQuery({
    queryKey: ['portal-bookings', memberId],
    queryFn: () =>
      api.get<ApiResponse<ClassBookingDto[]>>(`/booking/bookings/member/${memberId}`).then(r => r.data),
    enabled: !!memberId,
  })
  const bookings = bookingsRes?.data ?? []

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

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Classes</h1>

      <div className="flex space-x-4 mb-6 border-b">
        <button
          onClick={() => setTab('schedule')}
          className={`pb-2 px-1 text-sm font-medium ${
            tab === 'schedule' ? 'border-b-2 border-emerald-600 text-emerald-600' : 'text-gray-500'
          }`}
        >
          Weekly Schedule
        </button>
        <button
          onClick={() => setTab('bookings')}
          className={`pb-2 px-1 text-sm font-medium ${
            tab === 'bookings' ? 'border-b-2 border-emerald-600 text-emerald-600' : 'text-gray-500'
          }`}
        >
          My Bookings ({bookings.filter(b => b.status === 'CONFIRMED').length})
        </button>
      </div>

      {tab === 'schedule' && (
        <div className="grid grid-cols-1 md:grid-cols-7 gap-2">
          {dayNames.map((day, i) => {
            const dayDate = new Date(weekStart)
            dayDate.setDate(dayDate.getDate() + i)
            const dayStr = dayDate.toISOString().split('T')[0]
            const daySchedules = schedules.filter(s => s.startTime.startsWith(dayStr) && !s.cancelled)

            return (
              <div key={day} className="bg-white rounded-lg shadow p-3">
                <h3 className="font-semibold text-sm text-gray-700 mb-2 text-center">{day}</h3>
                <p className="text-xs text-gray-400 text-center mb-3">{dayDate.toLocaleDateString()}</p>
                {daySchedules.length === 0 ? (
                  <p className="text-xs text-gray-400 text-center">No classes</p>
                ) : (
                  <div className="space-y-2">
                    {daySchedules.map(s => {
                      const isBooked = bookedScheduleIds.has(s.id)
                      const isFull = s.bookedCount >= s.capacity
                      return (
                        <div key={s.id}
                          className={`p-2 rounded text-xs border ${
                            isBooked ? 'border-emerald-300 bg-emerald-50' : 'border-gray-200'
                          }`}
                        >
                          <p className="font-semibold" style={{ color: s.categoryColor || '#374151' }}>
                            {s.className}
                          </p>
                          <p className="text-gray-500">
                            {new Date(s.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          </p>
                          <p className="text-gray-500">{s.trainerName}</p>
                          <p className={`${isFull ? 'text-red-500' : 'text-gray-500'}`}>
                            {s.bookedCount}/{s.capacity}
                            {s.waitlistCount > 0 ? ` (+${s.waitlistCount} waitlist)` : ''}
                          </p>
                          {isBooked ? (
                            <span className="text-emerald-600 font-medium">Booked</span>
                          ) : (
                            <button
                              onClick={() => bookMutation.mutate(s.id)}
                              disabled={bookMutation.isPending}
                              className={`mt-1 w-full py-1 rounded text-white text-xs ${
                                isFull ? 'bg-orange-500 hover:bg-orange-600' : 'bg-emerald-600 hover:bg-emerald-700'
                              }`}
                            >
                              {isFull ? 'Join Waitlist' : 'Book'}
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

      {tab === 'bookings' && (
        <div className="space-y-3">
          {bookings.length === 0 ? (
            <p className="text-gray-500">No bookings yet.</p>
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
