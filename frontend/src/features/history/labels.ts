import type { ChangeAction } from './types'

export const ACTION_LABEL: Record<ChangeAction, string> = {
  CREATE: 'Tạo',
  UPDATE: 'Sửa',
  DELETE: 'Xoá',
  RESTORE: 'Khôi phục',
}
