import { http, HttpResponse } from 'msw'

import type { Occupancy, PersonDetail, PersonSummary } from '@/features/people'

import { occupancy as occupancyFixture, page, personDetail, problem } from './fixtures'
import { server } from './server'

/** Một request ghi mà backend giả đã nhận. */
export type PeopleWrite = { method: string; path: string; body: unknown }

/**
 * Backend giả của người và cư trú, có trạng thái, theo luật ở spec business rules 5.6–5.7: trùng
 * giấy tờ trả 409 kèm `id` người đã có, trùng thời gian ở cùng một chỗ trả 409, `version` được
 * kiểm tra, trạng thái cư trú tính từ ngày so với `today`.
 *
 * @param options.today ngày "hôm nay" của backend, `YYYY-MM-DD`
 * @param options.placesUnder các `id` địa điểm nằm trong cây con của một địa điểm (cho `subtreeOf`)
 * @param options.placeNames tên địa điểm theo `id`, cho cột `placeName`
 */
export function mockPeopleBackend(options: {
  people?: Partial<PersonDetail>[]
  occupancies?: Partial<Occupancy>[]
  today?: string
  placesUnder?: Record<string, string[]>
  placeNames?: Record<string, string>
}) {
  const today = options.today ?? '2026-11-16'
  // Người đã xoá, giữ lại để khôi phục; `version` đã tăng như sau một lần xoá thật.
  const trashed = new Map<string, PersonDetail>()
  const people = new Map<string, PersonDetail>()
  for (const seed of options.people ?? []) {
    const full = personDetail(seed)
    people.set(full.id, full)
  }
  const stays = new Map<string, Occupancy>()
  for (const seed of options.occupancies ?? []) {
    const full = occupancyFixture(seed)
    stays.set(full.id, full)
  }
  const writes: PeopleWrite[] = []
  let nextId = 8000

  const statusOf = (o: Occupancy): Occupancy['status'] =>
    o.startDate > today ? 'UPCOMING' : o.endDate != null && o.endDate < today ? 'PAST' : 'CURRENT'

  function row(o: Occupancy): Occupancy {
    return {
      ...o,
      status: statusOf(o),
      personFullName: people.get(o.personId)?.fullName ?? o.personFullName,
      placeName: options.placeNames?.[o.placeId] ?? o.placeName,
    }
  }

  function detail(id: string): PersonDetail {
    const person = people.get(id)!
    const current = [...stays.values()].filter((o) => o.personId === id).map(row).filter((o) => o.status === 'CURRENT')
    return { ...person, currentOccupancies: current }
  }

  function summary(p: PersonDetail): PersonSummary {
    const { id, fullName, phone, idType, idNumber, gender, version } = p
    return { id, fullName, phone, idType, idNumber, gender, avatarFileId: p.avatarFileId ?? null, version }
  }

  const notFound = (kind: string, id: string) =>
    HttpResponse.json(problem(404, `${kind} ${id} not found`), { status: 404 })
  const conflict = (detail: string, code?: string) =>
    HttpResponse.json({ ...problem(409, detail), ...(code ? { code } : {}) }, { status: 409 })
  const stale = (kind: string, id: string) => conflict(`${kind} ${id} was changed by someone else`, 'VERSION_CONFLICT')

  async function record(request: Request) {
    const body = request.method === 'DELETE' ? null : await request.json()
    writes.push({ method: request.method, path: new URL(request.url).pathname.replace('/api/registry', ''), body })
    return body as Record<string, unknown> | null
  }

  function documentOwner(body: Record<string, unknown>, except?: string) {
    if (!body.idType || !body.idNumber) return null
    return [...people.values()].find(
      (p) => p.id !== except && p.idType === body.idType && p.idNumber === body.idNumber,
    )
  }

  function overlaps(candidate: Occupancy) {
    const end = (o: Occupancy) => o.endDate ?? '9999-12-31'
    return [...stays.values()].some(
      (o) =>
        o.id !== candidate.id &&
        o.personId === candidate.personId &&
        o.placeId === candidate.placeId &&
        o.startDate <= end(candidate) &&
        candidate.startDate <= end(o),
    )
  }

  const sortedPeople = () => [...people.values()].sort((a, b) => a.fullName.localeCompare(b.fullName))

  server.use(
    http.get('/api/registry/people', ({ request }) => {
      // Như backend: `q` là "chứa" trong họ tên hay số điện thoại (đủ cho test); các bộ lọc khác không giả lập.
      const q = new URL(request.url).searchParams.get('q')?.toLowerCase()
      const found = sortedPeople().filter((p) => !q || p.fullName.toLowerCase().includes(q) || (p.phone ?? '').includes(q))
      return HttpResponse.json(page(found.map(summary)))
    }),
    http.get('/api/registry/people/search', ({ request }) => {
      const query = new URL(request.url).searchParams
      const name = query.get('name')
      const phone = query.get('phone')
      const idNumber = query.get('idNumber')
      const found = sortedPeople().filter((p) =>
        name != null ? p.fullName.startsWith(name) : phone != null ? p.phone === phone : p.idNumber === idNumber,
      )
      return HttpResponse.json(page(found.map(summary)))
    }),
    http.get('/api/registry/people/:id', ({ params }) => {
      const id = params.id as string
      return people.has(id) ? HttpResponse.json(detail(id)) : notFound('Person', id)
    }),
    http.post('/api/registry/people', async ({ request }) => {
      const body = (await record(request))!
      const owner = documentOwner(body)
      if (owner) return conflict(`Person ${owner.id} already has this document`)
      const id = String(nextId++)
      const { versionNote: _note, ...fields } = body
      people.set(id, personDetail({ ...(fields as Partial<PersonDetail>), id, version: 1 }))
      return HttpResponse.json(detail(id), { status: 201 })
    }),
    http.put('/api/registry/people/:id', async ({ request, params }) => {
      const id = params.id as string
      const body = (await record(request))!
      const person = people.get(id)
      if (!person) return notFound('Person', id)
      if (body.version !== person.version) return stale('Person', id)
      const owner = documentOwner(body, id)
      if (owner) return conflict(`Person ${owner.id} already has this document`)
      const { version: _version, versionNote: _note, ...fields } = body
      people.set(id, {
        ...personDetail({ id, createdAt: person.createdAt }),
        ...(fields as Partial<PersonDetail>),
        version: person.version + 1,
      })
      return HttpResponse.json(detail(id))
    }),
    http.delete('/api/registry/people/:id', async ({ request, params }) => {
      const id = params.id as string
      await record(request)
      const person = people.get(id)
      if (!person) return notFound('Person', id)
      if (Number(new URL(request.url).searchParams.get('version')) !== person.version) return stale('Person', id)
      const active = [...stays.values()].filter((o) => o.personId === id && statusOf(o) !== 'PAST').length
      if (active > 0) return conflict(`Person ${id} still has ${active} current or upcoming occupancies`)
      people.delete(id)
      trashed.set(id, { ...person, version: person.version + 1 })
      return new HttpResponse(null, { status: 204 })
    }),
    http.post('/api/registry/people/:id/restore', async ({ request, params }) => {
      const id = params.id as string
      const body = (await record(request))! as { version: number }
      const gone = trashed.get(id)
      if (!gone) return notFound('Person', id)
      if (body.version !== gone.version) return stale('Person', id)
      trashed.delete(id)
      people.set(id, { ...gone, version: gone.version + 1 })
      return HttpResponse.json(detail(id))
    }),
    // Mọi lần ở của một địa điểm hay một người, mới nhất trước, như GET /occupancies/timeline.
    http.get('/api/registry/occupancies/timeline', ({ request }) => {
      const query = new URL(request.url).searchParams
      const placeId = query.get('placeId')
      const personId = query.get('personId')
      const rows = [...stays.values()]
        .map(row)
        .filter((o) => (placeId != null ? o.placeId === placeId : o.personId === personId))
        .sort((a, b) => b.startDate.localeCompare(a.startDate))
      return HttpResponse.json({ rows, truncated: false })
    }),
    http.get('/api/registry/occupancies', ({ request }) => {
      const query = new URL(request.url).searchParams
      const placeId = query.get('placeId')
      const subtreeOf = query.get('subtreeOf')
      const personId = query.get('personId')
      let found = [...stays.values()].map(row)
      if (placeId != null) {
        const status = query.get('status') ?? 'CURRENT'
        found = found.filter((o) => o.placeId === placeId && o.status === status)
      } else if (subtreeOf != null) {
        const inside = new Set([subtreeOf, ...(options.placesUnder?.[subtreeOf] ?? [])])
        found = found.filter((o) => inside.has(o.placeId) && o.status === 'CURRENT')
      } else {
        found = found.filter((o) => o.personId === personId).sort((a, b) => b.startDate.localeCompare(a.startDate))
      }
      return HttpResponse.json(page(found))
    }),
    http.post('/api/registry/occupancies', async ({ request }) => {
      const body = (await record(request))!
      const personId = body.personId as string
      if (!people.has(personId)) return notFound('Person', personId)
      const created = occupancyFixture({
        id: String(nextId++),
        placeId: body.placeId as string,
        personId,
        relation: body.relation as Occupancy['relation'],
        startDate: body.startDate as string,
        endDate: (body.endDate as string | undefined) ?? null,
        note: (body.note as string | undefined) ?? null,
        version: 1,
      })
      if (overlaps(created)) return conflict(`Person ${personId} already has an overlapping occupancy at this place`)
      stays.set(created.id, created)
      return HttpResponse.json(row(created), { status: 201 })
    }),
    http.put('/api/registry/occupancies/:id', async ({ request, params }) => {
      const id = params.id as string
      const body = (await record(request))!
      const stay = stays.get(id)
      if (!stay) return notFound('Occupancy', id)
      if (body.version !== stay.version) return stale('Occupancy', id)
      const next: Occupancy = {
        ...stay,
        relation: body.relation as Occupancy['relation'],
        startDate: body.startDate as string,
        endDate: (body.endDate as string | undefined) ?? null,
        note: (body.note as string | undefined) ?? null,
        version: stay.version + 1,
      }
      if (overlaps(next)) return conflict(`Person ${stay.personId} already has an overlapping occupancy at this place`)
      stays.set(id, next)
      return HttpResponse.json(row(next))
    }),
    http.post('/api/registry/occupancies/:id/end', async ({ request, params }) => {
      const id = params.id as string
      const body = (await record(request))!
      const stay = stays.get(id)
      if (!stay) return notFound('Occupancy', id)
      if (body.version !== stay.version) return stale('Occupancy', id)
      if (stay.endDate != null && stay.endDate < today) return conflict(`Occupancy ${id} has already ended`)
      const next = { ...stay, endDate: (body.endDate as string | undefined) ?? today, version: stay.version + 1 }
      stays.set(id, next)
      return HttpResponse.json(row(next))
    }),
    http.delete('/api/registry/occupancies/:id', async ({ request, params }) => {
      const id = params.id as string
      await record(request)
      const stay = stays.get(id)
      if (!stay) return notFound('Occupancy', id)
      if (Number(new URL(request.url).searchParams.get('version')) !== stay.version) return stale('Occupancy', id)
      stays.delete(id)
      return new HttpResponse(null, { status: 204 })
    }),
  )

  return {
    people,
    stays,
    writes,
    /** Giả lập người khác vừa sửa hồ sơ `id`. */
    editPersonElsewhere(id: string, changes: Partial<PersonDetail> = {}) {
      const person = people.get(id)!
      people.set(id, { ...person, ...changes, version: person.version + 1 })
    },
  }
}
