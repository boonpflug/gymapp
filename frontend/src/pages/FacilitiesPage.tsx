import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../api/client'
import type { ApiResponse } from '../types'

interface FacilityDto {
  id: string
  name: string
  description?: string
  street?: string
  city?: string
  state?: string
  postalCode?: string
  country?: string
  timezone?: string
  phone?: string
  email?: string
  websiteUrl?: string
  openingHours?: string
  logoUrl?: string
  brandColor?: string
  bannerImageUrl?: string
  maxOccupancy?: number
  parentFacilityId?: string
  parentFacilityName?: string
  active: boolean
  memberCount: number
  employeeCount: number
  childFacilities?: FacilityDto[]
  createdAt: string
}

interface FacilitySummaryDto {
  facility: FacilityDto
  activeMembers: number
  checkInsToday: number
  newMembersThisMonth: number
  revenueThisMonth: number
  currentOccupancy: number
}

interface ConsolidatedDashboardDto {
  totalFacilities: number
  totalActiveMembers: number
  totalCheckInsToday: number
  totalNewMembersThisMonth: number
  totalRevenueThisMonth: number
  totalOutstandingPayments: number
  facilitySummaries: FacilitySummaryDto[]
}

interface CreateFacilityForm {
  name: string
  description: string
  street: string
  city: string
  state: string
  postalCode: string
  country: string
  timezone: string
  phone: string
  email: string
  websiteUrl: string
  openingHours: string
  brandColor: string
  maxOccupancy: string
  parentFacilityId: string
}

const emptyForm: CreateFacilityForm = {
  name: '', description: '', street: '', city: '', state: '', postalCode: '',
  country: '', timezone: 'Europe/Berlin', phone: '', email: '', websiteUrl: '',
  openingHours: '', brandColor: '#4f46e5', maxOccupancy: '', parentFacilityId: '',
}

export default function FacilitiesPage() {
  const { t } = useTranslation()
  const [tab, setTab] = useState<'dashboard' | 'facilities'>('dashboard')
  const [showCreate, setShowCreate] = useState(false)
  const [form, setForm] = useState<CreateFacilityForm>(emptyForm)
  const [editId, setEditId] = useState<string | null>(null)
  const qc = useQueryClient()

  const { data: facilitiesRes } = useQuery({
    queryKey: ['facilities'],
    queryFn: () => api.get<ApiResponse<FacilityDto[]>>('/facilities/list').then(r => r.data),
  })
  const facilities = facilitiesRes?.data ?? []

  const { data: dashRes } = useQuery({
    queryKey: ['facilities-dashboard'],
    queryFn: () => api.get<ApiResponse<ConsolidatedDashboardDto>>('/facilities/dashboard/consolidated').then(r => r.data),
    enabled: tab === 'dashboard',
  })
  const dashboard = dashRes?.data

  const createMutation = useMutation({
    mutationFn: (data: Record<string, unknown>) =>
      editId
        ? api.put(`/facilities/${editId}`, data)
        : api.post('/facilities', data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['facilities'] })
      qc.invalidateQueries({ queryKey: ['facilities-dashboard'] })
      setShowCreate(false)
      setForm(emptyForm)
      setEditId(null)
    },
  })

  const deactivateMutation = useMutation({
    mutationFn: (id: string) => api.post(`/facilities/${id}/deactivate`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['facilities'] })
      qc.invalidateQueries({ queryKey: ['facilities-dashboard'] })
    },
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    createMutation.mutate({
      ...form,
      maxOccupancy: form.maxOccupancy ? parseInt(form.maxOccupancy) : null,
      parentFacilityId: form.parentFacilityId || null,
    })
  }

  const openEdit = (f: FacilityDto) => {
    setEditId(f.id)
    setForm({
      name: f.name,
      description: f.description ?? '',
      street: f.street ?? '',
      city: f.city ?? '',
      state: f.state ?? '',
      postalCode: f.postalCode ?? '',
      country: f.country ?? '',
      timezone: f.timezone ?? 'Europe/Berlin',
      phone: f.phone ?? '',
      email: f.email ?? '',
      websiteUrl: f.websiteUrl ?? '',
      openingHours: f.openingHours ?? '',
      brandColor: f.brandColor ?? '#4f46e5',
      maxOccupancy: f.maxOccupancy?.toString() ?? '',
      parentFacilityId: f.parentFacilityId ?? '',
    })
    setShowCreate(true)
  }

  const tabs = [
    { key: 'dashboard' as const, label: t('facilities.dashboard') },
    { key: 'facilities' as const, label: t('facilities.management') },
  ]

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">{t('facilities.title')}</h1>
      </div>

      <div className="flex space-x-4 mb-6 border-b">
        {tabs.map(tabItem => (
          <button
            key={tabItem.key}
            onClick={() => setTab(tabItem.key)}
            className={`pb-2 px-1 text-sm font-medium ${
              tab === tabItem.key
                ? 'border-b-2 border-brand-600 text-brand-600'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            {tabItem.label}
          </button>
        ))}
      </div>

      {tab === 'dashboard' && dashboard && (
        <div>
          {/* Summary cards */}
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4 mb-8">
            <StatCard label={t('facilities.totalFacilities')} value={dashboard.totalFacilities} />
            <StatCard label={t('facilities.activeMembers')} value={dashboard.totalActiveMembers} />
            <StatCard label={t('facilities.checkInsToday')} value={dashboard.totalCheckInsToday} />
            <StatCard label={t('facilities.newThisMonth')} value={dashboard.totalNewMembersThisMonth} />
            <StatCard
              label={t('facilities.revenueMonth')}
              value={`€${(dashboard.totalRevenueThisMonth ?? 0).toLocaleString('de-DE', { minimumFractionDigits: 2 })}`}
            />
            <StatCard
              label={t('facilities.outstanding')}
              value={`€${(dashboard.totalOutstandingPayments ?? 0).toLocaleString('de-DE', { minimumFractionDigits: 2 })}`}
              color="text-red-600"
            />
          </div>

          {/* Per-facility breakdown */}
          <h2 className="text-lg font-semibold mb-4">{t('facilities.perFacilityBreakdown')}</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {dashboard.facilitySummaries.map(s => (
              <div key={s.facility.id} className="bg-white rounded-lg shadow p-5">
                <div className="flex items-center justify-between mb-3">
                  <h3 className="font-semibold text-lg">{s.facility.name}</h3>
                  {s.facility.brandColor && (
                    <span
                      className="w-4 h-4 rounded-full inline-block"
                      style={{ backgroundColor: s.facility.brandColor }}
                    />
                  )}
                </div>
                {s.facility.city && (
                  <p className="text-sm text-gray-500 mb-3">{s.facility.city}{s.facility.country ? `, ${s.facility.country}` : ''}</p>
                )}
                <div className="grid grid-cols-2 gap-3 text-sm">
                  <div>
                    <p className="text-gray-500">{t('facilities.members')}</p>
                    <p className="font-semibold text-lg">{s.activeMembers}</p>
                  </div>
                  <div>
                    <p className="text-gray-500">{t('facilities.checkInsToday')}</p>
                    <p className="font-semibold text-lg">{s.checkInsToday}</p>
                  </div>
                  <div>
                    <p className="text-gray-500">{t('facilities.newThisMonth')}</p>
                    <p className="font-semibold text-lg">{s.newMembersThisMonth}</p>
                  </div>
                  <div>
                    <p className="text-gray-500">{t('facilities.occupancy')}</p>
                    <p className="font-semibold text-lg">
                      {s.currentOccupancy}
                      {s.facility.maxOccupancy ? `/${s.facility.maxOccupancy}` : ''}
                    </p>
                  </div>
                  <div className="col-span-2">
                    <p className="text-gray-500">{t('facilities.revenueMonth')}</p>
                    <p className="font-semibold text-lg text-green-600">
                      €{(s.revenueThisMonth ?? 0).toLocaleString('de-DE', { minimumFractionDigits: 2 })}
                    </p>
                  </div>
                </div>
              </div>
            ))}
          </div>
          {dashboard.facilitySummaries.length === 0 && (
            <p className="text-gray-500 text-center py-8">{t('facilities.noFacilitiesCreated')}</p>
          )}
        </div>
      )}

      {tab === 'facilities' && (
        <div>
          <div className="flex justify-end mb-4">
            <button
              onClick={() => { setShowCreate(true); setEditId(null); setForm(emptyForm) }}
              className="bg-brand-600 text-white px-4 py-2 rounded text-sm hover:bg-brand-700"
            >
              {t('facilities.addFacility')}
            </button>
          </div>

          {/* Facility cards */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {facilities.map(f => (
              <div key={f.id} className="bg-white rounded-lg shadow p-5">
                <div className="flex items-center justify-between mb-2">
                  <h3 className="font-semibold text-lg">{f.name}</h3>
                  <div className="flex items-center gap-2">
                    {f.brandColor && (
                      <span className="w-3 h-3 rounded-full" style={{ backgroundColor: f.brandColor }} />
                    )}
                    <span className={`text-xs px-2 py-0.5 rounded ${f.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                      {f.active ? t('facilities.active') : t('facilities.inactive')}
                    </span>
                  </div>
                </div>
                {f.description && <p className="text-sm text-gray-500 mb-2">{f.description}</p>}
                <div className="text-sm text-gray-600 space-y-1 mb-3">
                  {f.street && <p>{f.street}, {f.city} {f.postalCode}</p>}
                  {f.country && <p>{f.country}</p>}
                  {f.phone && <p>{t('facilities.tel')} {f.phone}</p>}
                  {f.email && <p>{t('facilities.emailLabel')} {f.email}</p>}
                  {f.timezone && <p>{t('facilities.tz')} {f.timezone}</p>}
                  {f.maxOccupancy && <p>{t('facilities.maxOccupancy')} {f.maxOccupancy}</p>}
                  {f.parentFacilityName && (
                    <p className="text-brand-600">{t('facilities.parent')} {f.parentFacilityName}</p>
                  )}
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-gray-500">{f.memberCount} {t('facilities.membersCount')}</span>
                  <div className="flex gap-2">
                    <button
                      onClick={() => openEdit(f)}
                      className="text-brand-600 hover:text-brand-700"
                    >
                      {t('facilities.edit')}
                    </button>
                    {f.active && (
                      <button
                        onClick={() => deactivateMutation.mutate(f.id)}
                        className="text-red-600 hover:text-red-800"
                      >
                        {t('facilities.deactivate')}
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
          {facilities.length === 0 && (
            <p className="text-gray-500 text-center py-8">{t('facilities.noFacilitiesYet')}</p>
          )}
        </div>
      )}

      {/* Create / Edit Modal */}
      {showCreate && (
        <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto">
            <h2 className="text-lg font-semibold mb-4">{editId ? t('facilities.editFacility') : t('facilities.addFacility')}</h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-1">{t('facilities.nameLabel')}</label>
                  <input
                    value={form.name}
                    onChange={e => setForm({ ...form, name: e.target.value })}
                    required
                    className="w-full border rounded px-3 py-2 text-sm"
                  />
                </div>
                <div className="col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-1">{t('facilities.description')}</label>
                  <textarea
                    value={form.description}
                    onChange={e => setForm({ ...form, description: e.target.value })}
                    rows={2}
                    className="w-full border rounded px-3 py-2 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{t('facilities.street')}</label>
                  <input value={form.street} onChange={e => setForm({ ...form, street: e.target.value })} className="w-full border rounded px-3 py-2 text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{t('facilities.city')}</label>
                  <input value={form.city} onChange={e => setForm({ ...form, city: e.target.value })} className="w-full border rounded px-3 py-2 text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{t('facilities.state')}</label>
                  <input value={form.state} onChange={e => setForm({ ...form, state: e.target.value })} className="w-full border rounded px-3 py-2 text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{t('facilities.postalCode')}</label>
                  <input value={form.postalCode} onChange={e => setForm({ ...form, postalCode: e.target.value })} className="w-full border rounded px-3 py-2 text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{t('facilities.country')}</label>
                  <input value={form.country} onChange={e => setForm({ ...form, country: e.target.value })} className="w-full border rounded px-3 py-2 text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{t('facilities.timezone')}</label>
                  <input value={form.timezone} onChange={e => setForm({ ...form, timezone: e.target.value })} className="w-full border rounded px-3 py-2 text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{t('facilities.phone')}</label>
                  <input value={form.phone} onChange={e => setForm({ ...form, phone: e.target.value })} className="w-full border rounded px-3 py-2 text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{t('facilities.email')}</label>
                  <input type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} className="w-full border rounded px-3 py-2 text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{t('facilities.website')}</label>
                  <input value={form.websiteUrl} onChange={e => setForm({ ...form, websiteUrl: e.target.value })} className="w-full border rounded px-3 py-2 text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{t('facilities.maxOccupancyLabel')}</label>
                  <input type="number" value={form.maxOccupancy} onChange={e => setForm({ ...form, maxOccupancy: e.target.value })} className="w-full border rounded px-3 py-2 text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{t('facilities.brandColor')}</label>
                  <div className="flex gap-2 items-center">
                    <input type="color" value={form.brandColor} onChange={e => setForm({ ...form, brandColor: e.target.value })} className="w-10 h-10 rounded border" />
                    <input value={form.brandColor} onChange={e => setForm({ ...form, brandColor: e.target.value })} className="w-full border rounded px-3 py-2 text-sm" />
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{t('facilities.parentFacility')}</label>
                  <select
                    value={form.parentFacilityId}
                    onChange={e => setForm({ ...form, parentFacilityId: e.target.value })}
                    className="w-full border rounded px-3 py-2 text-sm"
                  >
                    <option value="">{t('facilities.noneIndependent')}</option>
                    {facilities.filter(f => f.id !== editId).map(f => (
                      <option key={f.id} value={f.id}>{f.name}</option>
                    ))}
                  </select>
                </div>
                <div className="col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-1">{t('facilities.openingHours')}</label>
                  <textarea
                    value={form.openingHours}
                    onChange={e => setForm({ ...form, openingHours: e.target.value })}
                    rows={3}
                    placeholder='e.g., {"mon":"06:00-22:00","tue":"06:00-22:00"}'
                    className="w-full border rounded px-3 py-2 text-sm"
                  />
                </div>
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => { setShowCreate(false); setEditId(null); setForm(emptyForm) }}
                  className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800"
                >
                  {t('facilities.cancel')}
                </button>
                <button
                  type="submit"
                  disabled={createMutation.isPending}
                  className="bg-brand-600 text-white px-4 py-2 rounded text-sm hover:bg-brand-700 disabled:opacity-50"
                >
                  {createMutation.isPending ? t('facilities.saving') : editId ? t('staff.update') : t('facilities.create')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

function StatCard({ label, value, color }: { label: string; value: string | number; color?: string }) {
  return (
    <div className="bg-white rounded-lg shadow p-4">
      <p className="text-xs text-gray-500 mb-1">{label}</p>
      <p className={`text-xl font-bold ${color ?? 'text-gray-900'}`}>{value}</p>
    </div>
  )
}
