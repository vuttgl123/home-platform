/** Ảnh tối đa 10 MB, như media-service. */
export const MAX_PHOTO_BYTES = 10 * 1024 * 1024

/** Kiểu file nhận được; dùng cho `accept` của ô chọn file. */
export const PHOTO_TYPES = ['image/jpeg', 'image/png', 'image/webp']

/** Nội dung ảnh gốc. Không gian nằm trên đường dẫn vì `<img>` không gửi được header. */
export function photoUrl(ws: string, fileId: string): string {
  return `/api/media/workspaces/${ws}/files/${fileId}`
}

/** Ảnh thu nhỏ JPEG, cạnh dài tối đa 480 px, đã xoay đúng chiều. */
export function thumbnailUrl(ws: string, fileId: string): string {
  return `${photoUrl(ws, fileId)}/thumbnail`
}

/** Lý do một file không gửi được (kiểm tra trước khi tải lên), hay `null` nếu gửi được. */
export function checkPhotoFile(file: File): string | null {
  if (!PHOTO_TYPES.includes(file.type)) return 'Chỉ nhận ảnh JPEG, PNG, WebP'
  if (file.size > MAX_PHOTO_BYTES) return 'Ảnh lớn hơn 10 MB'
  return null
}
