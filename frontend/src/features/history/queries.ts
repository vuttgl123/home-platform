import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useCursorList, type ApiError, type CursorList } from '@/api'

import { getVersion, listVersions, restoreVersion } from './api'
import type { EntityKind, Version, VersionSummary } from './types'

export const historyKeys = {
  all: (ws: string) => ['history', ws] as const,
  entity: (ws: string, kind: EntityKind, id: string) => ['history', ws, kind, id] as const,
  list: (ws: string, kind: EntityKind, id: string) => ['history', ws, kind, id, 'list'] as const,
  version: (ws: string, kind: EntityKind, id: string, versionNo: number) => ['history', ws, kind, id, versionNo] as const,
}

export function useVersions(ws: string, kind: EntityKind, id: string): CursorList<VersionSummary & { id: string }> {
  return useCursorList({
    queryKey: historyKeys.list(ws, kind, id),
    // useCursorList bỏ trùng theo `id`; số version là khoá tự nhiên của một dòng.
    fetchPage: async (cursor) => {
      const page = await listVersions(ws, kind, id, cursor)
      return { ...page, items: page.items.map((item) => ({ ...item, id: String(item.versionNo) })) }
    },
  })
}

export function useVersion<Snapshot>(ws: string, kind: EntityKind, id: string, versionNo: number) {
  return useQuery<Version<Snapshot>, ApiError>({
    queryKey: historyKeys.version(ws, kind, id, versionNo),
    queryFn: () => getVersion<Snapshot>(ws, kind, id, versionNo),
    // Nội dung của một version cũ không bao giờ đổi.
    staleTime: Infinity,
  })
}

export function useRestoreVersion(ws: string, kind: EntityKind, id: string) {
  const queryClient = useQueryClient()
  return useMutation<unknown, ApiError, { versionNo: number; currentVersion: number; note?: string }>({
    mutationFn: ({ versionNo, currentVersion, note }) => restoreVersion(ws, kind, id, versionNo, currentVersion, note),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: historyKeys.list(ws, kind, id) }),
  })
}
