import type { Tone } from '@/components/common/ToneBadge'
import { daysBetween } from '@/lib/dates'
import { formatDate } from '@/lib/format'

import type { ItemStatus } from './types'

export const ITEM_STATUS_LABEL: Record<ItemStatus, string> = {
  IN_USE: 'Đang dùng',
  BROKEN: 'Hỏng',
  STORED: 'Cất kho',
  REMOVED: 'Đã gỡ',
}

/** Nghĩa màu của trạng thái đồ: đang dùng tốt, hỏng cần sửa, cất kho cần để ý, đã gỡ là quá khứ. */
export const ITEM_STATUS_TONE: Record<ItemStatus, Tone> = { IN_USE: 'success', BROKEN: 'danger', STORED: 'warning', REMOVED: 'neutral' }

/** Bảo hành sắp hết (trong 30 ngày, như Việc cần làm) thì cần để ý; đã hết thì không còn gì để làm. */
const WARRANTY_WARNING_DAYS = 30

/** Dòng "Bảo hành tới …" của thẻ Thông tin, kèm số ngày còn lại và màu. `today` là `YYYY-MM-DD`. */
export function warrantyLine(warrantyUntil: string, today: string): { text: string; tone: Tone } {
  const days = daysBetween(today, warrantyUntil)
  const head = `Bảo hành tới ${formatDate(warrantyUntil)} · `
  if (days < 0) return { text: `${head}đã hết`, tone: 'neutral' }
  if (days === 0) return { text: `${head}hết hôm nay`, tone: 'warning' }
  return { text: `${head}còn ${days} ngày`, tone: days <= WARRANTY_WARNING_DAYS ? 'warning' : 'neutral' }
}
