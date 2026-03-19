import { Routes, Route, Navigate, useLocation } from 'react-router-dom'
import { useAuthStore } from './store/authStore'
import Layout from './components/Layout'
import PortalLayout from './components/PortalLayout'
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
import FacilitiesPage from './pages/FacilitiesPage'
import MarketingPage from './pages/MarketingPage'
import PortalDashboard from './pages/portal/PortalDashboard'
import PortalProfile from './pages/portal/PortalProfile'
import PortalContracts from './pages/portal/PortalContracts'
import PortalInvoices from './pages/portal/PortalInvoices'
import PortalClasses from './pages/portal/PortalClasses'
import PortalTraining from './pages/portal/PortalTraining'
import PortalCheckins from './pages/portal/PortalCheckins'
import PublicStudio from './pages/public/PublicStudio'

function StaffRoute({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAuthStore((s) => !!s.accessToken)
  const role = useAuthStore((s) => s.user?.role)
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (role === 'MEMBER') return <Navigate to="/portal" replace />
  return <>{children}</>
}

function MemberRoute({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAuthStore((s) => !!s.accessToken)
  const location = useLocation()
  if (!isAuthenticated) return <Navigate to="/login" state={{ from: location.pathname }} replace />
  return <>{children}</>
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      {/* Staff / Admin dashboard — members get redirected to /portal */}
      <Route
        path="/"
        element={
          <StaffRoute>
            <Layout />
          </StaffRoute>
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
        <Route path="facilities" element={<FacilitiesPage />} />
        <Route path="marketing" element={<MarketingPage />} />
      </Route>
      {/* Member self-service portal */}
      <Route
        path="/portal"
        element={
          <MemberRoute>
            <PortalLayout />
          </MemberRoute>
        }
      >
        <Route index element={<PortalDashboard />} />
        <Route path="profile" element={<PortalProfile />} />
        <Route path="contracts" element={<PortalContracts />} />
        <Route path="invoices" element={<PortalInvoices />} />
        <Route path="classes" element={<PortalClasses />} />
        <Route path="training" element={<PortalTraining />} />
        <Route path="checkins" element={<PortalCheckins />} />
      </Route>
      {/* Public studio portal (no login required) */}
      <Route path="/studio/:slug" element={<PublicStudio />} />
    </Routes>
  )
}
