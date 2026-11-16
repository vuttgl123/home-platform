import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { useCallback } from 'react'

import { useCursorList, type ApiError, type CursorList } from '@/api'
import { isId } from '@/lib/ids'

import {
  createPlace,
  getPlace,
  listChildren,
  listTopLevel,
  searchPlaces,
  updatePlace,
  type PlaceCreateBody,
  type PlaceUpdateBody,
} from './api'
import type { PlaceDetail, PlaceSummary } from './types'

/**
 * Khoá cache của địa điểm. Khoá ngắn là tiền tố của khoá dài, nên làm cũ `details(ws)` là làm cũ
 * mọi trang chi tiết của không gian.
 */
export const placeKeys = {
  all: (ws: string) => ['places', ws] as const,
  lists: (ws: string) => ['places', ws, 'children'] as const,
  /** Con của `parentId`; `null` là cấp cao nhất. */
  children: (ws: string, parentId: string | null) => ['places', ws, 'children', parentId ?? 'root'] as const,
  details: (ws: string) => ['places', ws, 'detail'] as const,
  detail: (ws: string, id: string) => ['places', ws, 'detail', id] as const,
  searches: (ws: string) => ['places', ws, 'search'] as const,
  search: (ws: string, name: string) => ['places', ws, 'search', name] as const,
}

/**
 * Con của một địa điểm (`null`: cấp cao nhất), tải dần. Cây và tab "Bên trong" dùng chung khoá
 * này nên dùng chung dữ liệu.
 * @param options.enabled `false` thì chưa tải; nút của cây chỉ tải con khi được mở
 */
export function useChildren(
  ws: string,
  parentId: string | null,
  options: { enabled?: boolean } = {},
): CursorList<PlaceSummary> {
  return useCursorList({
    queryKey: placeKeys.children(ws, parentId),
    fetchPage: (cursor) => (parentId == null ? listTopLevel(ws, cursor) : listChildren(ws, parentId, cursor)),
    enabled: options.enabled,
  })
}

/** Một địa điểm đầy đủ. `id` trống hoặc sai dạng thì không gọi. */
export function usePlace(ws: string, id: string | undefined) {
  return useQuery<PlaceDetail, ApiError>({
    queryKey: placeKeys.detail(ws, id ?? ''),
    queryFn: () => getPlace(ws, id as string),
    enabled: isId(id),
  })
}

/** Tìm theo tên bắt đầu bằng `name`; `name` rỗng thì không gọi. */
export function usePlaceSearch(ws: string, name: string): CursorList<PlaceSummary> {
  return useCursorList({
    queryKey: placeKeys.search(ws, name),
    fetchPage: (cursor) => searchPlaces(ws, name, cursor),
    enabled: name !== '',
  })
}

/**
 * Lấy bản mới nhất của một địa điểm từ server (bỏ qua cache) và cập nhật cache. Dùng khi một lệnh
 * ghi gặp 409 "đã có người sửa".
 */
export function useFetchFreshPlace(ws: string): (id: string) => Promise<PlaceDetail> {
  const queryClient = useQueryClient()
  return useCallback(
    (id: string) =>
      queryClient.fetchQuery({ queryKey: placeKeys.detail(ws, id), queryFn: () => getPlace(ws, id), staleTime: 0 }),
    [queryClient, ws],
  )
}

/**
 * Sau khi một địa điểm đổi: đặt chi tiết của nó bằng response và làm cũ những gì có thể đã hiện
 * nội dung cũ của nó.
 *
 * @param scope `'self'`: danh sách con của cha và kết quả tìm kiếm (thêm). `'descendants'`: thêm mọi
 *   trang chi tiết khác, vì đường dẫn tổ tiên hay địa chỉ thừa hưởng của con cháu mang tên hoặc địa
 *   chỉ cũ (sửa, đặt địa chỉ). `'tree'`: cả cây (di chuyển).
 */
export function refreshAfterWrite(
  queryClient: QueryClient,
  ws: string,
  place: PlaceDetail,
  scope: 'self' | 'descendants' | 'tree',
): void {
  queryClient.setQueryData(placeKeys.detail(ws, place.id), place)
  const isOwnDetail = (key: readonly unknown[]) => key[2] === 'detail' && key[3] === place.id
  if (scope === 'tree') {
    void queryClient.invalidateQueries({ queryKey: placeKeys.all(ws), predicate: (query) => !isOwnDetail(query.queryKey) })
    return
  }
  void queryClient.invalidateQueries({ queryKey: placeKeys.children(ws, place.parentId) })
  void queryClient.invalidateQueries({ queryKey: placeKeys.searches(ws) })
  if (scope === 'descendants') {
    void queryClient.invalidateQueries({ queryKey: placeKeys.details(ws), predicate: (query) => !isOwnDetail(query.queryKey) })
  }
}

export function useCreatePlace(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<PlaceDetail, ApiError, PlaceCreateBody>({
    mutationFn: (body) => createPlace(ws, body),
    onSuccess: (created) => refreshAfterWrite(queryClient, ws, created, 'self'),
  })
}

export function useUpdatePlace(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<PlaceDetail, ApiError, { id: string; body: PlaceUpdateBody }>({
    mutationFn: ({ id, body }) => updatePlace(ws, id, body),
    onSuccess: (updated) => refreshAfterWrite(queryClient, ws, updated, 'descendants'),
  })
}
