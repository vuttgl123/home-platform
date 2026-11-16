import { http, HttpResponse } from 'msw'

import type { PlaceDetail, PlaceSearchResult, PlaceSummary } from '@/features/places'

import { page, placeDetail, problem } from './fixtures'
import { server } from './server'

type Address = NonNullable<PlaceDetail['address']>

/** Một request ghi mà backend giả đã nhận. */
export type WriteRequest = { method: string; path: string; body: unknown }

/**
 * Backend giả của địa điểm, có trạng thái: ghi xong thì lần đọc sau thấy dữ liệu mới, `version`
 * được kiểm tra như backend thật. Dùng cho test cần thấy cây và trang cập nhật sau khi ghi.
 *
 * @param initial các địa điểm ban đầu; chỉ cần `id`, `parentId`, `name` và những gì test xem tới
 */
export function mockPlaceBackend(initial: Partial<PlaceDetail>[]) {
  const places = new Map<string, PlaceDetail>()
  for (const seed of initial) {
    const full = placeDetail(seed)
    places.set(full.id, full)
  }
  const writes: WriteRequest[] = []
  let nextId = 9000

  function chain(id: string): PlaceDetail[] {
    const lineage: PlaceDetail[] = []
    let current = places.get(id)
    while (current?.parentId != null) {
      current = places.get(current.parentId)
      if (current) lineage.unshift(current)
    }
    return lineage
  }

  function detail(id: string): PlaceDetail {
    const own = places.get(id)!
    const ancestors = chain(id)
    const withAddress = [...ancestors, own].reverse().find((candidate) => candidate.address != null)
    return {
      ...own,
      depth: ancestors.length,
      path: `/${[...ancestors, own].map((p) => p.id).join('/')}/`,
      ancestors: ancestors.map((p) => ({ id: p.id, name: p.name })),
      effectiveAddress: withAddress ? { fromPlaceId: withAddress.id, address: withAddress.address! } : null,
    }
  }

  function summary(p: PlaceDetail): PlaceSummary {
    const { id, parentId, placeTypeId, code, name, status, coverFileId, version } = p
    const hasChildren = [...places.values()].some((other) => other.parentId === id)
    return { id, parentId, placeTypeId, code, name, status, depth: chain(id).length, coverFileId, version, hasChildren }
  }

  function searchResult(p: PlaceDetail): PlaceSearchResult {
    return { ...summary(p), ancestors: chain(p.id).map((a) => ({ id: a.id, name: a.name })) }
  }

  function childrenOf(parentId: string | null) {
    return [...places.values()]
      .filter((p) => (p.parentId ?? null) === parentId)
      .sort((a, b) => a.name.localeCompare(b.name))
      .map(summary)
  }

  const notFound = (id: string) => HttpResponse.json(problem(404, `Place ${id} not found`), { status: 404 })
  const stale = (id: string) =>
    HttpResponse.json(problem(409, `Place ${id} was changed by someone else`), { status: 409 })

  /** Ghi lại request, kiểm tra `version`; trả response lỗi nếu có, không thì địa điểm. */
  async function write(request: Request, id: string, version: number | null) {
    const body = request.method === 'DELETE' ? null : await request.json()
    writes.push({ method: request.method, path: new URL(request.url).pathname.replace('/api/registry', ''), body })
    const place = places.get(id)
    if (!place) return { error: notFound(id) }
    const expected = version ?? (body as { version: number }).version
    if (expected !== place.version) return { error: stale(id) }
    return { place, body: body as Record<string, unknown> }
  }

  function save(place: PlaceDetail, changes: Partial<PlaceDetail>) {
    const next = { ...place, ...changes, version: place.version + 1 }
    places.set(place.id, next)
    return HttpResponse.json(detail(place.id))
  }

  server.use(
    http.get('/api/registry/places', () => HttpResponse.json(page(childrenOf(null)))),
    http.get('/api/registry/places/search', ({ request }) => {
      const name = new URL(request.url).searchParams.get('name') ?? ''
      return HttpResponse.json(page([...places.values()].filter((p) => p.name.startsWith(name)).map(searchResult)))
    }),
    http.get('/api/registry/places/:id/children', ({ params }) =>
      HttpResponse.json(page(childrenOf(params.id as string))),
    ),
    http.get('/api/registry/places/:id', ({ params }) => {
      const id = params.id as string
      return places.has(id) ? HttpResponse.json(detail(id)) : notFound(id)
    }),
    http.post('/api/registry/places', async ({ request }) => {
      const body = (await request.json()) as Record<string, unknown>
      writes.push({ method: 'POST', path: '/places', body })
      const id = String(nextId++)
      places.set(
        id,
        placeDetail({
          id,
          parentId: (body.parentId as string | undefined) ?? null,
          placeTypeId: body.placeTypeId as string,
          name: body.name as string,
          code: (body.code as string | undefined) ?? null,
          description: (body.description as string | undefined) ?? null,
          status: (body.status as PlaceDetail['status'] | undefined) ?? 'ACTIVE',
          attributes: (body.attributes as Record<string, unknown> | undefined) ?? null,
        }),
      )
      return HttpResponse.json(detail(id), { status: 201 })
    }),
    http.put('/api/registry/places/:id', async ({ request, params }) => {
      const result = await write(request, params.id as string, null)
      if (result.error) return result.error
      const { version: _version, note: _note, ...changes } = result.body
      return save(result.place, {
        code: null,
        description: null,
        attributes: null,
        coverFileId: null,
        ...(changes as Partial<PlaceDetail>),
      })
    }),
    http.post('/api/registry/places/:id/move', async ({ request, params }) => {
      const result = await write(request, params.id as string, null)
      if (result.error) return result.error
      return save(result.place, { parentId: (result.body.parentId as string | null) ?? null })
    }),
    http.put('/api/registry/places/:id/address', async ({ request, params }) => {
      const result = await write(request, params.id as string, null)
      if (result.error) return result.error
      return save(result.place, { address: { geohash: null, ...(result.body.address as object) } as Address })
    }),
    http.delete('/api/registry/places/:id/address', async ({ request, params }) => {
      const version = Number(new URL(request.url).searchParams.get('version'))
      const result = await write(request, params.id as string, version)
      if (result.error) return result.error
      return save(result.place, { address: null })
    }),
    http.delete('/api/registry/places/:id', async ({ request, params }) => {
      const id = params.id as string
      const version = Number(new URL(request.url).searchParams.get('version'))
      const result = await write(request, id, version)
      if (result.error) return result.error
      places.delete(id)
      return new HttpResponse(null, { status: 204 })
    }),
  )

  return {
    places,
    writes,
    /** Giả lập người khác vừa sửa địa điểm `id`: đổi vài trường và tăng `version`. */
    editElsewhere(id: string, changes: Partial<PlaceDetail> = {}) {
      const place = places.get(id)!
      places.set(id, { ...place, ...changes, version: place.version + 1 })
    },
  }
}
