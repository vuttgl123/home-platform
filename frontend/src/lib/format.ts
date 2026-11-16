// Cách hiện ngày, giờ, số trên giao diện tiếng Việt.

const numberFormat = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 6 })

/** `'2026-01-31'` → `'31/01/2026'`. Đọc thẳng chuỗi, không qua `Date`, để múi giờ không làm lệch ngày. */
export function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.slice(0, 10).split('-')
  return `${day}/${month}/${year}`
}

/** Một thời điểm (ISO-8601, giờ UTC) theo giờ của máy người xem: `'31/01/2026 14:05'`. */
export function formatDateTime(isoInstant: string): string {
  const at = new Date(isoInstant)
  const two = (value: number) => String(value).padStart(2, '0')
  return `${two(at.getDate())}/${two(at.getMonth() + 1)}/${at.getFullYear()} ${two(at.getHours())}:${two(at.getMinutes())}`
}

export function formatNumber(value: number): string {
  return numberFormat.format(value)
}
