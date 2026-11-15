/**
 * `value` có dạng một `id` của backend (một dãy chữ số) hay không.
 *
 * `id` là số 64 bit, luôn được giữ ở dạng chuỗi. Một `id` lấy từ URL phải qua hàm này trước khi
 * gửi đi: backend trả 400 cho `id` sai dạng, còn người dùng cần thấy "Không tìm thấy".
 */
export function isId(value: string | null | undefined): value is string {
  return value != null && /^\d+$/.test(value)
}
