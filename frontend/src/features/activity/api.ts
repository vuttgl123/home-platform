import { registry, unwrap, workspaceHeaders, type CursorPage } from '@/api'

import type { ActivityEntry, EntityType } from './types'

/** Hoạt động gần đây của không gian, mới nhất trước; `entityType` null là mọi loại. */
export async function listActivity(ws: string, entityType: EntityType | null, cursor?: string): Promise<CursorPage<ActivityEntry>> {
  const call = registry.GET('/activity', {
    params: { header: workspaceHeaders(ws), query: { entityType: entityType ?? undefined, cursor } },
  })
  return (await unwrap(call)) as CursorPage<ActivityEntry>
}
