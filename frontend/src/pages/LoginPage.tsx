import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useLogin } from '../api/auth'
import { useAuthStore } from '../store/authStore'

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [tenantId] = useState('demo_gym')
  const login = useLogin()
  const navigate = useNavigate()
  const location = useLocation()
  const setTenant = useAuthStore((s) => s.setTenantId)

  const from = (location.state as { from?: string })?.from
  const isPortalLogin = from?.startsWith('/portal') || location.pathname === '/portal/login'

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setTenant(tenantId)
    login.mutate(
      { email, password },
      {
        onSuccess: (data) => {
          if (from) {
            navigate(from)
          } else if (data.user.role === 'MEMBER') {
            navigate('/portal')
          } else {
            navigate('/')
          }
        },
      },
    )
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-gradient-to-br from-brand-950 via-brand-900 to-brand-950">
      {/* Subtle pattern overlay */}
      <div className="absolute inset-0 opacity-5" style={{
        backgroundImage: 'radial-gradient(circle at 1px 1px, white 1px, transparent 0)',
        backgroundSize: '40px 40px',
      }} />

      <div className="relative z-10 w-full max-w-md px-4">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-brand-500 rounded-xl flex items-center justify-center shadow-lg shadow-brand-500/30">
              <span className="text-white font-bold text-lg">F</span>
            </div>
            <h1 className="text-3xl font-semibold text-white tracking-tight">Fitagend</h1>
          </div>
          <p className="text-brand-400 text-sm">
            {isPortalLogin ? 'Member Portal' : 'Studio Management Platform'}
          </p>
        </div>

        {/* Card */}
        <div className="bg-white rounded-2xl shadow-2xl shadow-black/20 p-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-1">
            {isPortalLogin ? 'Welcome back' : 'Sign in'}
          </h2>
          <p className="text-sm text-gray-500 mb-6">
            {isPortalLogin ? 'Access your membership dashboard' : 'Enter your credentials to continue'}
          </p>

          <form onSubmit={handleSubmit} className="space-y-4">
            <input type="hidden" value={tenantId} />
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">Email</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
                className="w-full px-3.5 py-2.5 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent transition-shadow"
                placeholder="you@studio.com"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">Password</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete="current-password"
                className="w-full px-3.5 py-2.5 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent transition-shadow"
              />
            </div>

            {login.isError && (
              <div className="bg-red-50 text-red-700 text-sm px-4 py-3 rounded-xl">
                Login failed. Please check your credentials.
              </div>
            )}

            <button
              type="submit"
              disabled={login.isPending}
              className="w-full bg-brand-600 text-white py-2.5 rounded-xl font-medium text-sm hover:bg-brand-700 focus:ring-2 focus:ring-brand-500 focus:ring-offset-2 disabled:opacity-50 transition-colors"
            >
              {login.isPending ? 'Signing in...' : 'Sign in'}
            </button>
          </form>

          <div className="mt-6 pt-4 border-t text-center">
            {!isPortalLogin ? (
              <p className="text-sm text-gray-500">
                Member?{' '}
                <a href="/portal" className="text-brand-600 hover:text-brand-700 font-medium">
                  Open Member Portal
                </a>
              </p>
            ) : (
              <p className="text-sm text-gray-500">
                Staff?{' '}
                <a href="/login" className="text-brand-600 hover:text-brand-700 font-medium">
                  Open Staff Dashboard
                </a>
              </p>
            )}
          </div>
        </div>

        {/* Footer */}
        <p className="text-center text-brand-600/50 text-xs mt-6">
          Powered by Fitagend
        </p>
      </div>
    </div>
  )
}
