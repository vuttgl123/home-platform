import { useQuery } from '@tanstack/react-query'

import type { ApiError } from '@/api'

import { getOverview } from './api'
import type { Overview } from './types'

export const overviewKeys = {
  get: (ws: string) => ['overview', ws] as const,
}

/**
 * Số liệu tổng quan của không gian. Chúng gộp từ mọi loại bản ghi, nên luôn được coi là cũ: mỗi lần
 * mở trang là đọc lại, và mọi lệnh ghi thành công cũng làm nó đọc lại (`refreshAfterWrites`).
 * @param options.enabled `false` thì chưa đọc (không gian chưa có danh mục)
 */
export function useOverview(ws: string, options: { enabled?: boolean } = {}) {
  return useQuery<Overview, ApiError>({
    queryKey: overviewKeys.get(ws),
    queryFn: () => getOverview(ws),
    staleTime: 0,
    enabled: options.enabled ?? true,
    meta: { refreshAfterWrites: true },
  })
}
