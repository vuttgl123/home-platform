import { useMutation, useQueryClient, type QueryClient } from '@tanstack/react-query'

import { useCursorList, type ApiError, type CursorList } from '@/api'
import { itemKeys } from '@/features/items'

import { createMaintenance, deleteMaintenance, listMaintenance, restoreMaintenance, updateMaintenance } from './api'
import type { MaintenanceCreateBody, MaintenanceEntry, MaintenanceUpdateBody } from './types'

export const maintenanceKeys = {
  all: (ws: string) => ['maintenance', ws] as const,
  list: (ws: string, itemId: string) => ['maintenance', ws, itemId] as const,
}

export function useMaintenance(ws: string, itemId: string): CursorList<MaintenanceEntry> {
  return useCursorList({ queryKey: maintenanceKeys.list(ws, itemId), fetchPage: (cursor) => listMaintenance(ws, itemId, cursor) })
}

/** Nhật ký đổi thì món cũng đổi theo: hạn bảo trì kế tiếp, và trạng thái khi sửa xong. */
function refresh(queryClient: QueryClient, ws: string, itemId: string) {
  void queryClient.invalidateQueries({ queryKey: maintenanceKeys.list(ws, itemId) })
  void queryClient.invalidateQueries({ queryKey: itemKeys.detail(ws, itemId) })
  void queryClient.invalidateQueries({ queryKey: itemKeys.lists(ws) })
}

export function useCreateMaintenance(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<MaintenanceEntry, ApiError, MaintenanceCreateBody>({
    mutationFn: (body) => createMaintenance(ws, body),
    onSuccess: (created) => refresh(queryClient, ws, created.itemId),
  })
}

export function useUpdateMaintenance(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<MaintenanceEntry, ApiError, { id: string; body: MaintenanceUpdateBody }>({
    mutationFn: ({ id, body }) => updateMaintenance(ws, id, body),
    onSuccess: (updated) => refresh(queryClient, ws, updated.itemId),
  })
}

export function useDeleteMaintenance(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<void, ApiError, MaintenanceEntry>({
    mutationFn: (entry) => deleteMaintenance(ws, entry.id, entry.version),
    onSuccess: (_, entry) => refresh(queryClient, ws, entry.itemId),
  })
}

/** "Hoàn tác" sau khi xoá một dòng: khôi phục với `version` lúc xoá cộng 1. */
export async function undoDeleteMaintenance(queryClient: QueryClient, ws: string, entry: MaintenanceEntry): Promise<void> {
  await restoreMaintenance(ws, entry.id, entry.version + 1)
  refresh(queryClient, ws, entry.itemId)
}
