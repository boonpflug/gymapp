import { useState, useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../api/client'
import { useAuthStore } from '../store/authStore'
import type { ApiResponse, CheckInDto, OccupancyDto, MemberDto, AccessEventDto } from '../types'
import SockJS from 'sockjs-client/dist/sockjs'
import { Client } from '@stomp/stompjs'

export default function CheckInPage() {
  const { t } = useTranslation()
  const [search, setSearch] = useState('')
  const [eventLog, setEventLog] = useState<AccessEventDto[]>([])
  const queryClient = useQueryClient()
  const tenantId = useAuthStore((s) => s.tenantId)
  const stompRef = useRef<Client | null>(null)

  // WebSocket for live occupancy and event feed
  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      onConnect: () => {
        client.subscribe(`/topic/occupancy/${tenantId}`, () => {
          queryClient.invalidateQueries({ queryKey: ['occupancy'] })
        })
        client.subscribe(`/topic/access-events/${tenantId}`, (msg) => {
          const event: AccessEventDto = JSON.parse(msg.body)
          setEventLog((prev) => [event, ...prev].slice(0, 50))
        })
      },
    })
    client.activate()
    stompRef.current = client
    return () => {
      client.deactivate()
    }
  }, [tenantId, queryClient])

  // Occupancy
  const { data: occupancyData } = useQuery({
    queryKey: ['occupancy'],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<OccupancyDto>>('/checkin/occupancy')
      return data.data
    },
    refetchInterval: 30000,
  })

  // Member search for check-in
  const { data: memberData, isLoading: membersLoading } = useQuery({
    queryKey: ['members-checkin', search],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<MemberDto[]>>('/members', {
        params: { name: search || undefined, size: 10 },
      })
      return data
    },
    enabled: search.length >= 2,
  })

  // Recent check-ins
  const { data: recentData } = useQuery({
    queryKey: ['recent-checkins'],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<{ content: CheckInDto[] }>>('/checkin/recent', {
        params: { size: 15 },
      })
      return data.data?.content ?? []
    },
    refetchInterval: 10000,
  })

  // Manual check-in mutation
  const checkInMutation = useMutation({
    mutationFn: async (memberId: string) => {
      const { data } = await api.post<ApiResponse<CheckInDto>>('/checkin/manual', { memberId })
      return data.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recent-checkins'] })
      queryClient.invalidateQueries({ queryKey: ['occupancy'] })
      setSearch('')
    },
  })

  // Check-out mutation
  const checkOutMutation = useMutation({
    mutationFn: async (memberId: string) => {
      await api.post('/checkin/checkout', { memberId })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recent-checkins'] })
      queryClient.invalidateQueries({ queryKey: ['occupancy'] })
    },
  })

  const members = memberData?.data ?? []
  const recentCheckIns = recentData ?? []

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-800">{t('checkin.title')}</h1>
        <OccupancyBadge occupancy={occupancyData} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left column: search + check-in */}
        <div className="lg:col-span-2 space-y-6">
          {/* Member search */}
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-lg font-semibold text-gray-700 mb-4">{t('checkin.manualCheckin')}</h2>
            <input
              type="text"
              placeholder={t('checkin.searchMember')}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-brand-500"
            />

            {search.length >= 2 && (
              <div className="mt-3 border rounded-md divide-y max-h-64 overflow-auto">
                {membersLoading ? (
                  <div className="px-4 py-3 text-sm text-gray-500">{t('checkin.searching')}</div>
                ) : members.length === 0 ? (
                  <div className="px-4 py-3 text-sm text-gray-500">{t('checkin.noMembersFound')}</div>
                ) : (
                  members.map((member) => (
                    <div
                      key={member.id}
                      className="flex items-center justify-between px-4 py-3 hover:bg-gray-50"
                    >
                      <div>
                        <div className="text-sm font-medium text-gray-900">
                          {member.firstName} {member.lastName}
                        </div>
                        <div className="text-xs text-gray-500">
                          #{member.memberNumber} &middot; {member.email}
                        </div>
                      </div>
                      <button
                        onClick={() => checkInMutation.mutate(member.id)}
                        disabled={checkInMutation.isPending}
                        className="bg-green-600 text-white px-4 py-1.5 rounded-md text-sm hover:bg-green-700 disabled:opacity-50"
                      >
                        {t('checkin.checkIn')}
                      </button>
                    </div>
                  ))
                )}
              </div>
            )}

            {checkInMutation.isSuccess && checkInMutation.data && (
              <div
                className={`mt-3 p-3 rounded-md text-sm ${
                  checkInMutation.data.status === 'SUCCESS'
                    ? 'bg-green-50 text-green-800'
                    : 'bg-red-50 text-red-800'
                }`}
              >
                {checkInMutation.data.status === 'SUCCESS'
                  ? t('checkin.checkedInSuccess', { name: checkInMutation.data.memberName })
                  : t('checkin.denied', { reason: checkInMutation.data.denialReason })}
              </div>
            )}
            {checkInMutation.isError && (
              <div className="mt-3 p-3 rounded-md text-sm bg-red-50 text-red-800">
                {t('checkin.checkInFailed')}
              </div>
            )}
          </div>

          {/* Recent check-ins */}
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <div className="px-6 py-4 border-b">
              <h2 className="text-lg font-semibold text-gray-700">{t('checkin.recentCheckins')}</h2>
            </div>
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    {t('checkin.member')}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    {t('checkin.method')}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    {t('checkin.status')}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    {t('checkin.time')}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    {t('checkin.action')}
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {recentCheckIns.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-6 py-4 text-center text-gray-500 text-sm">
                      {t('checkin.noRecentCheckins')}
                    </td>
                  </tr>
                ) : (
                  recentCheckIns.map((ci) => (
                    <tr key={ci.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 text-sm">
                        <div className="font-medium text-gray-900">{ci.memberName}</div>
                        <div className="text-xs text-gray-500">#{ci.memberNumber}</div>
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-500">{ci.method}</td>
                      <td className="px-6 py-4">
                        <CheckInStatusBadge status={ci.status} reason={ci.denialReason} />
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-500">
                        <div>{new Date(ci.checkInTime).toLocaleDateString()}</div>
                        <div className="text-xs">{new Date(ci.checkInTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</div>
                      </td>
                      <td className="px-6 py-4">
                        {ci.status === 'SUCCESS' && !ci.checkOutTime && (
                          <button
                            onClick={() => checkOutMutation.mutate(ci.memberId)}
                            className="text-sm text-brand-600 hover:text-brand-700"
                          >
                            {t('checkin.checkOut')}
                          </button>
                        )}
                        {ci.checkOutTime && (
                          <span className="text-xs text-gray-400">
                            {t('checkin.out')} {new Date(ci.checkOutTime).toLocaleTimeString()}
                          </span>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Right column: event monitor */}
        <div className="space-y-6">
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <div className="px-4 py-3 border-b bg-gray-50">
              <h2 className="text-sm font-semibold text-gray-700">{t('checkin.liveEventMonitor')}</h2>
            </div>
            <div className="max-h-[600px] overflow-auto divide-y">
              {eventLog.length === 0 ? (
                <div className="px-4 py-8 text-center text-sm text-gray-400">
                  {t('checkin.waitingForEvents')}
                </div>
              ) : (
                eventLog.map((event) => (
                  <div key={event.id} className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <EventTypeDot type={event.eventType} />
                      <span className="text-sm font-medium text-gray-900">
                        {event.eventType.replace(/_/g, ' ')}
                      </span>
                    </div>
                    {event.memberName && (
                      <div className="text-xs text-gray-600 mt-1">{event.memberName}</div>
                    )}
                    {event.reasonCode && (
                      <div className="text-xs text-red-600 mt-0.5">{event.reasonCode}</div>
                    )}
                    <div className="text-xs text-gray-400 mt-1">
                      {new Date(event.createdAt).toLocaleTimeString()}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

function OccupancyBadge({ occupancy }: { occupancy?: OccupancyDto }) {
  const { t } = useTranslation()
  if (!occupancy) return null
  const { currentCount, maxCapacity, atCapacity } = occupancy
  return (
    <div
      className={`px-4 py-2 rounded-lg text-sm font-semibold ${
        atCapacity ? 'bg-red-100 text-red-800' : 'bg-blue-100 text-blue-800'
      }`}
    >
      {currentCount} {maxCapacity ? `/ ${maxCapacity}` : ''} {t('checkin.checkedIn')}
      {atCapacity && ` (${t('checkin.full')})`}
    </div>
  )
}

function CheckInStatusBadge({ status, reason }: { status: string; reason?: string }) {
  const isSuccess = status === 'SUCCESS'
  return (
    <span
      title={reason}
      className={`px-2 py-1 text-xs font-medium rounded-full ${
        isSuccess ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
      }`}
    >
      {status}
    </span>
  )
}

function EventTypeDot({ type }: { type: string }) {
  const colors: Record<string, string> = {
    CHECK_IN_SUCCESS: 'bg-green-500',
    CHECK_IN_DENIED: 'bg-red-500',
    CHECK_OUT: 'bg-blue-500',
    DEVICE_ONLINE: 'bg-green-400',
    DEVICE_OFFLINE: 'bg-gray-400',
    DEVICE_ERROR: 'bg-orange-500',
  }
  return <span className={`inline-block w-2 h-2 rounded-full ${colors[type] || 'bg-gray-400'}`} />
}
