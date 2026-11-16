import { Link } from 'react-router'

import type { CursorList } from '@/api'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadMoreButton } from '@/components/common/LoadMoreButton'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { formatDate } from '@/lib/format'

import { ITEM_STATUS_LABEL } from '../labels'
import type { ItemSummary } from '../types'

/**
 * Bảng đồ đạc. `showPlace` thêm cột địa điểm (khi xem cả toà nhà); `removed` thay cột trạng thái
 * bằng ngày gỡ.
 */
export function ItemList({
  ws,
  list,
  empty,
  showPlace = false,
  removed = false,
}: {
  ws: string
  list: CursorList<ItemSummary>
  empty: string
  showPlace?: boolean
  removed?: boolean
}) {
  if (list.isLoading) {
    return (
      <div className="flex flex-col gap-2" aria-label="Đang tải">
        <Skeleton className="h-9" />
        <Skeleton className="h-9" />
      </div>
    )
  }
  if (list.error) {
    return <ErrorState error={list.error} onRetry={list.refetch} />
  }
  if (list.items.length === 0) {
    return <p className="py-2 text-sm text-muted-foreground">{empty}</p>
  }
  return (
    <div className="flex flex-col gap-2">
      <div className="overflow-x-auto rounded-lg border">
        <table className="w-full text-sm">
          <thead className="bg-muted/50 text-left text-muted-foreground">
            <tr>
              <th className="px-3 py-2 font-medium">Tên</th>
              {showPlace && <th className="px-3 py-2 font-medium">Ở tại</th>}
              <th className="px-3 py-2 font-medium">Nhóm</th>
              <th className="px-3 py-2 font-medium">Số lượng</th>
              <th className="px-3 py-2 font-medium">{removed ? 'Ngày gỡ' : 'Trạng thái'}</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {list.items.map((item) => (
              <tr key={item.id}>
                <td className="px-3 py-2">
                  <Link to={`/w/${ws}/items/${item.id}`} className="font-medium hover:underline">
                    {item.name}
                  </Link>
                  {item.code && <span className="ml-2 text-xs text-muted-foreground">{item.code}</span>}
                  {item.ownerName && <span className="block text-xs text-muted-foreground">{`của ${item.ownerName}`}</span>}
                </td>
                {showPlace && (
                  <td className="px-3 py-2">
                    <Link to={`/w/${ws}/places/${item.placeId}`} className="hover:underline">
                      {item.placeName}
                    </Link>
                  </td>
                )}
                <td className="px-3 py-2">{item.categoryName ?? '—'}</td>
                <td className="px-3 py-2 whitespace-nowrap">{[item.quantity, item.unit].filter((part) => part != null).join(' ')}</td>
                <td className="px-3 py-2 whitespace-nowrap">
                  {removed ? (
                    item.removedAt ? formatDate(item.removedAt) : '—'
                  ) : (
                    <Badge variant={item.status === 'BROKEN' ? 'destructive' : 'secondary'}>{ITEM_STATUS_LABEL[item.status]}</Badge>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {list.hasMore && <LoadMoreButton onClick={list.loadMore} isLoading={list.isLoadingMore} />}
    </div>
  )
}
