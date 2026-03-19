import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../api/client'
import type {
  ApiResponse,
  CommunicationTemplateDto,
  NotificationRuleDto,
  SentMessageDto,
  MessageStatsDto,
  ChannelType,
  TriggerEvent,
  DelayDirection,
  MessageStatus,
} from '../types'

const CHANNEL_TYPES: ChannelType[] = ['EMAIL', 'SMS', 'PUSH', 'LETTER', 'WHATSAPP']
const TRIGGER_EVENTS: TriggerEvent[] = [
  'WELCOME', 'BIRTHDAY', 'NEW_MEMBERSHIP', 'PAYMENT_FAILED', 'PAYMENT_SUCCESS',
  'APPOINTMENT_REMINDER', 'CONTRACT_EXPIRY', 'CONTRACT_CANCELLATION', 'TRIAL_BOOKED',
  'MEMBERSHIP_ANNIVERSARY', 'CLASS_REMINDER', 'TRAINING_PLAN_PUBLISHED', 'CUSTOM',
]
const DELAY_DIRECTIONS: DelayDirection[] = ['IMMEDIATE', 'BEFORE', 'AFTER']

type Tab = 'templates' | 'rules' | 'messages' | 'stats'

export default function CommunicationPage() {
  const [activeTab, setActiveTab] = useState<Tab>('templates')

  const tabs: { key: Tab; label: string }[] = [
    { key: 'templates', label: 'Templates' },
    { key: 'rules', label: 'Notification Rules' },
    { key: 'messages', label: 'Message History' },
    { key: 'stats', label: 'Stats' },
  ]

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Communication</h1>
      <div className="border-b border-gray-200 mb-6">
        <nav className="flex space-x-8">
          {tabs.map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`pb-3 px-1 text-sm font-medium border-b-2 ${
                activeTab === tab.key
                  ? 'border-indigo-500 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {activeTab === 'templates' && <TemplateList />}
      {activeTab === 'rules' && <RuleList />}
      {activeTab === 'messages' && <MessageHistory />}
      {activeTab === 'stats' && <StatsView />}
    </div>
  )
}

// ===================== TEMPLATES =====================

function TemplateList() {
  const queryClient = useQueryClient()
  const [showCreate, setShowCreate] = useState(false)
  const [selectedTemplate, setSelectedTemplate] = useState<CommunicationTemplateDto | null>(null)
  const [channelFilter, setChannelFilter] = useState<string>('')

  const { data, isLoading } = useQuery({
    queryKey: ['comm-templates', channelFilter],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '50' })
      if (channelFilter) params.set('channelType', channelFilter)
      const res = await api.get<ApiResponse<CommunicationTemplateDto[]>>(
        `/communication/templates?${params}`
      )
      return res.data
    },
  })

  const createMutation = useMutation({
    mutationFn: (template: Record<string, unknown>) =>
      api.post('/communication/templates', template),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['comm-templates'] })
      setShowCreate(false)
    },
  })

  const deactivateMutation = useMutation({
    mutationFn: (id: string) => api.post(`/communication/templates/${id}/deactivate`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['comm-templates'] })
      setSelectedTemplate(null)
    },
  })

  const templates = data?.data ?? []

  const channelColor = (type: ChannelType) => {
    const colors: Record<ChannelType, string> = {
      EMAIL: 'bg-blue-100 text-blue-700',
      SMS: 'bg-green-100 text-green-700',
      PUSH: 'bg-purple-100 text-purple-700',
      LETTER: 'bg-gray-100 text-gray-700',
      WHATSAPP: 'bg-emerald-100 text-emerald-700',
    }
    return colors[type] || 'bg-gray-100 text-gray-600'
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <select
          value={channelFilter}
          onChange={(e) => setChannelFilter(e.target.value)}
          className="border rounded-lg px-3 py-2 text-sm"
        >
          <option value="">All Channels</option>
          {CHANNEL_TYPES.map((c) => (
            <option key={c} value={c}>{c}</option>
          ))}
        </select>
        <button
          onClick={() => setShowCreate(true)}
          className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-indigo-700"
        >
          Create Template
        </button>
      </div>

      {isLoading ? (
        <p className="text-gray-500">Loading templates...</p>
      ) : (
        <div className="space-y-3">
          {templates.map((t) => (
            <div
              key={t.id}
              onClick={() => setSelectedTemplate(t)}
              className="bg-white rounded-lg shadow p-4 cursor-pointer hover:shadow-md transition-shadow"
            >
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="font-medium text-gray-900">{t.name}</h3>
                  {t.subject && <p className="text-sm text-gray-500 mt-1">Subject: {t.subject}</p>}
                </div>
                <div className="flex items-center gap-2">
                  <span className={`text-xs px-2 py-0.5 rounded ${channelColor(t.channelType)}`}>
                    {t.channelType}
                  </span>
                  {t.category && (
                    <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded">{t.category}</span>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {showCreate && (
        <CreateTemplateModal
          onClose={() => setShowCreate(false)}
          onSubmit={(data) => createMutation.mutate(data)}
          isLoading={createMutation.isPending}
        />
      )}

      {selectedTemplate && (
        <TemplateDetailModal
          template={selectedTemplate}
          onClose={() => setSelectedTemplate(null)}
          onDeactivate={(id) => deactivateMutation.mutate(id)}
        />
      )}
    </div>
  )
}

function CreateTemplateModal({
  onClose,
  onSubmit,
  isLoading,
}: {
  onClose: () => void
  onSubmit: (data: Record<string, unknown>) => void
  isLoading: boolean
}) {
  const [form, setForm] = useState({
    name: '',
    channelType: 'EMAIL' as ChannelType,
    subject: '',
    bodyHtml: '',
    bodyText: '',
    category: '',
    locale: 'en',
    brandColor: '#4F46E5',
  })

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto p-6">
        <h2 className="text-lg font-bold mb-4">Create Template</h2>
        <div className="space-y-3">
          <input
            placeholder="Template name *"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
          />
          <select
            value={form.channelType}
            onChange={(e) => setForm({ ...form, channelType: e.target.value as ChannelType })}
            className="w-full border rounded px-3 py-2 text-sm"
          >
            {CHANNEL_TYPES.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
          <input
            placeholder="Subject (for email)"
            value={form.subject}
            onChange={(e) => setForm({ ...form, subject: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
          />
          <textarea
            placeholder="Body HTML — use {{member.firstName}}, {{studio.name}}, etc."
            value={form.bodyHtml}
            onChange={(e) => setForm({ ...form, bodyHtml: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm font-mono"
            rows={6}
          />
          <textarea
            placeholder="Body plain text (fallback)"
            value={form.bodyText}
            onChange={(e) => setForm({ ...form, bodyText: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
            rows={3}
          />
          <div className="grid grid-cols-3 gap-3">
            <input
              placeholder="Category"
              value={form.category}
              onChange={(e) => setForm({ ...form, category: e.target.value })}
              className="border rounded px-3 py-2 text-sm"
            />
            <input
              placeholder="Locale"
              value={form.locale}
              onChange={(e) => setForm({ ...form, locale: e.target.value })}
              className="border rounded px-3 py-2 text-sm"
            />
            <input
              type="color"
              value={form.brandColor}
              onChange={(e) => setForm({ ...form, brandColor: e.target.value })}
              className="border rounded px-1 py-1 h-10"
              title="Brand color"
            />
          </div>
        </div>
        <div className="flex justify-end gap-3 mt-6">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600">Cancel</button>
          <button
            onClick={() => onSubmit({
              ...form,
              subject: form.subject || undefined,
              bodyText: form.bodyText || undefined,
              category: form.category || undefined,
            })}
            disabled={!form.name || isLoading}
            className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-indigo-700 disabled:opacity-50"
          >
            {isLoading ? 'Creating...' : 'Create Template'}
          </button>
        </div>
      </div>
    </div>
  )
}

function TemplateDetailModal({
  template,
  onClose,
  onDeactivate,
}: {
  template: CommunicationTemplateDto
  onClose: () => void
  onDeactivate: (id: string) => void
}) {
  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto p-6">
        <div className="flex justify-between items-start mb-4">
          <h2 className="text-lg font-bold">{template.name}</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl">&times;</button>
        </div>
        <div className="space-y-3">
          <span className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded">{template.channelType}</span>
          {template.subject && (
            <p className="text-sm"><span className="font-medium">Subject:</span> {template.subject}</p>
          )}
          {template.bodyHtml && (
            <div>
              <h4 className="text-sm font-medium mb-1">Body HTML</h4>
              <div className="bg-gray-50 rounded p-3 text-xs font-mono whitespace-pre-wrap max-h-48 overflow-y-auto">
                {template.bodyHtml}
              </div>
            </div>
          )}
          {template.bodyText && (
            <div>
              <h4 className="text-sm font-medium mb-1">Body Text</h4>
              <p className="text-sm text-gray-600 bg-gray-50 rounded p-3">{template.bodyText}</p>
            </div>
          )}
          <div className="flex gap-4 text-xs text-gray-500">
            {template.category && <span>Category: {template.category}</span>}
            {template.locale && <span>Locale: {template.locale}</span>}
          </div>
        </div>
        <div className="flex justify-end gap-2 mt-6 border-t pt-4">
          <button
            onClick={() => onDeactivate(template.id)}
            className="text-sm text-red-600 hover:text-red-800"
          >
            Deactivate
          </button>
        </div>
      </div>
    </div>
  )
}

// ===================== NOTIFICATION RULES =====================

function RuleList() {
  const queryClient = useQueryClient()
  const [showCreate, setShowCreate] = useState(false)

  const { data, isLoading } = useQuery({
    queryKey: ['notification-rules'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<NotificationRuleDto[]>>('/communication/rules')
      return res.data
    },
  })

  const toggleMutation = useMutation({
    mutationFn: (id: string) => api.post(`/communication/rules/${id}/toggle`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notification-rules'] }),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.post(`/communication/rules/${id}/delete`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notification-rules'] }),
  })

  const createMutation = useMutation({
    mutationFn: (rule: Record<string, unknown>) => api.post('/communication/rules', rule),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notification-rules'] })
      setShowCreate(false)
    },
  })

  const rules = data?.data ?? []

  return (
    <div>
      <div className="flex justify-end mb-4">
        <button
          onClick={() => setShowCreate(true)}
          className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-indigo-700"
        >
          Create Rule
        </button>
      </div>

      {isLoading ? (
        <p className="text-gray-500">Loading rules...</p>
      ) : rules.length === 0 ? (
        <p className="text-gray-400">No notification rules yet. Create one to automate communications.</p>
      ) : (
        <div className="space-y-3">
          {rules.map((rule) => (
            <div key={rule.id} className="bg-white rounded-lg shadow p-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="font-medium text-gray-900">{rule.name}</h3>
                  <p className="text-xs text-gray-500 mt-1">
                    Trigger: <span className="font-medium">{rule.triggerEvent.replace(/_/g, ' ')}</span>
                    {' '}&rarr;{' '}
                    <span className="font-medium">{rule.channelType}</span>
                    {rule.delayDays > 0 && (
                      <> ({rule.delayDays} days {rule.delayDirection.toLowerCase()})</>
                    )}
                    {rule.templateName && <> &middot; Template: {rule.templateName}</>}
                  </p>
                  {rule.description && (
                    <p className="text-sm text-gray-600 mt-1">{rule.description}</p>
                  )}
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => toggleMutation.mutate(rule.id)}
                    className={`text-xs px-3 py-1 rounded ${
                      rule.active
                        ? 'bg-green-100 text-green-700 hover:bg-green-200'
                        : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
                    }`}
                  >
                    {rule.active ? 'Active' : 'Inactive'}
                  </button>
                  <button
                    onClick={() => deleteMutation.mutate(rule.id)}
                    className="text-xs text-red-400 hover:text-red-600"
                  >
                    Delete
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {showCreate && (
        <CreateRuleModal
          onClose={() => setShowCreate(false)}
          onSubmit={(data) => createMutation.mutate(data)}
          isLoading={createMutation.isPending}
        />
      )}
    </div>
  )
}

function CreateRuleModal({
  onClose,
  onSubmit,
  isLoading,
}: {
  onClose: () => void
  onSubmit: (data: Record<string, unknown>) => void
  isLoading: boolean
}) {
  const [form, setForm] = useState({
    name: '',
    triggerEvent: 'WELCOME' as TriggerEvent,
    templateId: '',
    channelType: 'EMAIL' as ChannelType,
    delayDays: 0,
    delayDirection: 'IMMEDIATE' as DelayDirection,
    description: '',
  })

  const { data: templates } = useQuery({
    queryKey: ['comm-templates-for-rules'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<CommunicationTemplateDto[]>>(
        '/communication/templates?size=100'
      )
      return res.data
    },
  })

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
        <h2 className="text-lg font-bold mb-4">Create Notification Rule</h2>
        <div className="space-y-3">
          <input
            placeholder="Rule name *"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
          />
          <select
            value={form.triggerEvent}
            onChange={(e) => setForm({ ...form, triggerEvent: e.target.value as TriggerEvent })}
            className="w-full border rounded px-3 py-2 text-sm"
          >
            {TRIGGER_EVENTS.map((t) => (
              <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>
            ))}
          </select>
          <select
            value={form.templateId}
            onChange={(e) => setForm({ ...form, templateId: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
          >
            <option value="">Select template *</option>
            {(templates?.data ?? []).map((t) => (
              <option key={t.id} value={t.id}>{t.name} ({t.channelType})</option>
            ))}
          </select>
          <select
            value={form.channelType}
            onChange={(e) => setForm({ ...form, channelType: e.target.value as ChannelType })}
            className="w-full border rounded px-3 py-2 text-sm"
          >
            {CHANNEL_TYPES.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
          <div className="grid grid-cols-2 gap-3">
            <select
              value={form.delayDirection}
              onChange={(e) => setForm({ ...form, delayDirection: e.target.value as DelayDirection })}
              className="border rounded px-3 py-2 text-sm"
            >
              {DELAY_DIRECTIONS.map((d) => (
                <option key={d} value={d}>{d}</option>
              ))}
            </select>
            <input
              type="number"
              placeholder="Delay days"
              value={form.delayDays}
              onChange={(e) => setForm({ ...form, delayDays: parseInt(e.target.value) || 0 })}
              className="border rounded px-3 py-2 text-sm"
              disabled={form.delayDirection === 'IMMEDIATE'}
            />
          </div>
          <textarea
            placeholder="Description"
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            className="w-full border rounded px-3 py-2 text-sm"
            rows={2}
          />
        </div>
        <div className="flex justify-end gap-3 mt-6">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600">Cancel</button>
          <button
            onClick={() => onSubmit({
              ...form,
              description: form.description || undefined,
            })}
            disabled={!form.name || !form.templateId || isLoading}
            className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-indigo-700 disabled:opacity-50"
          >
            {isLoading ? 'Creating...' : 'Create Rule'}
          </button>
        </div>
      </div>
    </div>
  )
}

// ===================== MESSAGE HISTORY =====================

function MessageHistory() {
  const queryClient = useQueryClient()
  const [statusFilter, setStatusFilter] = useState<string>('')

  const { data, isLoading } = useQuery({
    queryKey: ['sent-messages', statusFilter],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '50' })
      if (statusFilter) params.set('status', statusFilter)
      const res = await api.get<ApiResponse<SentMessageDto[]>>(
        `/communication/messages?${params}`
      )
      return res.data
    },
  })

  const resendMutation = useMutation({
    mutationFn: (id: string) => api.post(`/communication/messages/${id}/resend`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['sent-messages'] }),
  })

  const messages = data?.data ?? []

  const statusColor = (status: MessageStatus) => {
    const colors: Record<MessageStatus, string> = {
      PENDING: 'bg-yellow-100 text-yellow-700',
      SENT: 'bg-blue-100 text-blue-700',
      DELIVERED: 'bg-green-100 text-green-700',
      FAILED: 'bg-red-100 text-red-700',
      OPENED: 'bg-purple-100 text-purple-700',
    }
    return colors[status]
  }

  return (
    <div>
      <div className="mb-4">
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="border rounded-lg px-3 py-2 text-sm"
        >
          <option value="">All Statuses</option>
          <option value="PENDING">Pending</option>
          <option value="SENT">Sent</option>
          <option value="DELIVERED">Delivered</option>
          <option value="FAILED">Failed</option>
          <option value="OPENED">Opened</option>
        </select>
      </div>

      {isLoading ? (
        <p className="text-gray-500">Loading messages...</p>
      ) : messages.length === 0 ? (
        <p className="text-gray-400">No messages sent yet.</p>
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">Member</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">Channel</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">Subject</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">Status</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">Sent</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {messages.map((msg) => (
                <tr key={msg.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm">{msg.memberName || msg.memberId}</td>
                  <td className="px-4 py-3 text-sm">{msg.channelType}</td>
                  <td className="px-4 py-3 text-sm text-gray-600 truncate max-w-xs">
                    {msg.subject || msg.bodyPreview?.substring(0, 60) || '—'}
                  </td>
                  <td className="px-4 py-3">
                    <span className={`text-xs px-2 py-0.5 rounded ${statusColor(msg.status)}`}>
                      {msg.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-xs text-gray-500">
                    {msg.sentAt ? new Date(msg.sentAt).toLocaleString() : '—'}
                  </td>
                  <td className="px-4 py-3">
                    {msg.status === 'FAILED' && (
                      <button
                        onClick={() => resendMutation.mutate(msg.id)}
                        className="text-xs text-indigo-600 hover:text-indigo-800"
                      >
                        Resend
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

// ===================== STATS =====================

function StatsView() {
  const { data, isLoading } = useQuery({
    queryKey: ['message-stats'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<MessageStatsDto>>('/communication/messages/stats?days=30')
      return res.data.data
    },
  })

  if (isLoading || !data) return <p className="text-gray-500">Loading stats...</p>

  const stats = [
    { label: 'Total Sent', value: data.totalSent, color: 'bg-blue-50 text-blue-700' },
    { label: 'Delivered', value: data.delivered, color: 'bg-green-50 text-green-700' },
    { label: 'Opened', value: data.opened, color: 'bg-purple-50 text-purple-700' },
    { label: 'Failed', value: data.failed, color: 'bg-red-50 text-red-700' },
    { label: 'Pending', value: data.pending, color: 'bg-yellow-50 text-yellow-700' },
  ]

  return (
    <div>
      <p className="text-sm text-gray-500 mb-4">Last 30 days</p>
      <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
        {stats.map((s) => (
          <div key={s.label} className={`rounded-lg p-4 ${s.color}`}>
            <p className="text-2xl font-bold">{s.value}</p>
            <p className="text-sm mt-1">{s.label}</p>
          </div>
        ))}
      </div>
      {data.totalSent > 0 && (
        <div className="mt-6 bg-white rounded-lg shadow p-4">
          <h3 className="text-sm font-medium mb-3">Delivery Rate</h3>
          <div className="flex items-center gap-4">
            <div className="flex-1 bg-gray-200 rounded-full h-3">
              <div
                className="bg-green-500 h-3 rounded-full"
                style={{ width: `${(data.delivered / data.totalSent * 100).toFixed(1)}%` }}
              />
            </div>
            <span className="text-sm font-medium">
              {(data.delivered / data.totalSent * 100).toFixed(1)}%
            </span>
          </div>
        </div>
      )}
    </div>
  )
}
