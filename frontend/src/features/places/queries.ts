import { useQuery } from '@tanstack/react-query'

import { useCursorList, type ApiError, type CursorList } from '@/api'
import { isId } from '@/lib/ids'

import { getPlace, listChildren, listTopLevel, searchPlaces } from './api'
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
