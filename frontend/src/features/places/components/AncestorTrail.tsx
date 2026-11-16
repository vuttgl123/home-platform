import type { PlaceRef } from '../types'

/** Chuỗi tổ tiên dạng "Toà A › Tầng 3", để phân biệt hai địa điểm cùng tên. Không có tổ tiên thì không hiện gì. */
export function AncestorTrail({ ancestors, id }: { ancestors: PlaceRef[]; id?: string }) {
  if (ancestors.length === 0) {
    return null
  }
  return (
    <span id={id} className="block truncate text-xs text-muted-foreground">
      {ancestors.map((ancestor) => ancestor.name).join(' › ')}
    </span>
  )
}
