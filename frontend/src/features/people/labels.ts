import type { Tone } from '@/components/common/ToneBadge'

import type { Gender, OccupancyStatus, Relation } from './types'

export const RELATION_LABEL: Record<Relation, string> = {
  OWNER: 'Chủ',
  RESIDENT: 'Người ở',
  GUEST: 'Khách',
  STAFF: 'Nhân viên',
  OTHER: 'Khác',
}

export const STATUS_LABEL: Record<OccupancyStatus, string> = {
  CURRENT: 'Đang ở',
  UPCOMING: 'Sắp đến',
  PAST: 'Đã rời',
}

export const GENDER_LABEL: Record<Gender, string> = { MALE: 'Nam', FEMALE: 'Nữ', OTHER: 'Khác' }

/** Nghĩa màu của một dòng cư trú: đang ở, sắp đến (cần chuẩn bị), đã rời. */
export const OCCUPANCY_STATUS_TONE: Record<OccupancyStatus, Tone> = { CURRENT: 'success', UPCOMING: 'warning', PAST: 'neutral' }
