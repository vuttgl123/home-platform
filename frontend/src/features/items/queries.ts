import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { useCallback } from 'react'

import { useCursorList, type ApiError, type CursorList } from '@/api'
import { isId } from '@/lib/ids'

import {
  changeItemOwner,
  createItem,
  deleteItem,
  getItem,
  listAllItems,
  listItemsAtPlace,
  listItemsInSubtree,
  listItemsOwnedBy,
  moveItem,
  putBackItem,
  quickFindItems,
  reissueItemCode,
  removeItem,
  replaceItem,
  restoreItem,
  updateItem,
  type ItemFilter,
} from './api'
import type { ItemBody, ItemDetail, ItemSummary, ItemUpdateBody } from './types'

export const itemKeys = {
  all: (ws: string) => ['items', ws] as const,
  lists: (ws: string) => ['items', ws, 'list'] as const,
  atPlace: (ws: string, placeId: string, removed: boolean) => ['items', ws, 'list', 'place', placeId, removed] as const,
  inSubtree: (ws: string, placeId: string) => ['items', ws, 'list', 'subtree', placeId] as const,
  ownedBy: (ws: string, personId: string) => ['items', ws, 'list', 'owner', personId] as const,
  everywhere: (ws: string, filter: ItemFilter) =>
    ['items', ws, 'list', 'all', filter.status ?? 'current', filter.categoryId ?? 'any'] as const,
  details: (ws: string) => ['items', ws, 'detail'] as const,
  detail: (ws: string, id: string) => ['items', ws, 'detail', id] as const,
}

export function useItemsAtPlace(
  ws: string,
  placeId: string,
  removed: boolean,
  options: { enabled?: boolean } = {},
): CursorList<ItemSummary> {
  return useCursorList({
    queryKey: itemKeys.atPlace(ws, placeId, removed),
    fetchPage: (cursor) => listItemsAtPlace(ws, placeId, removed, cursor),
    enabled: options.enabled,
  })
}

export function useItemsInSubtree(ws: string, placeId: string, enabled: boolean): CursorList<ItemSummary> {
  return useCursorList({
    queryKey: itemKeys.inSubtree(ws, placeId),
    fetchPage: (cursor) => listItemsInSubtree(ws, placeId, cursor),
    enabled,
  })
}

/** Đồ của cả không gian theo bộ lọc. */
export function useAllItems(ws: string, filter: ItemFilter): CursorList<ItemSummary> {
  return useCursorList({ queryKey: itemKeys.everywhere(ws, filter), fetchPage: (cursor) => listAllItems(ws, filter, cursor) })
}

/** Đồ chưa gỡ của riêng một người. */
export function useItemsOwnedBy(ws: string, personId: string): CursorList<ItemSummary> {
  return useCursorList({ queryKey: itemKeys.ownedBy(ws, personId), fetchPage: (cursor) => listItemsOwnedBy(ws, personId, cursor) })
}

/** Một món đầy đủ; `id` trống hoặc sai dạng thì không gọi. */
export function useItem(ws: string, id: string | null | undefined) {
  return useQuery<ItemDetail, ApiError>({
    queryKey: itemKeys.detail(ws, id ?? ''),
    queryFn: () => getItem(ws, id as string),
    enabled: isId(id),
  })
}

/** Lấy bản mới nhất của một món (bỏ qua cache) và cập nhật cache; dùng sau 409 "đã có người sửa". */
export function useFetchFreshItem(ws: string): (id: string) => Promise<ItemDetail> {
  const queryClient = useQueryClient()
  return useCallback(
    (id: string) => queryClient.fetchQuery({ queryKey: itemKeys.detail(ws, id), queryFn: () => getItem(ws, id), staleTime: 0 }),
    [queryClient, ws],
  )
}

/**
 * Sau một lệnh ghi đồ đạc: đặt chi tiết của từng món vừa đổi, làm cũ mọi danh sách đồ. Một lệnh
 * có thể chạm hai địa điểm (chuyển chỗ, thay ở chỗ khác) và các toà chứa chúng, nên không đoán
 * từng khoá.
 */
export function refreshItems(queryClient: QueryClient, ws: string, ...changed: ItemDetail[]): void {
  for (const item of changed) {
    queryClient.setQueryData(itemKeys.detail(ws, item.id), item)
  }
  void queryClient.invalidateQueries({ queryKey: itemKeys.lists(ws) })
}

/** Bỏ chi tiết của một món khỏi cache, sau khi đã rời trang của nó. */
export function forgetItem(queryClient: QueryClient, ws: string, id: string): void {
  queryClient.removeQueries({ queryKey: itemKeys.detail(ws, id), exact: true })
}

export function useCreateItem(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<ItemDetail, ApiError, ItemBody>({
    mutationFn: (body) => createItem(ws, body),
    onSuccess: (created) => refreshItems(queryClient, ws, created),
  })
}

export function useUpdateItem(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<ItemDetail, ApiError, { id: string; body: ItemUpdateBody }>({
    mutationFn: ({ id, body }) => updateItem(ws, id, body),
    onSuccess: (updated) => refreshItems(queryClient, ws, updated),
  })
}

export function useMoveItem(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<ItemDetail, ApiError, { item: ItemDetail; placeId: string }>({
    mutationFn: ({ item, placeId }) => moveItem(ws, item.id, item.version, placeId),
    onSuccess: (moved) => refreshItems(queryClient, ws, moved),
  })
}

export function useChangeItemOwner(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<ItemDetail, ApiError, { item: ItemDetail; ownerPersonId: string | null }>({
    mutationFn: ({ item, ownerPersonId }) => changeItemOwner(ws, item.id, item.version, ownerPersonId),
    onSuccess: (changed) => refreshItems(queryClient, ws, changed),
  })
}

export function useReissueItemCode(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<ItemDetail, ApiError, ItemDetail>({
    mutationFn: (item) => reissueItemCode(ws, item.id, item.version),
    onSuccess: (updated) => refreshItems(queryClient, ws, updated),
  })
}

export function useRemoveItem(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<ItemDetail, ApiError, { item: ItemDetail; removedAt: string }>({
    mutationFn: ({ item, removedAt }) => removeItem(ws, item.id, item.version, removedAt),
    onSuccess: (removed) => refreshItems(queryClient, ws, removed),
  })
}

export function useReplaceItem(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<
    { removed: ItemDetail; replacement: ItemDetail },
    ApiError,
    { item: ItemDetail; removedAt: string; replacement: ItemBody }
  >({
    mutationFn: ({ item, removedAt, replacement }) => replaceItem(ws, item.id, item.version, removedAt, replacement),
    onSuccess: ({ removed, replacement }) => refreshItems(queryClient, ws, removed, replacement),
  })
}

export function usePutBackItem(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<ItemDetail, ApiError, ItemDetail>({
    mutationFn: (item) => putBackItem(ws, item.id, item.version),
    onSuccess: (back) => refreshItems(queryClient, ws, back),
  })
}

/** Xoá một món nhập nhầm. Nơi gọi rời trang của nó rồi mới `forgetItem`. */
export function useDeleteItem(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<void, ApiError, ItemDetail>({
    mutationFn: (item) => deleteItem(ws, item.id, item.version),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: itemKeys.lists(ws) }),
  })
}

/** "Hoàn tác" sau khi xoá một món: khôi phục với `version` lúc xoá cộng 1. */
export async function undoDeleteItem(queryClient: QueryClient, ws: string, id: string, deletedVersion: number): Promise<ItemDetail> {
  const restored = await restoreItem(ws, id, deletedVersion + 1)
  refreshItems(queryClient, ws, restored)
  return restored
}

/** Bỏ xoá một món nhập nhầm đã xoá. */
export function useRestoreItem(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<ItemDetail, ApiError, { id: string; version: number }>({
    mutationFn: ({ id, version }) => restoreItem(ws, id, version),
    onSuccess: (restored) => refreshItems(queryClient, ws, restored),
  })
}

/** Tìm nhanh cho ô tìm chung; `term` rỗng thì không gọi. */
export function useItemQuickFind(ws: string, term: string) {
  return useQuery<{ byCode: ItemSummary[]; byName: ItemSummary[] }, ApiError>({
    queryKey: [...itemKeys.lists(ws), 'quick', term],
    queryFn: () => quickFindItems(ws, term),
    enabled: term !== '',
  })
}
