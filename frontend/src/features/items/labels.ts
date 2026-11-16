import type { ItemStatus } from './types'

export const ITEM_STATUS_LABEL: Record<ItemStatus, string> = {
  IN_USE: 'Đang dùng',
  BROKEN: 'Hỏng',
  STORED: 'Cất kho',
  REMOVED: 'Đã gỡ',
}
