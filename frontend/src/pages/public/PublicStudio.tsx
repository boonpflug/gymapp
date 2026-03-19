import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery, useMutation } from '@tanstack/react-query'
import api from '../../api/client'
import type { ApiResponse, ClassScheduleDto, ClassCategoryDto } from '../../types'

interface StudioProfile {
  name: string
  description?: string
  street?: string
  city?: string
  state?: string
  postalCode?: string
  country?: string
  phone?: string
  email?: string
  websiteUrl?: string
  openingHours?: string
  logoUrl?: string
  brandColor?: string
  bannerImageUrl?: string
  facilityCount: number
  facilities: { id: string; name: string; city?: string; street?: string; phone?: string; openingHours?: string }[]
}

interface TrialForm {
  firstName: string; lastName: string; email: string; phone: string
}

interface ContactForm {
  firstName: string; lastName: string; email: string; phone: string; interest: string; message: string
}

const emptyTrial: TrialForm = { firstName: '', lastName: '', email: '', phone: '' }
const emptyContact: ContactForm = { firstName: '', lastName: '', email: '', phone: '', interest: '', message: '' }

export default function PublicStudio() {
  const { slug } = useParams<{ slug: string }>()
  const [tab, setTab] = useState<'about' | 'classes' | 'book' | 'contact'>('about')
  const [trialForm, setTrialForm] = useState<TrialForm>(emptyTrial)
  const [contactForm, setContactForm] = useState<ContactForm>(emptyContact)
  const [selectedSchedule, setSelectedSchedule] = useState<string | null>(null)
  const [bookingSuccess, setBookingSuccess] = useState(false)
  const [contactSuccess, setContactSuccess] = useState(false)

  const { data: profileRes, isLoading, isError } = useQuery({
    queryKey: ['public-profile', slug],
    queryFn: () => api.get<ApiResponse<StudioProfile>>(`/public/${slug}/profile`).then(r => r.data),
  })
  const profile = profileRes?.data

  const { data: categoriesRes } = useQuery({
    queryKey: ['public-categories', slug],
    queryFn: () => api.get<ApiResponse<ClassCategoryDto[]>>(`/public/${slug}/classes/categories`).then(r => r.data),
    enabled: tab === 'classes' || tab === 'book',
  })
  const categories = categoriesRes?.data ?? []

  const weekStart = new Date()
  weekStart.setHours(0, 0, 0, 0)
  weekStart.setDate(weekStart.getDate() - weekStart.getDay() + 1)

  const { data: scheduleRes } = useQuery({
    queryKey: ['public-schedule', slug, weekStart.toISOString()],
    queryFn: () => api.get<ApiResponse<ClassScheduleDto[]>>(`/public/${slug}/classes/schedule`, {
      params: { weekStart: weekStart.toISOString() },
    }).then(r => r.data),
    enabled: tab === 'classes' || tab === 'book',
  })
  const schedules = scheduleRes?.data ?? []

  const trialMutation = useMutation({
    mutationFn: (data: TrialForm & { scheduleId: string }) =>
      api.post(`/public/${slug}/booking/trial`, data),
    onSuccess: () => {
      setBookingSuccess(true)
      setTrialForm(emptyTrial)
      setSelectedSchedule(null)
    },
  })

  const contactMutation = useMutation({
    mutationFn: (data: ContactForm) => api.post(`/public/${slug}/contact`, data),
    onSuccess: () => {
      setContactSuccess(true)
      setContactForm(emptyContact)
    },
  })

  const brandColor = profile?.brandColor || '#4f46e5'

  if (isLoading) return <div className="min-h-screen flex items-center justify-center"><p className="text-gray-400">Loading studio...</p></div>
  if (isError || !profile) return <div className="min-h-screen flex items-center justify-center"><p className="text-red-500">Studio not found.</p></div>

  const dayNames = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

  let openingHours: Record<string, string> = {}
  try { openingHours = profile.openingHours ? JSON.parse(profile.openingHours) : {} } catch { /* ignore */ }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header style={{ backgroundColor: brandColor }} className="text-white">
        <div className="max-w-5xl mx-auto px-6 py-12">
          <h1 className="text-4xl font-bold">{profile.name}</h1>
          {profile.description && <p className="mt-3 text-lg opacity-90">{profile.description}</p>}
          {profile.city && <p className="mt-2 opacity-75">{profile.street}, {profile.city} {profile.postalCode}</p>}
        </div>
      </header>

      {/* Nav */}
      <nav className="bg-white shadow sticky top-0 z-10">
        <div className="max-w-5xl mx-auto px-6 flex space-x-6">
          {(['about', 'classes', 'book', 'contact'] as const).map(t => (
            <button key={t} onClick={() => setTab(t)}
              className={`py-4 text-sm font-medium border-b-2 ${
                tab === t ? 'border-current' : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
              style={tab === t ? { color: brandColor, borderColor: brandColor } : {}}>
              {t === 'about' ? 'About' : t === 'classes' ? 'Class Schedule' : t === 'book' ? 'Book Trial' : 'Contact'}
            </button>
          ))}
        </div>
      </nav>

      <main className="max-w-5xl mx-auto px-6 py-8">
        {/* ABOUT */}
        {tab === 'about' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <div>
              <h2 className="text-2xl font-bold mb-4">About Us</h2>
              {profile.description && <p className="text-gray-600 mb-6">{profile.description}</p>}
              <div className="space-y-3 text-sm">
                {profile.phone && <p><span className="font-medium">Phone:</span> {profile.phone}</p>}
                {profile.email && <p><span className="font-medium">Email:</span> {profile.email}</p>}
                {profile.websiteUrl && <p><span className="font-medium">Website:</span> {profile.websiteUrl}</p>}
              </div>
            </div>
            <div>
              <h3 className="text-lg font-semibold mb-3">Opening Hours</h3>
              {Object.keys(openingHours).length > 0 ? (
                <div className="bg-white rounded-lg shadow p-4 space-y-2 text-sm">
                  {Object.entries(openingHours).map(([day, hours]) => (
                    <div key={day} className="flex justify-between">
                      <span className="font-medium capitalize">{day}</span>
                      <span className="text-gray-600">{hours}</span>
                    </div>
                  ))}
                </div>
              ) : <p className="text-gray-500">Contact us for opening hours.</p>}

              {profile.facilities.length > 1 && (
                <div className="mt-6">
                  <h3 className="text-lg font-semibold mb-3">Our Locations</h3>
                  <div className="space-y-3">
                    {profile.facilities.map(f => (
                      <div key={f.id} className="bg-white rounded-lg shadow p-4">
                        <p className="font-medium">{f.name}</p>
                        {f.street && <p className="text-sm text-gray-500">{f.street}, {f.city}</p>}
                        {f.phone && <p className="text-sm text-gray-500">Tel: {f.phone}</p>}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        {/* CLASS SCHEDULE */}
        {tab === 'classes' && (
          <div>
            <h2 className="text-2xl font-bold mb-4">Class Schedule</h2>
            {categories.length > 0 && (
              <div className="flex flex-wrap gap-2 mb-4">
                {categories.map(c => (
                  <span key={c.id} className="text-xs px-3 py-1 rounded-full text-white" style={{ backgroundColor: c.color || '#6b7280' }}>
                    {c.name}
                  </span>
                ))}
              </div>
            )}
            <div className="grid grid-cols-1 md:grid-cols-7 gap-2">
              {dayNames.map((day, i) => {
                const dayDate = new Date(weekStart)
                dayDate.setDate(dayDate.getDate() + i)
                const dayStr = dayDate.toISOString().split('T')[0]
                const daySchedules = schedules.filter(s => s.startTime.startsWith(dayStr) && !s.cancelled)

                return (
                  <div key={day} className="bg-white rounded-lg shadow p-3">
                    <h3 className="font-semibold text-sm text-center mb-1">{day}</h3>
                    <p className="text-xs text-gray-400 text-center mb-3">{dayDate.toLocaleDateString()}</p>
                    {daySchedules.length === 0 ? (
                      <p className="text-xs text-gray-400 text-center">No classes</p>
                    ) : (
                      <div className="space-y-2">
                        {daySchedules.map(s => (
                          <div key={s.id} className="p-2 rounded text-xs border border-gray-200">
                            <p className="font-semibold" style={{ color: s.categoryColor || '#374151' }}>{s.className}</p>
                            <p className="text-gray-500">{new Date(s.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</p>
                            {s.trainerName && <p className="text-gray-400">{s.trainerName}</p>}
                            <p className="text-gray-500">{s.bookedCount}/{s.capacity}</p>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          </div>
        )}

        {/* BOOK TRIAL */}
        {tab === 'book' && (
          <div className="max-w-lg mx-auto">
            <h2 className="text-2xl font-bold mb-4">Book a Trial Session</h2>
            {bookingSuccess ? (
              <div className="bg-green-50 border border-green-200 rounded-lg p-6 text-center">
                <p className="text-green-800 font-semibold text-lg">Booking confirmed!</p>
                <p className="text-green-700 mt-2">Check your email for details. We look forward to seeing you!</p>
                <button onClick={() => setBookingSuccess(false)} className="mt-4 text-sm underline" style={{ color: brandColor }}>
                  Book another session
                </button>
              </div>
            ) : (
              <>
                <p className="text-gray-600 mb-6">Try us out with a free trial session. Select a class and fill in your details.</p>

                <div className="mb-6">
                  <label className="block text-sm font-medium text-gray-700 mb-2">Select a class</label>
                  <div className="space-y-2 max-h-60 overflow-y-auto">
                    {schedules.filter(s => !s.cancelled && s.bookedCount < s.capacity).map(s => (
                      <label key={s.id}
                        className={`flex items-center gap-3 p-3 border rounded cursor-pointer ${
                          selectedSchedule === s.id ? 'border-2' : 'border-gray-200'
                        }`}
                        style={selectedSchedule === s.id ? { borderColor: brandColor } : {}}>
                        <input type="radio" name="schedule" value={s.id}
                          checked={selectedSchedule === s.id}
                          onChange={() => setSelectedSchedule(s.id)} className="sr-only" />
                        <div className="flex-1">
                          <p className="font-medium text-sm">{s.className}</p>
                          <p className="text-xs text-gray-500">
                            {new Date(s.startTime).toLocaleDateString()} at{' '}
                            {new Date(s.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                            {s.trainerName ? ` — ${s.trainerName}` : ''}
                          </p>
                        </div>
                        <span className="text-xs text-gray-400">{s.bookedCount}/{s.capacity}</span>
                      </label>
                    ))}
                    {schedules.filter(s => !s.cancelled && s.bookedCount < s.capacity).length === 0 && (
                      <p className="text-gray-500 text-sm">No available classes this week.</p>
                    )}
                  </div>
                </div>

                <form onSubmit={e => {
                  e.preventDefault()
                  if (!selectedSchedule) return
                  trialMutation.mutate({ ...trialForm, scheduleId: selectedSchedule })
                }} className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <Input label="First Name *" value={trialForm.firstName} onChange={v => setTrialForm({ ...trialForm, firstName: v })} required />
                    <Input label="Last Name *" value={trialForm.lastName} onChange={v => setTrialForm({ ...trialForm, lastName: v })} required />
                  </div>
                  <Input label="Email *" type="email" value={trialForm.email} onChange={v => setTrialForm({ ...trialForm, email: v })} required />
                  <Input label="Phone" value={trialForm.phone} onChange={v => setTrialForm({ ...trialForm, phone: v })} />
                  {trialMutation.isError && <p className="text-red-500 text-sm">Booking failed. The class may be full or not available for trials.</p>}
                  <button type="submit" disabled={!selectedSchedule || trialMutation.isPending}
                    className="w-full text-white py-3 rounded-lg font-medium disabled:opacity-50"
                    style={{ backgroundColor: brandColor }}>
                    {trialMutation.isPending ? 'Booking...' : 'Book Trial Session'}
                  </button>
                </form>
              </>
            )}
          </div>
        )}

        {/* CONTACT */}
        {tab === 'contact' && (
          <div className="max-w-lg mx-auto">
            <h2 className="text-2xl font-bold mb-4">Get in Touch</h2>
            {contactSuccess ? (
              <div className="bg-green-50 border border-green-200 rounded-lg p-6 text-center">
                <p className="text-green-800 font-semibold">Message sent!</p>
                <p className="text-green-700 mt-2">We'll get back to you soon.</p>
                <button onClick={() => setContactSuccess(false)} className="mt-4 text-sm underline" style={{ color: brandColor }}>
                  Send another message
                </button>
              </div>
            ) : (
              <form onSubmit={e => {
                e.preventDefault()
                contactMutation.mutate(contactForm)
              }} className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <Input label="First Name *" value={contactForm.firstName} onChange={v => setContactForm({ ...contactForm, firstName: v })} required />
                  <Input label="Last Name *" value={contactForm.lastName} onChange={v => setContactForm({ ...contactForm, lastName: v })} required />
                </div>
                <Input label="Email *" type="email" value={contactForm.email} onChange={v => setContactForm({ ...contactForm, email: v })} required />
                <Input label="Phone" value={contactForm.phone} onChange={v => setContactForm({ ...contactForm, phone: v })} />
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">What are you interested in?</label>
                  <select value={contactForm.interest} onChange={e => setContactForm({ ...contactForm, interest: e.target.value })}
                    className="w-full border rounded px-3 py-2 text-sm">
                    <option value="">Select...</option>
                    <option value="Membership">Membership</option>
                    <option value="Trial session">Trial session</option>
                    <option value="Group classes">Group classes</option>
                    <option value="Personal training">Personal training</option>
                    <option value="Other">Other</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Message</label>
                  <textarea value={contactForm.message} onChange={e => setContactForm({ ...contactForm, message: e.target.value })}
                    rows={4} className="w-full border rounded px-3 py-2 text-sm" />
                </div>
                {contactMutation.isError && <p className="text-red-500 text-sm">Failed to send. Please try again.</p>}
                <button type="submit" disabled={contactMutation.isPending}
                  className="w-full text-white py-3 rounded-lg font-medium disabled:opacity-50"
                  style={{ backgroundColor: brandColor }}>
                  {contactMutation.isPending ? 'Sending...' : 'Send Message'}
                </button>
              </form>
            )}
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="bg-gray-800 text-gray-400 py-8 mt-12">
        <div className="max-w-5xl mx-auto px-6 text-center text-sm">
          <p>&copy; {new Date().getFullYear()} {profile.name}. All rights reserved.</p>
          {profile.phone && <p className="mt-1">Tel: {profile.phone}</p>}
          {profile.email && <p>Email: {profile.email}</p>}
        </div>
      </footer>
    </div>
  )
}

function Input({ label, value, onChange, type = 'text', required = false }: {
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
