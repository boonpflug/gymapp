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
      <div className="mb-8">
        <h1 className="text-2xl font-semibold text-gray-900">Dashboard</h1>
        <p className="text-sm text-gray-500 mt-1">Overview of your studio performance</p>
      </div>

      {/* Primary Stats */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        <StatCard
          title="Active Members"
          value={dash?.totalActiveMembers ?? '—'}
          icon="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z"
          accent="brand"
        />
        <StatCard
          title="Today's Check-ins"
          value={dash?.totalCheckInsToday ?? '—'}
          icon="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
          accent="emerald"
        />
        <StatCard
          title="Revenue (Month)"
          value={dash?.totalRevenueThisMonth != null ? `€${Number(dash.totalRevenueThisMonth).toLocaleString('de-DE', { minimumFractionDigits: 2 })}` : '—'}
          icon="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
          accent="blue"
        />
        <StatCard
          title="Outstanding"
          value={dash?.totalOutstandingPayments != null ? `€${Number(dash.totalOutstandingPayments).toLocaleString('de-DE', { minimumFractionDigits: 2 })}` : '—'}
          icon="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
          accent="amber"
        />
      </div>

      {/* Secondary Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5 mt-5">
        <StatCard title="Facilities" value={dash?.totalFacilities ?? '—'} accent="brand" icon="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
        <StatCard title="New Members (Month)" value={dash?.totalNewMembersThisMonth ?? '—'} accent="emerald" icon="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
        <StatCard title="Classes Today" value={todayClasses.filter(c => !c.cancelled).length} accent="blue" icon="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mt-8">
        {/* Recent Check-ins */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
          <h2 className="text-base font-semibold text-gray-900 mb-4">Recent Check-ins</h2>
          {recentCheckins.length === 0 ? (
            <p className="text-gray-400 text-sm">No recent check-ins.</p>
          ) : (
            <div className="space-y-3">
              {recentCheckins.map(c => (
                <div key={c.id} className="flex items-center justify-between py-2 border-b border-gray-50 last:border-0">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-full bg-brand-50 flex items-center justify-center">
                      <span className="text-brand-700 text-xs font-medium">{(c.memberName ?? '?')[0]}</span>
                    </div>
                    <div>
                      <p className="text-sm font-medium text-gray-900">{c.memberName ?? 'Unknown'}</p>
                      <p className="text-xs text-gray-400">{c.memberNumber}</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                      c.status === 'SUCCESS' ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-700'
                    }`}>{c.status}</span>
                    <p className="text-gray-400 text-xs mt-1">
                      {new Date(c.checkInTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Today's Classes */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
          <h2 className="text-base font-semibold text-gray-900 mb-4">Today's Classes</h2>
          {todayClasses.length === 0 ? (
            <p className="text-gray-400 text-sm">No classes scheduled today.</p>
          ) : (
            <div className="space-y-3">
              {todayClasses.filter(c => !c.cancelled).map(c => (
                <div key={c.id} className="flex items-center justify-between py-2 border-b border-gray-50 last:border-0">
                  <div className="flex items-center gap-3">
                    <div className="w-2 h-8 rounded-full" style={{ backgroundColor: c.categoryColor || '#0d9480' }} />
                    <div>
                      <p className="text-sm font-medium text-gray-900">{c.className}</p>
                      <p className="text-xs text-gray-400">{c.trainerName}{c.room ? ` — ${c.room}` : ''}</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="text-sm font-medium text-gray-900">{new Date(c.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</p>
                    <p className="text-xs text-gray-400">{c.bookedCount}/{c.capacity} booked</p>
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
          <h2 className="text-base font-semibold text-gray-900 mb-4">Facility Overview</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {dash.facilitySummaries.map(s => (
              <div key={s.facility.id} className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
                <div className="flex items-center gap-2.5 mb-3">
                  <div className="w-3 h-3 rounded-full" style={{ backgroundColor: s.facility.brandColor || '#0d9480' }} />
                  <h3 className="font-semibold text-gray-900">{s.facility.name}</h3>
                </div>
                <div className="grid grid-cols-2 gap-3 text-sm">
                  <div><p className="text-gray-400 text-xs">Members</p><p className="font-semibold text-gray-900">{s.activeMembers}</p></div>
                  <div><p className="text-gray-400 text-xs">Check-ins</p><p className="font-semibold text-gray-900">{s.checkInsToday}</p></div>
                  <div><p className="text-gray-400 text-xs">Occupancy</p><p className="font-semibold text-gray-900">{s.currentOccupancy}{s.facility.maxOccupancy ? `/${s.facility.maxOccupancy}` : ''}</p></div>
                  <div><p className="text-gray-400 text-xs">Revenue</p><p className="font-semibold text-emerald-600">€{Number(s.revenueThisMonth ?? 0).toFixed(0)}</p></div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

function StatCard({ title, value, icon, accent }: { title: string; value: string | number; icon: string; accent: string }) {
  const accentMap: Record<string, { bg: string; iconBg: string; iconText: string; valueText: string }> = {
    brand:   { bg: 'bg-white', iconBg: 'bg-brand-50',   iconText: 'text-brand-600',   valueText: 'text-gray-900' },
    emerald: { bg: 'bg-white', iconBg: 'bg-emerald-50', iconText: 'text-emerald-600', valueText: 'text-gray-900' },
    blue:    { bg: 'bg-white', iconBg: 'bg-blue-50',    iconText: 'text-blue-600',    valueText: 'text-gray-900' },
    amber:   { bg: 'bg-white', iconBg: 'bg-amber-50',   iconText: 'text-amber-600',   valueText: 'text-gray-900' },
  }
  const a = accentMap[accent] ?? accentMap.brand

  return (
    <div className={`${a.bg} rounded-xl shadow-sm border border-gray-100 p-5`}>
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-gray-500">{title}</p>
          <p className={`text-2xl font-semibold mt-1 ${a.valueText}`}>{value}</p>
        </div>
        <div className={`w-10 h-10 rounded-xl ${a.iconBg} flex items-center justify-center`}>
          <svg className={`w-5 h-5 ${a.iconText}`} fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={1.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d={icon} />
          </svg>
        </div>
      </div>
    </div>
  )
}
