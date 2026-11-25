import { ToneBadge } from '@/components/common/ToneBadge'

import { capacityText, capacityTone } from '../capacity'

/** Sức chứa của một địa điểm so với số người đang ở hôm nay (spec Đợt 6, L1 và L2). */
export function CapacityLine({ capacity, occupants }: { capacity: number | null | undefined; occupants: number }) {
  const tone = capacityTone(capacity, occupants)
  const text = capacityText(capacity, occupants)
  return tone ? (
    <ToneBadge tone={tone} className="tabular-nums">
      {text}
    </ToneBadge>
  ) : (
    <span className="text-muted-foreground tabular-nums">{text}</span>
  )
}
