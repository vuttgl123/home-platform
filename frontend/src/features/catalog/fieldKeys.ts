/**
 * Khoá gợi ý cho một mục tuỳ biến từ nhãn của nó: bỏ dấu, viết thường, ký tự khác chữ và số thành
 * `_`. Khoá phải bắt đầu bằng một chữ cái (`[a-z][a-z0-9_]*`), nên nhãn bắt đầu bằng số được thêm `f_`.
 */
export function suggestFieldKey(label: string): string {
  const key = label
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .replace(/[đĐ]/g, 'd')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .slice(0, 48)
  return key === '' ? '' : /^[a-z]/.test(key) ? key : `f_${key}`
}
