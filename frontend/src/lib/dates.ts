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
