import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import api from '../api/client'
import type {
  RevenueReportDto,
  MemberReportDto,
  CheckInReportDto,
  ClassReportDto,
} from '../types'

type Tab = 'revenue' | 'members' | 'checkins' | 'classes'

const defaultFrom = new Date(new Date().getFullYear(), new Date().getMonth() - 2, 1)
  .toISOString()
  .slice(0, 10)
const defaultTo = new Date().toISOString().slice(0, 10)

export default function ReportsPage() {
  const [tab, setTab] = useState<Tab>('revenue')
  const [from, setFrom] = useState(defaultFrom)
  const [to, setTo] = useState(defaultTo)

  const tabs: { key: Tab; label: string }[] = [
    { key: 'revenue', label: 'Revenue' },
    { key: 'members', label: 'Members' },
    { key: 'checkins', label: 'Check-ins' },
    { key: 'classes', label: 'Classes' },
  ]

  const handleExport = async () => {
    const typeMap: Record<Tab, string> = {
      revenue: 'REVENUE',
      members: 'MEMBERS',
      checkins: 'CHECKINS',
      classes: 'CLASSES',
    }
    try {
      const response = await api.get('/reports/export', {
        params: { type: typeMap[tab], from, to, format: 'CSV' },
        responseType: 'blob',
      })
      const blob = new Blob([response.data], { type: 'text/csv' })
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `Report_${typeMap[tab]}_${from}_${to}.csv`
      document.body.appendChild(a)
      a.click()
      a.remove()
      window.URL.revokeObjectURL(url)
    } catch {
      /* ignore */
    }
  }

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">Reports</h1>
          <p className="text-sm text-gray-500 mt-1">Analytics and reporting dashboard</p>
        </div>
        <button
          onClick={handleExport}
          className="inline-flex items-center gap-2 px-4 py-2 bg-emerald-600 text-white rounded-lg text-sm font-medium hover:bg-emerald-700 transition-colors"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
          </svg>
          Export CSV
        </button>
      </div>

      {/* Date range */}
      <div className="flex items-center gap-4 mb-6">
        <div className="flex items-center gap-2">
          <label className="text-sm text-gray-600 font-medium">From</label>
          <input
            type="date"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
            className="border rounded-lg px-3 py-2 text-sm"
          />
        </div>
        <div className="flex items-center gap-2">
          <label className="text-sm text-gray-600 font-medium">To</label>
          <input
            type="date"
            value={to}
            onChange={(e) => setTo(e.target.value)}
            className="border rounded-lg px-3 py-2 text-sm"
          />
        </div>
      </div>

      {/* Tabs */}
      <div className="border-b mb-6">
        <div className="flex gap-6">
          {tabs.map((t) => (
            <button
              key={t.key}
              onClick={() => setTab(t.key)}
              className={`pb-3 text-sm font-medium border-b-2 transition-colors ${
                tab === t.key
                  ? 'border-emerald-600 text-emerald-700'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>
      </div>

      {tab === 'revenue' && <RevenueTab from={from} to={to} />}
      {tab === 'members' && <MembersTab from={from} to={to} />}
      {tab === 'checkins' && <CheckinsTab from={from} to={to} />}
      {tab === 'classes' && <ClassesTab from={from} to={to} />}
    </div>
  )
}

// ── KPI Card ────────────────────────────────────────────

function KpiCard({ label, value, sub }: { label: string; value: string | number; sub?: string }) {
  return (
    <div className="bg-white rounded-xl border p-5">
      <p className="text-xs text-gray-500 font-medium uppercase tracking-wide">{label}</p>
      <p className="text-2xl font-bold text-gray-900 mt-1">{value}</p>
      {sub && <p className="text-xs text-gray-400 mt-1">{sub}</p>}
    </div>
  )
}

// ── Revenue Tab ─────────────────────────────────────────

function RevenueTab({ from, to }: { from: string; to: string }) {
  const { data, isLoading } = useQuery({
    queryKey: ['report-revenue', from, to],
    queryFn: () => api.get<RevenueReportDto>(`/reports/revenue`, { params: { from, to } }).then((r) => r.data),
    enabled: !!from && !!to,
  })

  if (isLoading) return <p className="text-sm text-gray-500">Loading...</p>
  if (!data) return <p className="text-sm text-gray-400">No data available</p>

  const fmt = (v: number) =>
    new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(v)

  return (
    <div>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
        <KpiCard label="Monthly Recurring Revenue" value={fmt(data.mrr)} />
        <KpiCard label="Total Revenue" value={fmt(data.totalRevenue)} sub="In selected period" />
        <KpiCard label="Payment Success Rate" value={`${data.paymentSuccessRate}%`} />
        <KpiCard label="Outstanding Receivables" value={fmt(data.outstandingReceivables)} />
      </div>

      <div className="bg-white rounded-xl border overflow-hidden">
        <div className="px-5 py-4 border-b">
          <h3 className="text-sm font-semibold text-gray-900">Monthly Revenue Breakdown</h3>
        </div>
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-gray-50 text-left">
              <th className="px-5 py-3 font-medium text-gray-600">Month</th>
              <th className="px-5 py-3 font-medium text-gray-600 text-right">Revenue</th>
              <th className="px-5 py-3 font-medium text-gray-600 text-right">Invoices</th>
              <th className="px-5 py-3 font-medium text-gray-600 text-right">Paid</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {data.monthlyRevenue?.map((m) => (
              <tr key={m.month} className="hover:bg-gray-50">
                <td className="px-5 py-3 text-gray-900 font-medium">{m.month}</td>
                <td className="px-5 py-3 text-gray-700 text-right">{fmt(m.revenue)}</td>
                <td className="px-5 py-3 text-gray-700 text-right">{m.invoiceCount}</td>
                <td className="px-5 py-3 text-gray-700 text-right">{m.paidCount}</td>
              </tr>
            ))}
            {(!data.monthlyRevenue || data.monthlyRevenue.length === 0) && (
              <tr>
                <td colSpan={4} className="px-5 py-8 text-center text-gray-400">
                  No revenue data for this period
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}

// ── Members Tab ─────────────────────────────────────────

function MembersTab({ from, to }: { from: string; to: string }) {
  const { data, isLoading } = useQuery({
    queryKey: ['report-members', from, to],
    queryFn: () => api.get<MemberReportDto>(`/reports/members`, { params: { from, to } }).then((r) => r.data),
    enabled: !!from && !!to,
  })

  if (isLoading) return <p className="text-sm text-gray-500">Loading...</p>
  if (!data) return <p className="text-sm text-gray-400">No data available</p>

  return (
    <div>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
        <KpiCard label="Active Members" value={data.totalActive} />
        <KpiCard label="Inactive Members" value={data.totalInactive} />
        <KpiCard label="New in Period" value={data.newThisMonth} />
        <KpiCard label="Churn Rate" value={`${data.churnRate}%`} />
      </div>

      <div className="bg-white rounded-xl border overflow-hidden">
        <div className="px-5 py-4 border-b">
          <h3 className="text-sm font-semibold text-gray-900">Cancellation Reasons</h3>
        </div>
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-gray-50 text-left">
              <th className="px-5 py-3 font-medium text-gray-600">Reason</th>
              <th className="px-5 py-3 font-medium text-gray-600 text-right">Count</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {data.cancellationReasons && Object.entries(data.cancellationReasons).map(([reason, count]) => (
              <tr key={reason} className="hover:bg-gray-50">
                <td className="px-5 py-3 text-gray-900">{reason}</td>
                <td className="px-5 py-3 text-gray-700 text-right">{count}</td>
              </tr>
            ))}
            {(!data.cancellationReasons || Object.keys(data.cancellationReasons).length === 0) && (
              <tr>
                <td colSpan={2} className="px-5 py-8 text-center text-gray-400">
                  No cancellations in this period
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}

// ── Check-ins Tab ───────────────────────────────────────

function CheckinsTab({ from, to }: { from: string; to: string }) {
  const { data, isLoading } = useQuery({
    queryKey: ['report-checkins', from, to],
    queryFn: () => api.get<CheckInReportDto>(`/reports/checkins`, { params: { from, to } }).then((r) => r.data),
    enabled: !!from && !!to,
  })

  if (isLoading) return <p className="text-sm text-gray-500">Loading...</p>
  if (!data) return <p className="text-sm text-gray-400">No data available</p>

  return (
    <div>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5 mb-8">
        <KpiCard label="Total Check-ins" value={data.totalCheckIns.toLocaleString()} />
        <KpiCard label="Average Daily" value={data.avgDaily} />
        <KpiCard label="Peak Hours" value={Object.keys(data.peakHours ?? {}).length > 0 ? `${Object.keys(data.peakHours)[0]}:00` : '-'} sub="Most active hour" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        {/* Peak Hours */}
        <div className="bg-white rounded-xl border overflow-hidden">
          <div className="px-5 py-4 border-b">
            <h3 className="text-sm font-semibold text-gray-900">Check-ins by Hour</h3>
          </div>
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 text-left">
                <th className="px-5 py-3 font-medium text-gray-600">Hour</th>
                <th className="px-5 py-3 font-medium text-gray-600 text-right">Count</th>
                <th className="px-5 py-3 font-medium text-gray-600 w-1/2">Distribution</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {data.peakHours && (() => {
                const maxVal = Math.max(...Object.values(data.peakHours), 1)
                return Object.entries(data.peakHours).map(([hour, count]) => (
                  <tr key={hour} className="hover:bg-gray-50">
                    <td className="px-5 py-2.5 text-gray-900 font-medium">{String(hour).padStart(2, '0')}:00</td>
                    <td className="px-5 py-2.5 text-gray-700 text-right">{count}</td>
                    <td className="px-5 py-2.5">
                      <div className="h-3 bg-gray-100 rounded-full overflow-hidden">
                        <div
                          className="h-full bg-emerald-500 rounded-full"
                          style={{ width: `${(count / maxVal) * 100}%` }}
                        />
                      </div>
                    </td>
                  </tr>
                ))
              })()}
            </tbody>
          </table>
        </div>

        {/* Peak Days */}
        <div className="bg-white rounded-xl border overflow-hidden">
          <div className="px-5 py-4 border-b">
            <h3 className="text-sm font-semibold text-gray-900">Check-ins by Day of Week</h3>
          </div>
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 text-left">
                <th className="px-5 py-3 font-medium text-gray-600">Day</th>
                <th className="px-5 py-3 font-medium text-gray-600 text-right">Count</th>
                <th className="px-5 py-3 font-medium text-gray-600 w-1/2">Distribution</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {data.peakDays && (() => {
                const maxVal = Math.max(...Object.values(data.peakDays), 1)
                return Object.entries(data.peakDays).map(([day, count]) => (
                  <tr key={day} className="hover:bg-gray-50">
                    <td className="px-5 py-2.5 text-gray-900 font-medium">{day}</td>
                    <td className="px-5 py-2.5 text-gray-700 text-right">{count}</td>
                    <td className="px-5 py-2.5">
                      <div className="h-3 bg-gray-100 rounded-full overflow-hidden">
                        <div
                          className="h-full bg-emerald-500 rounded-full"
                          style={{ width: `${(count / maxVal) * 100}%` }}
                        />
                      </div>
                    </td>
                  </tr>
                ))
              })()}
            </tbody>
          </table>
        </div>
      </div>

      {/* Top Members */}
      <div className="bg-white rounded-xl border overflow-hidden">
        <div className="px-5 py-4 border-b">
          <h3 className="text-sm font-semibold text-gray-900">Top 10 Most Active Members</h3>
        </div>
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-gray-50 text-left">
              <th className="px-5 py-3 font-medium text-gray-600">#</th>
              <th className="px-5 py-3 font-medium text-gray-600">Member</th>
              <th className="px-5 py-3 font-medium text-gray-600 text-right">Check-ins</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {data.topMembers?.map((m, i) => (
              <tr key={m.memberId} className="hover:bg-gray-50">
                <td className="px-5 py-3 text-gray-500">{i + 1}</td>
                <td className="px-5 py-3 text-gray-900 font-medium">{m.memberName}</td>
                <td className="px-5 py-3 text-gray-700 text-right">{m.checkInCount}</td>
              </tr>
            ))}
            {(!data.topMembers || data.topMembers.length === 0) && (
              <tr>
                <td colSpan={3} className="px-5 py-8 text-center text-gray-400">
                  No check-in data for this period
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}

// ── Classes Tab ─────────────────────────────────────────

function ClassesTab({ from, to }: { from: string; to: string }) {
  const { data, isLoading } = useQuery({
    queryKey: ['report-classes', from, to],
    queryFn: () => api.get<ClassReportDto>(`/reports/classes`, { params: { from, to } }).then((r) => r.data),
    enabled: !!from && !!to,
  })

  if (isLoading) return <p className="text-sm text-gray-500">Loading...</p>
  if (!data) return <p className="text-sm text-gray-400">No data available</p>

  return (
    <div>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5 mb-8">
        <KpiCard label="Total Classes" value={data.totalClasses} />
        <KpiCard label="Avg Attendance Rate" value={`${data.avgAttendanceRate}%`} />
        <KpiCard
          label="Most Popular"
          value={data.mostPopular?.[0]?.className ?? '-'}
          sub={data.mostPopular?.[0] ? `${data.mostPopular[0].totalBookings} bookings` : undefined}
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Most Popular */}
        <div className="bg-white rounded-xl border overflow-hidden">
          <div className="px-5 py-4 border-b">
            <h3 className="text-sm font-semibold text-gray-900">Most Popular Classes</h3>
          </div>
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 text-left">
                <th className="px-5 py-3 font-medium text-gray-600">Class</th>
                <th className="px-5 py-3 font-medium text-gray-600 text-right">Bookings</th>
                <th className="px-5 py-3 font-medium text-gray-600 text-right">Avg Attend.</th>
                <th className="px-5 py-3 font-medium text-gray-600 text-right">Utilization</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {data.mostPopular?.map((c) => (
                <tr key={c.className} className="hover:bg-gray-50">
                  <td className="px-5 py-3 text-gray-900 font-medium">{c.className}</td>
                  <td className="px-5 py-3 text-gray-700 text-right">{c.totalBookings}</td>
                  <td className="px-5 py-3 text-gray-700 text-right">{c.avgAttendance}</td>
                  <td className="px-5 py-3 text-right">
                    <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${
                      c.capacityUtilization >= 80 ? 'bg-emerald-100 text-emerald-700' :
                      c.capacityUtilization >= 50 ? 'bg-yellow-100 text-yellow-700' :
                      'bg-red-100 text-red-700'
                    }`}>
                      {c.capacityUtilization}%
                    </span>
                  </td>
                </tr>
              ))}
              {(!data.mostPopular || data.mostPopular.length === 0) && (
                <tr>
                  <td colSpan={4} className="px-5 py-8 text-center text-gray-400">No class data</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Least Popular */}
        <div className="bg-white rounded-xl border overflow-hidden">
          <div className="px-5 py-4 border-b">
            <h3 className="text-sm font-semibold text-gray-900">Least Popular Classes</h3>
          </div>
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 text-left">
                <th className="px-5 py-3 font-medium text-gray-600">Class</th>
                <th className="px-5 py-3 font-medium text-gray-600 text-right">Bookings</th>
                <th className="px-5 py-3 font-medium text-gray-600 text-right">Avg Attend.</th>
                <th className="px-5 py-3 font-medium text-gray-600 text-right">Utilization</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {data.leastPopular?.map((c) => (
                <tr key={c.className} className="hover:bg-gray-50">
                  <td className="px-5 py-3 text-gray-900 font-medium">{c.className}</td>
                  <td className="px-5 py-3 text-gray-700 text-right">{c.totalBookings}</td>
                  <td className="px-5 py-3 text-gray-700 text-right">{c.avgAttendance}</td>
                  <td className="px-5 py-3 text-right">
                    <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${
                      c.capacityUtilization >= 80 ? 'bg-emerald-100 text-emerald-700' :
                      c.capacityUtilization >= 50 ? 'bg-yellow-100 text-yellow-700' :
                      'bg-red-100 text-red-700'
                    }`}>
                      {c.capacityUtilization}%
                    </span>
                  </td>
                </tr>
              ))}
              {(!data.leastPopular || data.leastPopular.length === 0) && (
                <tr>
                  <td colSpan={4} className="px-5 py-8 text-center text-gray-400">No class data</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
