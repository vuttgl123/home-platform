import type { Tone } from '@/components/common/ToneBadge'
import { formatNumber } from '@/lib/format'

/** Chữ mô tả sức chứa và số người đang ở; dùng cả ở trang in (không màu). */
export function capacityText(capacity: number | null | undefined, occupants: number): string {
  if (capacity == null) return `Chưa đặt · đang ở ${formatNumber(occupants)}`
  const ratio = `${formatNumber(occupants)}/${formatNumber(capacity)}`
  if (occupants < capacity) return `${ratio} người · còn ${formatNumber(capacity - occupants)} chỗ`
  if (occupants === capacity) return `${ratio} · đã đầy`
  return `${ratio} · vượt ${formatNumber(occupants - capacity)}`
}

/** Màu của sức chứa: còn chỗ, đã đầy, vượt; chưa đặt thì không có màu. */
export function capacityTone(capacity: number | null | undefined, occupants: number): Tone | null {
  if (capacity == null) return null
  if (occupants < capacity) return 'success'
  return occupants === capacity ? 'warning' : 'danger'
}
