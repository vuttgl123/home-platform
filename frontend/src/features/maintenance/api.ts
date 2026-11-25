import { registry, unwrap, workspaceHeaders, type CursorPage } from '@/api'

import type { MaintenanceCreateBody, MaintenanceEntry, MaintenanceUpdateBody } from './types'

/** Nhật ký của một món, mới làm trước. @throws ApiError 404 nếu món không có hay đã xoá */
export async function listMaintenance(ws: string, itemId: string, cursor?: string): Promise<CursorPage<MaintenanceEntry>> {
  const call = registry.GET('/maintenance', { params: { header: workspaceHeaders(ws), query: { itemId, cursor } } })
  return (await unwrap(call)) as CursorPage<MaintenanceEntry>
}

/** Ghi một dòng; `markFixed` cùng `itemVersion` thì món hỏng về Đang dùng. @throws ApiError 409 nếu `itemVersion` lệch */
export async function createMaintenance(ws: string, body: MaintenanceCreateBody): Promise<MaintenanceEntry> {
  const call = registry.POST('/maintenance', { params: { header: workspaceHeaders(ws) }, body })
  return (await unwrap(call)) as MaintenanceEntry
}

export async function updateMaintenance(ws: string, id: string, body: MaintenanceUpdateBody): Promise<MaintenanceEntry> {
  const call = registry.PUT('/maintenance/{id}', { params: { header: workspaceHeaders(ws), path: { id } }, body })
  return (await unwrap(call)) as MaintenanceEntry
}

export async function deleteMaintenance(ws: string, id: string, version: number): Promise<void> {
  await unwrap(registry.DELETE('/maintenance/{id}', { params: { header: workspaceHeaders(ws), path: { id }, query: { version } } }))
}

/** Bỏ xoá; `version` là version của dòng đã xoá. */
export async function restoreMaintenance(ws: string, id: string, version: number): Promise<MaintenanceEntry> {
  const call = registry.POST('/maintenance/{id}/restore', { params: { header: workspaceHeaders(ws), path: { id } }, body: { version } })
  return (await unwrap(call)) as MaintenanceEntry
}
