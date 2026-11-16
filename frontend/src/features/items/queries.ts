import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { useCallback } from 'react'

import { useCursorList, type ApiError, type CursorList } from '@/api'
import { isId } from '@/lib/ids'

import {
  createItem,
  deleteItem,
  getItem,
  listItemsAtPlace,
  listItemsInSubtree,
  moveItem,
  putBackItem,
  reissueItemCode,
  removeItem,
  replaceItem,
  restoreItem,
  updateItem,
} from './api'
import type { ItemBody, ItemDetail, ItemSummary, ItemUpdateBody } from './types'

export const itemKeys = {
  all: (ws: string) => ['items', ws] as const,
  lists: (ws: string) => ['items', ws, 'list'] as const,
  atPlace: (ws: string, placeId: string, removed: boolean) => ['items', ws, 'list', 'place', placeId, removed] as const,
  inSubtree: (ws: string, placeId: string) => ['items', ws, 'list', 'subtree', placeId] as const,
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

/** Bỏ xoá một món nhập nhầm đã xoá. */
export function useRestoreItem(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<ItemDetail, ApiError, { id: string; version: number }>({
    mutationFn: ({ id, version }) => restoreItem(ws, id, version),
    onSuccess: (restored) => refreshItems(queryClient, ws, restored),
  })
}
