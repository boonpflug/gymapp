import { Outlet, Link, useLocation } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'

const navItems = [
  { path: '/', label: 'Dashboard' },
  { path: '/members', label: 'Members' },
  { path: '/contracts', label: 'Contracts' },
  { path: '/checkin', label: 'Check-In' },
  { path: '/classes', label: 'Classes' },
  { path: '/training', label: 'Training' },
  { path: '/communication', label: 'Communication' },
  { path: '/sales', label: 'Sales' },
  { path: '/staff', label: 'Staff' },
  { path: '/facilities', label: 'Facilities' },
]

export default function Layout() {
  const location = useLocation()
  const user = useAuthStore((s) => s.user)
  const logout = useAuthStore((s) => s.logout)

  return (
    <div className="flex h-screen bg-gray-100">
      <aside className="w-64 bg-white shadow-md">
        <div className="p-6">
          <h1 className="text-xl font-bold text-indigo-600">Gym Platform</h1>
          <p className="text-sm text-gray-500 mt-1">
            {user?.firstName} {user?.lastName}
          </p>
          <span className="text-xs bg-indigo-100 text-indigo-700 px-2 py-0.5 rounded mt-1 inline-block">
            {user?.role}
          </span>
        </div>
        <nav className="mt-4">
          {navItems.map((item) => (
            <Link
              key={item.path}
              to={item.path}
              className={`block px-6 py-3 text-sm ${
                location.pathname === item.path
                  ? 'bg-indigo-50 text-indigo-700 border-r-2 border-indigo-700'
                  : 'text-gray-600 hover:bg-gray-50'
              }`}
            >
              {item.label}
            </Link>
          ))}
        </nav>
        <div className="absolute bottom-4 left-4">
          <button
            onClick={logout}
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
