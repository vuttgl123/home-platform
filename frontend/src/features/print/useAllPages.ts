import { useEffect } from 'react'

import type { ApiError, CursorList } from '@/api'

/**
 * Gọi `loadMore` tới khi hết trang; trả về list cùng cờ done. Một trang lỗi thì dừng ở đó (không
 * thử lại mãi), `error` cho biết vì sao.
 */
export function useAllPages<T>(list: CursorList<T>): { items: T[]; done: boolean; error: ApiError | null } {
  const { isLoaded, hasMore, isLoadingMore, error, loadMore } = list
  useEffect(() => {
    if (isLoaded && hasMore && !isLoadingMore && error == null) {
      loadMore()
    }
  }, [isLoaded, hasMore, isLoadingMore, error, loadMore])
  return { items: list.items, done: isLoaded && !hasMore, error }
}
