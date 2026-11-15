import { useCallback, useMemo, useState } from 'react'
import { useMatch } from 'react-router'

import { Button } from '@/components/ui/button'
import { SidebarInput } from '@/components/ui/sidebar'
import { useDebouncedValue } from '@/hooks/use-debounced-value'

import { useChildren, usePlace } from '../queries'
import { PLACE_ROUTE } from '../routes'
import { PlaceSearchResults } from './PlaceSearchResults'
import { PlaceTreeRows } from './PlaceTreeNode'
import { TreeContext, type TreeState } from './treeContext'

/**
 * Cây địa điểm của một không gian, nằm ở thanh bên.
 *
 * Mỗi cấp chỉ được tải khi nút cha được mở, từng trang một, nên toà nhà nghìn phòng không bị
 * tải hết một lần. Địa điểm đang mở trên URL được tô, và các tổ tiên của nó tự mở ra.
 *
 * Trạng thái mở đóng nằm trong component này: nơi dùng đặt `key` theo không gian để nó được đặt
 * lại khi đổi không gian.
 */
export function PlaceTree({ workspaceId }: { workspaceId: string }) {
  const selectedId = useMatch(PLACE_ROUTE)?.params.placeId
  const selected = usePlace(workspaceId, selectedId).data
  const [expanded, setExpanded] = useState<ReadonlySet<string>>(() => new Set())

  // Mở sẵn các tổ tiên mỗi khi địa điểm đang chọn đổi (hoặc bị chuyển sang chỗ khác). Làm ngay
  // trong lúc render thay vì trong effect: không có lần vẽ nào mà cây còn đóng. Sau đó người
  // dùng đóng nhánh nào thì nhánh đó ở yên, cho tới lần chọn kế tiếp.
  const lineage = selected ? [...selected.ancestors.map((ancestor) => ancestor.id), selected.id].join('/') : null
  const [openedFor, setOpenedFor] = useState<string | null>(null)
  if (lineage !== openedFor) {
    setOpenedFor(lineage)
    if (selected) {
      setExpanded((previous) => new Set([...previous, ...selected.ancestors.map((ancestor) => ancestor.id)]))
    }
  }

  const toggle = useCallback((placeId: string) => {
    setExpanded((previous) => {
      const next = new Set(previous)
      if (!next.delete(placeId)) {
        next.add(placeId)
      }
      return next
    })
  }, [])

  const tree = useMemo<TreeState>(
    () => ({ ws: workspaceId, selectedId, selected, expanded, toggle }),
    [workspaceId, selectedId, selected, expanded, toggle],
  )

  const [term, setTerm] = useState('')
  const settledTerm = useDebouncedValue(term.trim(), 300)
  // Xoá ô tìm thì cây hiện lại ngay, không chờ hết thời gian trễ.
  const searchFor = term.trim() === '' ? '' : settledTerm

  const topLevel = useChildren(workspaceId, null)
  const isEmpty = topLevel.isLoaded && topLevel.items.length === 0 && !topLevel.hasMore && selected == null

  return (
    <TreeContext value={tree}>
      <nav aria-label="Cây địa điểm" className="flex min-h-0 flex-1 flex-col gap-2 p-2">
        <SidebarInput
          type="search"
          aria-label="Tìm địa điểm"
          placeholder="Tìm địa điểm…"
          value={term}
          onChange={(event) => setTerm(event.target.value)}
        />
        <div className="min-h-0 flex-1 overflow-y-auto">
          {searchFor !== '' ? (
            <PlaceSearchResults ws={workspaceId} term={searchFor} />
          ) : isEmpty ? (
            <div className="flex flex-col items-start gap-2 px-2 py-1.5">
              <p className="text-sm text-muted-foreground">Chưa có địa điểm</p>
              {/* Task 6 gắn form thêm địa điểm. */}
              <Button variant="outline" size="sm" disabled>
                Thêm địa điểm
              </Button>
            </div>
          ) : (
            <ul role="tree" aria-label="Địa điểm">
              <PlaceTreeRows parentId={null} list={topLevel} />
            </ul>
          )}
        </div>
      </nav>
    </TreeContext>
  )
}
