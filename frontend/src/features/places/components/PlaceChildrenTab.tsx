import { ChevronRightIcon } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'

import { ErrorState } from '@/components/common/ErrorState'
import { LoadMoreButton } from '@/components/common/LoadMoreButton'
import { Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import { Switch } from '@/components/ui/switch'

import { useChildren } from '../queries'
import { placeUrl } from '../routes'
import { PlaceSubtreeTable } from './PlaceSubtreeTable'
import type { PlaceDetail } from '../types'

/**
 * Tab "Bên trong": các chỗ con của địa điểm (dùng chung dữ liệu với nhánh của nó trên cây), hoặc
 * với công tắc "Cả bên trong", mọi chỗ bên trong ở dạng bảng.
 * @param addAction nút thêm chỗ con
 */
export function PlaceChildrenTab({ ws, place, addAction }: { ws: string; place: PlaceDetail; addAction?: React.ReactNode }) {
  const children = useChildren(ws, place.id)
  const [whole, setWhole] = useState(false)

  if (children.isLoading) {
    return (
      <div className="flex max-w-3xl flex-col gap-2" aria-label="Đang tải">
        <Skeleton className="h-9" />
        <Skeleton className="h-9" />
      </div>
    )
  }
  if (children.error) {
    return <ErrorState error={children.error} onRetry={children.refetch} />
  }
  if (children.items.length === 0) {
    return (
      <Empty>
        <EmptyHeader>
          <EmptyTitle>Chưa có chỗ nào bên trong</EmptyTitle>
          <EmptyDescription>Tầng, phòng hay khu vực nằm trong {place.name} sẽ hiện ở đây.</EmptyDescription>
        </EmptyHeader>
        {addAction && <EmptyContent>{addAction}</EmptyContent>}
      </Empty>
    )
  }
  return (
    <div className="flex max-w-3xl flex-col gap-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <label className="flex items-center gap-2 text-sm">
          <Switch checked={whole} onCheckedChange={setWhole} aria-label="Cả bên trong" />
          Cả bên trong
        </label>
        {addAction}
      </div>
      {whole ? (
        <PlaceSubtreeTable ws={ws} place={place} />
      ) : (
        <ChildList ws={ws} list={children} />
      )}
    </div>
  )
}

function ChildList({ ws, list: children }: { ws: string; list: ReturnType<typeof useChildren> }) {
  return (
    <div className="flex flex-col gap-3">
      <ul className="divide-y rounded-lg border">
        {children.items.map((child) => (
          <li key={child.id}>
            <Link
              to={placeUrl(ws, child.id)}
              className="flex items-center gap-3 px-3 py-2 outline-none hover:bg-muted focus-visible:bg-muted"
            >
              <span className="min-w-0 flex-1 truncate">{child.name}</span>
              {child.code && <span className="shrink-0 text-xs text-muted-foreground">{child.code}</span>}
              <ChevronRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
            </Link>
          </li>
        ))}
      </ul>
      {children.hasMore && <LoadMoreButton onClick={children.loadMore} isLoading={children.isLoadingMore} />}
    </div>
  )
}
