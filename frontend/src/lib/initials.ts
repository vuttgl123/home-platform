/**
 * Chữ cái đầu của một tên cho ô avatar: chữ đầu của từ đầu và từ cuối ("Nguyễn Thị Lan" → "NL"),
 * một từ thì một chữ. Viết hoa theo tiếng Việt ("đỗ" → "Đ"). Tên trống → "?".
 */
export function initials(name: string | null | undefined): string {
  const words = (name ?? '').trim().split(/\s+/).filter(Boolean)
  if (words.length === 0) {
    return '?'
  }
  const first = words[0]
  const last = words.length > 1 ? words[words.length - 1] : ''
  return (Array.from(first)[0] + (last ? Array.from(last)[0] : '')).toLocaleUpperCase('vi')
}
