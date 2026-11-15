import { identity, unwrap, userHeaders } from '@/api'

import type { User } from './types'

/** Đăng ký người dùng mới; chưa cần là ai cả. */
export async function createUser(body: { email: string; displayName: string }): Promise<User> {
  return (await unwrap(identity.POST('/users', { body }))) as User
}

/**
 * Người dùng mang `userId`, hỏi bằng chính id đó. Dùng để kiểm tra một id trước khi vào phiên.
 * @throws ApiError 404 nếu không có người dùng đó
 */
export async function getUser(userId: string): Promise<User> {
  return (await unwrap(identity.GET('/users/me', { params: { header: { 'X-User-Id': userId } } }))) as User
}

/** Người dùng của phiên hiện tại. */
export async function getMe(): Promise<User> {
  return getUser(userHeaders()['X-User-Id'])
}
