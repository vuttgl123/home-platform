import type { Tone } from '@/components/common/ToneBadge'

import type { PlaceDetail } from './types'

/** Nhãn trạng thái của một địa điểm. */
export const PLACE_STATUS_LABEL: Record<PlaceDetail['status'], string> = { ACTIVE: 'Đang dùng', INACTIVE: 'Tạm ngưng' }

export const PLACE_STATUS_TONE: Record<PlaceDetail['status'], Tone> = { ACTIVE: 'success', INACTIVE: 'neutral' }
