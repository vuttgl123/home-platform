import { registry, unwrap, workspaceHeaders, type RegistrySchemas } from '@/api'

import type { CodeBackfillResult, CodeRule, CodeStatus, CodeTarget, FieldDef, FieldTarget, ItemCategory, PlaceType } from './types'

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

/**
 * Mục tuỳ biến của một đích, mọi trạng thái. Có `typeId`: mục chung và mục riêng của loại (nhóm)
 * đó; không có: mọi mục của đích.
 */
export async function listFieldDefs(ws: string, target: FieldTarget, typeId?: string): Promise<FieldDef[]> {
  const call = registry.GET('/field-defs', { params: { header: workspaceHeaders(ws), query: { target, typeId } } })
  return (await unwrap(call)) as FieldDef[]
}

/** Mọi nhóm đồ của không gian, kể cả nhóm đã lưu trữ. */
export async function listItemCategories(ws: string): Promise<ItemCategory[]> {
  const call = registry.GET('/item-categories', { params: { header: workspaceHeaders(ws) } })
  return (await unwrap(call)) as ItemCategory[]
}

type S = RegistrySchemas

const headers = (ws: string) => workspaceHeaders(ws)

/** Một lệnh ghi danh mục; mọi lệnh trả bản ghi sau khi ghi (xoá thì không trả gì). */
type Call = Promise<{ data?: unknown; error?: unknown; response: Response }>
const run = async (call: Call) => unwrap(call)

export const placeTypeApi = {
  create: (ws: string, body: S['PlaceTypeCreateRequest']) => run(registry.POST('/place-types', { params: { header: headers(ws) }, body })),
  update: (ws: string, id: string, body: S['PlaceTypeUpdateRequest']) =>
    run(registry.PUT('/place-types/{id}', { params: { header: headers(ws), path: { id } }, body })),
  archive: (ws: string, id: string) => run(registry.POST('/place-types/{id}/archive', { params: { header: headers(ws), path: { id } } })),
  unarchive: (ws: string, id: string) =>
    run(registry.POST('/place-types/{id}/unarchive', { params: { header: headers(ws), path: { id } } })),
  remove: (ws: string, id: string) => run(registry.DELETE('/place-types/{id}', { params: { header: headers(ws), path: { id } } })),
}

export const itemCategoryApi = {
  create: (ws: string, body: S['ItemCategoryRequest']) =>
    run(registry.POST('/item-categories', { params: { header: headers(ws) }, body })),
  update: (ws: string, id: string, body: S['ItemCategoryRequest']) =>
    run(registry.PUT('/item-categories/{id}', { params: { header: headers(ws), path: { id } }, body })),
  archive: (ws: string, id: string) =>
    run(registry.POST('/item-categories/{id}/archive', { params: { header: headers(ws), path: { id } } })),
  unarchive: (ws: string, id: string) =>
    run(registry.POST('/item-categories/{id}/unarchive', { params: { header: headers(ws), path: { id } } })),
  remove: (ws: string, id: string) =>
    run(registry.DELETE('/item-categories/{id}', { params: { header: headers(ws), path: { id } } })),
}

export const fieldDefApi = {
  create: (ws: string, body: S['FieldDefCreateRequest']) => run(registry.POST('/field-defs', { params: { header: headers(ws) }, body })),
  update: (ws: string, id: string, body: S['FieldDefUpdateRequest']) =>
    run(registry.PUT('/field-defs/{id}', { params: { header: headers(ws), path: { id } }, body })),
  archive: (ws: string, id: string) => run(registry.POST('/field-defs/{id}/archive', { params: { header: headers(ws), path: { id } } })),
  unarchive: (ws: string, id: string) =>
    run(registry.POST('/field-defs/{id}/unarchive', { params: { header: headers(ws), path: { id } } })),
}

/** Các dạng mã tự cấp của không gian: dạng chung của từng đích trước, rồi dạng riêng. */
export async function listCodeRules(ws: string): Promise<CodeRule[]> {
  return (await unwrap(registry.GET('/code-rules', { params: { header: headers(ws) } }))) as CodeRule[]
}

export const codeRuleApi = {
  put: (ws: string, target: CodeTarget, typeId: string, body: S['CodeRuleRequest']) =>
    run(registry.PUT('/code-rules/{target}/{typeId}', { params: { header: headers(ws), path: { target, typeId } }, body })),
  remove: (ws: string, target: CodeTarget, typeId: string) =>
    run(registry.DELETE('/code-rules/{target}/{typeId}', { params: { header: headers(ws), path: { target, typeId } } })),
}

/** Số địa điểm và món đồ chưa có mã. */
export async function getCodeStatus(ws: string): Promise<CodeStatus> {
  return (await unwrap(registry.GET('/codes/status', { params: { header: headers(ws) } }))) as CodeStatus
}

/** Cấp mã cho một lô bản ghi chưa có mã (tối đa 100), cũ trước. */
export async function backfillCodes(ws: string): Promise<CodeBackfillResult> {
  return (await unwrap(registry.POST('/codes/backfill', { params: { header: headers(ws) } }))) as CodeBackfillResult
}
