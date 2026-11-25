import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { useCallback } from 'react'

import { useCursorList, type ApiError, type CursorList } from '@/api'
import { isId } from '@/lib/ids'

import {
  createPlace,
  deletePlace,
  getPlace,
  listChildren,
  listSubtree,
  listTopLevel,
  movePlace,
  quickFindPlaces,
  reissuePlaceCode,
  removeAddress,
  restorePlace,
  searchPlaces,
  setAddress,
  updatePlace,
  type AddressBody,
  type PlaceCreateBody,
  type PlaceUpdateBody,
} from './api'
import type { PlaceDetail, PlaceSearchResult, PlaceSummary } from './types'

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
  subtrees: (ws: string) => ['places', ws, 'subtree'] as const,
  subtree: (ws: string, placeId: string) => ['places', ws, 'subtree', placeId] as const,
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

/** Mọi hậu duệ của một địa điểm, tải dần; chỉ tải khi `enabled`. */
export function useSubtree(ws: string, placeId: string, enabled: boolean): CursorList<PlaceSummary> {
  return useCursorList({ queryKey: placeKeys.subtree(ws, placeId), fetchPage: (cursor) => listSubtree(ws, placeId, cursor), enabled })
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
export function usePlaceSearch(ws: string, name: string): CursorList<PlaceSearchResult> {
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
  // Trang Chỗ trống (features/vacancies) phụ thuộc sức chứa và cây; khoá viết thẳng để không import vòng.
  void queryClient.invalidateQueries({ queryKey: ['vacancies', ws] })
  const isOwnDetail = (key: readonly unknown[]) => key[2] === 'detail' && key[3] === place.id
  if (scope === 'tree') {
    void queryClient.invalidateQueries({ queryKey: placeKeys.all(ws), predicate: (query) => !isOwnDetail(query.queryKey) })
    return
  }
  // Mọi danh sách con, không chỉ của cha: dòng của chính người cha (nằm trong danh sách của ông)
  // mang `hasChildren`, có thể vừa đổi. Chỉ danh sách đang hiện mới tải lại.
  void queryClient.invalidateQueries({ queryKey: placeKeys.lists(ws) })
  void queryClient.invalidateQueries({ queryKey: placeKeys.searches(ws) })
  void queryClient.invalidateQueries({ queryKey: placeKeys.subtrees(ws) })
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

export function useMovePlace(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<PlaceDetail, ApiError, { place: PlaceDetail; parentId: string | null }>({
    mutationFn: ({ place, parentId }) => movePlace(ws, place.id, place.version, parentId),
    // Cả cây con đổi chỗ: đường dẫn tổ tiên, địa chỉ thừa hưởng và hai nhánh của cây đều cũ.
    onSuccess: (moved) => refreshAfterWrite(queryClient, ws, moved, 'tree'),
  })
}

/**
 * Xoá một địa điểm. Chi tiết của nó vẫn nằm trong cache: nơi gọi rời trang của nó trước rồi mới
 * bỏ (`forgetPlace`), để trang đang mở không tải lại một địa điểm đã xoá.
 */
export function useDeletePlace(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<void, ApiError, PlaceDetail>({
    mutationFn: (place) => deletePlace(ws, place.id, place.version),
    onSuccess: () => {
      // Như `refreshAfterWrite`: cha có thể vừa thành lá.
      void queryClient.invalidateQueries({ queryKey: placeKeys.lists(ws) })
      void queryClient.invalidateQueries({ queryKey: placeKeys.searches(ws) })
      void queryClient.invalidateQueries({ queryKey: placeKeys.subtrees(ws) })
    },
  })
}

/** Bỏ chi tiết của một địa điểm khỏi cache. */
export function forgetPlace(queryClient: QueryClient, ws: string, id: string): void {
  queryClient.removeQueries({ queryKey: placeKeys.detail(ws, id), exact: true })
}

export function useSetAddress(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<PlaceDetail, ApiError, { place: PlaceDetail; address: AddressBody }>({
    mutationFn: ({ place, address }) => setAddress(ws, place.id, { version: place.version, address }),
    // Con cháu không có địa chỉ riêng thừa hưởng địa chỉ này.
    onSuccess: (updated) => refreshAfterWrite(queryClient, ws, updated, 'descendants'),
  })
}

export function useRemoveAddress(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<PlaceDetail, ApiError, PlaceDetail>({
    mutationFn: (place) => removeAddress(ws, place.id, place.version),
    onSuccess: (updated) => refreshAfterWrite(queryClient, ws, updated, 'descendants'),
  })
}

/** Cấp mã mới: mã hiện trên cây, trong kết quả tìm và ở đầu trang. */
export function useReissuePlaceCode(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<PlaceDetail, ApiError, PlaceDetail>({
    mutationFn: (place) => reissuePlaceCode(ws, place.id, place.version),
    onSuccess: (updated) => refreshAfterWrite(queryClient, ws, updated, 'self'),
  })
}

/**
 * "Hoàn tác" sau khi xoá: khôi phục với `version` lúc xoá cộng 1 (lần xoá làm tăng `version` một đơn
 * vị), rồi làm mới như sau một lần khôi phục. Hàm thường, không phải hook: hộp thoại xoá đã đóng.
 */
export async function undoDeletePlace(queryClient: QueryClient, ws: string, id: string, deletedVersion: number): Promise<PlaceDetail> {
  const restored = await restorePlace(ws, id, deletedVersion + 1)
  refreshAfterWrite(queryClient, ws, restored, 'self')
  return restored
}

/** Bỏ xoá một địa điểm: nó hiện lại trên cây, đúng chỗ cũ. */
export function useRestorePlace(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<PlaceDetail, ApiError, { id: string; version: number }>({
    mutationFn: ({ id, version }) => restorePlace(ws, id, version),
    onSuccess: (restored) => refreshAfterWrite(queryClient, ws, restored, 'self'),
  })
}

/** Tìm nhanh cho ô tìm chung; `term` rỗng thì không gọi. Khoá nằm dưới kết quả tìm nên lệnh ghi làm cũ nó. */
export function usePlaceQuickFind(ws: string, term: string) {
  return useQuery<{ byCode: PlaceSearchResult[]; byName: PlaceSearchResult[] }, ApiError>({
    queryKey: [...placeKeys.searches(ws), 'quick', term],
    queryFn: () => quickFindPlaces(ws, term),
    enabled: term !== '',
  })
}

/**
 * Đặt hay bỏ (`null`) ảnh bìa: một lần sửa bình thường, có version. Lệnh sửa ghi đè cả bản ghi nên
 * gửi lại đủ các trường đang có.
 */
export function useSetPlaceCover(ws: string) {
  const update = useUpdatePlace(ws)
  return (place: PlaceDetail, fileId: string | null) =>
    update.mutateAsync({
      id: place.id,
      body: {
        version: place.version,
        placeTypeId: place.placeTypeId,
        name: place.name,
        status: place.status,
        capacity: place.capacity ?? undefined,
        description: place.description ?? undefined,
        attributes: (place.attributes ?? undefined) as PlaceUpdateBody['attributes'],
        coverFileId: fileId ?? undefined,
      },
    })
}
