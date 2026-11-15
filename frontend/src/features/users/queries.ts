import { useMutation, useQuery } from '@tanstack/react-query'

import type { ApiError } from '@/api'

import { createUser, getMe, getUser } from './api'
import type { User } from './types'

export function useCreateUser() {
  return useMutation<User, ApiError, { email: string; displayName: string }>({ mutationFn: createUser })
}

/** Kiểm tra một id người dùng có tồn tại không; biến đầu vào là id cần kiểm tra. */
export function useCheckUser() {
  return useMutation<User, ApiError, string>({ mutationFn: getUser })
}

export function useCurrentUser() {
  return useQuery<User, ApiError>({ queryKey: ['users', 'me'], queryFn: getMe })
}
