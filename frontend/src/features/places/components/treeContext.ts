import { createContext, useContext } from 'react'

import type { PlaceDetail } from '../types'

/** Trạng thái chung của cây, để mọi nút đọc được mà không phải truyền qua từng cấp. */
export type TreeState = {
  ws: string
  /** Địa điểm đang mở trên URL; có ngay cả khi chi tiết của nó chưa tải xong. */
  selectedId: string | undefined
  selected: PlaceDetail | undefined
  expanded: ReadonlySet<string>
  toggle: (placeId: string) => void
}

export const TreeContext = createContext<TreeState | null>(null)

export function useTree(): TreeState {
  const tree = useContext(TreeContext)
  if (!tree) {
    throw new Error('useTree must be used inside PlaceTree')
  }
  return tree
}
