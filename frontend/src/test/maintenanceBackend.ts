import { http, HttpResponse } from 'msw'

import type { ItemDetail } from '@/features/items'
import type { MaintenanceEntry } from '@/features/maintenance'

import { problem } from './fixtures'
import { server } from './server'

export type MaintenanceWrite = { method: string; path: string; body: unknown }

/** Một dòng nhật ký bảo trì đủ trường, cho test. */
export function maintenanceEntry(overrides: Partial<MaintenanceEntry> = {}): MaintenanceEntry {
  return {
    id: '7000',
    itemId: '900',
    kind: 'REPAIR',
    doneOn: '2026-11-20',
    work: 'Thay ron cửa',
    doneBy: null,
    note: null,
    createdAt: '2026-11-20T03:00:00Z',
    createdBy: '42',
    updatedAt: '2026-11-20T03:00:00Z',
    updatedBy: '42',
    version: 1,
    ...overrides,
  }
}

/**
 * Backend giả của nhật ký bảo trì (spec Đợt 8, L1, L2), có trạng thái: mới làm trước, `version` được kiểm
 * tra, xoá rồi khôi phục được. Với `markFixed`, đổi món trong `items` (Map của `mockItemBackend`) về
 * `IN_USE` và tăng `version` của nó; `itemVersion` lệch thì 409 và không ghi gì.
 */
export function mockMaintenanceBackend(options: { entries?: Partial<MaintenanceEntry>[]; items?: Map<string, ItemDetail> }) {
  const entries = new Map<string, MaintenanceEntry>()
  for (const seed of options.entries ?? []) {
    const full = maintenanceEntry(seed)
    entries.set(full.id, full)
  }
  const deleted = new Map<string, MaintenanceEntry>()
  const writes: MaintenanceWrite[] = []
  let nextId = 7100
  const stale = (what: string) => HttpResponse.json({ ...problem(409, `${what} was changed by someone else`), code: 'VERSION_CONFLICT' }, { status: 409 })

  server.use(
    http.get('/api/registry/maintenance', ({ request }) => {
      const itemId = new URL(request.url).searchParams.get('itemId')
      const rows = [...entries.values()]
        .filter((entry) => entry.itemId === itemId)
        .sort((a, b) => b.doneOn.localeCompare(a.doneOn) || b.id.localeCompare(a.id))
      return HttpResponse.json({ items: rows, nextCursor: null })
    }),
    http.post('/api/registry/maintenance', async ({ request }) => {
      const body = (await request.json()) as Record<string, unknown>
      writes.push({ method: 'POST', path: '/maintenance', body })
      if (body.markFixed) {
        const item = options.items?.get(String(body.itemId))
        if (!item || item.version !== body.itemVersion) return stale(`Item ${String(body.itemId)}`)
        options.items!.set(item.id, { ...item, status: 'IN_USE', version: item.version + 1 })
      }
      const created = maintenanceEntry({
        id: String(nextId++),
        itemId: String(body.itemId),
        kind: body.kind as MaintenanceEntry['kind'],
        doneOn: String(body.doneOn),
        work: String(body.work),
        doneBy: (body.doneBy as string | undefined) ?? null,
        note: (body.note as string | undefined) ?? null,
      })
      entries.set(created.id, created)
      return HttpResponse.json(created, { status: 201 })
    }),
    http.put('/api/registry/maintenance/:id', async ({ request, params }) => {
      const body = (await request.json()) as Record<string, unknown>
      writes.push({ method: 'PUT', path: `/maintenance/${String(params.id)}`, body })
      const entry = entries.get(String(params.id))
      if (!entry) return HttpResponse.json(problem(404, 'Not found'), { status: 404 })
      if (entry.version !== body.version) return stale(`Maintenance ${entry.id}`)
      const updated: MaintenanceEntry = {
        ...entry,
        kind: body.kind as MaintenanceEntry['kind'],
        doneOn: String(body.doneOn),
        work: String(body.work),
        doneBy: (body.doneBy as string | undefined) ?? null,
        note: (body.note as string | undefined) ?? null,
        version: entry.version + 1,
      }
      entries.set(updated.id, updated)
      return HttpResponse.json(updated)
    }),
    http.delete('/api/registry/maintenance/:id', ({ request, params }) => {
      const version = Number(new URL(request.url).searchParams.get('version'))
      writes.push({ method: 'DELETE', path: `/maintenance/${String(params.id)}`, body: { version } })
      const entry = entries.get(String(params.id))
      if (!entry) return HttpResponse.json(problem(404, 'Not found'), { status: 404 })
      if (entry.version !== version) return stale(`Maintenance ${entry.id}`)
      entries.delete(entry.id)
      deleted.set(entry.id, { ...entry, version: entry.version + 1 })
      return new HttpResponse(null, { status: 204 })
    }),
    http.post('/api/registry/maintenance/:id/restore', async ({ request, params }) => {
      const body = (await request.json()) as { version: number }
      writes.push({ method: 'POST', path: `/maintenance/${String(params.id)}/restore`, body })
      const gone = deleted.get(String(params.id))
      if (!gone) return HttpResponse.json(problem(404, 'Not found'), { status: 404 })
      if (gone.version !== body.version) return stale(`Maintenance ${gone.id}`)
      const back = { ...gone, version: gone.version + 1 }
      deleted.delete(back.id)
      entries.set(back.id, back)
      return HttpResponse.json(back)
    }),
  )

  return { entries, writes }
}
