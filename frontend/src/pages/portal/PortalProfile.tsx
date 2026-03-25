import { useState, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
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
  const { t } = useTranslation()
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

  if (!profile) return <p className="text-gray-400">{t('portal.loadingProfile')}</p>

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">{t('portal.profile.title')}</h1>
        {!editing && (
          <button
            onClick={() => setEditing(true)}
            className="bg-brand-600 text-white px-4 py-2 rounded text-sm hover:bg-brand-700"
          >
            {t('portal.profile.editProfile')}
          </button>
        )}
      </div>

      <div className="bg-white rounded-lg shadow p-6">
        {editing ? (
          <form onSubmit={handleSave} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <Field label={t('portal.profile.firstName')} value={form.firstName} onChange={v => setForm({ ...form, firstName: v })} />
              <Field label={t('portal.profile.lastName')} value={form.lastName} onChange={v => setForm({ ...form, lastName: v })} />
              <Field label={t('portal.profile.email')} value={form.email} onChange={v => setForm({ ...form, email: v })} type="email" />
              <Field label={t('portal.profile.phone')} value={form.phone} onChange={v => setForm({ ...form, phone: v })} />
              <Field label={t('portal.profile.dateOfBirth')} value={form.dateOfBirth} onChange={v => setForm({ ...form, dateOfBirth: v })} type="date" />
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">{t('portal.profile.gender')}</label>
                <select
                  value={form.gender}
                  onChange={e => setForm({ ...form, gender: e.target.value })}
                  className="w-full border rounded px-3 py-2 text-sm"
                >
                  <option value="">{t('portal.profile.genderSelect')}</option>
                  <option value="MALE">{t('portal.profile.genderMale')}</option>
                  <option value="FEMALE">{t('portal.profile.genderFemale')}</option>
                  <option value="OTHER">{t('portal.profile.genderOther')}</option>
                </select>
              </div>
              <Field label={t('portal.profile.street')} value={form.street} onChange={v => setForm({ ...form, street: v })} />
              <Field label={t('portal.profile.city')} value={form.city} onChange={v => setForm({ ...form, city: v })} />
              <Field label={t('portal.profile.state')} value={form.state} onChange={v => setForm({ ...form, state: v })} />
              <Field label={t('portal.profile.postalCode')} value={form.postalCode} onChange={v => setForm({ ...form, postalCode: v })} />
              <Field label={t('portal.profile.country')} value={form.country} onChange={v => setForm({ ...form, country: v })} />
              <div className="col-span-2 border-t pt-4 mt-2">
                <h3 className="text-sm font-semibold text-gray-700 mb-3">{t('portal.profile.emergencyContact')}</h3>
                <div className="grid grid-cols-2 gap-4">
                  <Field label={t('portal.profile.name')} value={form.emergencyContactName} onChange={v => setForm({ ...form, emergencyContactName: v })} />
                  <Field label={t('portal.profile.phone')} value={form.emergencyContactPhone} onChange={v => setForm({ ...form, emergencyContactPhone: v })} />
                </div>
              </div>
            </div>
            <div className="flex justify-end gap-3 pt-2">
              <button type="button" onClick={() => setEditing(false)} className="px-4 py-2 text-sm text-gray-600">{t('portal.cancel')}</button>
              <button type="submit" disabled={updateMutation.isPending}
                className="bg-brand-600 text-white px-4 py-2 rounded text-sm hover:bg-brand-700 disabled:opacity-50">
                {updateMutation.isPending ? t('portal.profile.saving') : t('portal.profile.saveChanges')}
              </button>
            </div>
          </form>
        ) : (
          <div className="grid grid-cols-2 gap-y-4 gap-x-8">
            <ReadField label={t('portal.profile.memberNumber')} value={profile.memberNumber} />
            <ReadField label={t('portal.profile.status')} value={profile.status} />
            <ReadField label={t('portal.profile.firstName')} value={profile.firstName} />
            <ReadField label={t('portal.profile.lastName')} value={profile.lastName} />
            <ReadField label={t('portal.profile.email')} value={profile.email} />
            <ReadField label={t('portal.profile.phone')} value={profile.phone} />
            <ReadField label={t('portal.profile.dateOfBirth')} value={profile.dateOfBirth} />
            <ReadField label={t('portal.profile.gender')} value={profile.gender} />
            <ReadField label={t('portal.profile.street')} value={profile.street} />
            <ReadField label={t('portal.profile.city')} value={profile.city} />
            <ReadField label={t('portal.profile.state')} value={profile.state} />
            <ReadField label={t('portal.profile.postalCode')} value={profile.postalCode} />
            <ReadField label={t('portal.profile.country')} value={profile.country} />
            <ReadField label={t('portal.profile.joinDate')} value={profile.joinDate} />
            <div className="col-span-2 border-t pt-4 mt-2">
              <h3 className="text-sm font-semibold text-gray-700 mb-3">{t('portal.profile.emergencyContact')}</h3>
              <div className="grid grid-cols-2 gap-4">
                <ReadField label={t('portal.profile.name')} value={profile.emergencyContactName} />
                <ReadField label={t('portal.profile.phone')} value={profile.emergencyContactPhone} />
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
