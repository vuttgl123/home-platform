/** Tiền tố hợp lệ của một dạng mã: 1 đến 10 chữ cái không dấu hoặc số, bắt đầu bằng chữ (như backend). */
export const PREFIX_PATTERN = /^[A-Z][A-Z0-9]{0,9}$/

/** Mã đầu tiên mà một dạng mã sẽ cấp trong năm `year`. */
export function codeExample(rule: { prefix: string; digits: number; yearly: boolean }, year: number): string {
  return `${rule.prefix}-${rule.yearly ? `${year}-` : ''}${'1'.padStart(rule.digits, '0')}`
}

/**
 * Mã gợi ý cho một loại địa điểm từ tên của nó: bỏ dấu, viết hoa, ký tự khác chữ và số thành `_`.
 * Trùng một mã trong `taken` (không phân biệt hoa thường) thì thêm `_2`, `_3`…
 */
export function suggestTypeCode(name: string, taken: string[]): string {
  const base = name
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .replace(/[đĐ]/g, 'd')
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .slice(0, 46)
  if (base === '') return ''
  const used = new Set(taken.map((code) => code.toUpperCase()))
  if (!used.has(base)) return base
  let n = 2
  while (used.has(`${base}_${n}`)) n++
  return `${base}_${n}`
}
