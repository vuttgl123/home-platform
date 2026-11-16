import { http, HttpResponse } from 'msw'

import type { FieldDef, ItemCategory, PlaceType } from '@/features/catalog'

import { fieldDef, itemCategory, placeType, problem } from './fixtures'
import { server } from './server'

export type CatalogWrite = { method: string; path: string; body: unknown }

/**
 * Backend giả của danh mục, có trạng thái, theo spec business rules 5.2, 5.3, 5.8: mã loại trùng và
 * khoá mục tuỳ biến trùng trả 409, loại hay nhóm đang được dùng không xoá được (409, gợi ý lưu trữ).
 *
 * @param options.usedPlaceTypeIds loại đang có địa điểm dùng
 * @param options.usedCategoryIds nhóm đang có món đồ dùng
 */
export function mockCatalogBackend(options: {
  placeTypes?: Partial<PlaceType>[]
  itemCategories?: Partial<ItemCategory>[]
  fieldDefs?: Partial<FieldDef>[]
  usedPlaceTypeIds?: string[]
  usedCategoryIds?: string[]
}) {
  const types = (options.placeTypes ?? []).map((seed) => placeType(seed))
  const categories = (options.itemCategories ?? []).map((seed) => itemCategory(seed))
  const defs = (options.fieldDefs ?? []).map((seed) => fieldDef(seed))
  const writes: CatalogWrite[] = []
  let nextId = 7000

  async function record(request: Request) {
    const body = request.method === 'DELETE' ? null : await request.json().catch(() => null)
    writes.push({ method: request.method, path: new URL(request.url).pathname.replace('/api/registry', ''), body })
    return body as Record<string, unknown> | null
  }
  const conflict = (detail: string) => HttpResponse.json(problem(409, detail), { status: 409 })
  const sorted = <T extends { sortOrder: number; name?: string; label?: string }>(rows: T[]) =>
    [...rows].sort((a, b) => a.sortOrder - b.sortOrder || (a.name ?? a.label ?? '').localeCompare(b.name ?? b.label ?? ''))

  /** Lưu trữ, bỏ lưu trữ cho một danh sách. */
  function statusHandlers<T extends { id: string; status: 'ACTIVE' | 'ARCHIVED' }>(path: string, rows: T[]) {
    return ['archive', 'unarchive'].map((action) =>
      http.post(`/api/registry/${path}/:id/${action}`, async ({ request, params }) => {
        await record(request)
        const row = rows.find((candidate) => candidate.id === params.id)!
        row.status = action === 'archive' ? 'ARCHIVED' : 'ACTIVE'
        return HttpResponse.json(row)
      }),
    )
  }

  server.use(
    http.get('/api/registry/place-types', () => HttpResponse.json(sorted(types))),
    http.post('/api/registry/place-types', async ({ request }) => {
      const body = (await record(request))!
      if (types.some((type) => type.code === body.code)) return conflict(`Place type code ${body.code} already exists`)
      const created = placeType({ ...(body as Partial<PlaceType>), id: String(nextId++), sortOrder: (body.sortOrder as number) ?? 0 })
      types.push(created)
      return HttpResponse.json(created, { status: 201 })
    }),
    http.put('/api/registry/place-types/:id', async ({ request, params }) => {
      const body = (await record(request))!
      const type = types.find((candidate) => candidate.id === params.id)!
      Object.assign(type, body)
      return HttpResponse.json(type)
    }),
    http.delete('/api/registry/place-types/:id', async ({ request, params }) => {
      await record(request)
      const id = params.id as string
      if (options.usedPlaceTypeIds?.includes(id)) return conflict(`PlaceType ${id} is used by places; archive it instead`)
      types.splice(types.findIndex((type) => type.id === id), 1)
      return new HttpResponse(null, { status: 204 })
    }),
    ...statusHandlers('place-types', types),
    http.get('/api/registry/item-categories', () => HttpResponse.json(sorted(categories))),
    http.post('/api/registry/item-categories', async ({ request }) => {
      const body = (await record(request))!
      const created = itemCategory({ name: body.name as string, sortOrder: (body.sortOrder as number) ?? 0, id: String(nextId++) })
      categories.push(created)
      return HttpResponse.json(created, { status: 201 })
    }),
    http.put('/api/registry/item-categories/:id', async ({ request, params }) => {
      const body = (await record(request))!
      const category = categories.find((candidate) => candidate.id === params.id)!
      Object.assign(category, body)
      return HttpResponse.json(category)
    }),
    http.delete('/api/registry/item-categories/:id', async ({ request, params }) => {
      await record(request)
      const id = params.id as string
      if (options.usedCategoryIds?.includes(id)) return conflict(`ItemCategory ${id} is used by items; archive it instead`)
      categories.splice(categories.findIndex((category) => category.id === id), 1)
      return new HttpResponse(null, { status: 204 })
    }),
    ...statusHandlers('item-categories', categories),
    http.get('/api/registry/field-defs', ({ request }) => {
      const query = new URL(request.url).searchParams
      const target = query.get('target')
      const typeId = query.get('typeId')
      const found = defs.filter(
        (def) => def.target === target && (typeId == null || def.typeId === '0' || def.typeId === typeId),
      )
      return HttpResponse.json(sorted(found))
    }),
    http.post('/api/registry/field-defs', async ({ request }) => {
      const body = (await record(request))!
      const overlaps = defs.some(
        (def) =>
          def.target === body.target &&
          def.fieldKey === body.fieldKey &&
          (def.typeId === '0' || body.typeId === '0' || def.typeId === body.typeId),
      )
      if (overlaps) return conflict(`Field ${body.fieldKey} already exists for this target and type`)
      const created = fieldDef({ ...(body as Partial<FieldDef>), id: String(nextId++), status: 'ACTIVE' })
      defs.push(created)
      return HttpResponse.json(created, { status: 201 })
    }),
    http.put('/api/registry/field-defs/:id', async ({ request, params }) => {
      const body = (await record(request))!
      const def = defs.find((candidate) => candidate.id === params.id)!
      Object.assign(def, body)
      return HttpResponse.json(def)
    }),
    ...statusHandlers('field-defs', defs),
  )

  return { types, categories, defs, writes }
}
