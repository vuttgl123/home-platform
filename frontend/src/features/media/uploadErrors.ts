import { ApiError } from '@/api'

/** Câu báo lỗi tiếng Việt cho một ảnh tải lên không được; backend trả chi tiết bằng tiếng Anh. */
export function uploadErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 400) return 'Không đọc được ảnh này'
    if (error.status === 413) return 'Ảnh lớn hơn 10 MB'
    if (error.status === 409) return 'Đã đủ 100 ảnh'
    return error.detail
  }
  return String(error)
}
