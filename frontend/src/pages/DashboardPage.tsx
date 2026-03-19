import { useAuthStore } from '../store/authStore'

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user)

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Dashboard</h1>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard title="Active Members" value="--" color="indigo" />
        <StatCard title="Today's Check-ins" value="--" color="green" />
        <StatCard title="Monthly Revenue" value="--" color="blue" />
        <StatCard title="Outstanding Payments" value="--" color="red" />
      </div>
      <div className="mt-8 bg-white rounded-lg shadow p-6">
        <h2 className="text-lg font-semibold text-gray-700 mb-4">Welcome</h2>
        <p className="text-gray-600">
          Welcome back, {user?.firstName}! Use the sidebar to navigate
          to Members, Contracts, and more.
        </p>
      </div>
    </div>
  )
}

function StatCard({ title, value, color }: { title: string; value: string; color: string }) {
  const colorClasses: Record<string, string> = {
    indigo: 'bg-indigo-50 text-indigo-700',
    green: 'bg-green-50 text-green-700',
    blue: 'bg-blue-50 text-blue-700',
    red: 'bg-red-50 text-red-700',
  }

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <p className="text-sm text-gray-500">{title}</p>
      <p className={`text-3xl font-bold mt-2 ${colorClasses[color]?.split(' ')[1] || ''}`}>
        {value}
      </p>
    </div>
  )
}
