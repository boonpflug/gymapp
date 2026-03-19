import { Outlet, Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'

const portalNavItems = [
  { path: '/portal', label: 'Dashboard' },
  { path: '/portal/profile', label: 'My Profile' },
  { path: '/portal/contracts', label: 'Contracts' },
  { path: '/portal/invoices', label: 'Invoices' },
  { path: '/portal/classes', label: 'Classes' },
  { path: '/portal/training', label: 'Training' },
  { path: '/portal/checkins', label: 'Check-in History' },
]

export default function PortalLayout() {
  const location = useLocation()
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)
  const logout = useAuthStore((s) => s.logout)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="flex h-screen bg-gray-50">
      <aside className="w-64 bg-white shadow-md flex flex-col">
        <div className="p-6 border-b">
          <h1 className="text-xl font-bold text-emerald-600">Member Portal</h1>
          <p className="text-sm text-gray-500 mt-1">
            {user?.firstName} {user?.lastName}
          </p>
        </div>
        <nav className="flex-1 mt-2">
          {portalNavItems.map((item) => {
            const isActive = location.pathname === item.path ||
              (item.path !== '/portal' && location.pathname.startsWith(item.path))
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`block px-6 py-3 text-sm ${
                  isActive
                    ? 'bg-emerald-50 text-emerald-700 border-r-2 border-emerald-700'
                    : 'text-gray-600 hover:bg-gray-50'
                }`}
              >
                {item.label}
              </Link>
            )
          })}
        </nav>
        <div className="p-4 border-t">
          <button
            onClick={handleLogout}
            className="text-sm text-gray-500 hover:text-red-600"
          >
            Logout
          </button>
        </div>
      </aside>
      <main className="flex-1 overflow-auto p-8">
        <Outlet />
      </main>
    </div>
  )
}
