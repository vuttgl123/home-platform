import type { Tone } from '@/components/common/ToneBadge'

import type { ItemStatus } from './types'

export const ITEM_STATUS_LABEL: Record<ItemStatus, string> = {
  IN_USE: 'Đang dùng',
  BROKEN: 'Hỏng',
  STORED: 'Cất kho',
  REMOVED: 'Đã gỡ',
}

/** Nghĩa màu của trạng thái đồ: đang dùng tốt, hỏng cần sửa, cất kho cần để ý, đã gỡ là quá khứ. */
export const ITEM_STATUS_TONE: Record<ItemStatus, Tone> = { IN_USE: 'success', BROKEN: 'danger', STORED: 'warning', REMOVED: 'neutral' }
