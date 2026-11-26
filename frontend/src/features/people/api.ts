import { registry, unwrap, workspaceHeaders, type CursorPage } from '@/api'
import { toApiValues } from '@/features/catalog'
import { searchText } from '@/lib/searchText'

import type {
  CapacityCheck,
  CapacityRange,
  Occupancy,
  OccupancyBody,
  OccupancyTimelineData,
  OccupancyStatus,
  OccupancyUpdateBody,
  PersonBody,
  PersonDetail,
  PersonFilter,
  PersonSearchField,
  PersonSummary,
  PersonUpdateBody,
} from './types'

// Người

/** Tham số của `GET /people` cho một bộ lọc; cũng là khoá cache. */
export function personListQuery(filter: PersonFilter) {
  const attr = toApiValues(filter.conditions)
  return {
    q: searchText(filter.q),
    living: filter.living === 'yes' || filter.rootId ? true : filter.living === 'no' ? false : undefined,
    rootId: filter.living === 'no' ? undefined : (filter.rootId ?? undefined),
    attr: attr.length > 0 ? attr : undefined,
  }
}

/** Người chưa xoá theo bộ lọc, theo tên (spec Đợt 11, S4). */
export async function listPeople(
  ws: string,
  query: ReturnType<typeof personListQuery>,
  cursor?: string,
): Promise<CursorPage<PersonSummary>> {
  const call = registry.GET('/people', { params: { header: workspaceHeaders(ws), query: { ...query, cursor } } })
  return (await unwrap(call)) as CursorPage<PersonSummary>
}

/** Tìm theo tên bắt đầu bằng, đúng số điện thoại, hoặc đúng số giấy tờ. */
export async function searchPeople(
  ws: string,
  by: PersonSearchField,
  term: string,
  cursor?: string,
): Promise<CursorPage<PersonSummary>> {
  const call = registry.GET('/people/search', {
    params: { header: workspaceHeaders(ws), query: { [by]: term, cursor } },
  })
  return (await unwrap(call)) as CursorPage<PersonSummary>
}

/** @throws ApiError 404 nếu không có hoặc đã xoá */
export async function getPerson(ws: string, id: string): Promise<PersonDetail> {
  const call = registry.GET('/people/{id}', { params: { header: workspaceHeaders(ws), path: { id } } })
  return (await unwrap(call)) as PersonDetail
}

/** @throws ApiError 409 nếu đã có người mang cùng loại và số giấy tờ */
export async function createPerson(ws: string, body: PersonBody): Promise<PersonDetail> {
  const call = registry.POST('/people', { params: { header: workspaceHeaders(ws) }, body })
  return (await unwrap(call)) as PersonDetail
}

/** Ghi đè mọi trường sửa được; trường không gửi là bị xoá. */
export async function updatePerson(ws: string, id: string, body: PersonUpdateBody): Promise<PersonDetail> {
  const call = registry.PUT('/people/{id}', { params: { header: workspaceHeaders(ws), path: { id } }, body })
  return (await unwrap(call)) as PersonDetail
}

/** @throws ApiError 409 nếu người đó còn đang ở hoặc sắp đến ở đâu đó */
export async function deletePerson(ws: string, id: string, version: number): Promise<void> {
  await unwrap(
    registry.DELETE('/people/{id}', { params: { header: workspaceHeaders(ws), path: { id }, query: { version } } }),
  )
}

// Cư trú

/** Mọi lần ở (mọi trạng thái, tối đa 200, mới nhất trước) của một địa điểm hay một người. */
export async function getOccupancyTimeline(
  ws: string,
  owner: { placeId: string } | { personId: string },
): Promise<OccupancyTimelineData> {
  const call = registry.GET('/occupancies/timeline', { params: { header: workspaceHeaders(ws), query: owner } })
  return (await unwrap(call)) as OccupancyTimelineData
}

/** Sức chứa của một địa điểm và số người nhiều nhất đã có cùng một ngày trong khoảng ngày. */
export async function checkCapacity(ws: string, placeId: string, range: CapacityRange): Promise<CapacityCheck> {
  const call = registry.GET('/places/{id}/capacity-check', {
    params: { header: workspaceHeaders(ws), path: { id: placeId }, query: range },
  })
  return (await unwrap(call)) as CapacityCheck
}

/** Cư trú tại đúng một địa điểm, theo trạng thái. */
export async function listOccupanciesAtPlace(
  ws: string,
  placeId: string,
  status: OccupancyStatus,
  cursor?: string,
): Promise<CursorPage<Occupancy>> {
  const call = registry.GET('/occupancies', {
    params: { header: workspaceHeaders(ws), query: { placeId, status, cursor } },
  })
  return (await unwrap(call)) as CursorPage<Occupancy>
}

/** Người đang ở trong cả cây con của một địa điểm (ví dụ cả toà nhà). */
export async function listOccupanciesInSubtree(ws: string, placeId: string, cursor?: string): Promise<CursorPage<Occupancy>> {
  const call = registry.GET('/occupancies', {
    params: { header: workspaceHeaders(ws), query: { subtreeOf: placeId, cursor } },
  })
  return (await unwrap(call)) as CursorPage<Occupancy>
}

/** Mọi nơi một người đã và đang ở, mới nhất trước. */
export async function listOccupanciesOfPerson(ws: string, personId: string, cursor?: string): Promise<CursorPage<Occupancy>> {
  const call = registry.GET('/occupancies', {
    params: { header: workspaceHeaders(ws), query: { personId, cursor } },
  })
  return (await unwrap(call)) as CursorPage<Occupancy>
}

/** @throws ApiError 409 nếu người đó đã có một dòng giao thời gian ở cùng chỗ */
export async function createOccupancy(ws: string, body: OccupancyBody): Promise<Occupancy> {
  const call = registry.POST('/occupancies', { params: { header: workspaceHeaders(ws) }, body })
  return (await unwrap(call)) as Occupancy
}

/** Sửa quan hệ, ngày, ghi chú của một dòng nhập nhầm. */
export async function updateOccupancy(ws: string, id: string, body: OccupancyUpdateBody): Promise<Occupancy> {
  const call = registry.PUT('/occupancies/{id}', { params: { header: workspaceHeaders(ws), path: { id } }, body })
  return (await unwrap(call)) as Occupancy
}

/** Chuyển đi: đặt ngày kết thúc. Dòng không bị xoá. */
export async function endOccupancy(ws: string, id: string, version: number, endDate: string): Promise<Occupancy> {
  const call = registry.POST('/occupancies/{id}/end', {
    params: { header: workspaceHeaders(ws), path: { id } },
    body: { version, endDate },
  })
  return (await unwrap(call)) as Occupancy
}

/** Xoá một dòng nhập nhầm. */
export async function deleteOccupancy(ws: string, id: string, version: number): Promise<void> {
  await unwrap(
    registry.DELETE('/occupancies/{id}', { params: { header: workspaceHeaders(ws), path: { id }, query: { version } } }),
  )
}

/** Bỏ xoá một người; `version` là version của dòng đã xoá. */
export async function restorePerson(ws: string, id: string, version: number): Promise<PersonDetail> {
  const call = registry.POST('/people/{id}/restore', { params: { header: workspaceHeaders(ws), path: { id } }, body: { version } })
  return (await unwrap(call)) as PersonDetail
}

/** Tìm nhanh cho ô tìm chung: theo tên, và theo số điện thoại khi `term` chỉ gồm chữ số (từ 3 số); tối đa 5 mỗi kiểu. */
export async function quickFindPeople(ws: string, term: string): Promise<PersonSummary[]> {
  const find = async (by: 'name' | 'phone') =>
    ((await unwrap(registry.GET('/people/search', { params: { header: workspaceHeaders(ws), query: { [by]: term, limit: 5 } } }))) as CursorPage<PersonSummary>).items
  const lists = await Promise.all(/^\d{3,}$/.test(term) ? [find('name'), find('phone')] : [find('name')])
  const seen = new Set<string>()
  return lists.flat().filter((person) => !seen.has(person.id) && seen.add(person.id))
}
