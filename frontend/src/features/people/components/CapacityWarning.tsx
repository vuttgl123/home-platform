import { TriangleAlertIcon } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { formatDate, formatNumber } from '@/lib/format'

import type { CapacityCheck } from '../types'

/**
 * Cảnh báo khi thêm một người sẽ vượt sức chứa (spec Đợt 6, L3): `peak + 1 > capacity`. Chỉ báo,
 * không chặn; địa điểm chưa đặt sức chứa thì không báo.
 */
export function CapacityWarning({ placeName, check }: { placeName: string; check: CapacityCheck | undefined }) {
  if (check?.capacity == null || check.peak + 1 <= check.capacity) return null
  const crowded = check.peakDate
    ? `; ngày ${formatDate(check.peakDate)} đã có ${formatNumber(check.peak)} người`
    : ''
  return (
    <Alert className="border-warning/40 bg-warning/10 text-foreground">
      <TriangleAlertIcon className="text-warning" />
      <AlertDescription className="text-foreground">
        {`${placeName} chứa ${formatNumber(check.capacity)} người${crowded}. Vẫn lưu được.`}
      </AlertDescription>
    </Alert>
  )
}
