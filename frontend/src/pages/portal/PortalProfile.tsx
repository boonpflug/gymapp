import { useState, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../../api/client'
import type { ApiResponse, MemberDto } from '../../types'

interface ProfileForm {
  firstName: string
  lastName: string
  email: string
  phone: string
  dateOfBirth: string
  gender: string
  street: string
  city: string
  state: string
  postalCode: string
  country: string
  emergencyContactName: string
  emergencyContactPhone: string
}

export default function PortalProfile() {
  const qc = useQueryClient()
  const [editing, setEditing] = useState(false)
  const [form, setForm] = useState<ProfileForm>({
    firstName: '', lastName: '', email: '', phone: '', dateOfBirth: '',
    gender: '', street: '', city: '', state: '', postalCode: '', country: '',
    emergencyContactName: '', emergencyContactPhone: '',
  })

  const { data: profileRes } = useQuery({
    queryKey: ['portal-profile'],
    queryFn: () => api.get<ApiResponse<MemberDto>>('/portal/profile').then(r => r.data),
  })
  const profile = profileRes?.data

  useEffect(() => {
    if (profile) {
      setForm({
        firstName: profile.firstName ?? '',
        lastName: profile.lastName ?? '',
        email: profile.email ?? '',
        phone: profile.phone ?? '',
        dateOfBirth: profile.dateOfBirth ?? '',
        gender: profile.gender ?? '',
        street: profile.street ?? '',
        city: profile.city ?? '',
        state: profile.state ?? '',
        postalCode: profile.postalCode ?? '',
        country: profile.country ?? '',
        emergencyContactName: profile.emergencyContactName ?? '',
        emergencyContactPhone: profile.emergencyContactPhone ?? '',
      })
    }
  }, [profile])

  const updateMutation = useMutation({
    mutationFn: (data: ProfileForm) => api.put('/portal/profile', data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['portal-profile'] })
      setEditing(false)
    },
  })

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault()
    updateMutation.mutate(form)
  }

  if (!profile) return <p className="text-gray-400">Loading profile...</p>

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">My Profile</h1>
        {!editing && (
          <button
            onClick={() => setEditing(true)}
            className="bg-brand-600 text-white px-4 py-2 rounded text-sm hover:bg-brand-700"
          >
            Edit Profile
          </button>
        )}
      </div>

      <div className="bg-white rounded-lg shadow p-6">
        {editing ? (
          <form onSubmit={handleSave} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <Field label="First Name" value={form.firstName} onChange={v => setForm({ ...form, firstName: v })} />
              <Field label="Last Name" value={form.lastName} onChange={v => setForm({ ...form, lastName: v })} />
              <Field label="Email" value={form.email} onChange={v => setForm({ ...form, email: v })} type="email" />
              <Field label="Phone" value={form.phone} onChange={v => setForm({ ...form, phone: v })} />
              <Field label="Date of Birth" value={form.dateOfBirth} onChange={v => setForm({ ...form, dateOfBirth: v })} type="date" />
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Gender</label>
                <select
                  value={form.gender}
                  onChange={e => setForm({ ...form, gender: e.target.value })}
                  className="w-full border rounded px-3 py-2 text-sm"
                >
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
              <div className="col-span-2 border-t pt-4 mt-2">
                <h3 className="text-sm font-semibold text-gray-700 mb-3">Emergency Contact</h3>
                <div className="grid grid-cols-2 gap-4">
                  <Field label="Name" value={form.emergencyContactName} onChange={v => setForm({ ...form, emergencyContactName: v })} />
                  <Field label="Phone" value={form.emergencyContactPhone} onChange={v => setForm({ ...form, emergencyContactPhone: v })} />
                </div>
              </div>
            </div>
            <div className="flex justify-end gap-3 pt-2">
              <button type="button" onClick={() => setEditing(false)} className="px-4 py-2 text-sm text-gray-600">Cancel</button>
              <button type="submit" disabled={updateMutation.isPending}
                className="bg-brand-600 text-white px-4 py-2 rounded text-sm hover:bg-brand-700 disabled:opacity-50">
                {updateMutation.isPending ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </form>
        ) : (
          <div className="grid grid-cols-2 gap-y-4 gap-x-8">
            <ReadField label="Member Number" value={profile.memberNumber} />
            <ReadField label="Status" value={profile.status} />
            <ReadField label="First Name" value={profile.firstName} />
            <ReadField label="Last Name" value={profile.lastName} />
            <ReadField label="Email" value={profile.email} />
            <ReadField label="Phone" value={profile.phone} />
            <ReadField label="Date of Birth" value={profile.dateOfBirth} />
            <ReadField label="Gender" value={profile.gender} />
            <ReadField label="Street" value={profile.street} />
            <ReadField label="City" value={profile.city} />
            <ReadField label="State" value={profile.state} />
            <ReadField label="Postal Code" value={profile.postalCode} />
            <ReadField label="Country" value={profile.country} />
            <ReadField label="Join Date" value={profile.joinDate} />
            <div className="col-span-2 border-t pt-4 mt-2">
              <h3 className="text-sm font-semibold text-gray-700 mb-3">Emergency Contact</h3>
              <div className="grid grid-cols-2 gap-4">
                <ReadField label="Name" value={profile.emergencyContactName} />
                <ReadField label="Phone" value={profile.emergencyContactPhone} />
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

function Field({ label, value, onChange, type = 'text' }: {
  label: string; value: string; onChange: (v: string) => void; type?: string
}) {
  return (
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
      <input type={type} value={value} onChange={e => onChange(e.target.value)}
        className="w-full border rounded px-3 py-2 text-sm" />
    </div>
  )
}

function ReadField({ label, value }: { label: string; value?: string | null }) {
  return (
    <div>
      <p className="text-xs text-gray-500">{label}</p>
      <p className="text-sm font-medium">{value || '—'}</p>
    </div>
  )
}
