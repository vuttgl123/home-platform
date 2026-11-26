import { registry, unwrap, workspaceHeaders, type CursorPage, type RegistrySchemas } from '@/api'
import { toApiValues, type AttributeCondition } from '@/features/catalog'
import { searchText } from '@/lib/searchText'

import type { ItemBody, ItemDetail, ItemStatus, ItemSummary, ItemUpdateBody } from './types'

/** Đồ ở đúng một địa điểm: đang có (theo tên) hoặc đã gỡ (mới gỡ trước). */
export async function listItemsAtPlace(
  ws: string,
  placeId: string,
  removed: boolean,
  cursor?: string,
): Promise<CursorPage<ItemSummary>> {
  const call = registry.GET('/items', { params: { header: workspaceHeaders(ws), query: { placeId, removed, cursor } } })
  return (await unwrap(call)) as CursorPage<ItemSummary>
}

/** Đồ đang có trong cả cây con của một địa điểm (ví dụ cả toà nhà). */
export async function listItemsInSubtree(ws: string, placeId: string, cursor?: string): Promise<CursorPage<ItemSummary>> {
  const call = registry.GET('/items', { params: { header: workspaceHeaders(ws), query: { subtreeOf: placeId, cursor } } })
  return (await unwrap(call)) as CursorPage<ItemSummary>
}

/** @throws ApiError 404 nếu không có hoặc đã xoá */
export async function getItem(ws: string, id: string): Promise<ItemDetail> {
  const call = registry.GET('/items/{id}', { params: { header: workspaceHeaders(ws), path: { id } } })
  return (await unwrap(call)) as ItemDetail
}

export async function createItem(ws: string, body: ItemBody): Promise<ItemDetail> {
  const call = registry.POST('/items', { params: { header: workspaceHeaders(ws) }, body })
  return (await unwrap(call)) as ItemDetail
}

/** Ghi đè mọi trường sửa được; trường không gửi là bị xoá. */
export async function updateItem(ws: string, id: string, body: ItemUpdateBody): Promise<ItemDetail> {
  const call = registry.PUT('/items/{id}', { params: { header: workspaceHeaders(ws), path: { id } }, body })
  return (await unwrap(call)) as ItemDetail
}

/** Chuyển sang địa điểm khác; vị trí cũ còn trong lịch sử. */
export async function moveItem(ws: string, id: string, version: number, placeId: string): Promise<ItemDetail> {
  const call = registry.POST('/items/{id}/move', {
    params: { header: workspaceHeaders(ws), path: { id } },
    body: { version, placeId },
  })
  return (await unwrap(call)) as ItemDetail
}

/** Bộ lọc của trang Đồ đạc (spec Đợt 11, S5); `null`, `''`, `[]` là không lọc. */
export type ItemFilter = {
  status: ItemStatus | null
  categoryId: string | null
  q: string
  rootId: string | null
  conditions: AttributeCondition[]
}

/** Tham số của `GET /items` (cả không gian) cho một bộ lọc; cũng là khoá cache. */
export function itemListQuery(filter: ItemFilter) {
  const attr = toApiValues(filter.conditions)
  return {
    status: filter.status ?? undefined,
    categoryId: filter.categoryId ?? undefined,
    q: searchText(filter.q),
    rootId: filter.rootId ?? undefined,
    attr: attr.length > 0 ? attr : undefined,
  }
}

/** Đồ của cả không gian, theo tên: chưa gỡ, hoặc đúng `status`; lọc theo nhóm, chữ, toà, mục tuỳ biến. */
export async function listAllItems(
  ws: string,
  query: ReturnType<typeof itemListQuery>,
  cursor?: string,
): Promise<CursorPage<ItemSummary>> {
  const call = registry.GET('/items', { params: { header: workspaceHeaders(ws), query: { ...query, cursor } } })
  return (await unwrap(call)) as CursorPage<ItemSummary>
}

/** Đồ chưa gỡ của riêng một người, theo tên. */
export async function listItemsOwnedBy(ws: string, personId: string, cursor?: string): Promise<CursorPage<ItemSummary>> {
  const call = registry.GET('/items', { params: { header: workspaceHeaders(ws), query: { ownerId: personId, cursor } } })
  return (await unwrap(call)) as CursorPage<ItemSummary>
}

/** Đổi chủ; `ownerPersonId` `null` là trả món về cho địa điểm. */
export async function changeItemOwner(ws: string, id: string, version: number, ownerPersonId: string | null): Promise<ItemDetail> {
  // Kiểu sinh từ OpenAPI không cho `null`, nhưng backend hiểu `null` là "của địa điểm"; gửi rõ ra.
  const body = { version, ownerPersonId } as RegistrySchemas['ItemOwnerRequest']
  const call = registry.POST('/items/{id}/owner', { params: { header: workspaceHeaders(ws), path: { id } }, body })
  return (await unwrap(call)) as ItemDetail
}

/** Cấp mã mới theo dạng mã của nhóm hiện tại; mã cũ không còn tìm ra món này. */
export async function reissueItemCode(ws: string, id: string, version: number): Promise<ItemDetail> {
  const call = registry.POST('/items/{id}/reissue-code', {
    params: { header: workspaceHeaders(ws), path: { id } },
    body: { version },
  })
  return (await unwrap(call)) as ItemDetail
}

/** Gỡ: món vẫn hiện trong danh sách "đã gỡ" của địa điểm. */
export async function removeItem(ws: string, id: string, version: number, removedAt: string): Promise<ItemDetail> {
  const call = registry.POST('/items/{id}/remove', {
    params: { header: workspaceHeaders(ws), path: { id } },
    body: { version, removedAt },
  })
  return (await unwrap(call)) as ItemDetail
}

/** Gỡ món này và tạo món thay nó, trong một bước. */
export async function replaceItem(
  ws: string,
  id: string,
  version: number,
  removedAt: string,
  replacement: ItemBody,
): Promise<{ removed: ItemDetail; replacement: ItemDetail }> {
  const call = registry.POST('/items/{id}/replace', {
    params: { header: workspaceHeaders(ws), path: { id } },
    body: { version, removedAt, replacement },
  })
  return (await unwrap(call)) as { removed: ItemDetail; replacement: ItemDetail }
}

/** Đưa lại một món đã gỡ. @throws ApiError 409 nếu món thay nó còn đó */
export async function putBackItem(ws: string, id: string, version: number): Promise<ItemDetail> {
  const call = registry.POST('/items/{id}/put-back', {
    params: { header: workspaceHeaders(ws), path: { id } },
    body: { version },
  })
  return (await unwrap(call)) as ItemDetail
}

/** Xoá một món nhập nhầm. */
export async function deleteItem(ws: string, id: string, version: number): Promise<void> {
  await unwrap(
    registry.DELETE('/items/{id}', { params: { header: workspaceHeaders(ws), path: { id }, query: { version } } }),
  )
}

/** Bỏ xoá một món; `version` là version của dòng đã xoá. @throws ApiError 409 nếu địa điểm của nó đang bị xoá */
export async function restoreItem(ws: string, id: string, version: number): Promise<ItemDetail> {
  const call = registry.POST('/items/{id}/restore', { params: { header: workspaceHeaders(ws), path: { id } }, body: { version } })
  return (await unwrap(call)) as ItemDetail
}

/** Tìm nhanh cho ô tìm chung: tên bắt đầu bằng (kể cả đồ đã gỡ) và đúng mã, song song, tối đa 5 mỗi kiểu. */
export async function quickFindItems(ws: string, term: string): Promise<{ byCode: ItemSummary[]; byName: ItemSummary[] }> {
  const find = async (query: { name?: string; code?: string }) =>
    ((await unwrap(registry.GET('/items/search', { params: { header: workspaceHeaders(ws), query: { ...query, limit: 5 } } }))) as CursorPage<ItemSummary>).items
  const [byCode, byName] = await Promise.all([find({ code: term }), find({ name: term })])
  return { byCode, byName }
}
