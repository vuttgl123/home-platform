import { registry, unwrap, workspaceHeaders } from '@/api'

import type { FieldDef, FieldTarget, PlaceType } from './types'

/**
 * Tạo danh mục mặc định (loại địa điểm, nhóm đồ, mục tuỳ biến) cho một không gian. Gọi lại
 * không hại gì: không gian đã có danh mục thì backend không làm gì.
 */
export async function setupWorkspace(ws: string): Promise<void> {
  await unwrap(registry.POST('/setup', { params: { header: workspaceHeaders(ws) } }))
}

/** Mọi loại địa điểm của không gian, kể cả loại đã lưu trữ, theo thứ tự hiển thị. */
export async function listPlaceTypes(ws: string): Promise<PlaceType[]> {
  return (await unwrap(registry.GET('/place-types', { params: { header: workspaceHeaders(ws) } }))) as PlaceType[]
}

/** Mục tuỳ biến áp dụng cho một loại: mục chung của đích và mục riêng của loại đó, mọi trạng thái. */
export async function listFieldDefs(ws: string, target: FieldTarget, typeId: string): Promise<FieldDef[]> {
  const call = registry.GET('/field-defs', { params: { header: workspaceHeaders(ws), query: { target, typeId } } })
  return (await unwrap(call)) as FieldDef[]
}
