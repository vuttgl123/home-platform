import type { OwnershipStatus } from './types'

export const OWNERSHIP_STATUS_LABEL: Record<OwnershipStatus, string> = {
  CURRENT: 'Đang sở hữu',
  UPCOMING: 'Sắp tới',
  PAST: 'Đã thôi',
}
