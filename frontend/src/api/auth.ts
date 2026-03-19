import { useMutation } from '@tanstack/react-query'
import api from './client'
import { useAuthStore } from '../store/authStore'
import type { ApiResponse, AuthResponse } from '../types'

interface LoginInput {
  email: string
  password: string
}

interface RegisterInput extends LoginInput {
  firstName: string
  lastName: string
  tenantId?: string
}

export function useLogin() {
  const setAuth = useAuthStore((s) => s.setAuth)
  return useMutation({
    mutationFn: async (input: LoginInput) => {
      const { data } = await api.post<ApiResponse<AuthResponse>>('/auth/login', input)
      return data.data
    },
    onSuccess: (data) => {
      setAuth(data.accessToken, data.refreshToken, data.user)
    },
  })
}

export function useRegister() {
  const setAuth = useAuthStore((s) => s.setAuth)
  return useMutation({
    mutationFn: async (input: RegisterInput) => {
      const { data } = await api.post<ApiResponse<AuthResponse>>('/auth/register', input)
      return data.data
    },
    onSuccess: (data) => {
      setAuth(data.accessToken, data.refreshToken, data.user)
    },
  })
}
