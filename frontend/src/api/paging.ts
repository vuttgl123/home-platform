import { type InfiniteData, type QueryKey, useInfiniteQuery } from '@tanstack/react-query'
import { useMemo } from 'react'

import type { ApiError } from './errors'
import type { CursorPage } from './types'

/** Một danh sách tải dần từng trang. */
export type CursorList<T> = {
  /** Mọi phần tử đã tải, theo thứ tự của server. */
  items: T[]
  hasMore: boolean
  loadMore: () => void
  /** Đang tải trang đầu. */
  isLoading: boolean
  isLoadingMore: boolean
  /** Đã có ít nhất trang đầu. */
  isLoaded: boolean
  error: ApiError | null
  refetch: () => void
}

/**
 * Danh sách phân trang bằng cursor, có "tải thêm".
 *
 * Một `id` chỉ được giữ ở lần xuất hiện đầu: nếu có người thêm hay đổi tên một dòng giữa hai
 * lần tải, trang sau có thể trả lại dòng đã có ở trang trước.
 *
 * @param options.fetchPage lấy một trang; không có `cursor` là trang đầu
 * @param options.enabled `false` thì chưa tải (ví dụ nút của cây chưa được mở)
 */
export function useCursorList<T extends { id: string }>(options: {
  queryKey: QueryKey
  fetchPage: (cursor?: string) => Promise<CursorPage<T>>
  enabled?: boolean
  /** Như `meta` của `useQuery`, ví dụ `{ refreshAfterWrites: true }` cho danh sách gộp từ nhiều loại bản ghi. */
  meta?: Record<string, unknown>
}): CursorList<T> {
  const { queryKey, fetchPage, enabled = true, meta } = options
  const query = useInfiniteQuery<CursorPage<T>, ApiError, InfiniteData<CursorPage<T>>, QueryKey, string | undefined>({
    queryKey,
    queryFn: ({ pageParam }) => fetchPage(pageParam),
    initialPageParam: undefined,
    getNextPageParam: (lastPage) => lastPage.nextCursor ?? undefined,
    enabled,
    meta,
  })
  const pages = query.data?.pages
  const items = useMemo(() => {
    const seen = new Set<string>()
    return (pages ?? [])
      .flatMap((page) => page.items)
      .filter((item) => !seen.has(item.id) && seen.add(item.id))
  }, [pages])

  return {
    items,
    hasMore: query.hasNextPage,
    loadMore: () => {
      if (!query.isFetchingNextPage) {
        void query.fetchNextPage()
      }
    },
    isLoading: query.isLoading,
    isLoadingMore: query.isFetchingNextPage,
    isLoaded: query.isSuccess,
    error: query.error,
    refetch: () => void query.refetch(),
  }
}

/** Mọi phần tử của một danh sách phân trang, dùng cho danh sách ngắn cần đủ ngay (không gian của tôi). */
export async function fetchAllPages<T>(fetchPage: (cursor?: string) => Promise<CursorPage<T>>): Promise<T[]> {
  const items: T[] = []
  let cursor: string | undefined
  do {
    const page = await fetchPage(cursor)
    items.push(...page.items)
    cursor = page.nextCursor ?? undefined
  } while (cursor !== undefined)
  return items
}
