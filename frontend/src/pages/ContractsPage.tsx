import { useQuery } from '@tanstack/react-query'
import api from '../api/client'
import type { ApiResponse, MembershipTierDto } from '../types'

export default function ContractsPage() {
  const { data: tiersData, isLoading } = useQuery({
    queryKey: ['membership-tiers'],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<MembershipTierDto[]>>('/membership-tiers')
      return data
    },
  })

  const tiers = tiersData?.data ?? []

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Contracts & Membership Tiers</h1>

      <h2 className="text-lg font-semibold text-gray-700 mb-4">Membership Tiers</h2>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {isLoading ? (
          <p className="text-gray-500">Loading tiers...</p>
        ) : tiers.length === 0 ? (
          <p className="text-gray-500">No membership tiers configured</p>
        ) : (
          tiers.map((tier) => (
            <div key={tier.id} className="bg-white rounded-lg shadow p-6">
              <h3 className="text-lg font-semibold text-gray-800">{tier.name}</h3>
              <p className="text-3xl font-bold text-indigo-600 mt-2">
                {tier.monthlyPrice.toFixed(2)}
              </p>
              <p className="text-sm text-gray-500">per month</p>
              <div className="mt-4 text-sm text-gray-600 space-y-1">
                <p>Billing: {tier.billingCycle}</p>
                <p>Min term: {tier.minimumTermMonths} months</p>
                <p>Notice: {tier.noticePeriodDays} days</p>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  )
}
