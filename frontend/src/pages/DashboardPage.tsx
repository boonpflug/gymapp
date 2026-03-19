import { useQuery } from '@tanstack/react-query'
import api from '../api/client'
import type { ApiResponse, CheckInDto, ClassScheduleDto, ConsolidatedDashboardDto } from '../types'

export default function DashboardPage() {
  const { data: dashRes } = useQuery({
    queryKey: ['admin-dashboard'],
    queryFn: () => api.get<ApiResponse<ConsolidatedDashboardDto>>('/facilities/dashboard/consolidated').then(r => r.data),
    refetchInterval: 30000,
  })
  const dash = dashRes?.data

  const { data: checkinsRes } = useQuery({
    queryKey: ['dashboard-recent-checkins'],
    queryFn: () => api.get<ApiResponse<{ content: CheckInDto[] }>>('/checkin/recent', { params: { page: 0, size: 5 } }).then(r => r.data),
  })
  const recentCheckins: CheckInDto[] = checkinsRes?.data?.content ?? (Array.isArray(checkinsRes?.data) ? checkinsRes!.data as unknown as CheckInDto[] : [])

  const weekStart = new Date()
  weekStart.setHours(0, 0, 0, 0)

  const { data: scheduleRes } = useQuery({
    queryKey: ['dashboard-schedule'],
    queryFn: () => api.get<ApiResponse<ClassScheduleDto[]>>('/booking/schedules/range', {
      params: { from: weekStart.toISOString(), to: new Date(weekStart.getTime() + 86400000).toISOString() },
    }).then(r => r.data),
  })
  const rawSchedules = scheduleRes?.data
  const todayClasses: ClassScheduleDto[] = Array.isArray(rawSchedules) ? rawSchedules : []

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Dashboard</h1>

      {/* Stats cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard title="Active Members" value={dash?.totalActiveMembers ?? '—'} color="indigo" />
        <StatCard title="Today's Check-ins" value={dash?.totalCheckInsToday ?? '—'} color="green" />
        <StatCard
          title="Revenue (Month)"
          value={dash?.totalRevenueThisMonth != null ? `€${Number(dash.totalRevenueThisMonth).toLocaleString('de-DE', { minimumFractionDigits: 2 })}` : '—'}
          color="blue"
        />
        <StatCard
          title="Outstanding"
          value={dash?.totalOutstandingPayments != null ? `€${Number(dash.totalOutstandingPayments).toLocaleString('de-DE', { minimumFractionDigits: 2 })}` : '—'}
          color="red"
        />
      </div>

      {/* Secondary stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-6">
        <StatCard title="Facilities" value={dash?.totalFacilities ?? '—'} color="indigo" />
        <StatCard title="New Members (Month)" value={dash?.totalNewMembersThisMonth ?? '—'} color="green" />
        <StatCard title="Classes Today" value={todayClasses.filter(c => !c.cancelled).length} color="blue" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mt-8">
        {/* Recent Check-ins */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-lg font-semibold text-gray-700 mb-4">Recent Check-ins</h2>
          {recentCheckins.length === 0 ? (
            <p className="text-gray-400 text-sm">No recent check-ins.</p>
          ) : (
            <div className="space-y-3">
              {recentCheckins.map(c => (
                <div key={c.id} className="flex items-center justify-between text-sm">
                  <div>
                    <p className="font-medium">{c.memberName ?? 'Unknown'}</p>
                    <p className="text-gray-400 text-xs">{c.memberNumber}</p>
                  </div>
                  <div className="text-right">
                    <span className={`text-xs px-2 py-0.5 rounded ${
                      c.status === 'SUCCESS' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                    }`}>{c.status}</span>
                    <p className="text-gray-400 text-xs mt-1">
                      {new Date(c.checkInTime).toLocaleDateString()} {new Date(c.checkInTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Today's Classes */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-lg font-semibold text-gray-700 mb-4">Today's Classes</h2>
          {todayClasses.length === 0 ? (
            <p className="text-gray-400 text-sm">No classes scheduled today.</p>
          ) : (
            <div className="space-y-3">
              {todayClasses.filter(c => !c.cancelled).map(c => (
                <div key={c.id} className="flex items-center justify-between text-sm">
                  <div>
                    <p className="font-medium" style={{ color: c.categoryColor || '#374151' }}>{c.className}</p>
                    <p className="text-gray-400 text-xs">{c.trainerName} — {c.room}</p>
                  </div>
                  <div className="text-right">
                    <p className="font-medium">{new Date(c.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</p>
                    <p className="text-gray-400 text-xs">{c.bookedCount}/{c.capacity} booked</p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Per-facility breakdown */}
      {dash && dash.facilitySummaries && dash.facilitySummaries.length > 1 && (
        <div className="mt-8">
          <h2 className="text-lg font-semibold text-gray-700 mb-4">Facility Overview</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {dash.facilitySummaries.map(s => (
              <div key={s.facility.id} className="bg-white rounded-lg shadow p-4">
                <div className="flex items-center gap-2 mb-2">
                  {s.facility.brandColor && <span className="w-3 h-3 rounded-full" style={{ backgroundColor: s.facility.brandColor }} />}
                  <h3 className="font-semibold">{s.facility.name}</h3>
                </div>
                <div className="grid grid-cols-2 gap-2 text-sm">
                  <div><p className="text-gray-400">Members</p><p className="font-semibold">{s.activeMembers}</p></div>
                  <div><p className="text-gray-400">Check-ins</p><p className="font-semibold">{s.checkInsToday}</p></div>
                  <div><p className="text-gray-400">Occupancy</p><p className="font-semibold">{s.currentOccupancy}{s.facility.maxOccupancy ? `/${s.facility.maxOccupancy}` : ''}</p></div>
                  <div><p className="text-gray-400">Revenue</p><p className="font-semibold text-green-600">€{Number(s.revenueThisMonth ?? 0).toFixed(0)}</p></div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

function StatCard({ title, value, color }: { title: string; value: string | number; color: string }) {
  const colorClasses: Record<string, string> = {
    indigo: 'text-indigo-700',
    green: 'text-green-700',
    blue: 'text-blue-700',
    red: 'text-red-700',
  }

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <p className="text-sm text-gray-500">{title}</p>
      <p className={`text-3xl font-bold mt-2 ${colorClasses[color] || ''}`}>{value}</p>
    </div>
  )
}
