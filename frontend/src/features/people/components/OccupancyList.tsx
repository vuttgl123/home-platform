import type { ReactNode } from 'react'
import { Link } from 'react-router'

import { PersonAvatar } from '@/components/common/PersonAvatar'
import { ToneBadge } from '@/components/common/ToneBadge'
import type { CursorList } from '@/api'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadMoreButton } from '@/components/common/LoadMoreButton'
import { Skeleton } from '@/components/ui/skeleton'
import { formatDate } from '@/lib/format'

import { OCCUPANCY_STATUS_TONE, RELATION_LABEL, STATUS_LABEL } from '../labels'
import { PersonHoverCard } from './PersonHoverCard'
import type { Occupancy } from '../types'

/**
 * Bảng các dòng cư trú. Cột đầu là địa điểm (trang một người) hoặc người (tab Người ở của một
 * địa điểm); `showBoth` thêm cột địa điểm khi xem cả toà nhà.
 *
 * @param actions các nút thao tác của một dòng
 */
export function OccupancyList({
  ws,
  list,
  show,
  showBoth = false,
  empty,
  actions,
}: {
  ws: string
  list: CursorList<Occupancy>
  show: 'place' | 'person'
  showBoth?: boolean
  empty: string
  actions?: (row: Occupancy) => ReactNode
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

  const placeLink = (row: Occupancy) => (
    <Link to={`/w/${ws}/places/${row.placeId}`} className="font-medium hover:underline">
      {row.placeName}
    </Link>
  )
  const personLink = (row: Occupancy) => (
    <span className="inline-flex items-center gap-2">
      <PersonAvatar name={row.personFullName} size="sm" />
      <PersonHoverCard ws={ws} personId={row.personId} name={row.personFullName} />
    </span>
  )

  return (
    <div className="flex flex-col gap-2">
      <div className="overflow-x-auto rounded-lg border">
        <table className="w-full text-sm">
          <thead className="bg-muted/50 text-left text-muted-foreground">
            <tr>
              <th className="px-3 py-2 font-medium">{show === 'place' ? 'Địa điểm' : 'Người'}</th>
              {showBoth && <th className="px-3 py-2 font-medium">Ở tại</th>}
              <th className="px-3 py-2 font-medium">Quan hệ</th>
              <th className="px-3 py-2 font-medium">Từ ngày</th>
              <th className="px-3 py-2 font-medium">Tới ngày</th>
              <th className="px-3 py-2 font-medium">Trạng thái</th>
              {actions && <th className="px-3 py-2 font-medium sr-only">Thao tác</th>}
            </tr>
          </thead>
          <tbody className="divide-y">
            {list.items.map((row) => (
              <tr key={row.id}>
                <td className="px-3 py-2">{show === 'place' ? placeLink(row) : personLink(row)}</td>
                {showBoth && <td className="px-3 py-2">{placeLink(row)}</td>}
                <td className="px-3 py-2">{RELATION_LABEL[row.relation]}</td>
                <td className="px-3 py-2 whitespace-nowrap">{formatDate(row.startDate)}</td>
                <td className="px-3 py-2 whitespace-nowrap">{row.endDate ? formatDate(row.endDate) : '—'}</td>
                <td className="px-3 py-2">
                  <ToneBadge tone={OCCUPANCY_STATUS_TONE[row.status]}>{STATUS_LABEL[row.status]}</ToneBadge>
                </td>
                {actions && <td className="px-3 py-2 text-right">{actions(row)}</td>}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {list.hasMore && <LoadMoreButton onClick={list.loadMore} isLoading={list.isLoadingMore} />}
    </div>
  )
}
