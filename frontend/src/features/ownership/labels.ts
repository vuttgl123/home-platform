import type { Tone } from '@/components/common/ToneBadge'

import type { OwnershipStatus } from './types'

export const OWNERSHIP_STATUS_LABEL: Record<OwnershipStatus, string> = {
  CURRENT: 'Đang sở hữu',
  UPCOMING: 'Sắp tới',
  PAST: 'Đã thôi',
}

export const OWNERSHIP_STATUS_TONE: Record<OwnershipStatus, Tone> = { CURRENT: 'success', UPCOMING: 'warning', PAST: 'neutral' }
