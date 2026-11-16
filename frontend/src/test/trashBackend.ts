import { http, HttpResponse } from 'msw'

import type { TrashEntry } from '@/features/activity'

import { page, problem } from './fixtures'
import { server } from './server'

export type TrashWrite = { method: string; path: string; body: unknown }

const PATH = { PLACE: 'places', PERSON: 'people', ITEM: 'items' } as const

/**
 * Backend giả của thùng rác: `GET /trash` theo loại, và ba lệnh khôi phục. Khôi phục đúng `version`
 * thì dòng rời thùng rác; `version` lệch trả 409 "đã có người sửa"; `blocked` trả 409 với lý do riêng.
 */
export function mockTrashBackend(options: { entries: Partial<TrashEntry>[]; blocked?: Record<string, string> }) {
  const entries = options.entries.map(
    (seed, index): TrashEntry => ({
      entityType: 'PLACE',
      id: String(500 + index),
      name: `Bản ghi ${index}`,
      code: null,
      context: null,
      deletedAt: '2026-11-16T03:00:00Z',
      deletedBy: '42',
      version: 2,
      ...seed,
    }),
  )
  const writes: TrashWrite[] = []

  server.use(
    http.get('/api/registry/trash', ({ request }) => {
      const type = new URL(request.url).searchParams.get('entityType')
      return HttpResponse.json(page(entries.filter((entry) => entry.entityType === type)))
    }),
    ...(['PLACE', 'PERSON', 'ITEM'] as const).map((type) =>
      http.post(`/api/registry/${PATH[type]}/:id/restore`, async ({ request, params }) => {
        const body = (await request.json()) as { version: number }
        writes.push({ method: 'POST', path: `/${PATH[type]}/${params.id}/restore`, body })
        const index = entries.findIndex((entry) => entry.entityType === type && entry.id === params.id)
        const entry = entries[index]
        if (!entry) return HttpResponse.json(problem(404, 'Not found'), { status: 404 })
        const reason = options.blocked?.[entry.id]
        if (reason) return HttpResponse.json(problem(409, reason), { status: 409 })
        if (body.version !== entry.version) {
          return HttpResponse.json({ ...problem(409, `${type} ${entry.id} was changed by someone else`), code: 'VERSION_CONFLICT' }, { status: 409 })
        }
        entries.splice(index, 1)
        return HttpResponse.json({ id: entry.id, name: entry.name, fullName: entry.name, version: entry.version + 1 })
      }),
    ),
  )
  return { entries, writes }
}
