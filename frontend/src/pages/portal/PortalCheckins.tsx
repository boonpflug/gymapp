import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import api from '../../api/client'
import type { ApiResponse, CheckInDto } from '../../types'

export default function PortalCheckins() {
  const { t } = useTranslation()
  const [page, setPage] = useState(0)
  const pageSize = 20

  const { data: checkinsRes } = useQuery({
    queryKey: ['portal-checkins', page],
    queryFn: () =>
      api.get<ApiResponse<CheckInDto[]>>('/portal/checkins', {
        params: { page, size: pageSize },
      }).then(r => r.data),
  })
  const checkins = checkinsRes?.data ?? []
  const meta = checkinsRes?.meta

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">{t('portal.checkins.title')}</h1>

      {checkins.length === 0 ? (
        <p className="text-gray-500">{t('portal.checkins.noHistory')}</p>
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50">
              <tr>
                <th className="text-left px-4 py-3 font-medium text-gray-600">{t('portal.checkins.dateTime')}</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">{t('portal.checkins.method')}</th>
                <th className="text-center px-4 py-3 font-medium text-gray-600">{t('portal.checkins.status')}</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">{t('portal.checkins.checkout')}</th>
              </tr>
            </thead>
            <tbody>
              {checkins.map(c => (
                <tr key={c.id} className="border-t hover:bg-gray-50">
                  <td className="px-4 py-3">
                    {new Date(c.checkInTime).toLocaleString()}
                  </td>
                  <td className="px-4 py-3 text-gray-600">{c.method}</td>
                  <td className="px-4 py-3 text-center">
                    <span className={`text-xs px-2 py-1 rounded ${
                      c.status === 'SUCCESS' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                    }`}>
                      {c.status}
                    </span>
                    {c.denialReason && (
                      <p className="text-xs text-red-500 mt-1">{c.denialReason}</p>
                    )}
                  </td>
                  <td className="px-4 py-3 text-gray-600">
                    {c.checkOutTime ? new Date(c.checkOutTime).toLocaleTimeString() : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Pagination */}
          {meta && meta.totalPages > 1 && (
            <div className="flex items-center justify-between px-4 py-3 border-t bg-gray-50">
              <p className="text-sm text-gray-500">
                {t('portal.checkins.page', { current: meta.page + 1, total: meta.totalPages, elements: meta.totalElements })}
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-3 py-1 text-sm border rounded disabled:opacity-50"
                >
                  {t('portal.previous')}
                </button>
                <button
                  onClick={() => setPage(p => p + 1)}
                  disabled={page >= meta.totalPages - 1}
                  className="px-3 py-1 text-sm border rounded disabled:opacity-50"
                >
                  {t('portal.next')}
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
