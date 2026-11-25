import type { Tone } from '@/components/common/ToneBadge'

import type { MaintenanceKind } from './types'

export const MAINTENANCE_KIND_LABEL: Record<MaintenanceKind, string> = { REPAIR: 'Sửa chữa', SERVICE: 'Bảo trì' }

/** Sửa chữa là lúc có chuyện cần để ý; bảo trì định kỳ là việc bình thường. */
export const MAINTENANCE_KIND_TONE: Record<MaintenanceKind, Tone> = { REPAIR: 'warning', SERVICE: 'info' }
