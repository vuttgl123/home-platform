import type { Tone } from '@/components/common/ToneBadge'

import type { ChangeAction } from './types'

export const ACTION_LABEL: Record<ChangeAction, string> = {
  CREATE: 'Tạo',
  UPDATE: 'Sửa',
  DELETE: 'Xoá',
  RESTORE: 'Khôi phục',
}

/** Nghĩa màu của hành động: tạo thêm, sửa, xoá, khôi phục. */
export const ACTION_TONE: Record<ChangeAction, Tone> = { CREATE: 'success', UPDATE: 'info', DELETE: 'danger', RESTORE: 'violet' }
