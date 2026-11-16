import { registry, unwrap, workspaceHeaders, type CursorPage, type RegistrySchemas } from '@/api'

import type { PlaceDetail, PlaceSummary } from './types'

// Cây tải mỗi lần 50 dòng: nhánh nghìn phòng không phải bấm "Tải thêm" quá nhiều, mà một trang
// vẫn nhẹ. Backend cho tối đa 100.
const TREE_PAGE_SIZE = 50

/** Các địa điểm cấp cao nhất, theo tên. */
export async function listTopLevel(ws: string, cursor?: string): Promise<CursorPage<PlaceSummary>> {
  const call = registry.GET('/places', {
    params: { header: workspaceHeaders(ws), query: { cursor, limit: TREE_PAGE_SIZE } },
  })
  return (await unwrap(call)) as CursorPage<PlaceSummary>
}

/** Con trực tiếp của một địa điểm, theo tên. */
export async function listChildren(ws: string, parentId: string, cursor?: string): Promise<CursorPage<PlaceSummary>> {
  const call = registry.GET('/places/{id}/children', {
    params: { header: workspaceHeaders(ws), path: { id: parentId }, query: { cursor, limit: TREE_PAGE_SIZE } },
  })
  return (await unwrap(call)) as CursorPage<PlaceSummary>
}

/** Các địa điểm có tên bắt đầu bằng `name`, ở mọi cấp. */
export async function searchPlaces(ws: string, name: string, cursor?: string): Promise<CursorPage<PlaceSummary>> {
  const call = registry.GET('/places/search', {
    params: { header: workspaceHeaders(ws), query: { name, cursor } },
  })
  return (await unwrap(call)) as CursorPage<PlaceSummary>
}

/** @throws ApiError 404 nếu địa điểm không có hoặc đã xoá */
export async function getPlace(ws: string, id: string): Promise<PlaceDetail> {
  const call = registry.GET('/places/{id}', { params: { header: workspaceHeaders(ws), path: { id } } })
  return (await unwrap(call)) as PlaceDetail
}

export type PlaceCreateBody = RegistrySchemas['PlaceCreateRequest']
export type PlaceUpdateBody = RegistrySchemas['PlaceUpdateRequest']

export async function createPlace(ws: string, body: PlaceCreateBody): Promise<PlaceDetail> {
  const call = registry.POST('/places', { params: { header: workspaceHeaders(ws) }, body })
  return (await unwrap(call)) as PlaceDetail
}

/** Ghi đè mọi trường sửa được; trường không gửi là bị xoá. `body.version` là version đã đọc. */
export async function updatePlace(ws: string, id: string, body: PlaceUpdateBody): Promise<PlaceDetail> {
  const call = registry.PUT('/places/{id}', { params: { header: workspaceHeaders(ws), path: { id } }, body })
  return (await unwrap(call)) as PlaceDetail
}
