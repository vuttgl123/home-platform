import { http, HttpResponse } from 'msw'

import type { PlaceType } from '@/features/catalog'
import type { User } from '@/features/users'
import type { Workspace } from '@/features/workspaces'

import { page, placeType, user, workspace } from './fixtures'
import { server } from './server'

/**
 * Đăng ký những request mà khung trang luôn gọi: người dùng hiện tại, không gian của tôi, loại
 * địa điểm. Test nào cần câu trả lời khác thì gọi `server.use(...)` sau hàm này.
 */
export function mockShell(options: { me?: User; workspaces?: Workspace[]; placeTypes?: PlaceType[] } = {}) {
  server.use(
    http.get('/api/identity/users/me', () => HttpResponse.json(options.me ?? user())),
    http.get('/api/identity/workspaces', () => HttpResponse.json(page(options.workspaces ?? [workspace()]))),
    http.get('/api/registry/place-types', () => HttpResponse.json(options.placeTypes ?? [placeType()])),
  )
}

/** Ghi lại mọi request đi ra trong test, dạng `"POST /api/identity/users"`. */
export function recordRequests(): string[] {
  const requests: string[] = []
  server.events.on('request:start', ({ request }) => {
    requests.push(`${request.method} ${new URL(request.url).pathname}`)
  })
  return requests
}
