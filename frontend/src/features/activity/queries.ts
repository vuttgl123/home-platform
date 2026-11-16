import { useCursorList, type CursorList } from '@/api'

import { listActivity } from './api'
import type { ActivityEntry, EntityType } from './types'

export const activityKeys = {
  all: (ws: string) => ['activity', ws] as const,
  list: (ws: string, entityType: EntityType | null) => ['activity', ws, entityType ?? 'all'] as const,
}

export function useActivity(ws: string, entityType: EntityType | null): CursorList<ActivityEntry & { id: string }> {
  return useCursorList({
    queryKey: activityKeys.list(ws, entityType),
    fetchPage: async (cursor) => {
      const page = await listActivity(ws, entityType, cursor)
      // Một dòng là một version của một bản ghi: loại, id và số version cùng nhau là duy nhất.
      return {
        ...page,
        items: page.items.map((entry) => ({ ...entry, id: `${entry.entityType}:${entry.entityId}:${entry.versionNo}` })),
      }
    },
  })
}
