import { ChevronRightIcon } from 'lucide-react'
import { cn } from 'cn'
import { Link } from 'react-router'

import type { CursorList } from '@/api'
import { LoadMoreButton } from '@/components/common/LoadMoreButton'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'

import { useChildren } from '../queries'
import { placeUrl } from '../routes'
import type { PlaceSummary } from '../types'
import { useTree } from './treeContext'

/**
 * Các dòng của một cấp: con của `parentId` (`null` là cấp cao nhất), kèm trạng thái đang tải,
 * lỗi và dòng "Tải thêm". Trả về các `<li>`; nơi gọi bọc chúng trong `<ul>`.
 */
export function PlaceTreeRows({
  parentId,
  list,
  level,
}: {
  parentId: string | null
  list: CursorList<PlaceSummary>
  level: number
}) {
  const { selected } = useTree()

  if (list.isLoading) {
    return (
      <li role="none" className="flex flex-col gap-1.5 px-2 py-1.5">
        <span className="sr-only">Đang tải</span>
        <Skeleton className="h-4 w-3/4" />
        <Skeleton className="h-4 w-1/2" />
      </li>
    )
  }
  if (list.error) {
    return (
      <li role="none" className="flex items-center justify-between gap-2 px-2 py-1 text-sm text-muted-foreground">
        Không tải được
        <Button variant="outline" size="xs" onClick={list.refetch}>
          Thử lại
        </Button>
      </li>
    )
  }

  // Địa điểm đang mở thuộc cấp này nhưng nằm ở một trang chưa tải (mở thẳng một phòng trong nhánh
  // nghìn phòng): vẫn cho nó một dòng, để cây luôn chỉ ra mình đang ở đâu. Trang chứa nó tải về
  // thì nó có trong `items` và dòng thêm này tự mất.
  const pinned =
    selected && (selected.parentId ?? null) === parentId && !list.items.some((item) => item.id === selected.id)
      ? selected
      : null

  return (
    <>
      {list.items.map((place) => (
        <PlaceTreeNode key={place.id} place={place} level={level} />
      ))}
      {pinned && <PlaceTreeNode key={pinned.id} place={pinned} level={level} />}
      {list.hasMore && (
        <li role="none">
          <LoadMoreButton onClick={list.loadMore} isLoading={list.isLoadingMore} />
        </li>
      )}
    </>
  )
}

/** Một địa điểm trên cây: mũi tên mở đóng, tên là liên kết tới trang của nó, và các con khi mở. */
function PlaceTreeNode({
  place,
  level,
}: {
  place: Pick<PlaceSummary, 'id' | 'name'> & { hasChildren?: boolean }
  level: number
}) {
  const { ws, selectedId, selected, expanded, toggle, tabStopId } = useTree()
  const isOpen = expanded.has(place.id)
  const isSelected = selectedId === place.id
  // Địa điểm đang mở là con của nút này thì nút này không phải lá, dù danh sách có nói gì.
  const hasSelectedChild = selected?.parentId === place.id
  // `hasChildren` có trong mọi danh sách; dòng ghim từ trang chi tiết thì không biết, coi như có thể có.
  const knownLeaf = place.hasChildren === false && !hasSelectedChild
  // Luôn theo dõi cache của danh sách con, nhưng chỉ tải khi nút được mở và có thể có con.
  const children = useChildren(ws, place.id, { enabled: isOpen && !knownLeaf })
  // Danh sách đã tải mà trống (ví dụ con vừa bị xoá ở tab khác) cũng là lá.
  const isLeaf =
    knownLeaf || (children.isLoaded && children.items.length === 0 && !children.hasMore && !hasSelectedChild)

  return (
    <li
      role="treeitem"
      aria-label={place.name}
      aria-level={level}
      aria-expanded={isLeaf ? undefined : isOpen}
      aria-selected={isSelected}
      tabIndex={tabStopId === place.id ? 0 : -1}
      data-tree-row=""
      data-id={place.id}
      data-name={place.name}
      // Vòng focus chỉ trên dòng của chính nó, không lan xuống các dòng con lồng bên trong.
      className="outline-none [&:focus-visible>div]:ring-2 [&:focus-visible>div]:ring-sidebar-ring [&:focus-visible>div]:ring-inset"
    >
      <div
        className={cn(
          'flex h-7 items-center rounded-md pr-1 text-sm hover:bg-sidebar-accent hover:text-sidebar-accent-foreground',
          isSelected && 'bg-sidebar-accent font-medium text-sidebar-accent-foreground',
        )}
      >
        {isLeaf ? (
          <span className="flex size-6 shrink-0 items-center justify-center" aria-hidden="true">
            <span className="size-1 rounded-full bg-muted-foreground/50" />
          </span>
        ) : (
          <button
            type="button"
            className="flex size-6 shrink-0 items-center justify-center rounded-md text-muted-foreground outline-none hover:text-foreground focus-visible:ring-2 focus-visible:ring-sidebar-ring"
            aria-label={`${isOpen ? 'Đóng' : 'Mở'} ${place.name}`}
            aria-expanded={isOpen}
            tabIndex={-1}
            onClick={() => toggle(place.id)}
          >
            <ChevronRightIcon
              className={cn('size-3.5 transition-transform motion-reduce:transition-none', isOpen && 'rotate-90')}
              aria-hidden="true"
            />
          </button>
        )}
        <Link
          to={placeUrl(ws, place.id)}
          aria-current={isSelected ? 'page' : undefined}
          tabIndex={-1}
          className="min-w-0 flex-1 truncate rounded-sm py-1 outline-none focus-visible:ring-2 focus-visible:ring-sidebar-ring"
        >
          {place.name}
        </Link>
      </div>
      {isOpen && !isLeaf && (
        <ul role="group" className="ml-3 border-l border-sidebar-border pl-1">
          <PlaceTreeRows parentId={place.id} list={children} level={level + 1} />
        </ul>
      )}
    </li>
  )
}
