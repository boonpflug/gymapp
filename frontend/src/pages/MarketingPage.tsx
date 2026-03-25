import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../api/client'
import type {
  ApiResponse,
  PageMeta,
  CampaignDto,
  CampaignRecipientDto,
  CampaignStatsDto,
  AudienceCriteria,
  AudiencePreviewDto,
  AtRiskMemberDto,
  CampaignStatus,
  CampaignType,
} from '../types'

type Tab = 'campaigns' | 'audience' | 'atrisk'

export default function MarketingPage() {
  const { t } = useTranslation()
  const [activeTab, setActiveTab] = useState<Tab>('campaigns')

  const tabItems: { key: Tab; label: string }[] = [
    { key: 'campaigns', label: t('marketing.campaigns') },
    { key: 'audience', label: t('marketing.audienceBuilder') },
    { key: 'atrisk', label: t('marketing.atRiskMembers') },
  ]

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">{t('marketing.title')}</h1>
      <div className="border-b border-gray-200 mb-6">
        <nav className="flex space-x-8">
          {tabItems.map((tabItem) => (
            <button
              key={tabItem.key}
              onClick={() => setActiveTab(tabItem.key)}
              className={`py-3 px-1 border-b-2 text-sm font-medium ${
                activeTab === tabItem.key
                  ? 'border-brand-500 text-brand-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {tabItem.label}
            </button>
          ))}
        </nav>
      </div>
      {activeTab === 'campaigns' && <CampaignsTab />}
      {activeTab === 'audience' && <AudienceBuilderTab />}
      {activeTab === 'atrisk' && <AtRiskTab />}
    </div>
  )
}

// ── Campaigns Tab ──────────────────────────────────────

function CampaignsTab() {
  const { t } = useTranslation()
  const [page, setPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [showCreate, setShowCreate] = useState(false)
  const [selectedCampaign, setSelectedCampaign] = useState<CampaignDto | null>(null)

  const { data: statsData } = useQuery({
    queryKey: ['campaign-stats'],
    queryFn: () => api.get<ApiResponse<CampaignStatsDto>>('/marketing/campaigns/stats').then((r) => r.data),
  })

  const { data, isLoading } = useQuery({
    queryKey: ['campaigns', statusFilter, page],
    queryFn: () =>
      api
        .get<ApiResponse<CampaignDto[]>>('/marketing/campaigns', {
          params: { status: statusFilter || undefined, page, size: 20 },
        })
        .then((r) => r.data),
  })

  const campaigns = data?.data ?? []
  const meta = data?.meta as PageMeta | undefined
  const stats = statsData?.data

  return (
    <div>
      {/* Stats Cards */}
      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-6">
          <StatCard label={t('marketing.totalCampaigns')} value={stats.totalCampaigns} />
          <StatCard label={t('marketing.totalSent')} value={stats.totalSent} />
          <StatCard label={t('marketing.delivered')} value={stats.totalDelivered} />
          <StatCard label={t('marketing.opened')} value={stats.totalOpened} />
          <StatCard label={t('marketing.avgOpenRate')} value={`${stats.avgOpenRate.toFixed(1)}%`} />
        </div>
      )}

      {/* Filter + Create */}
      <div className="flex items-center justify-between mb-4">
        <select
          className="border rounded-lg px-3 py-2 text-sm"
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value)
            setPage(0)
          }}
        >
          <option value="">{t('marketing.allStatuses')}</option>
          <option value="DRAFT">{t('marketing.draft')}</option>
          <option value="SCHEDULED">{t('marketing.scheduled')}</option>
          <option value="SENDING">{t('marketing.sending')}</option>
          <option value="SENT">{t('marketing.sent')}</option>
          <option value="CANCELLED">{t('marketing.cancelled')}</option>
        </select>
        <button
          onClick={() => setShowCreate(true)}
          className="bg-brand-600 text-white px-4 py-2 rounded-lg hover:bg-brand-700 text-sm"
        >
          {t('marketing.newCampaign')}
        </button>
      </div>

      {/* Campaigns Table */}
      {isLoading ? (
        <p className="text-gray-500 text-sm">{t('marketing.loading')}</p>
      ) : campaigns.length === 0 ? (
        <p className="text-gray-500 text-sm">{t('marketing.noCampaignsFound')}</p>
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Type</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Recipients</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Sent</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Open Rate</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {campaigns.map((c) => (
                <tr key={c.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm font-medium text-gray-900">
                    <button onClick={() => setSelectedCampaign(c)} className="hover:text-brand-600">
                      {c.name}
                    </button>
                  </td>
                  <td className="px-4 py-3 text-sm">
                    <span className="bg-blue-100 text-blue-700 px-2 py-0.5 rounded text-xs">{c.campaignType}</span>
                  </td>
                  <td className="px-4 py-3 text-sm">
                    <CampaignStatusBadge status={c.status} />
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600">{c.totalRecipients ?? 0}</td>
                  <td className="px-4 py-3 text-sm text-gray-600">{c.sentCount ?? 0}</td>
                  <td className="px-4 py-3 text-sm text-gray-600">
                    {c.openRate != null ? `${c.openRate.toFixed(1)}%` : '—'}
                  </td>
                  <td className="px-4 py-3 text-sm">
                    <CampaignActions campaign={c} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {meta && meta.totalPages > 1 && (
            <div className="flex items-center justify-between px-4 py-3 border-t">
              <p className="text-sm text-gray-500">
                Page {meta.page + 1} of {meta.totalPages} ({meta.totalElements} total)
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-3 py-1 border rounded text-sm disabled:opacity-50"
                >
                  Previous
                </button>
                <button
                  onClick={() => setPage((p) => p + 1)}
                  disabled={page >= (meta.totalPages ?? 1) - 1}
                  className="px-3 py-1 border rounded text-sm disabled:opacity-50"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {showCreate && <CreateCampaignModal onClose={() => setShowCreate(false)} />}
      {selectedCampaign && (
        <CampaignDetailModal campaign={selectedCampaign} onClose={() => setSelectedCampaign(null)} />
      )}
    </div>
  )
}

// ── Campaign Actions ───────────────────────────────────

function CampaignActions({ campaign }: { campaign: CampaignDto }) {
  const qc = useQueryClient()

  const sendNow = useMutation({
    mutationFn: () => api.post(`/marketing/campaigns/${campaign.id}/send`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['campaigns'] })
      qc.invalidateQueries({ queryKey: ['campaign-stats'] })
    },
  })

  const schedule = useMutation({
    mutationFn: () => api.post(`/marketing/campaigns/${campaign.id}/schedule`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['campaigns'] }),
  })

  const cancel = useMutation({
    mutationFn: () => api.post(`/marketing/campaigns/${campaign.id}/cancel`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['campaigns'] }),
  })

  return (
    <div className="flex gap-2">
      {(campaign.status === 'DRAFT' || campaign.status === 'SCHEDULED') && (
        <button
          onClick={() => sendNow.mutate()}
          disabled={sendNow.isPending}
          className="text-brand-600 hover:text-brand-700 text-xs font-medium"
        >
          Send Now
        </button>
      )}
      {campaign.status === 'DRAFT' && campaign.scheduledAt && (
        <button
          onClick={() => schedule.mutate()}
          disabled={schedule.isPending}
          className="text-green-600 hover:text-green-800 text-xs font-medium"
        >
          Schedule
        </button>
      )}
      {campaign.status !== 'SENT' && campaign.status !== 'CANCELLED' && (
        <button
          onClick={() => cancel.mutate()}
          disabled={cancel.isPending}
          className="text-red-600 hover:text-red-800 text-xs font-medium"
        >
          Cancel
        </button>
      )}
    </div>
  )
}

// ── Create Campaign Modal ──────────────────────────────

function CreateCampaignModal({ onClose }: { onClose: () => void }) {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const [form, setForm] = useState({
    name: '',
    description: '',
    campaignType: 'EMAIL' as CampaignType,
    subject: '',
    bodyHtml: '',
    bodyText: '',
    scheduledAt: '',
    audienceCriteria: {
      memberStatuses: [] as string[],
      noCheckInDays: undefined as number | undefined,
      contractStatus: '',
      contractExpiresWithinDays: undefined as number | undefined,
      gender: '',
    },
  })

  const [previewData, setPreviewData] = useState<AudiencePreviewDto | null>(null)

  const createMutation = useMutation({
    mutationFn: (data: typeof form) =>
      api.post<ApiResponse<CampaignDto>>('/marketing/campaigns', {
        ...data,
        scheduledAt: data.scheduledAt ? new Date(data.scheduledAt).toISOString() : null,
        audienceCriteria: Object.fromEntries(
          Object.entries(data.audienceCriteria).filter(([, v]) => v !== '' && v !== undefined && !(Array.isArray(v) && v.length === 0))
        ),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['campaigns'] })
      onClose()
    },
  })

  const previewMutation = useMutation({
    mutationFn: (criteria: AudienceCriteria) =>
      api.post<ApiResponse<AudiencePreviewDto>>('/marketing/campaigns/audience/preview', criteria).then((r) => r.data),
    onSuccess: (data) => setPreviewData(data.data),
  })

  const handlePreview = () => {
    const criteria: AudienceCriteria = Object.fromEntries(
      Object.entries(form.audienceCriteria).filter(([, v]) => v !== '' && v !== undefined && !(Array.isArray(v) && v.length === 0))
    )
    previewMutation.mutate(criteria)
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 overflow-auto">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl p-6 m-4 max-h-[90vh] overflow-y-auto">
        <h2 className="text-lg font-semibold mb-4">{t('marketing.createCampaign')}</h2>
        <form
          onSubmit={(e) => {
            e.preventDefault()
            createMutation.mutate(form)
          }}
          className="space-y-4"
        >
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Name *</label>
              <input
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Type *</label>
              <select
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.campaignType}
                onChange={(e) => setForm({ ...form, campaignType: e.target.value as CampaignType })}
              >
                <option value="EMAIL">Email</option>
                <option value="SMS">SMS</option>
                <option value="PUSH">Push Notification</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
            <input
              className="w-full border rounded-lg px-3 py-2 text-sm"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Subject</label>
            <input
              className="w-full border rounded-lg px-3 py-2 text-sm"
              value={form.subject}
              onChange={(e) => setForm({ ...form, subject: e.target.value })}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Body (HTML)</label>
            <textarea
              className="w-full border rounded-lg px-3 py-2 text-sm"
              rows={4}
              value={form.bodyHtml}
              onChange={(e) => setForm({ ...form, bodyHtml: e.target.value })}
              placeholder={t('marketing.bodyHtmlPlaceholder')}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Schedule (optional)</label>
            <input
              type="datetime-local"
              className="w-full border rounded-lg px-3 py-2 text-sm"
              value={form.scheduledAt}
              onChange={(e) => setForm({ ...form, scheduledAt: e.target.value })}
            />
          </div>

          {/* Audience Criteria */}
          <div className="border-t pt-4">
            <h3 className="text-sm font-semibold text-gray-700 mb-3">Audience Criteria</h3>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs text-gray-500 mb-1">Member Status</label>
                <select
                  className="w-full border rounded px-2 py-1.5 text-sm"
                  multiple
                  size={3}
                  value={form.audienceCriteria.memberStatuses}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      audienceCriteria: {
                        ...form.audienceCriteria,
                        memberStatuses: Array.from(e.target.selectedOptions, (o) => o.value),
                      },
                    })
                  }
                >
                  <option value="ACTIVE">Active</option>
                  <option value="INACTIVE">Inactive</option>
                </select>
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1">No check-in for X days</label>
                <input
                  type="number"
                  className="w-full border rounded px-2 py-1.5 text-sm"
                  placeholder="e.g. 14"
                  value={form.audienceCriteria.noCheckInDays ?? ''}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      audienceCriteria: {
                        ...form.audienceCriteria,
                        noCheckInDays: e.target.value ? parseInt(e.target.value) : undefined,
                      },
                    })
                  }
                />
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1">Contract Status</label>
                <select
                  className="w-full border rounded px-2 py-1.5 text-sm"
                  value={form.audienceCriteria.contractStatus}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      audienceCriteria: { ...form.audienceCriteria, contractStatus: e.target.value },
                    })
                  }
                >
                  <option value="">Any</option>
                  <option value="ACTIVE">Active</option>
                  <option value="PENDING_CANCELLATION">Pending Cancellation</option>
                  <option value="CANCELLED">Cancelled</option>
                  <option value="EXPIRED">Expired</option>
                </select>
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1">Contract expires within (days)</label>
                <input
                  type="number"
                  className="w-full border rounded px-2 py-1.5 text-sm"
                  placeholder="e.g. 30"
                  value={form.audienceCriteria.contractExpiresWithinDays ?? ''}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      audienceCriteria: {
                        ...form.audienceCriteria,
                        contractExpiresWithinDays: e.target.value ? parseInt(e.target.value) : undefined,
                      },
                    })
                  }
                />
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1">Gender</label>
                <select
                  className="w-full border rounded px-2 py-1.5 text-sm"
                  value={form.audienceCriteria.gender}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      audienceCriteria: { ...form.audienceCriteria, gender: e.target.value },
                    })
                  }
                >
                  <option value="">Any</option>
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
            </div>

            <button
              type="button"
              onClick={handlePreview}
              disabled={previewMutation.isPending}
              className="mt-3 text-sm text-brand-600 hover:text-brand-700 font-medium"
            >
              {previewMutation.isPending ? 'Loading...' : 'Preview Audience'}
            </button>

            {previewData && (
              <div className="mt-2 bg-gray-50 rounded-lg p-3">
                <p className="text-sm font-medium text-gray-700 mb-1">
                  Matching members: <span className="text-brand-600">{previewData.totalCount}</span>
                </p>
                {previewData.sample.length > 0 && (
                  <div className="text-xs text-gray-500 space-y-0.5">
                    {previewData.sample.slice(0, 5).map((m) => (
                      <p key={m.memberId}>
                        {m.firstName} {m.lastName} — {m.email} ({m.status})
                      </p>
                    ))}
                    {previewData.totalCount > 5 && <p className="italic">...and {previewData.totalCount - 5} more</p>}
                  </div>
                )}
              </div>
            )}
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">
              Cancel
            </button>
            <button
              type="submit"
              disabled={!form.name || createMutation.isPending}
              className="bg-brand-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-brand-700 disabled:opacity-50"
            >
              {createMutation.isPending ? 'Creating...' : 'Create Campaign'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

// ── Campaign Detail Modal ──────────────────────────────

function CampaignDetailModal({ campaign, onClose }: { campaign: CampaignDto; onClose: () => void }) {
  const { t } = useTranslation()
  const [recipientPage, setRecipientPage] = useState(0)

  const { data: recipientsData } = useQuery({
    queryKey: ['campaign-recipients', campaign.id, recipientPage],
    queryFn: () =>
      api
        .get<ApiResponse<CampaignRecipientDto[]>>(`/marketing/campaigns/${campaign.id}/recipients`, {
          params: { page: recipientPage, size: 20 },
        })
        .then((r) => r.data),
  })

  const recipients = recipientsData?.data ?? []
  const recipientMeta = recipientsData?.meta as PageMeta | undefined

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-3xl p-6 m-4 max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-start mb-4">
          <div>
            <h2 className="text-lg font-semibold">{campaign.name}</h2>
            <div className="flex gap-2 mt-1">
              <CampaignStatusBadge status={campaign.status} />
              <span className="bg-blue-100 text-blue-700 px-2 py-0.5 rounded text-xs">{campaign.campaignType}</span>
            </div>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl">
            &times;
          </button>
        </div>

        {campaign.description && <p className="text-sm text-gray-600 mb-4">{campaign.description}</p>}

        {/* Performance Stats */}
        <div className="grid grid-cols-3 md:grid-cols-6 gap-3 mb-4">
          <MiniStat label="Recipients" value={campaign.totalRecipients ?? 0} />
          <MiniStat label="Sent" value={campaign.sentCount ?? 0} />
          <MiniStat label="Delivered" value={campaign.deliveredCount ?? 0} />
          <MiniStat label="Opened" value={campaign.openedCount ?? 0} />
          <MiniStat label="Clicked" value={campaign.clickedCount ?? 0} />
          <MiniStat label="Failed" value={campaign.failedCount ?? 0} />
        </div>

        {/* Rates */}
        <div className="grid grid-cols-3 gap-3 mb-6">
          <RateBar label="Delivery Rate" rate={campaign.deliveryRate} />
          <RateBar label="Open Rate" rate={campaign.openRate} />
          <RateBar label="Click Rate" rate={campaign.clickRate} />
        </div>

        {/* Recipients List */}
        <h3 className="text-sm font-semibold text-gray-700 mb-2">{t('marketing.recipientsTitle')}</h3>
        {recipients.length === 0 ? (
          <p className="text-sm text-gray-400">{t('marketing.noRecipientsYet')}</p>
        ) : (
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">Name</th>
                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">Address</th>
                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">Status</th>
                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">Sent</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {recipients.map((r) => (
                <tr key={r.id}>
                  <td className="px-3 py-2">{r.memberName ?? '—'}</td>
                  <td className="px-3 py-2 text-gray-500">{r.recipientAddress ?? '—'}</td>
                  <td className="px-3 py-2">
                    {r.status ? (
                      <span
                        className={`px-2 py-0.5 rounded text-xs ${
                          r.status === 'SENT' || r.status === 'DELIVERED'
                            ? 'bg-green-100 text-green-700'
                            : r.status === 'OPENED' || r.status === 'CLICKED'
                              ? 'bg-blue-100 text-blue-700'
                              : r.status === 'FAILED'
                                ? 'bg-red-100 text-red-700'
                                : 'bg-gray-100 text-gray-700'
                        }`}
                      >
                        {r.status}
                      </span>
                    ) : (
                      '—'
                    )}
                  </td>
                  <td className="px-3 py-2 text-gray-500">
                    {r.sentAt ? new Date(r.sentAt).toLocaleString() : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {recipientMeta && recipientMeta.totalPages > 1 && (
          <div className="flex justify-between items-center mt-2 text-xs text-gray-500">
            <span>
              Page {recipientMeta.page + 1} of {recipientMeta.totalPages}
            </span>
            <div className="flex gap-2">
              <button
                onClick={() => setRecipientPage((p) => Math.max(0, p - 1))}
                disabled={recipientPage === 0}
                className="px-2 py-1 border rounded disabled:opacity-50"
              >
                Prev
              </button>
              <button
                onClick={() => setRecipientPage((p) => p + 1)}
                disabled={recipientPage >= recipientMeta.totalPages - 1}
                className="px-2 py-1 border rounded disabled:opacity-50"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

// ── Audience Builder Tab ───────────────────────────────

function AudienceBuilderTab() {
  const { t } = useTranslation()
  const [criteria, setCriteria] = useState<AudienceCriteria>({})
  const [preview, setPreview] = useState<AudiencePreviewDto | null>(null)

  const previewMutation = useMutation({
    mutationFn: (c: AudienceCriteria) =>
      api.post<ApiResponse<AudiencePreviewDto>>('/marketing/campaigns/audience/preview', c).then((r) => r.data),
    onSuccess: (data) => setPreview(data.data),
  })

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      {/* Criteria Panel */}
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-sm font-semibold text-gray-700 mb-4">{t('marketing.defineAudienceSegment')}</h3>
        <div className="space-y-4">
          <div>
            <label className="block text-xs text-gray-500 mb-1">Member Status</label>
            <select
              className="w-full border rounded px-3 py-2 text-sm"
              multiple
              size={3}
              value={criteria.memberStatuses ?? []}
              onChange={(e) =>
                setCriteria({
                  ...criteria,
                  memberStatuses: Array.from(e.target.selectedOptions, (o) => o.value),
                })
              }
            >
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </select>
          </div>

          <div>
            <label className="block text-xs text-gray-500 mb-1">No check-in for X days</label>
            <input
              type="number"
              className="w-full border rounded px-3 py-2 text-sm"
              placeholder="e.g. 14"
              value={criteria.noCheckInDays ?? ''}
              onChange={(e) =>
                setCriteria({
                  ...criteria,
                  noCheckInDays: e.target.value ? parseInt(e.target.value) : undefined,
                })
              }
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs text-gray-500 mb-1">Min check-in interval (days)</label>
              <input
                type="number"
                className="w-full border rounded px-3 py-2 text-sm"
                value={criteria.minCheckInFrequencyDays ?? ''}
                onChange={(e) =>
                  setCriteria({
                    ...criteria,
                    minCheckInFrequencyDays: e.target.value ? parseInt(e.target.value) : undefined,
                  })
                }
              />
            </div>
            <div>
              <label className="block text-xs text-gray-500 mb-1">Max check-in interval (days)</label>
              <input
                type="number"
                className="w-full border rounded px-3 py-2 text-sm"
                value={criteria.maxCheckInFrequencyDays ?? ''}
                onChange={(e) =>
                  setCriteria({
                    ...criteria,
                    maxCheckInFrequencyDays: e.target.value ? parseInt(e.target.value) : undefined,
                  })
                }
              />
            </div>
          </div>

          <div>
            <label className="block text-xs text-gray-500 mb-1">Contract Status</label>
            <select
              className="w-full border rounded px-3 py-2 text-sm"
              value={criteria.contractStatus ?? ''}
              onChange={(e) => setCriteria({ ...criteria, contractStatus: e.target.value || undefined })}
            >
              <option value="">Any</option>
              <option value="ACTIVE">Active</option>
              <option value="PENDING_CANCELLATION">Pending Cancellation</option>
              <option value="CANCELLED">Cancelled</option>
              <option value="EXPIRED">Expired</option>
            </select>
          </div>

          <div>
            <label className="block text-xs text-gray-500 mb-1">Contract expires within (days)</label>
            <input
              type="number"
              className="w-full border rounded px-3 py-2 text-sm"
              placeholder="e.g. 30"
              value={criteria.contractExpiresWithinDays ?? ''}
              onChange={(e) =>
                setCriteria({
                  ...criteria,
                  contractExpiresWithinDays: e.target.value ? parseInt(e.target.value) : undefined,
                })
              }
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs text-gray-500 mb-1">Joined After</label>
              <input
                type="date"
                className="w-full border rounded px-3 py-2 text-sm"
                value={criteria.joinedAfter ?? ''}
                onChange={(e) => setCriteria({ ...criteria, joinedAfter: e.target.value || undefined })}
              />
            </div>
            <div>
              <label className="block text-xs text-gray-500 mb-1">Joined Before</label>
              <input
                type="date"
                className="w-full border rounded px-3 py-2 text-sm"
                value={criteria.joinedBefore ?? ''}
                onChange={(e) => setCriteria({ ...criteria, joinedBefore: e.target.value || undefined })}
              />
            </div>
          </div>

          <div>
            <label className="block text-xs text-gray-500 mb-1">Gender</label>
            <select
              className="w-full border rounded px-3 py-2 text-sm"
              value={criteria.gender ?? ''}
              onChange={(e) => setCriteria({ ...criteria, gender: e.target.value || undefined })}
            >
              <option value="">Any</option>
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
              <option value="OTHER">Other</option>
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs text-gray-500 mb-1">Min Age</label>
              <input
                type="number"
                className="w-full border rounded px-3 py-2 text-sm"
                value={criteria.minAge ?? ''}
                onChange={(e) =>
                  setCriteria({ ...criteria, minAge: e.target.value ? parseInt(e.target.value) : undefined })
                }
              />
            </div>
            <div>
              <label className="block text-xs text-gray-500 mb-1">Max Age</label>
              <input
                type="number"
                className="w-full border rounded px-3 py-2 text-sm"
                value={criteria.maxAge ?? ''}
                onChange={(e) =>
                  setCriteria({ ...criteria, maxAge: e.target.value ? parseInt(e.target.value) : undefined })
                }
              />
            </div>
          </div>

          <button
            onClick={() => previewMutation.mutate(criteria)}
            disabled={previewMutation.isPending}
            className="w-full bg-brand-600 text-white py-2 rounded-lg text-sm hover:bg-brand-700 disabled:opacity-50"
          >
            {previewMutation.isPending ? 'Loading...' : 'Preview Audience'}
          </button>
        </div>
      </div>

      {/* Preview Panel */}
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-sm font-semibold text-gray-700 mb-4">Audience Preview</h3>
        {!preview ? (
          <p className="text-sm text-gray-400">Configure criteria and click Preview to see matching members.</p>
        ) : (
          <>
            <div className="bg-brand-50 rounded-lg p-4 mb-4">
              <p className="text-2xl font-bold text-brand-600">{preview.totalCount}</p>
              <p className="text-sm text-gray-600">members match your criteria</p>
            </div>
            {preview.sample.length > 0 && (
              <table className="min-w-full divide-y divide-gray-200 text-sm">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">Name</th>
                    <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">Email</th>
                    <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {preview.sample.map((m) => (
                    <tr key={m.memberId}>
                      <td className="px-3 py-2">
                        {m.firstName} {m.lastName}
                      </td>
                      <td className="px-3 py-2 text-gray-500">{m.email}</td>
                      <td className="px-3 py-2">
                        <span className="bg-gray-100 text-gray-700 px-2 py-0.5 rounded text-xs">{m.status}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </>
        )}
      </div>
    </div>
  )
}

// ── At-Risk Members Tab ────────────────────────────────

function AtRiskTab() {
  const { t } = useTranslation()
  const [inactiveDays, setInactiveDays] = useState(14)

  const { data: summaryData } = useQuery({
    queryKey: ['atrisk-summary', inactiveDays],
    queryFn: () =>
      api
        .get<ApiResponse<Record<string, number>>>('/marketing/at-risk/summary', {
          params: { inactiveDays },
        })
        .then((r) => r.data),
  })

  const { data, isLoading } = useQuery({
    queryKey: ['atrisk-members', inactiveDays],
    queryFn: () =>
      api
        .get<ApiResponse<AtRiskMemberDto[]>>('/marketing/at-risk', {
          params: { inactiveDays },
        })
        .then((r) => r.data),
  })

  const members = data?.data ?? []
  const summary = summaryData?.data

  return (
    <div>
      {/* Summary Cards */}
      {summary && (
        <div className="grid grid-cols-3 gap-4 mb-6">
          <div className="bg-red-50 rounded-lg p-4">
            <p className="text-2xl font-bold text-red-600">{summary.HIGH ?? 0}</p>
            <p className="text-sm text-gray-600">{t('marketing.highRisk')}</p>
          </div>
          <div className="bg-yellow-50 rounded-lg p-4">
            <p className="text-2xl font-bold text-yellow-600">{summary.MEDIUM ?? 0}</p>
            <p className="text-sm text-gray-600">{t('marketing.mediumRisk')}</p>
          </div>
          <div className="bg-blue-50 rounded-lg p-4">
            <p className="text-2xl font-bold text-blue-600">{summary.LOW ?? 0}</p>
            <p className="text-sm text-gray-600">{t('marketing.lowRisk')}</p>
          </div>
        </div>
      )}

      {/* Inactive Threshold */}
      <div className="flex items-center gap-3 mb-4">
        <label className="text-sm text-gray-600">Inactive threshold (days):</label>
        <input
          type="number"
          className="border rounded px-3 py-1.5 text-sm w-20"
          value={inactiveDays}
          onChange={(e) => setInactiveDays(parseInt(e.target.value) || 14)}
          min={1}
        />
      </div>

      {/* At-Risk Members Table */}
      {isLoading ? (
        <p className="text-gray-500 text-sm">Analyzing member activity...</p>
      ) : members.length === 0 ? (
        <div className="bg-green-50 rounded-lg p-6 text-center">
          <p className="text-green-700 font-medium">No at-risk members detected!</p>
          <p className="text-sm text-gray-500 mt-1">All active members are visiting regularly.</p>
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Member</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Risk Level</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Last Visit</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Days Inactive</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Avg Weekly Visits</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Trend</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Reason</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {members.map((m) => (
                <tr key={m.memberId} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm">
                    <div>
                      <p className="font-medium text-gray-900">
                        {m.firstName} {m.lastName}
                      </p>
                      <p className="text-xs text-gray-500">{m.email}</p>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-sm">
                    <RiskBadge level={m.riskLevel} />
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600">
                    {m.lastCheckIn ? new Date(m.lastCheckIn).toLocaleDateString() : 'Never'}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600">{m.daysSinceLastCheckIn}</td>
                  <td className="px-4 py-3 text-sm text-gray-600">{m.avgWeeklyVisits.toFixed(1)}</td>
                  <td className="px-4 py-3 text-sm">
                    <span
                      className={`text-xs font-medium ${
                        m.visitTrend > 0 ? 'text-green-600' : m.visitTrend < 0 ? 'text-red-600' : 'text-gray-500'
                      }`}
                    >
                      {m.visitTrend > 0 ? '+' : ''}
                      {m.visitTrend.toFixed(0)}%
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-500">{m.riskReason}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

// ── Helper Components ──────────────────────────────────

function StatCard({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="bg-white rounded-lg shadow p-4">
      <p className="text-2xl font-bold text-gray-900">{value}</p>
      <p className="text-xs text-gray-500">{label}</p>
    </div>
  )
}

function MiniStat({ label, value }: { label: string; value: number }) {
  return (
    <div className="bg-gray-50 rounded-lg p-3 text-center">
      <p className="text-lg font-semibold text-gray-900">{value}</p>
      <p className="text-xs text-gray-500">{label}</p>
    </div>
  )
}

function RateBar({ label, rate }: { label: string; rate?: number | null }) {
  const val = rate ?? 0
  return (
    <div className="bg-gray-50 rounded-lg p-3">
      <div className="flex justify-between text-xs mb-1">
        <span className="text-gray-500">{label}</span>
        <span className="font-medium">{val.toFixed(1)}%</span>
      </div>
      <div className="w-full bg-gray-200 rounded-full h-2">
        <div className="bg-brand-600 h-2 rounded-full" style={{ width: `${Math.min(val, 100)}%` }} />
      </div>
    </div>
  )
}

function CampaignStatusBadge({ status }: { status: CampaignStatus }) {
  const colors: Record<CampaignStatus, string> = {
    DRAFT: 'bg-gray-100 text-gray-700',
    SCHEDULED: 'bg-yellow-100 text-yellow-700',
    SENDING: 'bg-blue-100 text-blue-700',
    SENT: 'bg-green-100 text-green-700',
    CANCELLED: 'bg-red-100 text-red-700',
  }
  return <span className={`px-2 py-0.5 rounded text-xs ${colors[status]}`}>{status}</span>
}

function RiskBadge({ level }: { level: string }) {
  const colors: Record<string, string> = {
    HIGH: 'bg-red-100 text-red-700',
    MEDIUM: 'bg-yellow-100 text-yellow-700',
    LOW: 'bg-blue-100 text-blue-700',
  }
  return <span className={`px-2 py-0.5 rounded text-xs font-medium ${colors[level] ?? 'bg-gray-100 text-gray-700'}`}>{level}</span>
}
