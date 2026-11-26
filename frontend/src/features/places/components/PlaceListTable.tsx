import { Link } from 'react-router'

import { ErrorState } from '@/components/common/ErrorState'
import { LoadMoreButton } from '@/components/common/LoadMoreButton'
import { ToneBadge } from '@/components/common/ToneBadge'
import { Skeleton } from '@/components/ui/skeleton'
import type { CursorList } from '@/api'

import { PLACE_STATUS_LABEL, PLACE_STATUS_TONE } from '../labels'
import type { PlaceRow } from '../types'

/** Trên điện thoại, ô phụ thành một dòng chữ nhỏ có nhãn đứng trước (như danh sách chỗ trống). */
const PHONE_LABEL = "max-sm:p-0 max-sm:col-span-2 max-sm:text-xs max-sm:text-muted-foreground max-sm:before:mr-1 max-sm:before:content-[attr(data-label)_':']"

/** Bảng địa điểm (spec Đợt 11, S3): mã, tên, loại, nằm trong, sức chứa, đang ở, trạng thái. */
export function PlaceListTable({ ws, list }: { ws: string; list: CursorList<PlaceRow> }) {
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
    return <p className="py-2 text-sm text-muted-foreground">Không có địa điểm nào khớp bộ lọc</p>
  }
  return (
    <div className="flex flex-col gap-2">
      <div className="overflow-x-auto rounded-lg border bg-card">
        <table className="w-full text-sm max-sm:block">
          <thead className="bg-muted/50 text-left text-muted-foreground max-sm:sr-only">
            <tr>
              <th className="px-3 py-2 font-medium">Mã</th>
              <th className="px-3 py-2 font-medium">Tên</th>
              <th className="px-3 py-2 font-medium">Loại</th>
              <th className="px-3 py-2 font-medium">Nằm trong</th>
              <th className="px-3 py-2 text-right font-medium">Sức chứa</th>
              <th className="px-3 py-2 text-right font-medium">Đang ở</th>
              <th className="px-3 py-2 font-medium">Trạng thái</th>
            </tr>
          </thead>
          <tbody className="divide-y max-sm:block">
            {list.items.map((place) => (
              <tr key={place.id} className="max-sm:grid max-sm:grid-cols-[1fr_auto] max-sm:gap-x-3 max-sm:gap-y-1 max-sm:px-3 max-sm:py-3">
                <td data-label="Mã" className={`px-3 py-2 whitespace-nowrap tabular-nums text-muted-foreground ${PHONE_LABEL}`}>
                  {place.code ?? '—'}
                </td>
                <td className="px-3 py-2 max-sm:order-first max-sm:p-0">
                  <Link to={`/w/${ws}/places/${place.id}`} className="font-medium hover:underline">
                    {place.name}
                  </Link>
                </td>
                <td data-label="Loại" className={`px-3 py-2 ${PHONE_LABEL}`}>
                  {place.placeTypeName ?? '—'}
                </td>
                <td data-label="Nằm trong" className={`px-3 py-2 ${PHONE_LABEL}`}>
                  {place.ancestors.length > 0 ? place.ancestors.map((ancestor) => ancestor.name).join(' › ') : '—'}
                </td>
                <td data-label="Sức chứa" className={`px-3 py-2 text-right tabular-nums max-sm:text-left ${PHONE_LABEL}`}>
                  {place.capacity ?? '—'}
                </td>
                <td data-label="Đang ở" className={`px-3 py-2 text-right tabular-nums max-sm:text-left ${PHONE_LABEL}`}>
                  {place.occupantCount}
                </td>
                <td className="px-3 py-2 max-sm:order-first max-sm:col-start-2 max-sm:row-start-1 max-sm:p-0">
                  <ToneBadge tone={PLACE_STATUS_TONE[place.status]}>{PLACE_STATUS_LABEL[place.status]}</ToneBadge>
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
