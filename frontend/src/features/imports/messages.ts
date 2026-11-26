import { ApiError } from '@/api'

export const TOO_LARGE = 'File lớn hơn 5 MB'

/** Lời nhắn khi gửi file hỏng ở tầng HTTP; lỗi trong file thì nằm trong báo cáo, không ở đây. */
export function importErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 413) return TOO_LARGE
    if (error.status === 404) return 'Máy chủ chưa hỗ trợ nhập Excel; cần cập nhật registry'
    return error.detail
  }
  return 'Không gửi được file'
}
