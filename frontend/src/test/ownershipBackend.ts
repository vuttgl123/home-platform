import { http, HttpResponse } from 'msw'

import type { Ownership } from '@/features/ownership'

import { ownership, page, problem } from './fixtures'
import { server } from './server'

export type OwnershipWrite = { method: string; path: string; body: unknown }

/**
 * Backend giả của sở hữu, có trạng thái, theo spec sở hữu mục 5.1, 5.2: không giao thời gian cùng
 * người cùng chỗ, tổng tỷ lệ không vượt 100 (xét ngày bắt đầu của dòng mới), `version` được kiểm tra.
 *
 * @param options.placesUnder các `id` địa điểm trong cây con của một địa điểm (cho `subtreeOf`)
 * @param options.placeNames, options.personNames tên theo `id`
 */
export function mockOwnershipBackend(options: {
  ownerships?: Partial<Ownership>[]
  today?: string
  placesUnder?: Record<string, string[]>
  placeNames?: Record<string, string>
  personNames?: Record<string, string>
}) {
  const today = options.today ?? '2026-11-16'
  const rows = new Map<string, Ownership>()
  for (const seed of options.ownerships ?? []) {
    const full = ownership(seed)
    rows.set(full.id, full)
  }
  const writes: OwnershipWrite[] = []
  let nextId = 9700

  const statusOf = (row: Ownership): Ownership['status'] =>
    row.startDate > today ? 'UPCOMING' : row.endDate != null && row.endDate < today ? 'PAST' : 'CURRENT'
  const view = (row: Ownership): Ownership => ({
    ...row,
    status: statusOf(row),
    placeName: options.placeNames?.[row.placeId] ?? row.placeName,
    personFullName: options.personNames?.[row.personId] ?? row.personFullName,
  })
  const conflict = (detail: string, code?: string) =>
    HttpResponse.json({ ...problem(409, detail), ...(code ? { code } : {}) }, { status: 409 })
  const covers = (row: Ownership, day: string) => row.startDate <= day && (row.endDate == null || row.endDate >= day)
  const overlaps = (row: Ownership, start: string, end: string | null) =>
    row.startDate <= (end ?? '9999-12-31') && (row.endDate == null || row.endDate >= start)

  /** Lỗi theo luật giao nhau và tổng tỷ lệ, hoặc `null`. */
  function ruleError(candidate: Ownership, excludeId: string | null) {
    const others = [...rows.values()].filter((row) => row.id !== excludeId && row.placeId === candidate.placeId)
    if (others.some((row) => row.personId === candidate.personId && overlaps(row, candidate.startDate, candidate.endDate))) {
      return conflict(`Person ${candidate.personId} already has an overlapping ownership at this place`)
    }
    if (candidate.sharePercent != null) {
      const total = others
        .filter((row) => row.sharePercent != null && covers(row, candidate.startDate))
        .reduce((sum, row) => sum + row.sharePercent!, candidate.sharePercent)
      if (total > 100) return conflict(`Ownership shares of place ${candidate.placeId} would total ${total}% on ${candidate.startDate}`)
    }
    return null
  }

  async function record(request: Request) {
    const body = request.method === 'DELETE' ? null : await request.json()
    writes.push({ method: request.method, path: new URL(request.url).pathname.replace('/api/registry', ''), body })
    return body as Record<string, unknown> | null
  }

  function check(id: string, version: unknown) {
    const row = rows.get(id)
    if (!row) return { error: HttpResponse.json(problem(404, `Ownership ${id} not found`), { status: 404 }) }
    if (version !== row.version) return { error: conflict(`Ownership ${id} was changed by someone else`, 'VERSION_CONFLICT') }
    return { row }
  }

  const save = (row: Ownership, changes: Partial<Ownership>) => {
    const next = { ...row, ...changes, version: row.version + 1 }
    rows.set(row.id, next)
    return view(next)
  }

  server.use(
    http.get('/api/registry/ownerships', ({ request }) => {
      const query = new URL(request.url).searchParams
      const placeId = query.get('placeId')
      const subtreeOf = query.get('subtreeOf')
      const personId = query.get('personId')
      let found = [...rows.values()].map(view)
      if (placeId != null) {
        const status = query.get('status') ?? 'CURRENT'
        found = found.filter((row) => row.placeId === placeId && row.status === status)
      } else if (subtreeOf != null) {
        const inside = new Set([subtreeOf, ...(options.placesUnder?.[subtreeOf] ?? [])])
        found = found.filter((row) => inside.has(row.placeId) && row.status === 'CURRENT')
      } else if (personId != null) {
        found = found.filter((row) => row.personId === personId).sort((a, b) => b.startDate.localeCompare(a.startDate))
      }
      return HttpResponse.json(page(found))
    }),
    http.post('/api/registry/ownerships', async ({ request }) => {
      const body = (await record(request))!
      const created = ownership({
        id: String(nextId++),
        placeId: body.placeId as string,
        personId: body.personId as string,
        sharePercent: (body.sharePercent as number | undefined) ?? null,
        startDate: body.startDate as string,
        endDate: (body.endDate as string | undefined) ?? null,
        documentNo: (body.documentNo as string | undefined) ?? null,
        note: (body.note as string | undefined) ?? null,
        version: 1,
      })
      const error = ruleError(created, null)
      if (error) return error
      rows.set(created.id, created)
      return HttpResponse.json(view(created), { status: 201 })
    }),
    http.put('/api/registry/ownerships/:id', async ({ request, params }) => {
      const body = (await record(request))!
      const result = check(params.id as string, body.version)
      if (result.error) return result.error
      const changes = {
        sharePercent: (body.sharePercent as number | undefined) ?? null,
        startDate: body.startDate as string,
        endDate: (body.endDate as string | undefined) ?? null,
        documentNo: (body.documentNo as string | undefined) ?? null,
        note: (body.note as string | undefined) ?? null,
      }
      const error = ruleError({ ...result.row, ...changes }, result.row.id)
      if (error) return error
      return HttpResponse.json(save(result.row, changes))
    }),
    http.post('/api/registry/ownerships/:id/end', async ({ request, params }) => {
      const body = (await record(request))!
      const result = check(params.id as string, body.version)
      if (result.error) return result.error
      if (result.row.endDate != null) return conflict(`Ownership ${result.row.id} has already ended`)
      return HttpResponse.json(save(result.row, { endDate: (body.endDate as string | undefined) ?? today }))
    }),
    http.post('/api/registry/ownerships/:id/transfer', async ({ request, params }) => {
      const body = (await record(request))!
      const result = check(params.id as string, body.version)
      if (result.error) return result.error
      const day = (body.transferDate as string | undefined) ?? today
      const started = ownership({
        id: String(nextId++),
        placeId: result.row.placeId,
        personId: body.toPersonId as string,
        sharePercent: result.row.sharePercent,
        startDate: day,
        endDate: null,
        documentNo: (body.documentNo as string | undefined) ?? null,
        note: (body.note as string | undefined) ?? null,
        version: 1,
      })
      const error = ruleError(started, result.row.id)
      if (error) return error
      const previous = new Date(`${day}T00:00:00Z`)
      previous.setUTCDate(previous.getUTCDate() - 1)
      const ended = save(result.row, { endDate: previous.toISOString().slice(0, 10) })
      rows.set(started.id, started)
      return HttpResponse.json({ ended, started: view(started) })
    }),
    http.delete('/api/registry/ownerships/:id', async ({ request, params }) => {
      await record(request)
      const id = params.id as string
      const result = check(id, Number(new URL(request.url).searchParams.get('version')))
      if (result.error) return result.error
      rows.delete(id)
      return new HttpResponse(null, { status: 204 })
    }),
  )

  return { rows, writes }
}
