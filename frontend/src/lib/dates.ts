/**
 * Múi giờ dùng để tính ngày "hôm nay" (ai đang ở, món nào còn), như backend
 * (`registry.business-zone`). Không dùng múi giờ của máy: máy đặt giờ khác sẽ chọn sai ngày.
 */
const BUSINESS_ZONE = 'Asia/Ho_Chi_Minh'

// en-CA viết ngày dạng YYYY-MM-DD.
const isoDate = new Intl.DateTimeFormat('en-CA', { timeZone: BUSINESS_ZONE, year: 'numeric', month: '2-digit', day: '2-digit' })

/** Ngày hôm nay ở Việt Nam, dạng `YYYY-MM-DD`. */
export function todayInBusinessZone(now: Date = new Date()): string {
  return isoDate.format(now)
}

/** Số ngày của một ngày `YYYY-MM-DD` tính từ mốc 1970, theo lịch (không lệch vì giờ mùa hè hay múi giờ). */
function dayNumber(value: string): number {
  const [year, month, day] = value.split('-').map(Number)
  return Date.UTC(year, month - 1, day) / 86_400_000
}

/** Số ngày từ `from` tới `to` theo lịch; âm khi `to` trước `from`. */
export function daysBetween(from: string, to: string): number {
  return dayNumber(to) - dayNumber(from)
}

/** Ngày `YYYY-MM-DD` cộng `days` ngày (âm là lùi lại). */
export function addDays(value: string, days: number): string {
  return new Date((dayNumber(value) + days) * 86_400_000).toISOString().slice(0, 10)
}
