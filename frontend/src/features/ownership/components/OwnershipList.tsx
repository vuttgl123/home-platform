import type { ReactNode } from 'react'
import { Link } from 'react-router'

import type { CursorList } from '@/api'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadMoreButton } from '@/components/common/LoadMoreButton'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { formatDate } from '@/lib/format'

import { OWNERSHIP_STATUS_LABEL } from '../labels'
import type { Ownership } from '../types'
import { formatShare } from './ownershipForm'

/**
 * Bảng các dòng sở hữu. Cột đầu là người (tab Sở hữu của địa điểm) hoặc địa điểm (trang một người);
 * `showBoth` thêm cột địa điểm khi xem cả toà nhà; `showStatus` thêm cột trạng thái.
 * @param showTotal thêm dòng tổng tỷ lệ khi danh sách đã tải hết và có ít nhất một dòng ghi tỷ lệ
 */
export function OwnershipList({
  ws,
  list,
  show,
  showBoth = false,
  showStatus = false,
  showTotal = false,
  empty,
  actions,
}: {
  ws: string
  list: CursorList<Ownership>
  show: 'place' | 'person'
  showBoth?: boolean
  showStatus?: boolean
  showTotal?: boolean
  empty: string
  actions?: (row: Ownership) => ReactNode
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
  const placeLink = (row: Ownership) => (
    <Link to={`/w/${ws}/places/${row.placeId}`} className="font-medium hover:underline">
      {row.placeName}
    </Link>
  )
  const personLink = (row: Ownership) => (
    <Link to={`/w/${ws}/people/${row.personId}`} className="font-medium hover:underline">
      {row.personFullName}
    </Link>
  )
  const shares = list.items.filter((row) => row.sharePercent != null)
  const total = showTotal && !list.hasMore && shares.length > 0 ? shares.reduce((sum, row) => sum + row.sharePercent!, 0) : null
  const columns = 4 + (showBoth ? 1 : 0) + (showStatus ? 1 : 0) + (actions ? 1 : 0)

  return (
    <div className="flex flex-col gap-2">
      <div className="overflow-x-auto rounded-lg border">
        <table className="w-full text-sm">
          <thead className="bg-muted/50 text-left text-muted-foreground">
            <tr>
              <th className="px-3 py-2 font-medium">{show === 'place' ? 'Địa điểm' : 'Người'}</th>
              {showBoth && <th className="px-3 py-2 font-medium">Chỗ</th>}
              <th className="px-3 py-2 font-medium">Tỷ lệ</th>
              <th className="px-3 py-2 font-medium">Từ ngày</th>
              <th className="px-3 py-2 font-medium">Tới ngày</th>
              {showStatus && <th className="px-3 py-2 font-medium">Trạng thái</th>}
              <th className="px-3 py-2 font-medium">Giấy tờ</th>
              {actions && <th className="px-3 py-2 font-medium sr-only">Thao tác</th>}
            </tr>
          </thead>
          <tbody className="divide-y">
            {list.items.map((row) => (
              <tr key={row.id}>
                <td className="px-3 py-2">{show === 'place' ? placeLink(row) : personLink(row)}</td>
                {showBoth && <td className="px-3 py-2">{placeLink(row)}</td>}
                <td className="px-3 py-2 whitespace-nowrap">{formatShare(row.sharePercent)}</td>
                <td className="px-3 py-2 whitespace-nowrap">{formatDate(row.startDate)}</td>
                <td className="px-3 py-2 whitespace-nowrap">{row.endDate ? formatDate(row.endDate) : '—'}</td>
                {showStatus && (
                  <td className="px-3 py-2">
                    <Badge variant={row.status === 'CURRENT' ? 'secondary' : 'outline'}>{OWNERSHIP_STATUS_LABEL[row.status]}</Badge>
                  </td>
                )}
                <td className="px-3 py-2">{row.documentNo ?? '—'}</td>
                {actions && <td className="px-3 py-2 text-right">{actions(row)}</td>}
              </tr>
            ))}
          </tbody>
          {total != null && (
            <tfoot className="border-t bg-muted/30">
              <tr>
                <td className="px-3 py-2 font-medium">Tổng</td>
                {showBoth && <td />}
                <td className="px-3 py-2 font-medium whitespace-nowrap">{formatShare(total)}</td>
                <td colSpan={columns - (showBoth ? 3 : 2)} />
              </tr>
            </tfoot>
          )}
        </table>
      </div>
      {list.hasMore && <LoadMoreButton onClick={list.loadMore} isLoading={list.isLoadingMore} />}
    </div>
  )
}
