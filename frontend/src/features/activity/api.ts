import { registry, unwrap, workspaceHeaders, type CursorPage } from '@/api'

import type { ActivityQuery } from './filters'
import type { ActivityEntry, EntityType, TrashEntry } from './types'

/**
 * Hoạt động gần đây của không gian, mới nhất trước.
 * @param query bộ lọc đã đổi sang tham số (`toQuery`); `{}` là mọi hoạt động
 * @param limit số dòng của trang; bỏ trống là cỡ trang mặc định của backend
 */
export async function listActivity(
  ws: string,
  query: ActivityQuery,
  cursor?: string,
  limit?: number,
): Promise<CursorPage<ActivityEntry>> {
  const call = registry.GET('/activity', {
    params: { header: workspaceHeaders(ws), query: { ...query, cursor, limit } },
  })
  return (await unwrap(call)) as CursorPage<ActivityEntry>
}

/** Những bản ghi đang bị xoá của một loại, mới xoá trước. */
export async function listTrash(ws: string, entityType: EntityType, cursor?: string): Promise<CursorPage<TrashEntry>> {
  const call = registry.GET('/trash', { params: { header: workspaceHeaders(ws), query: { entityType, cursor } } })
  return (await unwrap(call)) as CursorPage<TrashEntry>
}
