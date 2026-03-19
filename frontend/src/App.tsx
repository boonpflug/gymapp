import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './store/authStore'
import Layout from './components/Layout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import MembersPage from './pages/MembersPage'
import ContractsPage from './pages/ContractsPage'
import CheckInPage from './pages/CheckInPage'
import ClassesPage from './pages/ClassesPage'
import TrainingPage from './pages/TrainingPage'
import CommunicationPage from './pages/CommunicationPage'
import SalesPage from './pages/SalesPage'
import StaffPage from './pages/StaffPage'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAuthStore((s) => !!s.accessToken)
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return <>{children}</>
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="members" element={<MembersPage />} />
        <Route path="contracts" element={<ContractsPage />} />
        <Route path="checkin" element={<CheckInPage />} />
        <Route path="classes" element={<ClassesPage />} />
        <Route path="training" element={<TrainingPage />} />
        <Route path="communication" element={<CommunicationPage />} />
        <Route path="sales" element={<SalesPage />} />
        <Route path="staff" element={<StaffPage />} />
      </Route>
    </Routes>
  )
}
