import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../api/client'
import type { ApiResponse, MembershipTierDto, ContractDto, MemberDto } from '../types'

export default function ContractsPage() {
  const [tab, setTab] = useState<'tiers' | 'contracts'>('tiers')
  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Contracts & Membership Tiers</h1>
      <div className="flex space-x-4 mb-6 border-b">
        {[{ key: 'tiers' as const, label: 'Membership Tiers' }, { key: 'contracts' as const, label: 'Member Contracts' }].map(t => (
          <button key={t.key} onClick={() => setTab(t.key)}
            className={`pb-2 px-1 text-sm font-medium ${tab === t.key ? 'border-b-2 border-indigo-600 text-indigo-600' : 'text-gray-500 hover:text-gray-700'}`}>
            {t.label}
          </button>
        ))}
      </div>
      {tab === 'tiers' && <TiersTab />}
      {tab === 'contracts' && <ContractsTab />}
    </div>
  )
}

function TiersTab() {
  const qc = useQueryClient()
  const [showModal, setShowModal] = useState(false)
  const [editId, setEditId] = useState<string | null>(null)
  const [form, setForm] = useState({ name: '', description: '', monthlyPrice: '', billingCycle: 'MONTHLY', minimumTermMonths: '0', noticePeriodDays: '30', classAllowance: '', accessRules: '' })

  const { data: tiersRes } = useQuery({
    queryKey: ['tiers'],
    queryFn: () => api.get<ApiResponse<MembershipTierDto[]>>('/membership-tiers').then(r => r.data),
  })
  const tiers = tiersRes?.data ?? []

  const saveMutation = useMutation({
    mutationFn: (data: Record<string, unknown>) =>
      editId ? api.put(`/membership-tiers/${editId}`, data) : api.post('/membership-tiers', data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['tiers'] }); close() },
  })
  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.delete(`/membership-tiers/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['tiers'] }),
  })

  const openCreate = () => { setEditId(null); setForm({ name: '', description: '', monthlyPrice: '', billingCycle: 'MONTHLY', minimumTermMonths: '0', noticePeriodDays: '30', classAllowance: '', accessRules: '' }); setShowModal(true) }
  const openEdit = (t: MembershipTierDto) => {
    setEditId(t.id); setForm({ name: t.name, description: t.description ?? '', monthlyPrice: String(t.monthlyPrice), billingCycle: t.billingCycle, minimumTermMonths: String(t.minimumTermMonths), noticePeriodDays: String(t.noticePeriodDays), classAllowance: t.classAllowance != null ? String(t.classAllowance) : '', accessRules: t.accessRules ?? '' }); setShowModal(true)
  }
  const close = () => { setShowModal(false); setEditId(null) }
  const handleSubmit = (e: React.FormEvent) => { e.preventDefault(); saveMutation.mutate({ ...form, monthlyPrice: parseFloat(form.monthlyPrice), minimumTermMonths: parseInt(form.minimumTermMonths), noticePeriodDays: parseInt(form.noticePeriodDays), classAllowance: form.classAllowance ? parseInt(form.classAllowance) : null }) }

  return (
    <>
      <div className="flex justify-end mb-4">
        <button onClick={openCreate} className="bg-indigo-600 text-white px-4 py-2 rounded text-sm hover:bg-indigo-700">Add Tier</button>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {tiers.map(t => (
          <div key={t.id} className="bg-white rounded-lg shadow p-5">
            <h3 className="text-lg font-semibold">{t.name}</h3>
            <p className="text-3xl font-bold text-indigo-600 mt-2">&euro;{t.monthlyPrice}<span className="text-sm font-normal text-gray-400">/mo</span></p>
            {t.description && <p className="text-sm text-gray-500 mt-2">{t.description}</p>}
            <div className="text-xs text-gray-500 mt-3 space-y-1">
              <p>Billing: {t.billingCycle}</p>
              <p>Min term: {t.minimumTermMonths} months</p>
              <p>Notice: {t.noticePeriodDays} days</p>
              {t.classAllowance != null && <p>Classes: {t.classAllowance === 0 ? 'Unlimited' : t.classAllowance + '/mo'}</p>}
            </div>
            <div className="flex gap-2 mt-4 border-t pt-3">
              <button onClick={() => openEdit(t)} className="text-xs text-indigo-600 hover:text-indigo-800">Edit</button>
              <button onClick={() => deleteMutation.mutate(t.id)} className="text-xs text-red-600 hover:text-red-800">Delete</button>
            </div>
          </div>
        ))}
      </div>
      {showModal && (
        <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-lg">
            <h2 className="text-lg font-semibold mb-4">{editId ? 'Edit Tier' : 'Add Tier'}</h2>
            <form onSubmit={handleSubmit} className="space-y-3">
              <Inp label="Name *" value={form.name} onChange={v => setForm({ ...form, name: v })} required />
              <Inp label="Description" value={form.description} onChange={v => setForm({ ...form, description: v })} />
              <div className="grid grid-cols-2 gap-3">
                <Inp label="Monthly Price *" type="number" value={form.monthlyPrice} onChange={v => setForm({ ...form, monthlyPrice: v })} required />
                <div><label className="block text-sm font-medium text-gray-700 mb-1">Billing Cycle</label>
                  <select value={form.billingCycle} onChange={e => setForm({ ...form, billingCycle: e.target.value })} className="w-full border rounded px-3 py-2 text-sm">
                    <option value="MONTHLY">Monthly</option><option value="QUARTERLY">Quarterly</option><option value="YEARLY">Yearly</option>
                  </select></div>
                <Inp label="Min Term (months)" type="number" value={form.minimumTermMonths} onChange={v => setForm({ ...form, minimumTermMonths: v })} />
                <Inp label="Notice (days)" type="number" value={form.noticePeriodDays} onChange={v => setForm({ ...form, noticePeriodDays: v })} />
              </div>
              <Inp label="Class Allowance (0=unlimited)" type="number" value={form.classAllowance} onChange={v => setForm({ ...form, classAllowance: v })} />
              <Inp label="Access Rules" value={form.accessRules} onChange={v => setForm({ ...form, accessRules: v })} />
              <div className="flex justify-end gap-3 pt-2">
                <button type="button" onClick={close} className="px-4 py-2 text-sm text-gray-600">Cancel</button>
                <button type="submit" disabled={saveMutation.isPending} className="bg-indigo-600 text-white px-4 py-2 rounded text-sm disabled:opacity-50">{saveMutation.isPending ? 'Saving...' : editId ? 'Update' : 'Create'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  )
}

function ContractsTab() {
  const qc = useQueryClient()
  const [memberSearch, setMemberSearch] = useState('')
  const [selectedMember, setSelectedMember] = useState<MemberDto | null>(null)
  const [showCreate, setShowCreate] = useState(false)
  const [freezeId, setFreezeId] = useState<string | null>(null)
  const [cancelId, setCancelId] = useState<string | null>(null)
  const [freezeForm, setFreezeForm] = useState({ startDate: '', endDate: '', reason: '' })
  const [cancelReason, setCancelReason] = useState('')
  const [createForm, setCreateForm] = useState({ membershipTierId: '', startDate: '', discountCode: '' })

  const { data: membersRes } = useQuery({
    queryKey: ['members-lookup', memberSearch],
    queryFn: () => api.get<ApiResponse<MemberDto[]>>('/members', { params: { name: memberSearch, size: 8 } }).then(r => r.data),
    enabled: memberSearch.length >= 2 && !selectedMember,
  })

  const { data: contractsRes } = useQuery({
    queryKey: ['member-contracts', selectedMember?.id],
    queryFn: () => api.get<ApiResponse<ContractDto[]>>(`/contracts/member/${selectedMember!.id}`).then(r => r.data),
    enabled: !!selectedMember,
  })
  const contracts = contractsRes?.data ?? []

  const { data: tiersRes } = useQuery({ queryKey: ['tiers'], queryFn: () => api.get<ApiResponse<MembershipTierDto[]>>('/membership-tiers').then(r => r.data) })
  const tiers = tiersRes?.data ?? []

  const createMut = useMutation({
    mutationFn: (data: Record<string, unknown>) => api.post('/contracts', data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['member-contracts'] }); setShowCreate(false) },
  })
  const freezeMut = useMutation({
    mutationFn: ({ id, data }: { id: string; data: Record<string, string> }) => api.post(`/contracts/${id}/freeze`, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['member-contracts'] }); setFreezeId(null) },
  })
  const unfreezeMut = useMutation({ mutationFn: (id: string) => api.post(`/contracts/${id}/unfreeze`), onSuccess: () => qc.invalidateQueries({ queryKey: ['member-contracts'] }) })
  const cancelMut = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => api.post(`/contracts/${id}/cancel`, { reason }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['member-contracts'] }); setCancelId(null) },
  })
  const withdrawMut = useMutation({ mutationFn: (id: string) => api.post(`/contracts/${id}/withdraw-cancellation`), onSuccess: () => qc.invalidateQueries({ queryKey: ['member-contracts'] }) })

  const sc: Record<string, string> = { ACTIVE: 'bg-green-100 text-green-700', PAUSED: 'bg-yellow-100 text-yellow-700', PENDING_CANCELLATION: 'bg-orange-100 text-orange-700', CANCELLED: 'bg-red-100 text-red-700', EXPIRED: 'bg-gray-100 text-gray-600' }

  return (
    <>
      <div className="mb-6">
        <label className="block text-sm font-medium text-gray-700 mb-1">Search member</label>
        <input type="text" placeholder="Type member name..." value={memberSearch}
          onChange={e => { setMemberSearch(e.target.value); if (selectedMember) setSelectedMember(null) }}
          className="w-full max-w-md px-4 py-2 border rounded-md" />
        {!selectedMember && (membersRes?.data ?? []).length > 0 && (
          <div className="mt-1 bg-white border rounded shadow max-w-md max-h-40 overflow-y-auto">
            {(membersRes?.data ?? []).map(m => (
              <button key={m.id} onClick={() => { setSelectedMember(m); setMemberSearch(m.firstName + ' ' + m.lastName) }}
                className="w-full text-left px-4 py-2 text-sm hover:bg-gray-50">{m.firstName} {m.lastName} <span className="text-gray-400">({m.memberNumber})</span></button>
            ))}
          </div>
        )}
        {selectedMember && <button onClick={() => { setSelectedMember(null); setMemberSearch('') }} className="text-xs text-gray-500 mt-1 underline">Clear</button>}
      </div>

      {selectedMember && (
        <>
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-semibold">Contracts for {selectedMember.firstName} {selectedMember.lastName}</h3>
            <button onClick={() => setShowCreate(true)} className="bg-indigo-600 text-white px-4 py-2 rounded text-sm hover:bg-indigo-700">New Contract</button>
          </div>

          {contracts.length === 0 ? <p className="text-gray-500">No contracts.</p> : (
            <div className="space-y-4">
              {contracts.map(c => (
                <div key={c.id} className="bg-white rounded-lg shadow p-5">
                  <div className="flex items-center justify-between mb-3">
                    <div>
                      <h4 className="font-semibold text-lg">{c.membershipTierName}</h4>
                      <p className="text-sm text-gray-500">{c.startDate} — {c.endDate ?? 'Ongoing'}</p>
                    </div>
                    <span className={`text-xs px-3 py-1 rounded-full font-medium ${sc[c.status] ?? 'bg-gray-100'}`}>{c.status?.replace(/_/g, ' ')}</span>
                  </div>
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm mb-3">
                    <div><p className="text-gray-500">Amount</p><p className="font-semibold">&euro;{c.monthlyAmount}/mo</p></div>
                    <div><p className="text-gray-500">Next Billing</p><p className="font-semibold">{c.nextBillingDate ?? '—'}</p></div>
                    <div><p className="text-gray-500">Auto Renew</p><p className="font-semibold">{c.autoRenew ? 'Yes' : 'No'}</p></div>
                    {c.cancellationEffectiveDate && <div><p className="text-gray-500">Cancel Date</p><p className="font-semibold text-red-600">{c.cancellationEffectiveDate}</p></div>}
                  </div>
                  <div className="flex gap-3 border-t pt-3">
                    {c.status === 'ACTIVE' && <><button onClick={() => setFreezeId(c.id)} className="text-xs text-blue-600">Freeze</button><button onClick={() => setCancelId(c.id)} className="text-xs text-red-600">Cancel</button></>}
                    {c.status === 'PAUSED' && <button onClick={() => unfreezeMut.mutate(c.id)} className="text-xs text-green-600">Unfreeze</button>}
                    {c.status === 'PENDING_CANCELLATION' && <button onClick={() => withdrawMut.mutate(c.id)} className="text-xs text-green-600">Withdraw Cancellation</button>}
                  </div>
                  {freezeId === c.id && (
                    <div className="mt-3 bg-blue-50 border border-blue-200 rounded p-3 space-y-2">
                      <div className="grid grid-cols-2 gap-2">
                        <Inp label="Start" type="date" value={freezeForm.startDate} onChange={v => setFreezeForm({ ...freezeForm, startDate: v })} required />
                        <Inp label="End" type="date" value={freezeForm.endDate} onChange={v => setFreezeForm({ ...freezeForm, endDate: v })} required />
                      </div>
                      <Inp label="Reason" value={freezeForm.reason} onChange={v => setFreezeForm({ ...freezeForm, reason: v })} />
                      <div className="flex gap-2"><button onClick={() => freezeMut.mutate({ id: c.id, data: freezeForm })} className="text-xs bg-blue-600 text-white px-3 py-1 rounded">Freeze</button><button onClick={() => setFreezeId(null)} className="text-xs text-gray-500">Cancel</button></div>
                    </div>
                  )}
                  {cancelId === c.id && (
                    <div className="mt-3 bg-red-50 border border-red-200 rounded p-3 space-y-2">
                      <Inp label="Reason" value={cancelReason} onChange={setCancelReason} />
                      <div className="flex gap-2"><button onClick={() => cancelMut.mutate({ id: c.id, reason: cancelReason })} className="text-xs bg-red-600 text-white px-3 py-1 rounded">Confirm</button><button onClick={() => setCancelId(null)} className="text-xs text-gray-500">Back</button></div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}

          {showCreate && (
            <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
              <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
                <h2 className="text-lg font-semibold mb-4">New Contract for {selectedMember.firstName}</h2>
                <form onSubmit={e => { e.preventDefault(); createMut.mutate({ memberId: selectedMember.id, membershipTierId: createForm.membershipTierId, startDate: createForm.startDate || undefined, discountCode: createForm.discountCode || undefined }) }} className="space-y-3">
                  <div><label className="block text-sm font-medium text-gray-700 mb-1">Tier *</label>
                    <select value={createForm.membershipTierId} onChange={e => setCreateForm({ ...createForm, membershipTierId: e.target.value })} required className="w-full border rounded px-3 py-2 text-sm">
                      <option value="">Select...</option>{tiers.map(t => <option key={t.id} value={t.id}>{t.name} — €{t.monthlyPrice}/mo</option>)}
                    </select></div>
                  <Inp label="Start Date" type="date" value={createForm.startDate} onChange={v => setCreateForm({ ...createForm, startDate: v })} />
                  <Inp label="Discount Code" value={createForm.discountCode} onChange={v => setCreateForm({ ...createForm, discountCode: v })} />
                  <div className="flex justify-end gap-3 pt-2">
                    <button type="button" onClick={() => setShowCreate(false)} className="px-4 py-2 text-sm text-gray-600">Cancel</button>
                    <button type="submit" disabled={createMut.isPending || !createForm.membershipTierId} className="bg-indigo-600 text-white px-4 py-2 rounded text-sm disabled:opacity-50">{createMut.isPending ? 'Creating...' : 'Create'}</button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </>
      )}
    </>
  )
}

function Inp({ label, value, onChange, type = 'text', required = false }: {
  label: string; value: string; onChange: (v: string) => void; type?: string; required?: boolean
}) {
  return (
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
      <input type={type} value={value} onChange={e => onChange(e.target.value)} required={required} className="w-full border rounded px-3 py-2 text-sm" />
    </div>
  )
}
