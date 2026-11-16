import { http, HttpResponse } from 'msw'

import type { ItemDetail, ItemSummary } from '@/features/items'

import { itemDetail, page, problem } from './fixtures'
import { server } from './server'

export type ItemWrite = { method: string; path: string; body: unknown }

/**
 * Backend giả của đồ đạc, có trạng thái, theo spec business rules 5.9: gỡ đặt `removedAt` và
 * trạng thái REMOVED, thay tạo món mới và trỏ món cũ tới nó, đưa lại bị chặn khi món thay còn đó,
 * món đã gỡ chỉ đọc (trừ đưa lại và xoá), `version` được kiểm tra.
 *
 * @param options.placesUnder các `id` địa điểm trong cây con của một địa điểm (cho `subtreeOf`)
 * @param options.placeNames tên địa điểm theo `id`
 * @param options.categoryNames tên nhóm theo `id`
 */
export function mockItemBackend(options: {
  items?: Partial<ItemDetail>[]
  today?: string
  placesUnder?: Record<string, string[]>
  placeNames?: Record<string, string>
  categoryNames?: Record<string, string>
}) {
  const today = options.today ?? '2026-11-16'
  const items = new Map<string, ItemDetail>()
  for (const seed of options.items ?? []) {
    const full = itemDetail(seed)
    items.set(full.id, full)
  }
  const writes: ItemWrite[] = []
  let nextId = 9500

  function withNames(item: ItemDetail): ItemDetail {
    return {
      ...item,
      placeName: options.placeNames?.[item.placeId] ?? item.placeName,
      categoryName: item.categoryId ? (options.categoryNames?.[item.categoryId] ?? item.categoryName) : null,
    }
  }

  function summary(item: ItemDetail): ItemSummary {
    const { id, placeId, placeName, categoryId, categoryName, code, name, quantity, unit, status, removedAt, version } =
      withNames(item)
    return { id, placeId, placeName, categoryId, categoryName, code, name, quantity, unit, status, removedAt, version }
  }

  const notFound = (id: string) => HttpResponse.json(problem(404, `Item ${id} not found`), { status: 404 })
  const conflict = (detail: string, code?: string) =>
    HttpResponse.json({ ...problem(409, detail), ...(code ? { code } : {}) }, { status: 409 })
  const stale = (id: string) => conflict(`Item ${id} was changed by someone else`, 'VERSION_CONFLICT')

  async function record(request: Request) {
    const body = request.method === 'DELETE' ? null : await request.json()
    writes.push({ method: request.method, path: new URL(request.url).pathname.replace('/api/registry', ''), body })
    return body as Record<string, unknown> | null
  }

  /** Món đồ mới từ thân `ItemRequest`. */
  function build(body: Record<string, unknown>, placeId: string): ItemDetail {
    return itemDetail({
      id: String(nextId++),
      placeId,
      categoryId: (body.categoryId as string | undefined) ?? null,
      code: (body.code as string | undefined) ?? null,
      name: body.name as string,
      quantity: (body.quantity as number | undefined) ?? 1,
      unit: (body.unit as string | undefined) ?? null,
      status: (body.status as ItemDetail['status'] | undefined) ?? 'IN_USE',
      placedAt: (body.placedAt as string | undefined) ?? null,
      description: (body.description as string | undefined) ?? null,
      attributes: (body.attributes as Record<string, unknown> | undefined) ?? null,
      version: 1,
    })
  }

  /** Kiểm tra chung của mọi lệnh ghi trên một món: có, đúng version, (tuỳ) chưa gỡ. */
  function check(id: string, version: unknown, mustBeInPlace = true) {
    const item = items.get(id)
    if (!item) return { error: notFound(id) }
    if (version !== item.version) return { error: stale(id) }
    if (mustBeInPlace && item.removedAt != null) return { error: conflict(`Item ${id} is removed`) }
    return { item }
  }

  function save(item: ItemDetail, changes: Partial<ItemDetail>): ItemDetail {
    const next = { ...item, ...changes, version: item.version + 1 }
    items.set(item.id, next)
    return withNames(next)
  }

  server.use(
    http.get('/api/registry/items', ({ request }) => {
      const query = new URL(request.url).searchParams
      const placeId = query.get('placeId')
      const subtreeOf = query.get('subtreeOf')
      let found = [...items.values()]
      if (placeId != null) {
        const removed = query.get('removed') === 'true'
        found = found
          .filter((item) => item.placeId === placeId && (item.removedAt != null) === removed)
          .sort((a, b) => (removed ? (b.removedAt ?? '').localeCompare(a.removedAt ?? '') : a.name.localeCompare(b.name)))
      } else if (subtreeOf != null) {
        const inside = new Set([subtreeOf, ...(options.placesUnder?.[subtreeOf] ?? [])])
        found = found.filter((item) => inside.has(item.placeId) && item.removedAt == null).sort((a, b) => a.name.localeCompare(b.name))
      }
      return HttpResponse.json(page(found.map(summary)))
    }),
    http.get('/api/registry/items/:id', ({ params }) => {
      const id = params.id as string
      return items.has(id) ? HttpResponse.json(withNames(items.get(id)!)) : notFound(id)
    }),
    http.post('/api/registry/items', async ({ request }) => {
      const body = (await record(request))!
      const created = build(body, body.placeId as string)
      items.set(created.id, created)
      return HttpResponse.json(withNames(created), { status: 201 })
    }),
    http.put('/api/registry/items/:id', async ({ request, params }) => {
      const body = (await record(request))!
      const result = check(params.id as string, body.version)
      if (result.error) return result.error
      const { version: _version, note: _note, ...fields } = body
      return HttpResponse.json(
        save(result.item, {
          categoryId: null,
          code: null,
          unit: null,
          placedAt: null,
          description: null,
          attributes: null,
          coverFileId: null,
          ...(fields as Partial<ItemDetail>),
        }),
      )
    }),
    http.post('/api/registry/items/:id/move', async ({ request, params }) => {
      const body = (await record(request))!
      const result = check(params.id as string, body.version)
      if (result.error) return result.error
      return HttpResponse.json(save(result.item, { placeId: body.placeId as string }))
    }),
    http.post('/api/registry/items/:id/remove', async ({ request, params }) => {
      const body = (await record(request))!
      const result = check(params.id as string, body.version)
      if (result.error) return result.error
      const removedAt = (body.removedAt as string | undefined) ?? today
      return HttpResponse.json(save(result.item, { removedAt, status: 'REMOVED' }))
    }),
    http.post('/api/registry/items/:id/replace', async ({ request, params }) => {
      const body = (await record(request))!
      const result = check(params.id as string, body.version)
      if (result.error) return result.error
      const replacementBody = body.replacement as Record<string, unknown>
      const replacement = build(replacementBody, (replacementBody.placeId as string | undefined) ?? result.item.placeId)
      items.set(replacement.id, replacement)
      const removed = save(result.item, {
        removedAt: (body.removedAt as string | undefined) ?? today,
        status: 'REMOVED',
        replacedByItemId: replacement.id,
      })
      return HttpResponse.json({ removed, replacement: withNames(replacement) })
    }),
    http.post('/api/registry/items/:id/put-back', async ({ request, params }) => {
      const id = params.id as string
      const body = (await record(request))!
      const result = check(id, body.version, false)
      if (result.error) return result.error
      if (result.item.removedAt == null) return conflict(`Item ${id} is not removed`)
      const by = result.item.replacedByItemId
      if (by != null && items.has(by)) return conflict(`Item ${id} was replaced by item ${by}`)
      return HttpResponse.json(save(result.item, { removedAt: null, status: 'IN_USE', replacedByItemId: null }))
    }),
    http.delete('/api/registry/items/:id', async ({ request, params }) => {
      const id = params.id as string
      await record(request)
      const result = check(id, Number(new URL(request.url).searchParams.get('version')), false)
      if (result.error) return result.error
      items.delete(id)
      return new HttpResponse(null, { status: 204 })
    }),
  )

  return {
    items,
    writes,
    editElsewhere(id: string, changes: Partial<ItemDetail> = {}) {
      const item = items.get(id)!
      items.set(id, { ...item, ...changes, version: item.version + 1 })
    },
  }
}
