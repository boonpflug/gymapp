import { useQuery } from '@tanstack/react-query'
import api from '../../api/client'
import type { ApiResponse, InvoiceDto } from '../../types'

export default function PortalInvoices() {
  const { data: invoicesRes } = useQuery({
    queryKey: ['portal-invoices'],
    queryFn: () => api.get<ApiResponse<InvoiceDto[]>>('/portal/invoices').then(r => r.data),
  })
  const invoices = invoicesRes?.data ?? []

  const outstanding = invoices.filter(i => i.status === 'ISSUED' || i.status === 'OVERDUE')
  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">My Invoices</h1>

      {outstanding.length > 0 && (
        <div className="mb-6">
          <h2 className="text-lg font-semibold mb-3 text-red-700">Outstanding</h2>
          <div className="space-y-2">
            {outstanding.map(inv => (
              <InvoiceRow key={inv.id} invoice={inv} />
            ))}
          </div>
        </div>
      )}

      <h2 className="text-lg font-semibold mb-3">All Invoices</h2>
      {invoices.length === 0 ? (
        <p className="text-gray-500">No invoices found.</p>
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50">
              <tr>
                <th className="text-left px-4 py-3 font-medium text-gray-600">Invoice #</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">Issued</th>
                <th className="text-left px-4 py-3 font-medium text-gray-600">Due Date</th>
                <th className="text-right px-4 py-3 font-medium text-gray-600">Amount</th>
                <th className="text-right px-4 py-3 font-medium text-gray-600">Total</th>
                <th className="text-center px-4 py-3 font-medium text-gray-600">Status</th>
              </tr>
            </thead>
            <tbody>
              {invoices.map(inv => (
                <tr key={inv.id} className="border-t hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium">{inv.invoiceNumber}</td>
                  <td className="px-4 py-3 text-gray-600">
                    {inv.issuedAt ? new Date(inv.issuedAt).toLocaleDateString() : '—'}
                  </td>
                  <td className="px-4 py-3 text-gray-600">{inv.dueDate ?? '—'}</td>
                  <td className="px-4 py-3 text-right">&euro;{inv.amount}</td>
                  <td className="px-4 py-3 text-right font-semibold">&euro;{inv.totalAmount}</td>
                  <td className="px-4 py-3 text-center">
                    <StatusBadge status={inv.status} />
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

function InvoiceRow({ invoice }: { invoice: InvoiceDto }) {
  return (
    <div className="bg-white rounded-lg shadow p-4 flex items-center justify-between">
      <div>
        <p className="font-medium">{invoice.invoiceNumber}</p>
        <p className="text-sm text-gray-500">Due: {invoice.dueDate}</p>
      </div>
      <div className="text-right">
        <p className="text-lg font-semibold text-red-600">&euro;{invoice.totalAmount}</p>
        <StatusBadge status={invoice.status} />
      </div>
    </div>
  )
}

function StatusBadge({ status }: { status: string }) {
  const colors: Record<string, string> = {
    DRAFT: 'bg-gray-100 text-gray-600',
    ISSUED: 'bg-blue-100 text-blue-700',
    PAID: 'bg-green-100 text-green-700',
    OVERDUE: 'bg-red-100 text-red-700',
    CANCELLED: 'bg-gray-100 text-gray-500',
  }
  return (
    <span className={`text-xs px-2 py-1 rounded ${colors[status] ?? 'bg-gray-100 text-gray-600'}`}>
      {status}
    </span>
  )
}
