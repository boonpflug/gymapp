import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import api from '../api/client'
import type {
  ApiResponse,
  PageMeta,
  LoyaltyDashboardDto,
  LoyaltyRewardDto,
  LoyaltyBadgeDto,
  LoyaltyTierDto,
  ReferralDto,
  RewardType,
  BadgeCategory,
  BadgeCriteriaType,
  ReferralStatus,
} from '../types'

type Tab = 'dashboard' | 'rewards' | 'badges' | 'referrals' | 'configuration'

export default function LoyaltyPage() {
  const { t } = useTranslation()
  const [activeTab, setActiveTab] = useState<Tab>('dashboard')

  const tabs: { key: Tab; label: string }[] = [
    { key: 'dashboard', label: t('loyalty.dashboard') },
    { key: 'rewards', label: t('loyalty.rewards') },
    { key: 'badges', label: t('loyalty.badges') },
    { key: 'referrals', label: t('loyalty.referrals') },
    { key: 'configuration', label: t('loyalty.configuration') },
  ]

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">{t('loyalty.title')}</h1>
      <div className="border-b border-gray-200 mb-6">
        <nav className="flex space-x-8">
          {tabs.map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`py-3 px-1 border-b-2 text-sm font-medium ${
                activeTab === tab.key
                  ? 'border-amber-500 text-amber-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>
      {activeTab === 'dashboard' && <DashboardTab />}
      {activeTab === 'rewards' && <RewardsTab />}
      {activeTab === 'badges' && <BadgesTab />}
      {activeTab === 'referrals' && <ReferralsTab />}
      {activeTab === 'configuration' && <ConfigurationTab />}
    </div>
  )
}

// ── Dashboard Tab ──────────────────────────────────────

function DashboardTab() {
  const { t } = useTranslation()
  const { data, isLoading } = useQuery({
    queryKey: ['loyalty-dashboard'],
    queryFn: () => api.get<ApiResponse<LoyaltyDashboardDto>>('/loyalty/dashboard').then((r) => r.data),
  })

  const dashboard = data?.data

  if (isLoading) return <p className="text-gray-500 text-sm">{t('common.loading')}</p>
  if (!dashboard) return <p className="text-gray-500 text-sm">{t('common.noDataAvailable')}</p>

  return (
    <div>
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <StatCard label={t('loyalty.pointsIssuedThisMonth')} value={dashboard.pointsIssuedThisMonth.toLocaleString()} />
        <StatCard label={t('loyalty.pointsRedeemed')} value={dashboard.pointsRedeemedThisMonth.toLocaleString()} />
        <StatCard label={t('loyalty.redemptions')} value={dashboard.redemptionsThisMonth} />
        <StatCard label={t('loyalty.totalParticipants')} value={dashboard.totalParticipants.toLocaleString()} />
      </div>

      <div className="bg-white rounded-lg shadow overflow-hidden">
        <div className="px-4 py-3 border-b">
          <h3 className="text-sm font-semibold text-gray-900">{t('loyalty.topMembers')}</h3>
        </div>
        {dashboard.topMembers.length === 0 ? (
          <p className="text-gray-500 text-sm p-4">{t('loyalty.noMembersYet')}</p>
        ) : (
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('common.name')}</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('loyalty.points')}</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('loyalty.tier')}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {dashboard.topMembers.map((m, i) => (
                <tr key={m.memberId} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm font-medium text-gray-900">
                    <span className="text-amber-600 font-semibold mr-2">#{i + 1}</span>
                    {m.memberName}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600">{m.totalPoints.toLocaleString()}</td>
                  <td className="px-4 py-3 text-sm">
                    {m.tierName ? (
                      <span className="bg-amber-100 text-amber-700 px-2 py-0.5 rounded text-xs">{m.tierName}</span>
                    ) : (
                      <span className="text-gray-400 text-xs">{t('loyalty.noTier')}</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}

// ── Rewards Tab ────────────────────────────────────────

function RewardsTab() {
  const { t } = useTranslation()
  const [page, setPage] = useState(0)
  const [showCreate, setShowCreate] = useState(false)
  const qc = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['loyalty-rewards', page],
    queryFn: () =>
      api
        .get<ApiResponse<LoyaltyRewardDto[]>>('/loyalty/rewards', { params: { page, size: 20 } })
        .then((r) => r.data),
  })

  const rewards = data?.data ?? []
  const meta = data?.meta as PageMeta | undefined

  const deactivateMutation = useMutation({
    mutationFn: (id: string) => api.put(`/loyalty/rewards/${id}/deactivate`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['loyalty-rewards'] }),
  })

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <p className="text-sm text-gray-500">{meta ? t('loyalty.rewardsCount', { count: meta.totalElements }) : ''}</p>
        <button
          onClick={() => setShowCreate(true)}
          className="bg-amber-600 text-white px-4 py-2 rounded-lg hover:bg-amber-700 text-sm"
        >
          {t('loyalty.newReward')}
        </button>
      </div>

      {isLoading ? (
        <p className="text-gray-500 text-sm">{t('common.loading')}</p>
      ) : rewards.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <p className="text-gray-400 text-sm">{t('loyalty.noRewardsYet')}</p>
          <button onClick={() => setShowCreate(true)} className="mt-2 text-amber-600 text-sm font-medium hover:text-amber-700">
            {t('loyalty.createFirstReward')}
          </button>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {rewards.map((r) => (
              <div key={r.id} className="bg-white rounded-lg shadow p-4 flex flex-col">
                <div className="flex items-start justify-between mb-2">
                  <h4 className="text-sm font-semibold text-gray-900">{r.name}</h4>
                  <span className={`text-xs px-2 py-0.5 rounded ${r.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                    {r.active ? t('common.active') : t('common.inactive')}
                  </span>
                </div>
                {r.description && <p className="text-xs text-gray-500 mb-2">{r.description}</p>}
                <div className="flex flex-wrap gap-2 mb-3">
                  <RewardTypeBadge type={r.rewardType} />
                  <span className="bg-amber-100 text-amber-700 px-2 py-0.5 rounded text-xs font-medium">
                    {r.pointsCost.toLocaleString()} pts
                  </span>
                  {r.value != null && (
                    <span className="bg-gray-100 text-gray-600 px-2 py-0.5 rounded text-xs">
                      {t('loyalty.value')}: {r.value}
                    </span>
                  )}
                </div>
                <div className="flex items-center justify-between text-xs text-gray-500 mt-auto pt-2 border-t">
                  <span>
                    {r.totalRedeemed} {t('loyalty.redeemed')}
                    {r.totalAvailable != null && ` / ${r.totalAvailable} ${t('loyalty.available')}`}
                  </span>
                  {r.active && (
                    <button
                      onClick={() => deactivateMutation.mutate(r.id)}
                      disabled={deactivateMutation.isPending}
                      className="text-red-600 hover:text-red-800 font-medium"
                    >
                      {t('common.deactivate')}
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
          {meta && meta.totalPages > 1 && (
            <div className="flex items-center justify-between mt-4">
              <p className="text-sm text-gray-500">
                {t('loyalty.pageInfo', { current: meta.page + 1, total: meta.totalPages, count: meta.totalElements })}
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-3 py-1 border rounded text-sm disabled:opacity-50"
                >
                  {t('common.previous')}
                </button>
                <button
                  onClick={() => setPage((p) => p + 1)}
                  disabled={page >= (meta.totalPages ?? 1) - 1}
                  className="px-3 py-1 border rounded text-sm disabled:opacity-50"
                >
                  {t('common.next')}
                </button>
              </div>
            </div>
          )}
        </>
      )}

      {showCreate && <CreateRewardModal onClose={() => setShowCreate(false)} />}
    </div>
  )
}

function RewardTypeBadge({ type }: { type: RewardType }) {
  const colors: Record<RewardType, string> = {
    FREE_MONTH: 'bg-purple-100 text-purple-700',
    DISCOUNT: 'bg-blue-100 text-blue-700',
    CLASS_CREDIT: 'bg-teal-100 text-teal-700',
    MERCH: 'bg-pink-100 text-pink-700',
    CREDIT_TOPUP: 'bg-green-100 text-green-700',
    CUSTOM: 'bg-gray-100 text-gray-700',
  }
  const { t } = useTranslation()
  const labels: Record<RewardType, string> = {
    FREE_MONTH: t('loyalty.freeMonth'),
    DISCOUNT: t('loyalty.discount'),
    CLASS_CREDIT: t('loyalty.classCredit'),
    MERCH: t('loyalty.merchandise'),
    CREDIT_TOPUP: t('loyalty.creditTopup'),
    CUSTOM: t('loyalty.custom'),
  }
  return <span className={`px-2 py-0.5 rounded text-xs ${colors[type]}`}>{labels[type]}</span>
}

// ── Create Reward Modal ────────────────────────────────

function CreateRewardModal({ onClose }: { onClose: () => void }) {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const [form, setForm] = useState({
    name: '',
    description: '',
    rewardType: 'DISCOUNT' as RewardType,
    pointsCost: 0,
    value: '',
    imageUrl: '',
    maxRedemptionsPerMember: '',
    totalAvailable: '',
  })

  const createMutation = useMutation({
    mutationFn: (data: typeof form) =>
      api.post<ApiResponse<LoyaltyRewardDto>>('/loyalty/rewards', {
        ...data,
        value: data.value ? Number(data.value) : null,
        maxRedemptionsPerMember: data.maxRedemptionsPerMember ? Number(data.maxRedemptionsPerMember) : null,
        totalAvailable: data.totalAvailable ? Number(data.totalAvailable) : null,
        imageUrl: data.imageUrl || null,
        description: data.description || null,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['loyalty-rewards'] })
      onClose()
    },
  })

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 overflow-auto">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg p-6 m-4 max-h-[90vh] overflow-y-auto">
        <h2 className="text-lg font-semibold mb-4">{t('loyalty.createReward')}</h2>
        <form
          onSubmit={(e) => {
            e.preventDefault()
            createMutation.mutate(form)
          }}
          className="space-y-4"
        >
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('loyalty.nameRequired')}</label>
              <input
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('loyalty.typeRequired')}</label>
              <select
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.rewardType}
                onChange={(e) => setForm({ ...form, rewardType: e.target.value as RewardType })}
              >
                <option value="FREE_MONTH">{t('loyalty.freeMonth')}</option>
                <option value="DISCOUNT">{t('loyalty.discount')}</option>
                <option value="CLASS_CREDIT">{t('loyalty.classCredit')}</option>
                <option value="MERCH">{t('loyalty.merchandise')}</option>
                <option value="CREDIT_TOPUP">{t('loyalty.creditTopup')}</option>
                <option value="CUSTOM">{t('loyalty.custom')}</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t('common.description')}</label>
            <input
              className="w-full border rounded-lg px-3 py-2 text-sm"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('loyalty.pointsCostRequired')}</label>
              <input
                type="number"
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.pointsCost}
                onChange={(e) => setForm({ ...form, pointsCost: Number(e.target.value) })}
                min={0}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('loyalty.value')}</label>
              <input
                type="number"
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.value}
                onChange={(e) => setForm({ ...form, value: e.target.value })}
                placeholder={t('loyalty.valuePlaceholder')}
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t('loyalty.imageUrl')}</label>
            <input
              className="w-full border rounded-lg px-3 py-2 text-sm"
              value={form.imageUrl}
              onChange={(e) => setForm({ ...form, imageUrl: e.target.value })}
              placeholder="https://..."
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('loyalty.maxPerMember')}</label>
              <input
                type="number"
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.maxRedemptionsPerMember}
                onChange={(e) => setForm({ ...form, maxRedemptionsPerMember: e.target.value })}
                placeholder={t('loyalty.unlimitedPlaceholder')}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('loyalty.totalAvailable')}</label>
              <input
                type="number"
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.totalAvailable}
                onChange={(e) => setForm({ ...form, totalAvailable: e.target.value })}
                placeholder={t('loyalty.unlimitedPlaceholder')}
              />
            </div>
          </div>

          {createMutation.isError && (
            <p className="text-red-600 text-sm">{t('loyalty.failedCreateReward')}</p>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 border rounded-lg text-sm">
              {t('common.cancel')}
            </button>
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="bg-amber-600 text-white px-4 py-2 rounded-lg hover:bg-amber-700 text-sm disabled:opacity-50"
            >
              {createMutation.isPending ? t('common.creating') : t('loyalty.createReward')}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

// ── Badges Tab ─────────────────────────────────────────

function BadgesTab() {
  const { t } = useTranslation()
  const [showCreate, setShowCreate] = useState(false)
  const qc = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['loyalty-badges'],
    queryFn: () => api.get<ApiResponse<LoyaltyBadgeDto[]>>('/loyalty/badges').then((r) => r.data),
  })

  const badges = data?.data ?? []

  const deactivateMutation = useMutation({
    mutationFn: (id: string) => api.put(`/loyalty/badges/${id}/deactivate`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['loyalty-badges'] }),
  })

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <p className="text-sm text-gray-500">{t('loyalty.badgesCount', { count: badges.length })}</p>
        <button
          onClick={() => setShowCreate(true)}
          className="bg-amber-600 text-white px-4 py-2 rounded-lg hover:bg-amber-700 text-sm"
        >
          {t('loyalty.newBadge')}
        </button>
      </div>

      {isLoading ? (
        <p className="text-gray-500 text-sm">{t('common.loading')}</p>
      ) : badges.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <p className="text-gray-400 text-sm">{t('loyalty.noBadgesYet')}</p>
          <button onClick={() => setShowCreate(true)} className="mt-2 text-amber-600 text-sm font-medium hover:text-amber-700">
            {t('loyalty.createFirstBadge')}
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {badges.map((b) => (
            <div key={b.id} className="bg-white rounded-lg shadow p-4 flex flex-col">
              <div className="flex items-start justify-between mb-2">
                <div className="flex items-center gap-2">
                  <span className="text-2xl">{b.icon}</span>
                  <h4 className="text-sm font-semibold text-gray-900">{b.name}</h4>
                </div>
                <span className={`text-xs px-2 py-0.5 rounded ${b.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                  {b.active ? t('common.active') : t('common.inactive')}
                </span>
              </div>
              {b.description && <p className="text-xs text-gray-500 mb-2">{b.description}</p>}
              <div className="flex flex-wrap gap-2 mb-3">
                <BadgeCategoryBadge category={b.category} />
                <span className="bg-gray-100 text-gray-600 px-2 py-0.5 rounded text-xs">
                  {criteriaLabel(b.criteriaType, b.criteriaValue)}
                </span>
              </div>
              <div className="flex items-center justify-end text-xs mt-auto pt-2 border-t">
                {b.active && (
                  <button
                    onClick={() => deactivateMutation.mutate(b.id)}
                    disabled={deactivateMutation.isPending}
                    className="text-red-600 hover:text-red-800 font-medium"
                  >
                    {t('common.deactivate')}
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {showCreate && <CreateBadgeModal onClose={() => setShowCreate(false)} />}
    </div>
  )
}

function BadgeCategoryBadge({ category }: { category: BadgeCategory }) {
  const colors: Record<BadgeCategory, string> = {
    CHECKIN: 'bg-blue-100 text-blue-700',
    TRAINING: 'bg-purple-100 text-purple-700',
    MEMBERSHIP: 'bg-amber-100 text-amber-700',
    SOCIAL: 'bg-pink-100 text-pink-700',
    SPECIAL: 'bg-yellow-100 text-yellow-700',
  }
  return <span className={`px-2 py-0.5 rounded text-xs ${colors[category]}`}>{category}</span>
}

function criteriaLabel(type: BadgeCriteriaType, value: number): string {
  const labels: Record<BadgeCriteriaType, string> = {
    CHECKIN_COUNT: `${value} Check-ins`,
    SESSION_COUNT: `${value} Sessions`,
    MEMBER_DURATION_MONTHS: `${value} Months Member`,
    REFERRAL_COUNT: `${value} Referrals`,
    STREAK_DAYS: `${value}-Day Streak`,
    CUSTOM: `Custom (${value})`,
  }
  return labels[type]
}

// ── Create Badge Modal ─────────────────────────────────

function CreateBadgeModal({ onClose }: { onClose: () => void }) {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const [form, setForm] = useState({
    name: '',
    description: '',
    icon: '',
    category: 'CHECKIN' as BadgeCategory,
    criteriaType: 'CHECKIN_COUNT' as BadgeCriteriaType,
    criteriaValue: 0,
  })

  const createMutation = useMutation({
    mutationFn: (data: typeof form) =>
      api.post<ApiResponse<LoyaltyBadgeDto>>('/loyalty/badges', {
        ...data,
        description: data.description || null,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['loyalty-badges'] })
      onClose()
    },
  })

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 overflow-auto">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg p-6 m-4 max-h-[90vh] overflow-y-auto">
        <h2 className="text-lg font-semibold mb-4">{t('loyalty.createBadge')}</h2>
        <form
          onSubmit={(e) => {
            e.preventDefault()
            createMutation.mutate(form)
          }}
          className="space-y-4"
        >
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('loyalty.nameRequired')}</label>
              <input
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('loyalty.iconRequired')}</label>
              <input
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.icon}
                onChange={(e) => setForm({ ...form, icon: e.target.value })}
                placeholder={t('loyalty.iconPlaceholder')}
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t('common.description')}</label>
            <input
              className="w-full border rounded-lg px-3 py-2 text-sm"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('loyalty.categoryRequired')}</label>
              <select
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.category}
                onChange={(e) => setForm({ ...form, category: e.target.value as BadgeCategory })}
              >
                <option value="CHECKIN">{t('loyalty.checkin')}</option>
                <option value="TRAINING">{t('loyalty.training')}</option>
                <option value="MEMBERSHIP">{t('loyalty.membership')}</option>
                <option value="SOCIAL">{t('loyalty.social')}</option>
                <option value="SPECIAL">{t('loyalty.special')}</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('loyalty.criteriaTypeRequired')}</label>
              <select
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.criteriaType}
                onChange={(e) => setForm({ ...form, criteriaType: e.target.value as BadgeCriteriaType })}
              >
                <option value="CHECKIN_COUNT">{t('loyalty.checkinCount')}</option>
                <option value="SESSION_COUNT">{t('loyalty.sessionCount')}</option>
                <option value="MEMBER_DURATION_MONTHS">{t('loyalty.memberDurationMonths')}</option>
                <option value="REFERRAL_COUNT">{t('loyalty.referralCount')}</option>
                <option value="STREAK_DAYS">{t('loyalty.streakDays')}</option>
                <option value="CUSTOM">{t('loyalty.custom')}</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t('loyalty.criteriaValueRequired')}</label>
            <input
              type="number"
              className="w-full border rounded-lg px-3 py-2 text-sm"
              value={form.criteriaValue}
              onChange={(e) => setForm({ ...form, criteriaValue: Number(e.target.value) })}
              min={0}
              required
            />
          </div>

          {createMutation.isError && (
            <p className="text-red-600 text-sm">{t('loyalty.failedCreateBadge')}</p>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 border rounded-lg text-sm">
              {t('common.cancel')}
            </button>
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="bg-amber-600 text-white px-4 py-2 rounded-lg hover:bg-amber-700 text-sm disabled:opacity-50"
            >
              {createMutation.isPending ? t('common.creating') : t('loyalty.createBadge')}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

// ── Referrals Tab ──────────────────────────────────────

function ReferralsTab() {
  const { t } = useTranslation()
  const [memberFilter, setMemberFilter] = useState('')
  const [page, setPage] = useState(0)

  const { data, isLoading } = useQuery({
    queryKey: ['loyalty-referrals', memberFilter, page],
    queryFn: () => {
      const url = memberFilter
        ? `/loyalty/referrals/member/${memberFilter}`
        : '/loyalty/referrals'
      return api.get<ApiResponse<ReferralDto[]>>(url, { params: { page, size: 20 } }).then((r) => r.data)
    },
  })

  const referrals = data?.data ?? []
  const meta = data?.meta as PageMeta | undefined

  return (
    <div>
      <div className="flex items-center gap-4 mb-4">
        <div className="flex-1 max-w-xs">
          <input
            className="w-full border rounded-lg px-3 py-2 text-sm"
            placeholder={t('loyalty.filterByMemberId')}
            value={memberFilter}
            onChange={(e) => {
              setMemberFilter(e.target.value)
              setPage(0)
            }}
          />
        </div>
      </div>

      {isLoading ? (
        <p className="text-gray-500 text-sm">{t('common.loading')}</p>
      ) : referrals.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <p className="text-gray-400 text-sm">{t('loyalty.noReferralsFound')}</p>
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('loyalty.referrer')}</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('loyalty.referredEmail')}</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('loyalty.code')}</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('common.status')}</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('loyalty.pointsAwarded')}</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('common.date')}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {referrals.map((r) => (
                <tr key={r.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm font-medium text-gray-900">{r.referrerName}</td>
                  <td className="px-4 py-3 text-sm text-gray-600">{r.referredEmail}</td>
                  <td className="px-4 py-3 text-sm">
                    <code className="bg-gray-100 text-gray-700 px-2 py-0.5 rounded text-xs">{r.referralCode}</code>
                  </td>
                  <td className="px-4 py-3 text-sm">
                    <ReferralStatusBadge status={r.status} />
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600">
                    <span className="text-amber-600 font-medium">{r.referrerPointsAwarded}</span>
                    {r.referredPointsAwarded > 0 && (
                      <span className="text-gray-400 ml-1">/ {r.referredPointsAwarded}</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-500">
                    {new Date(r.createdAt).toLocaleDateString()}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {meta && meta.totalPages > 1 && (
            <div className="flex items-center justify-between px-4 py-3 border-t">
              <p className="text-sm text-gray-500">
                {t('loyalty.pageInfo', { current: meta.page + 1, total: meta.totalPages, count: meta.totalElements })}
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-3 py-1 border rounded text-sm disabled:opacity-50"
                >
                  {t('common.previous')}
                </button>
                <button
                  onClick={() => setPage((p) => p + 1)}
                  disabled={page >= (meta.totalPages ?? 1) - 1}
                  className="px-3 py-1 border rounded text-sm disabled:opacity-50"
                >
                  {t('common.next')}
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

function ReferralStatusBadge({ status }: { status: ReferralStatus }) {
  const styles: Record<ReferralStatus, string> = {
    PENDING: 'bg-yellow-100 text-yellow-700',
    SIGNED_UP: 'bg-blue-100 text-blue-700',
    CONVERTED: 'bg-green-100 text-green-700',
    EXPIRED: 'bg-gray-100 text-gray-500',
  }
  return <span className={`px-2 py-0.5 rounded text-xs ${styles[status]}`}>{status}</span>
}

// ── Configuration Tab ──────────────────────────────────

interface ConfigEntry {
  action: string
  points: number
}

function ConfigurationTab() {
  const { t } = useTranslation()
  const [showCreateTier, setShowCreateTier] = useState(false)

  // Points configuration
  const { data: configData, isLoading: configLoading } = useQuery({
    queryKey: ['loyalty-config'],
    queryFn: () => api.get<ApiResponse<Record<string, number>>>('/loyalty/config').then((r) => r.data),
  })

  // Tiers
  const { data: tiersData, isLoading: tiersLoading } = useQuery({
    queryKey: ['loyalty-tiers'],
    queryFn: () => api.get<ApiResponse<LoyaltyTierDto[]>>('/loyalty/tiers').then((r) => r.data),
  })

  const configMap = configData?.data ?? {}
  const configEntries: ConfigEntry[] = Object.entries(configMap).map(([action, points]) => ({ action, points }))
  const tiers = tiersData?.data ?? []

  return (
    <div className="space-y-8">
      {/* Points Configuration */}
      <div>
        <h3 className="text-lg font-semibold text-gray-900 mb-4">{t('loyalty.pointsConfiguration')}</h3>
        {configLoading ? (
          <p className="text-gray-500 text-sm">{t('common.loading')}</p>
        ) : configEntries.length === 0 ? (
          <div className="bg-white rounded-lg shadow p-8 text-center">
            <p className="text-gray-400 text-sm">{t('loyalty.noPointRulesYet')}</p>
          </div>
        ) : (
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('loyalty.action')}</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('loyalty.points')}</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{t('common.actions')}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {configEntries.map((entry) => (
                  <ConfigRow key={entry.action} entry={entry} />
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Tier Management */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900">{t('loyalty.loyaltyTiers')}</h3>
          <button
            onClick={() => setShowCreateTier(true)}
            className="bg-amber-600 text-white px-4 py-2 rounded-lg hover:bg-amber-700 text-sm"
          >
            {t('loyalty.newTier')}
          </button>
        </div>

        {tiersLoading ? (
          <p className="text-gray-500 text-sm">{t('common.loading')}</p>
        ) : tiers.length === 0 ? (
          <div className="bg-white rounded-lg shadow p-8 text-center">
            <p className="text-gray-400 text-sm">{t('loyalty.noTiersYet')}</p>
            <button onClick={() => setShowCreateTier(true)} className="mt-2 text-amber-600 text-sm font-medium hover:text-amber-700">
              {t('loyalty.createFirstTier')}
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {tiers
              .sort((a, b) => a.sortOrder - b.sortOrder)
              .map((tier) => (
                <div key={tier.id} className="bg-white rounded-lg shadow p-4 border-l-4" style={{ borderLeftColor: tier.color || '#f59e0b' }}>
                  <div className="flex items-start justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <span className="text-xl">{tier.icon}</span>
                      <h4 className="text-sm font-semibold text-gray-900">{tier.name}</h4>
                    </div>
                    <span className={`text-xs px-2 py-0.5 rounded ${tier.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                      {tier.active ? t('common.active') : t('common.inactive')}
                    </span>
                  </div>
                  <div className="space-y-1 text-xs text-gray-600">
                    <p>
                      <span className="font-medium text-gray-700">{t('loyalty.minPoints')}:</span> {tier.minPoints.toLocaleString()}
                    </p>
                    {tier.perks && (
                      <p>
                        <span className="font-medium text-gray-700">{t('loyalty.perks')}:</span> {tier.perks}
                      </p>
                    )}
                    <p>
                      <span className="font-medium text-gray-700">{t('loyalty.order')}:</span> {tier.sortOrder}
                    </p>
                  </div>
                </div>
              ))}
          </div>
        )}
      </div>

      {showCreateTier && <CreateTierModal onClose={() => setShowCreateTier(false)} />}
    </div>
  )
}

function ConfigRow({ entry }: { entry: ConfigEntry }) {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const [points, setPoints] = useState(entry.points)
  const [dirty, setDirty] = useState(false)

  const updateMutation = useMutation({
    mutationFn: () => api.put(`/loyalty/config/${entry.action}`, null, { params: { points } }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['loyalty-config'] })
      setDirty(false)
    },
  })

  const actionLabel = (action: string): string => {
    return action
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/^\w/, (c) => c.toUpperCase())
  }

  return (
    <tr className="hover:bg-gray-50">
      <td className="px-4 py-3 text-sm font-medium text-gray-900">{actionLabel(entry.action)}</td>
      <td className="px-4 py-3 text-sm">
        <input
          type="number"
          className="border rounded px-2 py-1 text-sm w-24"
          value={points}
          onChange={(e) => {
            setPoints(Number(e.target.value))
            setDirty(true)
          }}
        />
      </td>
      <td className="px-4 py-3 text-sm">
        {dirty && (
          <button
            onClick={() => updateMutation.mutate()}
            disabled={updateMutation.isPending}
            className="bg-amber-600 text-white px-3 py-1 rounded text-xs hover:bg-amber-700 disabled:opacity-50"
          >
            {updateMutation.isPending ? t('common.saving') : t('common.save')}
          </button>
        )}
        {updateMutation.isError && (
          <span className="text-red-600 text-xs ml-2">{t('common.failed')}</span>
        )}
      </td>
    </tr>
  )
}

// ── Create Tier Modal ──────────────────────────────────

function CreateTierModal({ onClose }: { onClose: () => void }) {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const [form, setForm] = useState({
    name: '',
    minPoints: 0,
    color: '#f59e0b',
    icon: '',
    perks: '',
    sortOrder: 0,
  })

  const createMutation = useMutation({
    mutationFn: (data: typeof form) =>
      api.post<ApiResponse<LoyaltyTierDto>>('/loyalty/tiers', data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['loyalty-tiers'] })
      onClose()
    },
  })

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 overflow-auto">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg p-6 m-4 max-h-[90vh] overflow-y-auto">
        <h2 className="text-lg font-semibold mb-4">{t('loyalty.createTier')}</h2>
        <form
          onSubmit={(e) => {
            e.preventDefault()
            createMutation.mutate(form)
          }}
          className="space-y-4"
        >
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('loyalty.nameRequired')}</label>
              <input
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('loyalty.iconRequired')}</label>
              <input
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.icon}
                onChange={(e) => setForm({ ...form, icon: e.target.value })}
                placeholder={t('loyalty.emojiPlaceholder')}
                required
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('loyalty.minPointsRequired')}</label>
              <input
                type="number"
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.minPoints}
                onChange={(e) => setForm({ ...form, minPoints: Number(e.target.value) })}
                min={0}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('loyalty.sortOrderRequired')}</label>
              <input
                type="number"
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.sortOrder}
                onChange={(e) => setForm({ ...form, sortOrder: Number(e.target.value) })}
                min={0}
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t('loyalty.color')}</label>
            <div className="flex items-center gap-2">
              <input
                type="color"
                className="w-10 h-10 rounded border cursor-pointer"
                value={form.color}
                onChange={(e) => setForm({ ...form, color: e.target.value })}
              />
              <input
                className="flex-1 border rounded-lg px-3 py-2 text-sm"
                value={form.color}
                onChange={(e) => setForm({ ...form, color: e.target.value })}
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t('loyalty.perks')}</label>
            <textarea
              className="w-full border rounded-lg px-3 py-2 text-sm"
              rows={3}
              value={form.perks}
              onChange={(e) => setForm({ ...form, perks: e.target.value })}
              placeholder={t('loyalty.perksPlaceholder')}
            />
          </div>

          {createMutation.isError && (
            <p className="text-red-600 text-sm">{t('loyalty.failedCreateTier')}</p>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 border rounded-lg text-sm">
              {t('common.cancel')}
            </button>
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="bg-amber-600 text-white px-4 py-2 rounded-lg hover:bg-amber-700 text-sm disabled:opacity-50"
            >
              {createMutation.isPending ? t('common.creating') : t('loyalty.createTier')}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

// ── Shared Components ──────────────────────────────────

function StatCard({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="bg-white rounded-lg shadow p-4">
      <p className="text-2xl font-bold text-gray-900">{value}</p>
      <p className="text-xs text-gray-500">{label}</p>
    </div>
  )
}
