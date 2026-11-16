import type { ApiError } from '@/api'

/**
 * Đưa lỗi của backend vào một form: lỗi của trường mà form có ô thì hiện cạnh ô đó, phần còn lại
 * trả về để hiện thành lỗi chung của form.
 *
 * @param setError `setError` của React Hook Form
 * @param isField tên trường (`name`, `attributes.area_m2`) có ô trong form hay không
 * @returns các thông báo chưa gắn được vào ô nào
 */
export function applyServerErrors(
  error: ApiError,
  setError: (name: string, error: { type: string; message: string }) => void,
  isField: (name: string) => boolean,
): string[] {
  const leftover: string[] = []
  for (const [field, message] of Object.entries(error.fieldErrors)) {
    if (isField(field)) {
      setError(field, { type: 'server', message })
    } else {
      leftover.push(`${field}: ${message}`)
    }
  }
  leftover.push(...error.formErrors)
  if (Object.keys(error.fieldErrors).length === 0 && error.formErrors.length === 0) {
    leftover.push(error.detail)
  }
  return leftover
}
