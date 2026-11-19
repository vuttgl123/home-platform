/** Giá trị trống (`null`, `undefined`, chuỗi rỗng) hiện "—". */
export function blankAsDash<T>(value: T): T | '—' {
  return value == null || value === '' ? '—' : value
}
