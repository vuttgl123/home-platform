import { registry, unwrap, workspaceHeaders, type CursorPage } from '@/api'

import type { ItemBody, ItemDetail, ItemSummary, ItemUpdateBody } from './types'

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
