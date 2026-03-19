import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useLogin } from '../api/auth'
import { useAuthStore } from '../store/authStore'

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [tenantId, setTenantId] = useState('demo_gym')
  const login = useLogin()
  const navigate = useNavigate()
  const location = useLocation()
  const setTenant = useAuthStore((s) => s.setTenantId)

  // Check if user was trying to reach a specific page
  const from = (location.state as { from?: string })?.from

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setTenant(tenantId)
    login.mutate(
      { email, password },
      {
        onSuccess: (data) => {
          // Redirect based on role
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

  const isPortalLogin = from?.startsWith('/portal') || location.pathname === '/portal/login'

  return (
    <div className={`min-h-screen flex items-center justify-center ${isPortalLogin ? 'bg-emerald-50' : 'bg-gray-100'}`}>
      <div className="bg-white p-8 rounded-lg shadow-md w-96">
        <h2 className={`text-2xl font-bold text-center mb-2 ${isPortalLogin ? 'text-emerald-600' : 'text-indigo-600'}`}>
          {isPortalLogin ? 'Member Portal' : 'Gym Platform'}
        </h2>
        {isPortalLogin && (
          <p className="text-center text-sm text-gray-500 mb-4">Log in to manage your membership</p>
        )}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Tenant ID</label>
            <input type="text" value={tenantId} onChange={(e) => setTenantId(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required autoComplete="current-password"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          </div>
          {login.isError && (
            <p className="text-red-500 text-sm">Login failed. Check your credentials and tenant ID.</p>
          )}
          <button type="submit" disabled={login.isPending}
            className={`w-full text-white py-2 rounded-md disabled:opacity-50 ${isPortalLogin ? 'bg-emerald-600 hover:bg-emerald-700' : 'bg-indigo-600 hover:bg-indigo-700'}`}>
            {login.isPending ? 'Logging in...' : 'Login'}
          </button>
        </form>
        {!isPortalLogin && (
          <p className="text-center text-xs text-gray-400 mt-4">
            Member? <a href="/portal" className="text-emerald-600 hover:underline">Go to Member Portal</a>
          </p>
        )}
        {isPortalLogin && (
          <p className="text-center text-xs text-gray-400 mt-4">
            Staff? <a href="/login" className="text-indigo-600 hover:underline">Go to Staff Login</a>
          </p>
        )}
      </div>
    </div>
  )
}
