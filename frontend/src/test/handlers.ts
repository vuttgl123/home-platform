import { http, HttpResponse } from 'msw'

import type { PlaceType } from '@/features/catalog'
import type { Overview } from '@/features/overview'
import type { PlaceSummary } from '@/features/places'
import type { User } from '@/features/users'
import type { Workspace } from '@/features/workspaces'

import { emptyOverview, emptyTodoSummary, page, placeType, user, workspace } from './fixtures'
import { server } from './server'

/**
 * Đăng ký những request mà khung trang luôn gọi: người dùng hiện tại, không gian của tôi, loại
 * địa điểm, cấp cao nhất của cây, số liệu của trang chủ (mặc định là không gian chưa có gì), và tóm
 * tắt việc cần làm cho thanh bên và chuông (mặc định không có việc gì). Test
 * nào cần câu trả lời khác thì gọi `server.use(...)` sau hàm này.
 */
export function mockShell(
  options: {
    me?: User
    workspaces?: Workspace[]
    placeTypes?: PlaceType[]
    places?: PlaceSummary[]
    overview?: Overview
  } = {},
) {
  server.use(
    http.get('/api/identity/users/me', () => HttpResponse.json(options.me ?? user())),
    http.get('/api/identity/workspaces', () => HttpResponse.json(page(options.workspaces ?? [workspace()]))),
    http.get('/api/registry/place-types', () => HttpResponse.json(options.placeTypes ?? [placeType()])),
    http.get('/api/registry/places', () => HttpResponse.json(page(options.places ?? []))),
    http.get('/api/registry/field-defs', () => HttpResponse.json([])),
    http.get('/api/registry/overview', () => HttpResponse.json(options.overview ?? emptyOverview())),
    http.get('/api/registry/todos/summary', () => HttpResponse.json(emptyTodoSummary())),
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
