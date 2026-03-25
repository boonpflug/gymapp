import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../api/client'
import type {
  ApiResponse,
  LeadDto,
  LeadStageDto,
  LeadActivityDto,
  PromoCodeDto,
  SalesPipelineDto,
  LeadSource,
  LeadActivityType,
  DiscountType,
} from '../types'

const LEAD_SOURCES: LeadSource[] = [
  'WEBSITE', 'WALK_IN', 'REFERRAL', 'SOCIAL_MEDIA', 'GOOGLE_ADS',
  'META_ADS', 'PARTNER', 'PHONE', 'EVENT', 'OTHER',
]

const ACTIVITY_TYPES: LeadActivityType[] = [
  'CALL', 'EMAIL', 'VISIT', 'NOTE', 'TASK', 'SMS', 'MEETING',
  'TRIAL_BOOKED', 'PROPOSAL_SENT',
]

type Tab = 'pipeline' | 'leads' | 'promo'

export default function SalesPage() {
  const { t } = useTranslation()
  const [activeTab, setActiveTab] = useState<Tab>('pipeline')

  const tabs: { key: Tab; label: string }[] = [
    { key: 'pipeline', label: t('sales.pipeline') },
    { key: 'leads', label: t('sales.allLeads') },
    { key: 'promo', label: t('sales.promoCodes') },
  ]

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">{t('sales.title')}</h1>
      <div className="border-b border-gray-200 mb-6">
        <nav className="flex space-x-8">
          {tabs.map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`pb-3 px-1 text-sm font-medium border-b-2 ${
                activeTab === tab.key
                  ? 'border-brand-500 text-brand-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {activeTab === 'pipeline' && <PipelineView />}
      {activeTab === 'leads' && <LeadsList />}
      {activeTab === 'promo' && <PromoCodeList />}
    </div>
  )
}

// ===================== PIPELINE VIEW =====================

function PipelineView() {
  const { t } = useTranslation()
  const queryClient = useQueryClient()

  const { data: pipeline, isLoading } = useQuery({
    queryKey: ['sales-pipeline'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<SalesPipelineDto>>('/sales/leads/pipeline')
      return res.data.data
    },
  })

  const initMutation = useMutation({
    mutationFn: () => api.post('/sales/stages/init-defaults'),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['sales-pipeline'] }),
  })

  if (isLoading) return <p className="text-gray-500">{t('sales.loadingPipeline')}</p>
  if (!pipeline) return null

  if (pipeline.stages.length === 0) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500 mb-4">{t('sales.noStagesConfigured')}</p>
        <button
          onClick={() => initMutation.mutate()}
          className="bg-brand-600 text-white px-6 py-2 rounded-lg text-sm hover:bg-brand-700"
        >
          {t('sales.initDefaultStages')}
        </button>
      </div>
    )
  }

  return (
    <div>
      <div className="grid grid-cols-3 gap-4 mb-6">
        <div className="bg-white rounded-lg shadow p-4">
          <p className="text-2xl font-bold">{pipeline.totalLeads}</p>
          <p className="text-sm text-gray-500">{t('sales.totalLeads')}</p>
        </div>
        <div className="bg-white rounded-lg shadow p-4">
          <p className="text-2xl font-bold text-green-600">{pipeline.convertedLeads}</p>
          <p className="text-sm text-gray-500">{t('sales.converted')}</p>
        </div>
        <div className="bg-white rounded-lg shadow p-4">
          <p className="text-2xl font-bold text-brand-600">{pipeline.conversionRate.toFixed(1)}%</p>
          <p className="text-sm text-gray-500">{t('sales.conversionRate')}</p>
        </div>
      </div>

      <div className="flex gap-4 overflow-x-auto pb-4">
        {pipeline.stages.map((stage) => (
          <PipelineColumn key={stage.id} stage={stage} />
        ))}
      </div>
    </div>
  )
}

function PipelineColumn({ stage }: { stage: LeadStageDto }) {
  const { t } = useTranslation()
  const { data } = useQuery({
    queryKey: ['pipeline-leads', stage.id],
    queryFn: async () => {
      const res = await api.get<ApiResponse<LeadDto[]>>(
        `/sales/leads?stageId=${stage.id}&size=50`
      )
      return res.data
    },
  })

  const leads = data?.data ?? []

  return (
    <div className="min-w-[280px] flex-shrink-0">
      <div className="flex items-center gap-2 mb-3">
        <div
          className="w-3 h-3 rounded-full"
          style={{ backgroundColor: stage.color || '#6B7280' }}
        />
        <h3 className="font-medium text-sm">{stage.name}</h3>
        <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">
          {stage.leadCount}
        </span>
      </div>
      <div className="space-y-2">
        {leads.map((lead) => (
          <div key={lead.id} className="bg-white rounded-lg shadow-sm p-3 border-l-4"
               style={{ borderLeftColor: stage.color || '#6B7280' }}>
            <p className="font-medium text-sm">{lead.firstName} {lead.lastName}</p>
            <p className="text-xs text-gray-500">
              {lead.source.replace(/_/g, ' ')}
              {lead.interest && <> &middot; {lead.interest}</>}
            </p>
            {lead.email && <p className="text-xs text-gray-400 mt-1">{lead.email}</p>}
            <div className="flex items-center justify-between mt-2">
              {lead.assignedStaffName && (
                <span className="text-xs text-brand-600">{lead.assignedStaffName}</span>
              )}
              <span className="text-xs text-gray-400">{lead.activityCount} {t('sales.activities')}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

// ===================== LEADS LIST =====================

function LeadsList() {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const [showCreate, setShowCreate] = useState(false)
  const [selectedLead, setSelectedLead] = useState<LeadDto | null>(null)
  const [nameFilter, setNameFilter] = useState('')
  const [sourceFilter, setSourceFilter] = useState<string>('')

  const { data, isLoading } = useQuery({
    queryKey: ['all-leads', nameFilter, sourceFilter],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '50' })
      if (nameFilter) params.set('name', nameFilter)
      if (sourceFilter) params.set('source', sourceFilter)
      const res = await api.get<ApiResponse<LeadDto[]>>(`/sales/leads?${params}`)
      return res.data
    },
  })

  const createMutation = useMutation({
    mutationFn: (lead: Record<string, unknown>) => api.post('/sales/leads', lead),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['all-leads'] })
      queryClient.invalidateQueries({ queryKey: ['sales-pipeline'] })
      setShowCreate(false)
    },
  })

  const convertMutation = useMutation({
    mutationFn: (leadId: string) => api.post(`/sales/leads/${leadId}/convert`, {}),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['all-leads'] })
      queryClient.invalidateQueries({ queryKey: ['sales-pipeline'] })
      setSelectedLead(null)
    },
  })

  const leads = data?.data ?? []

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <div className="flex gap-3">
          <input
            type="text"
            placeholder={t('sales.searchByName')}
            value={nameFilter}
            onChange={(e) => setNameFilter(e.target.value)}
            className="border rounded-lg px-3 py-2 text-sm w-64"
          />
          <select
            value={sourceFilter}
            onChange={(e) => setSourceFilter(e.target.value)}
            className="border rounded-lg px-3 py-2 text-sm"
          >
            <option value="">{t('sales.allSources')}</option>
            {LEAD_SOURCES.map((s) => (
              <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>
            ))}
          </select>
        </div>
        <button
          onClick={() => setShowCreate(true)}
          className="bg-brand-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-brand-700"
        >
          {t('sales.addLead')}
        </button>
      </div>

      {isLoading ? (
        <p className="text-gray-500">{t('sales.loadingLeads')}</p>
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">{t('sales.name')}</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">{t('sales.contact')}</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">{t('sales.source')}</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">{t('sales.stage')}</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">{t('sales.assigned')}</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">{t('sales.actionsCol')}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {leads.map((lead) => (
                <tr key={lead.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3">
                    <button
                      onClick={() => setSelectedLead(lead)}
                      className="text-sm font-medium text-brand-600 hover:underline"
                    >
                      {lead.firstName} {lead.lastName}
                    </button>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600">
                    {lead.email || lead.phone || '—'}
                  </td>
                  <td className="px-4 py-3">
                    <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded">
                      {lead.source.replace(/_/g, ' ')}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className="text-xs px-2 py-0.5 rounded text-white"
                      style={{ backgroundColor: lead.stageColor || '#6B7280' }}
                    >
                      {lead.stageName}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600">
                    {lead.assignedStaffName || '—'}
                  </td>
                  <td className="px-4 py-3">
                    {!lead.convertedMemberId && (
                      <button
                        onClick={() => convertMutation.mutate(lead.id)}
                        className="text-xs text-green-600 hover:text-green-800"
                      >
                        {t('sales.convert')}
                      </button>
                    )}
                    {lead.convertedMemberId && (
                      <span className="text-xs text-green-600">{t('sales.convertedLabel')}</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showCreate && (
        <CreateLeadModal
          onClose={() => setShowCreate(false)}
          onSubmit={(data) => createMutation.mutate(data)}
          isLoading={createMutation.isPending}
        />
      )}

      {selectedLead && (
        <LeadDetailModal
          lead={selectedLead}
          onClose={() => setSelectedLead(null)}
        />
      )}
    </div>
  )
}

function CreateLeadModal({
  onClose,
  onSubmit,
  isLoading,
}: {
  onClose: () => void
  onSubmit: (data: Record<string, unknown>) => void
  isLoading: boolean
}) {
  const { t } = useTranslation()
  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    source: 'WEBSITE' as LeadSource,
    interest: '',
    notes: '',
  })

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
        <h2 className="text-lg font-bold mb-4">{t('sales.addLeadTitle')}</h2>
        <div className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <input
              placeholder={t('sales.firstName')}
              value={form.firstName}
              onChange={(e) => setForm({ ...form, firstName: e.target.value })}
              className="border rounded px-3 py-2 text-sm"
            />
            <input
              placeholder={t('sales.lastName')}
              value={form.lastName}
              onChange={(e) => setForm({ ...form, lastName: e.target.value })}
              className="border rounded px-3 py-2 text-sm"
            />
          </div>
          <input
            placeholder={t('sales.email')}
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
          />
          <input
            placeholder={t('sales.phone')}
            value={form.phone}
            onChange={(e) => setForm({ ...form, phone: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
          />
          <select
            value={form.source}
            onChange={(e) => setForm({ ...form, source: e.target.value as LeadSource })}
            className="w-full border rounded px-3 py-2 text-sm"
          >
            {LEAD_SOURCES.map((s) => (
              <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>
            ))}
          </select>
          <input
            placeholder={t('sales.interest')}
            value={form.interest}
            onChange={(e) => setForm({ ...form, interest: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
          />
          <textarea
            placeholder={t('sales.notes')}
            value={form.notes}
            onChange={(e) => setForm({ ...form, notes: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
            rows={2}
          />
        </div>
        <div className="flex justify-end gap-3 mt-6">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600">{t('sales.cancel')}</button>
          <button
            onClick={() => onSubmit({
              ...form,
              email: form.email || undefined,
              phone: form.phone || undefined,
              interest: form.interest || undefined,
              notes: form.notes || undefined,
            })}
            disabled={!form.firstName || !form.lastName || isLoading}
            className="bg-brand-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-brand-700 disabled:opacity-50"
          >
            {isLoading ? t('sales.creating') : t('sales.addLead')}
          </button>
        </div>
      </div>
    </div>
  )
}

function LeadDetailModal({
  lead,
  onClose,
}: {
  lead: LeadDto
  onClose: () => void
}) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const [showAddActivity, setShowAddActivity] = useState(false)

  const { data: pipelineRes } = useQuery({
    queryKey: ['sales-pipeline'],
    queryFn: () => api.get<ApiResponse<SalesPipelineDto>>('/sales/leads/pipeline').then(r => r.data),
  })
  const stages = pipelineRes?.data?.stages ?? []

  const moveStageMutation = useMutation({
    mutationFn: (stageId: string) => api.post(`/sales/leads/${lead.id}/move`, { stageId }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sales-pipeline'] })
      queryClient.invalidateQueries({ queryKey: ['leads'] })
      onClose()
    },
  })

  const { data: activities } = useQuery({
    queryKey: ['lead-activities', lead.id],
    queryFn: async () => {
      const res = await api.get<ApiResponse<LeadActivityDto[]>>(
        `/sales/activities/lead/${lead.id}`
      )
      return res.data
    },
  })

  const addActivityMutation = useMutation({
    mutationFn: (data: Record<string, unknown>) => api.post('/sales/activities', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['lead-activities', lead.id] })
      setShowAddActivity(false)
    },
  })

  const activityList = activities?.data ?? []

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto p-6">
        <div className="flex justify-between items-start mb-4">
          <div>
            <h2 className="text-lg font-bold">{lead.firstName} {lead.lastName}</h2>
            <p className="text-sm text-gray-500">
              {lead.source.replace(/_/g, ' ')}
              {lead.interest && <> &middot; {lead.interest}</>}
            </p>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl">&times;</button>
        </div>

        <div className="space-y-2 mb-4 text-sm">
          {lead.email && <p>{t('sales.email')}: {lead.email}</p>}
          {lead.phone && <p>{t('sales.phone')}: {lead.phone}</p>}
          {lead.stageName && <p>{t('sales.stage')}: <span className="font-medium" style={{ color: lead.stageColor || '#374151' }}>{lead.stageName}</span></p>}
          {lead.assignedStaffName && <p>{t('sales.assigned')}: {lead.assignedStaffName}</p>}
          {lead.notes && <p className="text-gray-600">{lead.notes}</p>}
        </div>

        {stages.length > 0 && (
          <div className="mb-4">
            <label className="block text-xs font-medium text-gray-500 mb-1">{t('sales.moveToStage')}</label>
            <div className="flex flex-wrap gap-1">
              {stages.map(s => (
                <button key={s.id} onClick={() => moveStageMutation.mutate(s.id)}
                  disabled={s.id === lead.stageId || moveStageMutation.isPending}
                  className={`text-xs px-2 py-1 rounded border ${s.id === lead.stageId ? 'bg-gray-200 text-gray-500 cursor-default' : 'hover:bg-gray-50 cursor-pointer'}`}
                  style={s.id !== lead.stageId ? { borderColor: s.color || '#d1d5db', color: s.color || '#374151' } : {}}>
                  {s.name}
                </button>
              ))}
            </div>
          </div>
        )}

        <div className="flex items-center justify-between mb-3">
          <h3 className="text-sm font-medium">{t('sales.activityLog')} ({activityList.length})</h3>
          <button
            onClick={() => setShowAddActivity(!showAddActivity)}
            className="text-xs text-brand-600 hover:text-brand-700"
          >
            {t('sales.addActivity')}
          </button>
        </div>

        {showAddActivity && (
          <AddActivityForm
            leadId={lead.id}
            onSubmit={(data) => addActivityMutation.mutate(data)}
            isLoading={addActivityMutation.isPending}
          />
        )}

        <div className="space-y-2">
          {activityList.map((a) => (
            <div key={a.id} className="bg-gray-50 rounded p-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-medium bg-brand-100 text-brand-700 px-2 py-0.5 rounded">
                  {a.activityType}
                </span>
                <span className="text-xs text-gray-400">
                  {new Date(a.createdAt).toLocaleDateString()}
                </span>
              </div>
              {a.description && <p className="text-sm text-gray-700 mt-1">{a.description}</p>}
              {a.outcome && <p className="text-xs text-gray-500 mt-1">{t('sales.outcome')}: {a.outcome}</p>}
              {a.staffName && <p className="text-xs text-gray-400 mt-1">{t('sales.by')} {a.staffName}</p>}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

function AddActivityForm({
  leadId,
  onSubmit,
  isLoading,
}: {
  leadId: string
  onSubmit: (data: Record<string, unknown>) => void
  isLoading: boolean
}) {
  const { t } = useTranslation()
  const [form, setForm] = useState({
    activityType: 'NOTE' as LeadActivityType,
    description: '',
    outcome: '',
    dueDate: '',
  })

  return (
    <div className="bg-blue-50 rounded p-3 mb-3 space-y-2">
      <select
        value={form.activityType}
        onChange={(e) => setForm({ ...form, activityType: e.target.value as LeadActivityType })}
        className="w-full border rounded px-2 py-1 text-xs"
      >
        {ACTIVITY_TYPES.map((t) => (
          <option key={t} value={t}>{t}</option>
        ))}
      </select>
      <input
        placeholder={t('communication.description')}
        value={form.description}
        onChange={(e) => setForm({ ...form, description: e.target.value })}
        className="w-full border rounded px-2 py-1 text-xs"
      />
      <div className="flex gap-2">
        <input
          placeholder={t('sales.outcome')}
          value={form.outcome}
          onChange={(e) => setForm({ ...form, outcome: e.target.value })}
          className="flex-1 border rounded px-2 py-1 text-xs"
        />
        {form.activityType === 'TASK' && (
          <input
            type="date"
            value={form.dueDate}
            onChange={(e) => setForm({ ...form, dueDate: e.target.value })}
            className="border rounded px-2 py-1 text-xs"
          />
        )}
      </div>
      <button
        onClick={() => onSubmit({
          leadId,
          activityType: form.activityType,
          description: form.description || undefined,
          outcome: form.outcome || undefined,
          dueDate: form.dueDate || undefined,
        })}
        disabled={isLoading}
        className="bg-brand-600 text-white px-3 py-1 rounded text-xs hover:bg-brand-700 disabled:opacity-50"
      >
        {isLoading ? t('sales.adding') : t('sales.add')}
      </button>
    </div>
  )
}

// ===================== PROMO CODES =====================

function PromoCodeList() {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const [showCreate, setShowCreate] = useState(false)

  const { data, isLoading } = useQuery({
    queryKey: ['promo-codes'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<PromoCodeDto[]>>('/sales/promo-codes?size=50')
      return res.data
    },
  })

  const createMutation = useMutation({
    mutationFn: (code: Record<string, unknown>) => api.post('/sales/promo-codes', code),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['promo-codes'] })
      setShowCreate(false)
    },
  })

  const deactivateMutation = useMutation({
    mutationFn: (id: string) => api.post(`/sales/promo-codes/${id}/deactivate`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['promo-codes'] }),
  })

  const codes = data?.data ?? []

  return (
    <div>
      <div className="flex justify-end mb-4">
        <button
          onClick={() => setShowCreate(true)}
          className="bg-brand-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-brand-700"
        >
          {t('sales.createPromoCode')}
        </button>
      </div>

      {isLoading ? (
        <p className="text-gray-500">{t('sales.loadingPromoCodes')}</p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {codes.map((code) => (
            <div key={code.id} className="bg-white rounded-lg shadow p-4">
              <div className="flex items-center justify-between mb-2">
                <span className="font-mono font-bold text-lg">{code.code}</span>
                <span className={`text-xs px-2 py-0.5 rounded ${
                  !code.active ? 'bg-gray-100 text-gray-500' :
                  code.expired ? 'bg-red-100 text-red-700' :
                  code.exhausted ? 'bg-yellow-100 text-yellow-700' :
                  'bg-green-100 text-green-700'
                }`}>
                  {!code.active ? t('communication.inactive') : code.expired ? t('sales.expired') : code.exhausted ? t('sales.exhausted') : t('communication.active')}
                </span>
              </div>
              {code.description && <p className="text-sm text-gray-600">{code.description}</p>}
              <p className="text-sm mt-2">
                <span className="font-medium">
                  {code.discountType === 'PERCENTAGE' ? `${code.discountValue}%` : `${code.discountValue} EUR`}
                </span>
                {' '}{t('sales.off')}
              </p>
              <div className="text-xs text-gray-500 mt-2 space-y-1">
                <p>{t('sales.used')} {code.currentUsages}{code.maxUsages ? ` / ${code.maxUsages}` : ''}</p>
                {code.expiresAt && <p>{t('sales.expires')} {new Date(code.expiresAt).toLocaleDateString()}</p>}
              </div>
              {code.active && !code.expired && (
                <button
                  onClick={() => deactivateMutation.mutate(code.id)}
                  className="text-xs text-red-500 hover:text-red-700 mt-3"
                >
                  {t('classes.deactivate')}
                </button>
              )}
            </div>
          ))}
        </div>
      )}

      {showCreate && (
        <CreatePromoModal
          onClose={() => setShowCreate(false)}
          onSubmit={(data) => createMutation.mutate(data)}
          isLoading={createMutation.isPending}
        />
      )}
    </div>
  )
}

function CreatePromoModal({
  onClose,
  onSubmit,
  isLoading,
}: {
  onClose: () => void
  onSubmit: (data: Record<string, unknown>) => void
  isLoading: boolean
}) {
  const { t } = useTranslation()
  const [form, setForm] = useState({
    code: '',
    description: '',
    discountType: 'PERCENTAGE' as DiscountType,
    discountValue: '',
    expiresAt: '',
    maxUsages: '',
  })

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
        <h2 className="text-lg font-bold mb-4">{t('sales.createPromoCode')}</h2>
        <div className="space-y-3">
          <input
            placeholder={t('sales.codePlaceholder')}
            value={form.code}
            onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })}
            className="w-full border rounded px-3 py-2 text-sm font-mono"
          />
          <input
            placeholder={t('sales.descriptionPlaceholder')}
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
          />
          <div className="grid grid-cols-2 gap-3">
            <select
              value={form.discountType}
              onChange={(e) => setForm({ ...form, discountType: e.target.value as DiscountType })}
              className="border rounded px-3 py-2 text-sm"
            >
              <option value="PERCENTAGE">{t('sales.percentage')}</option>
              <option value="FIXED">{t('sales.fixed')}</option>
            </select>
            <input
              type="number"
              placeholder={t('sales.valuePlaceholder')}
              value={form.discountValue}
              onChange={(e) => setForm({ ...form, discountValue: e.target.value })}
              className="border rounded px-3 py-2 text-sm"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <input
              type="datetime-local"
              value={form.expiresAt}
              onChange={(e) => setForm({ ...form, expiresAt: e.target.value })}
              className="border rounded px-3 py-2 text-sm"
              placeholder="Expires at"
            />
            <input
              type="number"
              placeholder={t('sales.maxUsages')}
              value={form.maxUsages}
              onChange={(e) => setForm({ ...form, maxUsages: e.target.value })}
              className="border rounded px-3 py-2 text-sm"
            />
          </div>
        </div>
        <div className="flex justify-end gap-3 mt-6">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600">{t('sales.cancel')}</button>
          <button
            onClick={() => onSubmit({
              code: form.code,
              description: form.description || undefined,
              discountType: form.discountType,
              discountValue: parseFloat(form.discountValue),
              expiresAt: form.expiresAt ? new Date(form.expiresAt).toISOString() : undefined,
              maxUsages: form.maxUsages ? parseInt(form.maxUsages) : undefined,
            })}
            disabled={!form.code || !form.discountValue || isLoading}
            className="bg-brand-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-brand-700 disabled:opacity-50"
          >
            {isLoading ? t('sales.creating') : t('sales.createCode')}
          </button>
        </div>
      </div>
    </div>
  )
}
