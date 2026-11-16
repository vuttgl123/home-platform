import { registry, unwrap, workspaceHeaders, type CursorPage, type RegistrySchemas } from '@/api'

import type { PlaceDetail, PlaceSearchResult, PlaceSummary } from './types'

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

/** Mọi hậu duệ chưa xoá của một địa điểm (không gồm nó), cha luôn đứng trước con. */
export async function listSubtree(ws: string, placeId: string, cursor?: string): Promise<CursorPage<PlaceSummary>> {
  const call = registry.GET('/places/{id}/subtree', {
    params: { header: workspaceHeaders(ws), path: { id: placeId }, query: { cursor, limit: TREE_PAGE_SIZE } },
  })
  return (await unwrap(call)) as CursorPage<PlaceSummary>
}

/** Các địa điểm có tên bắt đầu bằng `name`, ở mọi cấp, kèm tổ tiên của từng cái. */
export async function searchPlaces(ws: string, name: string, cursor?: string): Promise<CursorPage<PlaceSearchResult>> {
  const call = registry.GET('/places/search', {
    params: { header: workspaceHeaders(ws), query: { name, cursor } },
  })
  return (await unwrap(call)) as CursorPage<PlaceSearchResult>
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

export type PlaceAddressBody = RegistrySchemas['PlaceAddressRequest']
export type AddressBody = RegistrySchemas['AddressRequest']

/**
 * Chuyển một địa điểm (cùng cả cây con của nó) sang cha mới.
 * @param parentId cha mới; `null` là lên cấp cao nhất
 */
export async function movePlace(ws: string, id: string, version: number, parentId: string | null): Promise<PlaceDetail> {
  // Kiểu sinh từ OpenAPI không cho `null`, nhưng backend hiểu `parentId: null` là "lên cấp cao
  // nhất"; gửi rõ ra cho dễ đọc khi xem request.
  const body = { version, parentId } as RegistrySchemas['PlaceMoveRequest']
  const call = registry.POST('/places/{id}/move', { params: { header: workspaceHeaders(ws), path: { id } }, body })
  return (await unwrap(call)) as PlaceDetail
}

/** Cấp mã mới theo dạng mã của loại hiện tại; mã cũ không còn tìm ra địa điểm này. */
export async function reissuePlaceCode(ws: string, id: string, version: number): Promise<PlaceDetail> {
  const call = registry.POST('/places/{id}/reissue-code', {
    params: { header: workspaceHeaders(ws), path: { id } },
    body: { version },
  })
  return (await unwrap(call)) as PlaceDetail
}

/** @throws ApiError 409 nếu còn chỗ con, người đang ở hay đồ đạc, hoặc `version` đã đổi */
export async function deletePlace(ws: string, id: string, version: number): Promise<void> {
  await unwrap(
    registry.DELETE('/places/{id}', { params: { header: workspaceHeaders(ws), path: { id }, query: { version } } }),
  )
}

/** Đặt (tạo hoặc thay) địa chỉ của chính địa điểm này. */
export async function setAddress(ws: string, id: string, body: PlaceAddressBody): Promise<PlaceDetail> {
  const call = registry.PUT('/places/{id}/address', { params: { header: workspaceHeaders(ws), path: { id } }, body })
  return (await unwrap(call)) as PlaceDetail
}

/** Gỡ địa chỉ riêng; địa điểm lại dùng địa chỉ của tổ tiên, nếu có. */
export async function removeAddress(ws: string, id: string, version: number): Promise<PlaceDetail> {
  const call = registry.DELETE('/places/{id}/address', {
    params: { header: workspaceHeaders(ws), path: { id }, query: { version } },
  })
  return (await unwrap(call)) as PlaceDetail
}

/** Bỏ xoá một địa điểm; `version` là version của dòng đã xoá. @throws ApiError 409 nếu cha còn đang bị xoá */
export async function restorePlace(ws: string, id: string, version: number): Promise<PlaceDetail> {
  const call = registry.POST('/places/{id}/restore', { params: { header: workspaceHeaders(ws), path: { id } }, body: { version } })
  return (await unwrap(call)) as PlaceDetail
}

/** Số kết quả mỗi loại của ô tìm chung. */
const QUICK_LIMIT = 5

/** Tìm nhanh: tên bắt đầu bằng và đúng mã, song song, mỗi loại tối đa 5 kết quả. */
export async function quickFindPlaces(ws: string, term: string): Promise<{ byCode: PlaceSearchResult[]; byName: PlaceSearchResult[] }> {
  const find = async (query: { name?: string; code?: string }) =>
    ((await unwrap(registry.GET('/places/search', { params: { header: workspaceHeaders(ws), query: { ...query, limit: QUICK_LIMIT } } }))) as CursorPage<PlaceSearchResult>).items
  const [byCode, byName] = await Promise.all([find({ code: term }), find({ name: term })])
  return { byCode, byName }
}
