import type { PlaceDetail } from './types'

/** Nhãn trạng thái của một địa điểm. */
export const PLACE_STATUS_LABEL: Record<PlaceDetail['status'], string> = { ACTIVE: 'Đang dùng', INACTIVE: 'Tạm ngưng' }
