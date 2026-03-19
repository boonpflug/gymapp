import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { UserDto } from '../types'
import api from '../api/client'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: UserDto | null
  tenantId: string | null
  setAuth: (accessToken: string, refreshToken: string, user: UserDto) => void
  setTenantId: (tenantId: string) => void
  refresh: () => Promise<void>
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      tenantId: null,
      setAuth: (accessToken, refreshToken, user) =>
        set({ accessToken, refreshToken, user }),
      setTenantId: (tenantId) => set({ tenantId }),
      refresh: async () => {
        const { refreshToken } = get()
        if (!refreshToken) throw new Error('No refresh token')
        const { data } = await api.post('/auth/refresh', { refreshToken })
        set({
          accessToken: data.data.accessToken,
          refreshToken: data.data.refreshToken,
          user: data.data.user,
        })
      },
      logout: () =>
        set({ accessToken: null, refreshToken: null, user: null }),
    }),
    { name: 'gym-auth' },
  ),
)
