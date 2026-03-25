import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import api from '../../api/client'
import type { ApiResponse, ContractDto } from '../../types'

export default function PortalContracts() {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const [cancelId, setCancelId] = useState<string | null>(null)
  const [cancelReason, setCancelReason] = useState('')
  const [showFreezeHint, setShowFreezeHint] = useState<string | null>(null)

  const { data: contractsRes } = useQuery({
    queryKey: ['portal-contracts'],
    queryFn: () => api.get<ApiResponse<ContractDto[]>>('/portal/contracts').then(r => r.data),
  })
  const contracts = contractsRes?.data ?? []

  const cancelMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) =>
      api.post(`/portal/contracts/${id}/cancel`, { reason }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['portal-contracts'] })
      setCancelId(null)
      setCancelReason('')
      setShowFreezeHint(null)
    },
  })

  const withdrawMutation = useMutation({
    mutationFn: (id: string) => api.post(`/portal/contracts/${id}/withdraw-cancellation`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['portal-contracts'] }),
  })

  const startCancel = (id: string) => {
    setShowFreezeHint(id)
  }

  const proceedToCancel = (id: string) => {
    setShowFreezeHint(null)
    setCancelId(id)
  }

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">{t('portal.contracts.title')}</h1>

      {contracts.length === 0 ? (
        <p className="text-gray-500">{t('portal.contracts.noContracts')}</p>
      ) : (
        <div className="space-y-4">
          {contracts.map(c => (
            <div key={c.id} className="bg-white rounded-lg shadow p-5">
              <div className="flex items-center justify-between mb-3">
                <div>
                  <h3 className="text-lg font-semibold">{c.membershipTierName}</h3>
                  <p className="text-sm text-gray-500">
                    {t('portal.contracts.started', { date: c.startDate })} {c.endDate ? `| ${t('portal.contracts.ends', { date: c.endDate })}` : `| ${t('portal.ongoing')}`}
                  </p>
                </div>
                <span className={`text-xs px-3 py-1 rounded-full font-medium ${
                  c.status === 'ACTIVE' ? 'bg-green-100 text-green-700' :
                  c.status === 'PAUSED' ? 'bg-yellow-100 text-yellow-700' :
                  c.status === 'PENDING_CANCELLATION' ? 'bg-orange-100 text-orange-700' :
                  c.status === 'CANCELLED' ? 'bg-red-100 text-red-700' :
                  'bg-gray-100 text-gray-600'
                }`}>
                  {c.status?.replace(/_/g, ' ')}
                </span>
              </div>

              <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm mb-4">
                <div>
                  <p className="text-gray-500">{t('portal.contracts.monthlyAmount')}</p>
                  <p className="font-semibold">&euro;{c.monthlyAmount}</p>
                </div>
                <div>
                  <p className="text-gray-500">{t('portal.contracts.nextBilling')}</p>
                  <p className="font-semibold">{c.nextBillingDate ?? '—'}</p>
                </div>
                <div>
                  <p className="text-gray-500">{t('portal.contracts.autoRenew')}</p>
                  <p className="font-semibold">{c.autoRenew ? t('portal.contracts.yes') : t('portal.contracts.no')}</p>
                </div>
                {c.cancellationEffectiveDate && (
                  <div>
                    <p className="text-gray-500">{t('portal.contracts.cancellationEffective')}</p>
                    <p className="font-semibold text-red-600">{c.cancellationEffectiveDate}</p>
                  </div>
                )}
              </div>

              {/* Actions */}
              <div className="flex gap-3 border-t pt-3">
                {(c.status === 'ACTIVE' || c.status === 'PAUSED') && (
                  <button
                    onClick={() => startCancel(c.id)}
                    className="text-sm text-red-600 hover:text-red-800"
                  >
                    {t('portal.contracts.cancelContract')}
                  </button>
                )}
                {c.status === 'PENDING_CANCELLATION' && (
                  <button
                    onClick={() => withdrawMutation.mutate(c.id)}
                    disabled={withdrawMutation.isPending}
                    className="text-sm text-brand-600 hover:text-brand-700"
                  >
                    {t('portal.contracts.withdrawCancellation')}
                  </button>
                )}
              </div>

              {/* Freeze suggestion popup */}
              {showFreezeHint === c.id && (
                <div className="mt-3 bg-blue-50 border border-blue-200 rounded p-4">
                  <p className="text-sm text-blue-800 font-medium mb-2">
                    {t('portal.contracts.freezeHintTitle')}
                  </p>
                  <p className="text-sm text-blue-700 mb-3">
                    {t('portal.contracts.freezeHintBody')}
                  </p>
                  <div className="flex gap-3">
                    <button
                      onClick={() => setShowFreezeHint(null)}
                      className="text-sm bg-blue-600 text-white px-3 py-1.5 rounded hover:bg-blue-700"
                    >
                      {t('portal.contracts.considerFreezing')}
                    </button>
                    <button
                      onClick={() => proceedToCancel(c.id)}
                      className="text-sm text-red-600 hover:text-red-800"
                    >
                      {t('portal.contracts.proceedToCancel')}
                    </button>
                  </div>
                </div>
              )}

              {/* Cancel form */}
              {cancelId === c.id && (
                <div className="mt-3 bg-red-50 border border-red-200 rounded p-4">
                  <p className="text-sm font-medium text-red-800 mb-2">{t('portal.contracts.cancelContractTitle')}</p>
                  <textarea
                    value={cancelReason}
                    onChange={e => setCancelReason(e.target.value)}
                    placeholder={t('portal.contracts.cancelReasonPlaceholder')}
                    rows={2}
                    className="w-full border rounded px-3 py-2 text-sm mb-3"
                  />
                  <div className="flex gap-3">
                    <button
                      onClick={() => cancelMutation.mutate({ id: c.id, reason: cancelReason })}
                      disabled={cancelMutation.isPending}
                      className="text-sm bg-red-600 text-white px-3 py-1.5 rounded hover:bg-red-700 disabled:opacity-50"
                    >
                      {cancelMutation.isPending ? t('portal.contracts.cancelling') : t('portal.contracts.confirmCancellation')}
                    </button>
                    <button
                      onClick={() => { setCancelId(null); setCancelReason('') }}
                      className="text-sm text-gray-600"
                    >
                      {t('portal.back')}
                    </button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
