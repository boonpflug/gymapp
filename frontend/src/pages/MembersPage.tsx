import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../api/client'
import type { ApiResponse, MemberDto } from '../types'

interface MemberForm {
  firstName: string; lastName: string; email: string; phone: string
  dateOfBirth: string; gender: string
  street: string; city: string; state: string; postalCode: string; country: string
  emergencyContactName: string; emergencyContactPhone: string; healthNotes: string
}

const emptyForm: MemberForm = {
  firstName: '', lastName: '', email: '', phone: '', dateOfBirth: '', gender: '',
  street: '', city: '', state: '', postalCode: '', country: '',
  emergencyContactName: '', emergencyContactPhone: '', healthNotes: '',
}

export default function MembersPage() {
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [showModal, setShowModal] = useState(false)
  const [editId, setEditId] = useState<string | null>(null)
  const [form, setForm] = useState<MemberForm>(emptyForm)
  const qc = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['members', search, page],
    queryFn: () => api.get<ApiResponse<MemberDto[]>>('/members', {
      params: { name: search || undefined, page, size: 20 },
    }).then(r => r.data),
  })
  const members = data?.data ?? []
  const meta = data?.meta

  const createMutation = useMutation({
    mutationFn: (data: MemberForm) => api.post('/members', data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['members'] }); closeModal() },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: MemberForm }) => api.put(`/members/${id}`, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['members'] }); closeModal() },
  })

  const deactivateMutation = useMutation({
    mutationFn: (id: string) => api.post(`/members/${id}/deactivate`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['members'] }),
  })

  const openCreate = () => { setEditId(null); setForm(emptyForm); setShowModal(true) }

  const openEdit = (m: MemberDto) => {
    setEditId(m.id)
    setForm({
      firstName: m.firstName ?? '', lastName: m.lastName ?? '',
      email: m.email ?? '', phone: m.phone ?? '',
      dateOfBirth: m.dateOfBirth ?? '', gender: m.gender ?? '',
      street: m.street ?? '', city: m.city ?? '',
      state: m.state ?? '', postalCode: m.postalCode ?? '',
      country: m.country ?? '',
      emergencyContactName: m.emergencyContactName ?? '',
      emergencyContactPhone: m.emergencyContactPhone ?? '',
      healthNotes: m.healthNotes ?? '',
    })
    setShowModal(true)
  }

  const closeModal = () => { setShowModal(false); setEditId(null); setForm(emptyForm) }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (editId) {
      updateMutation.mutate({ id: editId, data: form })
    } else {
      createMutation.mutate(form)
    }
  }

  const isPending = createMutation.isPending || updateMutation.isPending

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Members</h1>
        <button onClick={openCreate} className="bg-indigo-600 text-white px-4 py-2 rounded-md hover:bg-indigo-700 text-sm">
          Add Member
        </button>
      </div>

      <div className="mb-4">
        <input type="text" placeholder="Search by name..." value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0) }}
          className="w-full max-w-md px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500" />
      </div>

      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Member #</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Email</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Phone</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Joined</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {isLoading ? (
              <tr><td colSpan={7} className="px-6 py-4 text-center text-gray-500">Loading...</td></tr>
            ) : members.length === 0 ? (
              <tr><td colSpan={7} className="px-6 py-4 text-center text-gray-500">No members found</td></tr>
            ) : (
              members.map((m) => (
                <tr key={m.id} className="hover:bg-gray-50 cursor-pointer" onClick={() => openEdit(m)}>
                  <td className="px-6 py-4 text-sm text-gray-900">{m.memberNumber}</td>
                  <td className="px-6 py-4 text-sm text-gray-900">{m.firstName} {m.lastName}</td>
                  <td className="px-6 py-4 text-sm text-gray-500">{m.email}</td>
                  <td className="px-6 py-4 text-sm text-gray-500">{m.phone ?? '—'}</td>
                  <td className="px-6 py-4"><StatusBadge status={m.status} /></td>
                  <td className="px-6 py-4 text-sm text-gray-500">{m.joinDate}</td>
                  <td className="px-6 py-4 text-right" onClick={e => e.stopPropagation()}>
                    {m.status === 'ACTIVE' && (
                      <button onClick={() => deactivateMutation.mutate(m.id)}
                        className="text-xs text-red-600 hover:text-red-800">Deactivate</button>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
        {meta && meta.totalPages > 1 && (
          <div className="flex items-center justify-between px-6 py-3 border-t bg-gray-50">
            <p className="text-sm text-gray-500">Page {meta.page + 1} of {meta.totalPages} ({meta.totalElements} members)</p>
            <div className="flex gap-2">
              <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
                className="px-3 py-1 text-sm border rounded disabled:opacity-50">Previous</button>
              <button onClick={() => setPage(p => p + 1)} disabled={page >= meta.totalPages - 1}
                className="px-3 py-1 text-sm border rounded disabled:opacity-50">Next</button>
            </div>
          </div>
        )}
      </div>

      {/* Create/Edit Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto">
            <h2 className="text-lg font-semibold mb-4">{editId ? 'Edit Member' : 'Add Member'}</h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <Field label="First Name *" value={form.firstName} onChange={v => setForm({ ...form, firstName: v })} required />
                <Field label="Last Name *" value={form.lastName} onChange={v => setForm({ ...form, lastName: v })} required />
                <Field label="Email *" type="email" value={form.email} onChange={v => setForm({ ...form, email: v })} required />
                <Field label="Phone" value={form.phone} onChange={v => setForm({ ...form, phone: v })} />
                <Field label="Date of Birth" type="date" value={form.dateOfBirth} onChange={v => setForm({ ...form, dateOfBirth: v })} />
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Gender</label>
                  <select value={form.gender} onChange={e => setForm({ ...form, gender: e.target.value })} className="w-full border rounded px-3 py-2 text-sm">
                    <option value="">Select</option>
                    <option value="MALE">Male</option>
                    <option value="FEMALE">Female</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>
                <Field label="Street" value={form.street} onChange={v => setForm({ ...form, street: v })} />
                <Field label="City" value={form.city} onChange={v => setForm({ ...form, city: v })} />
                <Field label="State" value={form.state} onChange={v => setForm({ ...form, state: v })} />
                <Field label="Postal Code" value={form.postalCode} onChange={v => setForm({ ...form, postalCode: v })} />
                <Field label="Country" value={form.country} onChange={v => setForm({ ...form, country: v })} />
              </div>
              <div className="border-t pt-4 mt-2">
                <h3 className="text-sm font-semibold text-gray-700 mb-3">Emergency Contact</h3>
                <div className="grid grid-cols-2 gap-4">
                  <Field label="Name" value={form.emergencyContactName} onChange={v => setForm({ ...form, emergencyContactName: v })} />
                  <Field label="Phone" value={form.emergencyContactPhone} onChange={v => setForm({ ...form, emergencyContactPhone: v })} />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Health Notes</label>
                <textarea value={form.healthNotes} onChange={e => setForm({ ...form, healthNotes: e.target.value })}
                  rows={2} className="w-full border rounded px-3 py-2 text-sm" />
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <button type="button" onClick={closeModal} className="px-4 py-2 text-sm text-gray-600">Cancel</button>
                <button type="submit" disabled={isPending}
                  className="bg-indigo-600 text-white px-4 py-2 rounded text-sm hover:bg-indigo-700 disabled:opacity-50">
                  {isPending ? 'Saving...' : editId ? 'Update' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

function Field({ label, value, onChange, type = 'text', required = false }: {
  label: string; value: string; onChange: (v: string) => void; type?: string; required?: boolean
}) {
  return (
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
      <input type={type} value={value} onChange={e => onChange(e.target.value)} required={required}
        className="w-full border rounded px-3 py-2 text-sm" />
    </div>
  )
}

function StatusBadge({ status }: { status: string }) {
  const colors: Record<string, string> = {
    ACTIVE: 'bg-green-100 text-green-800',
    INACTIVE: 'bg-gray-100 text-gray-800',
    DELETED: 'bg-red-100 text-red-800',
  }
  return (
    <span className={`px-2 py-1 text-xs font-medium rounded-full ${colors[status] || 'bg-gray-100'}`}>{status}</span>
  )
}
