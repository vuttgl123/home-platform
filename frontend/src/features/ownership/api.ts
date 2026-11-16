import { registry, unwrap, workspaceHeaders, type CursorPage } from '@/api'

import type { Ownership, OwnershipBody, OwnershipStatus, OwnershipTransferBody, OwnershipUpdateBody } from './types'

/** Sở hữu tại đúng một địa điểm, theo trạng thái. */
export async function listOwnershipsAtPlace(
  ws: string,
  placeId: string,
  status: OwnershipStatus,
  cursor?: string,
): Promise<CursorPage<Ownership>> {
  const call = registry.GET('/ownerships', { params: { header: workspaceHeaders(ws), query: { placeId, status, cursor } } })
  return (await unwrap(call)) as CursorPage<Ownership>
}

/** Sở hữu hiện tại trong cả cây con của một địa điểm (các căn của một toà). */
export async function listOwnershipsInSubtree(ws: string, placeId: string, cursor?: string): Promise<CursorPage<Ownership>> {
  const call = registry.GET('/ownerships', { params: { header: workspaceHeaders(ws), query: { subtreeOf: placeId, cursor } } })
  return (await unwrap(call)) as CursorPage<Ownership>
}

/** Mọi chỗ một người đã và đang sở hữu, mới nhất trước. */
export async function listOwnershipsOfPerson(ws: string, personId: string, cursor?: string): Promise<CursorPage<Ownership>> {
  const call = registry.GET('/ownerships', { params: { header: workspaceHeaders(ws), query: { personId, cursor } } })
  return (await unwrap(call)) as CursorPage<Ownership>
}

/** @throws ApiError 409 nếu giao thời gian với dòng khác của cùng người, hoặc tổng tỷ lệ vượt 100% */
export async function createOwnership(ws: string, body: OwnershipBody): Promise<Ownership> {
  return (await unwrap(registry.POST('/ownerships', { params: { header: workspaceHeaders(ws) }, body }))) as Ownership
}

/** Sửa tỷ lệ, ngày, giấy tờ, ghi chú của một dòng nhập nhầm. */
export async function updateOwnership(ws: string, id: string, body: OwnershipUpdateBody): Promise<Ownership> {
  const call = registry.PUT('/ownerships/{id}', { params: { header: workspaceHeaders(ws), path: { id } }, body })
  return (await unwrap(call)) as Ownership
}

/** Thôi sở hữu: đặt ngày cuối cùng còn sở hữu. */
export async function endOwnership(ws: string, id: string, version: number, endDate: string): Promise<Ownership> {
  const call = registry.POST('/ownerships/{id}/end', {
    params: { header: workspaceHeaders(ws), path: { id } },
    body: { version, endDate },
  })
  return (await unwrap(call)) as Ownership
}

/** Chuyển nhượng cả dòng cho người khác. */
export async function transferOwnership(
  ws: string,
  id: string,
  body: OwnershipTransferBody,
): Promise<{ ended: Ownership; started: Ownership }> {
  const call = registry.POST('/ownerships/{id}/transfer', { params: { header: workspaceHeaders(ws), path: { id } }, body })
  return (await unwrap(call)) as { ended: Ownership; started: Ownership }
}

/** Xoá một dòng nhập nhầm. */
export async function deleteOwnership(ws: string, id: string, version: number): Promise<void> {
  await unwrap(
    registry.DELETE('/ownerships/{id}', { params: { header: workspaceHeaders(ws), path: { id }, query: { version } } }),
  )
}
