import { QueryCache, QueryClient } from '@tanstack/react-query'

import { ApiError } from './errors'

/** Lỗi có thể tự hết khi gọi lại: không tới được máy chủ, hoặc máy chủ đang trục trặc. */
function isTransient(error: unknown): boolean {
  return error instanceof ApiError && (error.status === 0 || error.status >= 500)
}

/**
 * Cache dữ liệu server của ứng dụng.
 *
 * - Dữ liệu được coi là mới trong 30 giây; sau một lệnh ghi, hook của lệnh đó tự làm cũ những
 *   khoá liên quan.
 * - Không tải lại khi quay lại cửa sổ: cây địa điểm có nhiều danh sách đang mở, tải lại cả loạt
 *   mỗi lần đổi cửa sổ là thừa.
 * - Lệnh đọc thử lại một lần với lỗi tạm thời; lỗi 4xx và mọi lệnh ghi không thử lại.
 */
export function createQueryClient(): QueryClient {
  return new QueryClient({
    queryCache: new QueryCache({
      onError: (error) => {
        // Lỗi 4xx là kết quả bình thường (không tìm thấy, sai dữ liệu); phần còn lại cần được biết.
        if (!(error instanceof ApiError) || isTransient(error)) {
          console.error(error)
        }
      },
    }),
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        refetchOnWindowFocus: false,
        retry: (failureCount, error) => failureCount < 1 && isTransient(error),
      },
      mutations: { retry: false },
    },
  })
}
