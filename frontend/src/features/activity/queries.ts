import { useQuery } from '@tanstack/react-query'

import { type ApiError, currentUserId, useCursorList, type CursorList } from '@/api'

import { listActivity, listTrash } from './api'
import { rangeIsValid, toQuery } from './filters'
import type { ActivityEntry, ActivityFilter, EntityType, TrashEntry } from './types'

export const activityKeys = {
  all: (ws: string) => ['activity', ws] as const,
  list: (ws: string, filter: ActivityFilter) => ['activity', ws, 'list', filter] as const,
  recent: (ws: string) => ['activity', ws, 'recent'] as const,
}

/** Số dòng của phần "Hoạt động gần đây" trên trang tổng quan. */
const RECENT_LIMIT = 5

/** Mấy thay đổi mới nhất của không gian, một trang ngắn không có "tải thêm". */
export function useRecentActivity(ws: string) {
  return useQuery<ActivityEntry[], ApiError>({
    queryKey: activityKeys.recent(ws),
    queryFn: async () => (await listActivity(ws, {}, undefined, RECENT_LIMIT)).items,
    staleTime: 0,
  })
}

/**
 * Danh sách của trang Hoạt động theo một bộ lọc. Khoảng ngày ngược (`from` sau `to`) thì không
 * hỏi server: trang báo lỗi ngay, và backend cũng sẽ từ chối.
 */
export function useActivity(ws: string, filter: ActivityFilter): CursorList<ActivityEntry & { id: string }> {
  const me = currentUserId()
  return useCursorList({
    // Người dùng nằm trong khoá vì "chỉ việc tôi làm" phụ thuộc vào nó.
    queryKey: [...activityKeys.list(ws, filter), me],
    enabled: rangeIsValid(filter),
    fetchPage: async (cursor) => {
      const page = await listActivity(ws, toQuery(filter, me), cursor)
      // Một dòng là một version của một bản ghi: loại, id và số version cùng nhau là duy nhất.
      return {
        ...page,
        items: page.items.map((entry) => ({ ...entry, id: `${entry.entityType}:${entry.entityId}:${entry.versionNo}` })),
      }
    },
  })
}

export const trashKeys = {
  all: (ws: string) => ['trash', ws] as const,
  list: (ws: string, entityType: EntityType) => ['trash', ws, entityType] as const,
}

export function useTrash(ws: string, entityType: EntityType): CursorList<TrashEntry> {
  return useCursorList({ queryKey: trashKeys.list(ws, entityType), fetchPage: (cursor) => listTrash(ws, entityType, cursor) })
}
