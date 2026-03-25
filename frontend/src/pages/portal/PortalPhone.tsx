import { useState, useMemo, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import api from '../../api/client'
import type { ApiResponse, MemberDto, ContractDto, ClassScheduleDto, TrainingPlanDto, CheckInDto } from '../../types'

type AppTab = 'home' | 'checkin' | 'classes' | 'training' | 'profile'

export default function PortalPhone() {
  const { t } = useTranslation()
  return (
    <div className="min-h-screen bg-gray-100 flex flex-col items-center justify-center py-8 px-4">
      <div className="text-center mb-6">
        <h1 className="text-xl font-semibold text-gray-900">{t('portal.phone.title')}</h1>
        <p className="text-sm text-gray-500 mt-1">{t('portal.phone.subtitle')}</p>
      </div>
      <PhoneFrame />
      <p className="text-xs text-gray-400 mt-6">{t('portal.phone.simulation')}</p>
    </div>
  )
}

/* -- Phone Hardware Frame -- */

function PhoneFrame() {
  const { t } = useTranslation()
  const [tab, setTab] = useState<AppTab>('home')

  return (
    <div className="relative">
      {/* Phone bezel */}
      <div className="w-[375px] h-[812px] bg-black rounded-[55px] p-[12px] shadow-2xl shadow-black/40 ring-1 ring-gray-700">
        {/* Screen */}
        <div className="w-full h-full bg-white rounded-[43px] overflow-hidden flex flex-col relative">
          {/* Notch */}
          <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[120px] h-[30px] bg-black rounded-b-2xl z-30" />

          {/* Status bar */}
          <div className="h-[50px] bg-brand-950 flex items-end justify-between px-8 pb-1 text-white text-[11px] font-medium z-20">
            <span>9:41</span>
            <div className="flex items-center gap-1">
              <SignalIcon />
              <WifiIcon />
              <BatteryIcon />
            </div>
          </div>

          {/* App content */}
          <div className="flex-1 overflow-y-auto bg-gray-50">
            {tab === 'home' && <HomeScreen />}
            {tab === 'checkin' && <CheckInScreen />}
            {tab === 'classes' && <ClassesScreen />}
            {tab === 'training' && <TrainingScreen />}
            {tab === 'profile' && <ProfileScreen />}
          </div>

          {/* Bottom tab bar */}
          <div className="h-[82px] bg-white border-t border-gray-200 flex items-start pt-2 px-2 pb-6">
            {([
              { id: 'home' as AppTab, label: t('portal.phone.home'), icon: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-4 0h4' },
              { id: 'checkin' as AppTab, label: t('portal.phone.checkIn'), icon: 'M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z' },
              { id: 'classes' as AppTab, label: t('portal.classes.title'), icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z' },
              { id: 'training' as AppTab, label: t('portal.training.title'), icon: 'M13 10V3L4 14h7v7l9-11h-7z' },
              { id: 'profile' as AppTab, label: t('portal.phone.profile'), icon: 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z' },
            ]).map(tb => (
              <button
                key={tb.id}
                onClick={() => setTab(tb.id)}
                className={`flex-1 flex flex-col items-center gap-0.5 py-1 rounded-lg transition-colors ${
                  tab === tb.id ? 'text-brand-600' : 'text-gray-400'
                }`}
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={tab === tb.id ? 2 : 1.5}>
                  <path strokeLinecap="round" strokeLinejoin="round" d={tb.icon} />
                </svg>
                <span className="text-[10px] font-medium">{tb.label}</span>
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}

/* -- Home Screen -- */

function HomeScreen() {
  const { t } = useTranslation()
  const { data: profileRes } = useQuery({
    queryKey: ['portal-profile'],
    queryFn: () => api.get<ApiResponse<MemberDto>>('/portal/profile').then(r => r.data),
  })
  const member = profileRes?.data

  const { data: contractsRes } = useQuery({
    queryKey: ['portal-contracts'],
    queryFn: () => api.get<ApiResponse<ContractDto[]>>('/portal/contracts').then(r => r.data),
  })
  const rawContracts = contractsRes?.data
  const contracts: ContractDto[] = Array.isArray(rawContracts) ? rawContracts : (rawContracts as any)?.content ?? []
  const activeContract = contracts.find(c => c.status === 'ACTIVE')

  const { data: occupancyRes } = useQuery({
    queryKey: ['portal-occupancy'],
    queryFn: () => api.get<ApiResponse<{ current: number; max: number }>>('/portal/occupancy').then(r => r.data),
    refetchInterval: 30000,
  })
  const occupancy = occupancyRes?.data

  return (
    <div className="px-5 pt-3 pb-6">
      {/* Header */}
      <div className="flex items-center gap-3 mb-5">
        <div className="w-12 h-12 rounded-full bg-brand-100 flex items-center justify-center">
          <span className="text-brand-700 font-bold text-lg">
            {member?.firstName?.[0]}{member?.lastName?.[0]}
          </span>
        </div>
        <div>
          <p className="text-[15px] font-semibold text-gray-900">
            {t('portal.phone.hi', { name: member?.firstName ?? '...' })}
          </p>
          <p className="text-[12px] text-gray-500">{t('portal.phone.welcomeBackStudio')}</p>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="grid grid-cols-2 gap-3 mb-5">
        <div className="bg-brand-600 rounded-2xl p-4 text-white">
          <svg className="w-6 h-6 mb-2 opacity-80" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={1.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z" />
          </svg>
          <p className="text-[13px] font-semibold">{t('portal.phone.quickCheckIn')}</p>
          <p className="text-[10px] opacity-75 mt-0.5">{t('portal.phone.scanToEnter')}</p>
        </div>
        <div className="bg-white rounded-2xl p-4 border border-gray-200">
          <svg className="w-6 h-6 mb-2 text-brand-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={1.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          <p className="text-[13px] font-semibold text-gray-900">{t('portal.phone.bookClass')}</p>
          <p className="text-[10px] text-gray-500 mt-0.5">{t('portal.phone.viewSchedule')}</p>
        </div>
      </div>

      {/* Membership Card */}
      <div className="bg-gradient-to-br from-brand-800 to-brand-950 rounded-2xl p-5 text-white mb-5">
        <div className="flex justify-between items-start mb-4">
          <div>
            <p className="text-[10px] uppercase tracking-wider opacity-60">{t('portal.phone.membership')}</p>
            <p className="text-[15px] font-semibold mt-0.5">{activeContract?.membershipTierName ?? t('portal.phone.noActivePlan')}</p>
          </div>
          <div className="w-8 h-8 bg-white/10 rounded-lg flex items-center justify-center">
            <span className="text-white font-bold text-sm">F</span>
          </div>
        </div>
        <div className="flex justify-between text-[11px]">
          <div>
            <p className="opacity-60">{t('portal.member')}</p>
            <p className="font-medium">{member?.firstName} {member?.lastName}</p>
          </div>
          <div className="text-right">
            <p className="opacity-60">ID</p>
            <p className="font-mono font-medium">{member?.memberNumber}</p>
          </div>
        </div>
      </div>

      {/* Occupancy */}
      <div className="bg-white rounded-2xl p-4 border border-gray-200 mb-5">
        <div className="flex items-center justify-between mb-2">
          <p className="text-[13px] font-semibold text-gray-900">{t('portal.phone.studioOccupancy')}</p>
          <div className="flex items-center gap-1">
            <span className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
            <span className="text-[11px] text-green-600 font-medium">{t('portal.phone.live')}</span>
          </div>
        </div>
        <div className="flex items-end gap-3">
          <span className="text-3xl font-bold text-gray-900">{occupancy?.current ?? '—'}</span>
          <span className="text-[12px] text-gray-500 mb-1">{t('portal.phone.capacity', { max: occupancy?.max ?? '—' })}</span>
        </div>
        <div className="w-full bg-gray-100 rounded-full h-2 mt-2">
          <div
            className="bg-brand-500 h-2 rounded-full transition-all"
            style={{ width: `${occupancy ? Math.min((occupancy.current / occupancy.max) * 100, 100) : 0}%` }}
          />
        </div>
      </div>

      {/* Recent Activity */}
      <p className="text-[13px] font-semibold text-gray-900 mb-3">{t('portal.phone.recentActivity')}</p>
      <div className="space-y-2">
        {[
          { icon: '9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z', label: t('portal.phone.checkedIn'), time: t('portal.phone.today'), color: 'text-green-600' },
          { icon: '8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z', label: t('portal.phone.bookedMorningYoga'), time: t('portal.phone.yesterday'), color: 'text-brand-600' },
          { icon: '13 10V3L4 14h7v7l9-11h-7z', label: t('portal.phone.completedWorkout'), time: t('portal.phone.twoDaysAgo'), color: 'text-orange-500' },
        ].map((a, i) => (
          <div key={i} className="flex items-center gap-3 bg-white rounded-xl px-3 py-2.5 border border-gray-100">
            <svg className={`w-4 h-4 ${a.color} flex-shrink-0`} fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={1.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d={'M' + a.icon} />
            </svg>
            <div className="flex-1 min-w-0">
              <p className="text-[12px] font-medium text-gray-900">{a.label}</p>
              <p className="text-[10px] text-gray-400">{a.time}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

/* -- Check-In Screen (QR Code + Real Check-In) -- */

function CheckInScreen() {
  const { t } = useTranslation()
  const qc = useQueryClient()

  const { data: profileRes } = useQuery({
    queryKey: ['portal-profile'],
    queryFn: () => api.get<ApiResponse<MemberDto>>('/portal/profile').then(r => r.data),
  })
  const member = profileRes?.data

  const [scanMode, setScanMode] = useState(false)
  const [checkInResult, setCheckInResult] = useState<{ status: string; message: string; time?: string } | null>(null)
  const [scanning, setScanning] = useState(false)

  // Check if currently checked in
  const { data: checkinsRes } = useQuery({
    queryKey: ['portal-checkins-recent'],
    queryFn: () => api.get<ApiResponse<CheckInDto[]>>('/portal/checkins', { params: { page: 0, size: 1 } }).then(r => r.data),
  })
  const rawCheckins = checkinsRes?.data
  const lastCheckins: CheckInDto[] = Array.isArray(rawCheckins) ? rawCheckins : (rawCheckins as any)?.content ?? []
  const lastCheckIn = lastCheckins[0]
  const isCheckedIn = lastCheckIn?.status === 'SUCCESS' && !lastCheckIn?.checkOutTime

  const checkInMutation = useMutation({
    mutationFn: () => api.post<ApiResponse<CheckInDto>>('/portal/checkin'),
    onSuccess: (res) => {
      const data = res.data?.data
      if (data?.status === 'SUCCESS') {
        setCheckInResult({ status: 'success', message: t('portal.phone.checkInSuccessful'), time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) })
      } else {
        setCheckInResult({ status: 'denied', message: data?.denialReason ?? t('portal.phone.checkInDenied') })
      }
      qc.invalidateQueries({ queryKey: ['portal-checkins-recent'] })
      qc.invalidateQueries({ queryKey: ['portal-occupancy'] })
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.errors?.[0]?.message ?? t('portal.phone.checkInFailed')
      setCheckInResult({ status: 'error', message: msg })
    },
  })

  const checkOutMutation = useMutation({
    mutationFn: () => api.post('/portal/checkout'),
    onSuccess: () => {
      setCheckInResult({ status: 'checkout', message: t('portal.phone.checkedOutSuccessfully'), time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) })
      qc.invalidateQueries({ queryKey: ['portal-checkins-recent'] })
      qc.invalidateQueries({ queryKey: ['portal-occupancy'] })
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.errors?.[0]?.message ?? t('portal.phone.checkOutFailed')
      setCheckInResult({ status: 'error', message: msg })
    },
  })

  // Simulate RFID scan delay then trigger real check-in
  const handleRfidScan = () => {
    setScanning(true)
    setCheckInResult(null)
    setTimeout(() => {
      setScanning(false)
      checkInMutation.mutate()
    }, 2000)
  }

  // QR "scan" = instant check-in
  const handleQrCheckIn = () => {
    setCheckInResult(null)
    checkInMutation.mutate()
  }

  // Auto-clear result after 5s
  useEffect(() => {
    if (checkInResult) {
      const timer = setTimeout(() => setCheckInResult(null), 5000)
      return () => clearTimeout(timer)
    }
  }, [checkInResult])

  // Generate a deterministic QR-like pattern from member ID
  const qrGrid = useMemo(() => {
    const seed = member?.id ?? 'demo'
    const grid: boolean[][] = []
    for (let r = 0; r < 21; r++) {
      const row: boolean[] = []
      for (let c = 0; c < 21; c++) {
        if ((r < 7 && c < 7) || (r < 7 && c > 13) || (r > 13 && c < 7)) {
          if (r < 7 && c < 7) {
            row.push(r === 0 || r === 6 || c === 0 || c === 6 || (r >= 2 && r <= 4 && c >= 2 && c <= 4))
          } else if (r < 7 && c > 13) {
            row.push(r === 0 || r === 6 || c === 14 || c === 20 || (r >= 2 && r <= 4 && c >= 16 && c <= 18))
          } else {
            row.push(r === 14 || r === 20 || c === 0 || c === 6 || (r >= 16 && r <= 18 && c >= 2 && c <= 4))
          }
        } else {
          const hash = (seed.charCodeAt(r % seed.length) * 31 + c * 17 + r * 53) % 100
          row.push(hash < 45)
        }
      }
      grid.push(row)
    }
    return grid
  }, [member?.id])

  return (
    <div className="px-5 pt-3 pb-6 flex flex-col items-center">
      {/* Header */}
      <div className="w-full flex items-center justify-between mb-4">
        <div>
          <p className="text-[16px] font-bold text-gray-900">{t('portal.phone.checkInTitle')}</p>
          <p className="text-[11px] text-gray-500">
            {isCheckedIn ? t('portal.phone.currentlyCheckedIn') : t('portal.phone.tapOrScan')}
          </p>
        </div>
        <button
          onClick={() => { setScanMode(!scanMode); setCheckInResult(null) }}
          className="px-3 py-1.5 bg-brand-50 text-brand-700 rounded-lg text-[11px] font-medium"
        >
          {scanMode ? t('portal.phone.showQR') : t('portal.phone.rfidMode')}
        </button>
      </div>

      {/* Result banner */}
      {checkInResult && (
        <div className={`w-full rounded-2xl p-4 mb-4 flex items-center gap-3 ${
          checkInResult.status === 'success' ? 'bg-green-50 border border-green-200' :
          checkInResult.status === 'checkout' ? 'bg-blue-50 border border-blue-200' :
          checkInResult.status === 'denied' ? 'bg-yellow-50 border border-yellow-200' :
          'bg-red-50 border border-red-200'
        }`}>
          <div className={`w-10 h-10 rounded-full flex items-center justify-center ${
            checkInResult.status === 'success' ? 'bg-green-100' :
            checkInResult.status === 'checkout' ? 'bg-blue-100' :
            checkInResult.status === 'denied' ? 'bg-yellow-100' :
            'bg-red-100'
          }`}>
            {checkInResult.status === 'success' ? (
              <svg className="w-5 h-5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
              </svg>
            ) : checkInResult.status === 'checkout' ? (
              <svg className="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
              </svg>
            ) : (
              <svg className="w-5 h-5 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            )}
          </div>
          <div>
            <p className={`text-[13px] font-semibold ${
              checkInResult.status === 'success' ? 'text-green-800' :
              checkInResult.status === 'checkout' ? 'text-blue-800' :
              'text-red-800'
            }`}>{checkInResult.message}</p>
            {checkInResult.time && (
              <p className="text-[11px] text-gray-500">at {checkInResult.time}</p>
            )}
          </div>
        </div>
      )}

      {!scanMode ? (
        <>
          {/* QR Code -- tappable for check-in */}
          <button
            onClick={handleQrCheckIn}
            disabled={checkInMutation.isPending}
            className="bg-white rounded-3xl shadow-lg p-6 mb-4 border border-gray-100 transition-transform active:scale-95 disabled:opacity-60"
          >
            <div className="w-[180px] h-[180px] mx-auto">
              <svg viewBox="0 0 21 21" className="w-full h-full">
                {qrGrid.map((row, r) =>
                  row.map((cell, c) =>
                    cell ? <rect key={`${r}-${c}`} x={c} y={r} width={1} height={1} fill="#1a1a1a" /> : null
                  )
                )}
              </svg>
            </div>
            <p className="text-center text-[11px] text-gray-500 mt-3 font-mono">
              {member?.memberNumber ?? 'MBR-00000'}
            </p>
          </button>

          <p className="text-[12px] text-brand-600 font-medium mb-4">
            {checkInMutation.isPending ? t('portal.phone.checkingIn') : t('portal.phone.tapQrToCheckIn')}
          </p>

          {/* Check-out button if already checked in */}
          {isCheckedIn && (
            <button
              onClick={() => checkOutMutation.mutate()}
              disabled={checkOutMutation.isPending}
              className="w-full py-3 rounded-2xl border-2 border-red-200 text-red-600 text-[13px] font-semibold mb-4 active:bg-red-50 disabled:opacity-50"
            >
              {checkOutMutation.isPending ? t('portal.phone.checkingOut') : t('portal.phone.checkOut')}
            </button>
          )}

          {/* Member info */}
          <div className="w-full bg-brand-50 rounded-2xl p-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-brand-200 flex items-center justify-center">
                <span className="text-brand-800 font-bold text-sm">
                  {member?.firstName?.[0]}{member?.lastName?.[0]}
                </span>
              </div>
              <div>
                <p className="text-[13px] font-semibold text-gray-900">{member?.firstName} {member?.lastName}</p>
                <p className="text-[11px] text-gray-500">
                  {isCheckedIn ? t('portal.phone.currentlyInStudio') : t('portal.phone.activeMembership')}
                </p>
              </div>
              <div className="ml-auto">
                <span className={`w-3 h-3 rounded-full block ${isCheckedIn ? 'bg-green-500 animate-pulse' : 'bg-gray-300'}`} />
              </div>
            </div>
          </div>
        </>
      ) : (
        /* RFID Scan Mode */
        <div className="flex-1 flex flex-col items-center justify-center py-6">
          <button
            onClick={handleRfidScan}
            disabled={scanning || checkInMutation.isPending}
            className="w-40 h-40 rounded-full border-4 border-dashed border-brand-300 flex items-center justify-center mb-6 transition-all active:scale-95 disabled:opacity-60"
            style={scanning ? { animation: 'pulse 1s ease-in-out infinite' } : {}}
          >
            <svg className={`w-16 h-16 ${scanning ? 'text-brand-600' : 'text-brand-400'}`} fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={1}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M8.111 16.404a5.5 5.5 0 017.778 0M12 20h.01m-7.08-7.071c3.904-3.905 10.236-3.905 14.14 0M1.394 9.393c5.857-5.858 15.355-5.858 21.213 0" />
            </svg>
          </button>
          <p className="text-[15px] font-semibold text-gray-900 mb-1">
            {scanning ? t('portal.phone.scanning') : t('portal.phone.tapToSimulateRfid')}
          </p>
          <p className="text-[12px] text-gray-500 text-center px-8">
            {scanning
              ? t('portal.phone.readingCard')
              : t('portal.phone.rfidHint')
            }
          </p>
          {scanning && (
            <div className="mt-6 flex items-center gap-2 text-brand-600">
              <span className="w-2 h-2 rounded-full bg-brand-500 animate-ping" />
              <span className="text-[12px] font-medium">{t('portal.phone.processing')}</span>
            </div>
          )}

          {/* Check-out in RFID mode too */}
          {isCheckedIn && !scanning && (
            <button
              onClick={() => checkOutMutation.mutate()}
              disabled={checkOutMutation.isPending}
              className="mt-6 px-6 py-2.5 rounded-2xl border-2 border-red-200 text-red-600 text-[13px] font-semibold active:bg-red-50 disabled:opacity-50"
            >
              {checkOutMutation.isPending ? t('portal.phone.checkingOut') : t('portal.phone.checkOut')}
            </button>
          )}
        </div>
      )}
    </div>
  )
}

/* -- Classes Screen -- */

function ClassesScreen() {
  const { t } = useTranslation()
  const weekStart = useMemo(() => {
    const d = new Date()
    d.setHours(12, 0, 0, 0)
    const day = d.getDay()
    d.setDate(d.getDate() - day + (day === 0 ? -6 : 1))
    return d
  }, [])

  const { data: scheduleRes } = useQuery({
    queryKey: ['phone-schedule', weekStart.toISOString()],
    queryFn: () =>
      api.get<ApiResponse<ClassScheduleDto[]>>('/booking/schedules/weekly', {
        params: { weekStart: weekStart.toISOString() },
      }).then(r => r.data),
  })
  const raw = scheduleRes?.data
  const schedules: ClassScheduleDto[] = Array.isArray(raw) ? raw : (raw as any)?.content ?? []

  const now = new Date()
  const upcoming = schedules
    .filter(s => !s.cancelled && new Date(s.startTime) > now)
    .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime())
    .slice(0, 8)

  return (
    <div className="px-5 pt-3 pb-6">
      <p className="text-[16px] font-bold text-gray-900 mb-1">{t('portal.classes.title')}</p>
      <p className="text-[11px] text-gray-500 mb-5">{t('portal.classes.upcomingThisWeek')}</p>

      {upcoming.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-[13px] text-gray-400">{t('portal.classes.noUpcomingClasses')}</p>
        </div>
      ) : (
        <div className="space-y-3">
          {upcoming.map(s => {
            const dt = new Date(s.startTime)
            const isFull = s.bookedCount >= s.capacity
            return (
              <div key={s.id} className="bg-white rounded-2xl p-4 border border-gray-100 shadow-sm">
                <div className="flex items-start justify-between">
                  <div className="flex gap-3">
                    <div className="w-1 h-12 rounded-full" style={{ backgroundColor: s.categoryColor || '#0b8e6c' }} />
                    <div>
                      <p className="text-[13px] font-semibold text-gray-900">{s.className}</p>
                      <p className="text-[11px] text-gray-500">{s.trainerName ?? 'TBA'}</p>
                      <div className="flex items-center gap-2 mt-1">
                        <span className="text-[10px] text-gray-400">
                          {dt.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' })}
                        </span>
                        <span className="text-[10px] text-gray-400">
                          {dt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      </div>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className={`text-[11px] font-medium ${isFull ? 'text-red-500' : 'text-gray-600'}`}>
                      {s.bookedCount}/{s.capacity}
                    </p>
                    <button className={`mt-1 px-3 py-1 rounded-lg text-[11px] font-medium ${
                      isFull
                        ? 'bg-orange-100 text-orange-700'
                        : 'bg-brand-600 text-white'
                    }`}>
                      {isFull ? t('portal.classes.joinWaitlist') : t('portal.classes.book')}
                    </button>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

/* -- Training Screen -- */

function TrainingScreen() {
  const { t } = useTranslation()
  const { data: profileRes } = useQuery({
    queryKey: ['portal-profile'],
    queryFn: () => api.get<ApiResponse<MemberDto>>('/portal/profile').then(r => r.data),
  })
  const memberId = profileRes?.data?.id

  const { data: plansRes } = useQuery({
    queryKey: ['phone-plans', memberId],
    queryFn: () => api.get<ApiResponse<TrainingPlanDto[]>>(`/training/plans/member/${memberId}`).then(r => r.data),
    enabled: !!memberId,
  })
  const rawPlans = plansRes?.data
  const plans: TrainingPlanDto[] = Array.isArray(rawPlans) ? rawPlans : (rawPlans as any)?.content ?? []
  const activePlan = plans.find(p => p.status === 'PUBLISHED') ?? plans[0]

  return (
    <div className="px-5 pt-3 pb-6">
      <p className="text-[16px] font-bold text-gray-900 mb-1">{t('portal.training.title')}</p>
      <p className="text-[11px] text-gray-500 mb-5">{t('portal.training.yourActiveTrainingPlan')}</p>

      {!activePlan ? (
        <div className="text-center py-12">
          <svg className="w-12 h-12 text-gray-300 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={1}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
          <p className="text-[13px] text-gray-400">{t('portal.training.noTrainingPlan')}</p>
          <p className="text-[11px] text-gray-400 mt-1">{t('portal.training.askTrainerSetup')}</p>
        </div>
      ) : (
        <>
          {/* Plan card */}
          <div className="bg-gradient-to-br from-brand-600 to-brand-800 rounded-2xl p-5 text-white mb-5">
            <p className="text-[10px] uppercase tracking-wider opacity-60">{t('portal.training.activePlan')}</p>
            <p className="text-[16px] font-bold mt-1">{activePlan.name}</p>
            {activePlan.description && (
              <p className="text-[11px] opacity-75 mt-1">{activePlan.description}</p>
            )}
            <div className="flex gap-4 mt-3 text-[11px]">
              <div>
                <p className="opacity-60">{t('portal.training.exercises')}</p>
                <p className="font-semibold">{activePlan.exercises?.length ?? 0}</p>
              </div>
              <div>
                <p className="opacity-60">{t('portal.training.category')}</p>
                <p className="font-semibold">{activePlan.category ?? t('portal.training.general')}</p>
              </div>
            </div>
          </div>

          {/* Exercise list */}
          <p className="text-[12px] font-semibold text-gray-700 mb-3">{t('portal.training.exercises')}</p>
          <div className="space-y-2">
            {(activePlan.exercises ?? []).slice(0, 6).map((ex, i) => (
              <div key={ex.exerciseId ?? i} className="bg-white rounded-xl px-4 py-3 border border-gray-100 flex items-center gap-3">
                <div className="w-8 h-8 rounded-lg bg-brand-50 flex items-center justify-center">
                  <span className="text-brand-700 text-[11px] font-bold">{i + 1}</span>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-[12px] font-medium text-gray-900 truncate">{ex.exerciseName}</p>
                  <p className="text-[10px] text-gray-500">
                    {ex.sets}x{ex.reps} {ex.weight ? `@ ${ex.weight}kg` : ''}
                  </p>
                </div>
                <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={1.5}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
                </svg>
              </div>
            ))}
          </div>

          {/* Start workout button */}
          <button className="w-full mt-5 bg-brand-600 text-white py-3 rounded-2xl text-[14px] font-semibold shadow-lg shadow-brand-500/30">
            {t('portal.training.startWorkout')}
          </button>
        </>
      )}
    </div>
  )
}

/* -- Profile Screen -- */

function ProfileScreen() {
  const { t } = useTranslation()
  const { data: profileRes } = useQuery({
    queryKey: ['portal-profile'],
    queryFn: () => api.get<ApiResponse<MemberDto>>('/portal/profile').then(r => r.data),
  })
  const member = profileRes?.data

  const menuItems = [
    { icon: 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z', label: t('portal.phone.personalInformation') },
    { icon: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z', label: t('portal.phone.myContracts') },
    { icon: '17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2z', label: t('portal.phone.invoicesAndPayments') },
    { icon: '12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z', label: t('portal.phone.checkInHistory') },
    { icon: '10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z', label: t('portal.phone.settings') },
    { icon: '8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z', label: t('portal.phone.helpAndSupport') },
  ]

  return (
    <div className="px-5 pt-3 pb-6">
      {/* Profile header */}
      <div className="flex flex-col items-center mb-6">
        <div className="w-20 h-20 rounded-full bg-brand-100 flex items-center justify-center mb-3">
          <span className="text-brand-700 font-bold text-2xl">
            {member?.firstName?.[0]}{member?.lastName?.[0]}
          </span>
        </div>
        <p className="text-[16px] font-bold text-gray-900">{member?.firstName} {member?.lastName}</p>
        <p className="text-[12px] text-gray-500">{member?.email}</p>
        <span className="mt-2 px-3 py-1 bg-brand-50 text-brand-700 rounded-full text-[11px] font-medium">
          {member?.memberNumber}
        </span>
      </div>

      {/* Menu */}
      <div className="bg-white rounded-2xl border border-gray-100 overflow-hidden">
        {menuItems.map((item, i) => (
          <div key={i} className={`flex items-center gap-3 px-4 py-3.5 ${i > 0 ? 'border-t border-gray-50' : ''}`}>
            <svg className="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={1.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d={'M' + item.icon} />
            </svg>
            <span className="flex-1 text-[13px] text-gray-900">{item.label}</span>
            <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={1.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
            </svg>
          </div>
        ))}
      </div>

      {/* Sign out */}
      <button className="w-full mt-5 py-3 text-red-500 text-[13px] font-medium">
        {t('portal.phone.signOut')}
      </button>

      <p className="text-center text-[10px] text-gray-400 mt-3">Fitagend v1.0.0</p>
    </div>
  )
}

/* -- Status bar icons -- */

function SignalIcon() {
  return (
    <svg className="w-4 h-3" viewBox="0 0 17 12" fill="white">
      <rect x="0" y="9" width="3" height="3" rx="0.5" opacity="0.4" />
      <rect x="4.5" y="6" width="3" height="6" rx="0.5" opacity="0.6" />
      <rect x="9" y="3" width="3" height="9" rx="0.5" opacity="0.8" />
      <rect x="13.5" y="0" width="3" height="12" rx="0.5" />
    </svg>
  )
}

function WifiIcon() {
  return (
    <svg className="w-4 h-3" viewBox="0 0 16 12" fill="none" stroke="white" strokeWidth="1.5">
      <path d="M1 4c3.9-3.5 10.1-3.5 14 0" opacity="0.5" />
      <path d="M3.5 7c2.5-2.3 6.5-2.3 9 0" opacity="0.75" />
      <circle cx="8" cy="10.5" r="1.2" fill="white" stroke="none" />
    </svg>
  )
}

function BatteryIcon() {
  return (
    <svg className="w-6 h-3" viewBox="0 0 27 13" fill="none">
      <rect x="0.5" y="0.5" width="23" height="12" rx="2.5" stroke="white" strokeWidth="1" />
      <rect x="25" y="4" width="2" height="5" rx="1" fill="white" opacity="0.5" />
      <rect x="2" y="2" width="18" height="8.5" rx="1.5" fill="white" />
    </svg>
  )
}
