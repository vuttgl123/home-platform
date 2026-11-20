import { Link } from 'react-router'

import { ToneBadge } from '@/components/common/ToneBadge'
import type { CursorList } from '@/api'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadMoreButton } from '@/components/common/LoadMoreButton'
import { Skeleton } from '@/components/ui/skeleton'
import { formatDate } from '@/lib/format'

import { ITEM_STATUS_LABEL, ITEM_STATUS_TONE } from '../labels'
import type { ItemSummary } from '../types'

/** Trên điện thoại, ô phụ (chỗ, nhóm, số lượng) thành một dòng chữ nhỏ có nhãn đứng trước. */
const PHONE_LABEL = "max-sm:p-0 max-sm:col-span-2 max-sm:text-xs max-sm:text-muted-foreground max-sm:before:mr-1 max-sm:before:content-[attr(data-label)_':']"

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
      {/* Trên điện thoại mỗi dòng thành một thẻ: tên và trạng thái ở trên, chỗ, nhóm, số lượng ở dưới. */}
      <div className="overflow-x-auto rounded-lg border bg-card">
        <table className="w-full text-sm max-sm:block">
          <thead className="bg-muted/50 text-left text-muted-foreground max-sm:sr-only">
            <tr>
              <th className="px-3 py-2 font-medium">Tên</th>
              {showPlace && <th className="px-3 py-2 font-medium">Ở tại</th>}
              <th className="px-3 py-2 font-medium">Nhóm</th>
              <th className="px-3 py-2 font-medium">Số lượng</th>
              <th className="px-3 py-2 font-medium">{removed ? 'Ngày gỡ' : 'Trạng thái'}</th>
            </tr>
          </thead>
          <tbody className="divide-y max-sm:block">
            {list.items.map((item) => (
              <tr key={item.id} className="max-sm:grid max-sm:grid-cols-[1fr_auto] max-sm:gap-x-3 max-sm:gap-y-0.5 max-sm:px-3 max-sm:py-3">
                <td className="px-3 py-2 max-sm:p-0">
                  <Link to={`/w/${ws}/items/${item.id}`} className="font-medium hover:underline">
                    {item.name}
                  </Link>
                  {item.code && <span className="ml-2 text-xs whitespace-nowrap text-muted-foreground tabular-nums">{item.code}</span>}
                  {item.ownerName && <span className="block text-xs text-muted-foreground">{`của ${item.ownerName}`}</span>}
                </td>
                {showPlace && (
                  <td data-label="Ở tại" className={`px-3 py-2 ${PHONE_LABEL}`}>
                    <Link to={`/w/${ws}/places/${item.placeId}`} className="hover:underline">
                      {item.placeName}
                    </Link>
                  </td>
                )}
                <td data-label="Nhóm" className={`px-3 py-2 ${PHONE_LABEL}`}>
                  {item.categoryName ?? '—'}
                </td>
                <td data-label="Số lượng" className={`px-3 py-2 whitespace-nowrap ${PHONE_LABEL}`}>
                  {[item.quantity, item.unit].filter((part) => part != null).join(' ')}
                </td>
                <td className="px-3 py-2 whitespace-nowrap max-sm:col-start-2 max-sm:row-start-1 max-sm:justify-self-end max-sm:p-0">
                  {removed ? (
                    item.removedAt ? formatDate(item.removedAt) : '—'
                  ) : (
                    <ToneBadge tone={ITEM_STATUS_TONE[item.status]}>{ITEM_STATUS_LABEL[item.status]}</ToneBadge>
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
