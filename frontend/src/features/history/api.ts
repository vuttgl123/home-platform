import { registry, unwrap, workspaceHeaders, type CursorPage } from '@/api'

import type { EntityKind, Version, VersionSummary } from './types'

type Call = Promise<{ data?: unknown; error?: unknown; response: Response }>

/** Các version của một bản ghi, mới nhất trước. */
export async function listVersions(ws: string, kind: EntityKind, id: string, cursor?: string): Promise<CursorPage<VersionSummary>> {
  const params = { header: workspaceHeaders(ws), path: { id }, query: { cursor } }
  const call: Call =
    kind === 'places'
      ? registry.GET('/places/{id}/versions', { params })
      : kind === 'people'
        ? registry.GET('/people/{id}/versions', { params })
        : registry.GET('/items/{id}/versions', { params })
  return (await unwrap(call)) as CursorPage<VersionSummary>
}

/** Một version kèm nội dung của bản ghi lúc đó. */
export async function getVersion<Snapshot>(ws: string, kind: EntityKind, id: string, versionNo: number): Promise<Version<Snapshot>> {
  const params = { header: workspaceHeaders(ws), path: { id, versionNo } }
  const call: Call =
    kind === 'places'
      ? registry.GET('/places/{id}/versions/{versionNo}', { params })
      : kind === 'people'
        ? registry.GET('/people/{id}/versions/{versionNo}', { params })
        : registry.GET('/items/{id}/versions/{versionNo}', { params })
  return (await unwrap(call)) as Version<Snapshot>
}

/**
 * Khôi phục nội dung của một version cũ: một lần sửa mang nội dung đó, ghi thành version mới.
 * @param currentVersion `version` hiện tại của bản ghi (không phải số của version cũ)
 * @returns bản ghi sau khi khôi phục, đúng dạng chi tiết của loại đó
 */
export async function restoreVersion(
  ws: string,
  kind: EntityKind,
  id: string,
  versionNo: number,
  currentVersion: number,
  note?: string,
): Promise<unknown> {
  const options = { params: { header: workspaceHeaders(ws), path: { id, versionNo } }, body: { version: currentVersion, note } }
  const call: Call =
    kind === 'places'
      ? registry.POST('/places/{id}/versions/{versionNo}/restore', options)
      : kind === 'people'
        ? registry.POST('/people/{id}/versions/{versionNo}/restore', options)
        : registry.POST('/items/{id}/versions/{versionNo}/restore', options)
  return unwrap(call)
}
