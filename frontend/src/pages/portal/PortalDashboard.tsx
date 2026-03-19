import { useQuery } from '@tanstack/react-query'
import api from '../../api/client'
import type { ApiResponse, MemberDto, ContractDto, OccupancyDto } from '../../types'

export default function PortalDashboard() {
  const { data: profileRes } = useQuery({
    queryKey: ['portal-profile'],
    queryFn: () => api.get<ApiResponse<MemberDto>>('/portal/profile').then(r => r.data),
  })
  const profile = profileRes?.data

  const { data: contractsRes } = useQuery({
    queryKey: ['portal-contracts'],
    queryFn: () => api.get<ApiResponse<ContractDto[]>>('/portal/contracts').then(r => r.data),
  })
  const contracts = contractsRes?.data ?? []

  const { data: occupancyRes } = useQuery({
    queryKey: ['portal-occupancy'],
    queryFn: () => api.get<ApiResponse<OccupancyDto>>('/portal/occupancy').then(r => r.data),
    refetchInterval: 30000,
  })
  const occupancy = occupancyRes?.data

  const activeContracts = contracts.filter(c => c.status === 'ACTIVE' || c.status === 'PAUSED')

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">
        Welcome back{profile ? `, ${profile.firstName}` : ''}!
      </h1>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        {/* Membership status */}
        <div className="bg-white rounded-lg shadow p-5">
          <h3 className="text-sm text-gray-500 mb-2">Membership Status</h3>
          {profile && (
            <span className={`text-lg font-semibold ${
              profile.status === 'ACTIVE' ? 'text-green-600' :
              profile.status === 'INACTIVE' ? 'text-red-600' : 'text-gray-600'
            }`}>
              {profile.status}
            </span>
          )}
          <p className="text-sm text-gray-500 mt-1">Member #{profile?.memberNumber}</p>
        </div>

        {/* Active contracts */}
        <div className="bg-white rounded-lg shadow p-5">
          <h3 className="text-sm text-gray-500 mb-2">Active Contracts</h3>
          <p className="text-lg font-semibold">{activeContracts.length}</p>
          {activeContracts[0] && (
            <p className="text-sm text-gray-500 mt-1">
              Next billing: {activeContracts[0].nextBillingDate}
            </p>
          )}
        </div>

        {/* Live Occupancy */}
        <div className="bg-white rounded-lg shadow p-5">
          <h3 className="text-sm text-gray-500 mb-2">Studio Occupancy</h3>
          {occupancy ? (
            <>
              <p className={`text-lg font-semibold ${occupancy.atCapacity ? 'text-red-600' : 'text-green-600'}`}>
                {occupancy.currentCount}
                {occupancy.maxCapacity ? ` / ${occupancy.maxCapacity}` : ''}
              </p>
              <p className="text-sm text-gray-500 mt-1">
                {occupancy.atCapacity ? 'At capacity' : 'Open'}
              </p>
            </>
          ) : (
            <p className="text-gray-400">Loading...</p>
          )}
        </div>
      </div>

      {/* Contracts overview */}
      <div className="bg-white rounded-lg shadow p-5">
        <h2 className="text-lg font-semibold mb-4">Your Contracts</h2>
        {contracts.length === 0 ? (
          <p className="text-gray-500">No contracts found.</p>
        ) : (
          <div className="space-y-3">
            {contracts.map(c => (
              <div key={c.id} className="flex items-center justify-between border-b pb-3">
                <div>
                  <p className="font-medium">{c.membershipTierName}</p>
                  <p className="text-sm text-gray-500">
                    {c.startDate} &mdash; {c.endDate ?? 'Ongoing'}
                  </p>
                </div>
                <div className="text-right">
                  <span className={`text-xs px-2 py-1 rounded ${
                    c.status === 'ACTIVE' ? 'bg-green-100 text-green-700' :
                    c.status === 'PAUSED' ? 'bg-yellow-100 text-yellow-700' :
                    c.status === 'PENDING_CANCELLATION' ? 'bg-orange-100 text-orange-700' :
                    c.status === 'CANCELLED' ? 'bg-red-100 text-red-700' :
                    'bg-gray-100 text-gray-600'
                  }`}>
                    {c.status?.replace('_', ' ')}
                  </span>
                  <p className="text-sm font-semibold mt-1">
                    &euro;{c.monthlyAmount}/mo
                  </p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
