/** Chữ tìm ngắn hơn thì không gửi: backend bỏ qua (spec Đợt 11, S1), và một chữ cái khớp gần như mọi dòng. */
export const MIN_SEARCH_LENGTH = 2

/** Chữ tìm sẽ gửi API: đã cắt hai đầu; quá ngắn thì không có. */
export function searchText(q: string): string | undefined {
  const text = q.trim()
  return [...text].length >= MIN_SEARCH_LENGTH ? text : undefined
}
