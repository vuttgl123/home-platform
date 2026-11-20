import { Link } from 'react-router'

import { ToneBadge } from '@/components/common/ToneBadge'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadMoreButton } from '@/components/common/LoadMoreButton'
import { Skeleton } from '@/components/ui/skeleton'
import { usePlaceTypes } from '@/features/catalog'

import { PLACE_STATUS_LABEL, PLACE_STATUS_TONE } from '../labels'
import { useSubtree } from '../queries'
import { placeUrl } from '../routes'
import type { PlaceDetail } from '../types'

/** Bảng mọi chỗ bên trong một địa điểm, theo thứ tự cây (cha trước con), tên thụt theo cấp. */
export function PlaceSubtreeTable({ ws, place }: { ws: string; place: PlaceDetail }) {
  const subtree = useSubtree(ws, place.id, true)
  const types = usePlaceTypes(ws).data ?? []

  if (subtree.isLoading) {
    return (
      <div className="flex flex-col gap-2" aria-label="Đang tải">
        <Skeleton className="h-9" />
        <Skeleton className="h-9" />
      </div>
    )
  }
  if (subtree.error) {
    return <ErrorState error={subtree.error} onRetry={subtree.refetch} />
  }
  return (
    <div className="flex flex-col gap-2">
      <div className="overflow-x-auto rounded-lg border">
        <table className="w-full text-sm" aria-label="Cả bên trong">
          <thead className="bg-muted/50 text-left text-muted-foreground">
            <tr>
              <th className="px-3 py-2 font-medium">Tên</th>
              <th className="px-3 py-2 font-medium">Loại</th>
              <th className="px-3 py-2 font-medium">Mã</th>
              <th className="px-3 py-2 font-medium">Trạng thái</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {subtree.items.map((row) => (
              <tr key={row.id}>
                <td className="py-2 pr-3" style={{ paddingLeft: `${0.75 + Math.max(0, row.depth - place.depth - 1)}rem` }}>
                  <Link to={placeUrl(ws, row.id)} className="font-medium hover:underline">
                    {row.name}
                  </Link>
                </td>
                <td className="px-3 py-2">{types.find((type) => type.id === row.placeTypeId)?.name ?? '—'}</td>
                <td className="px-3 py-2 font-mono text-xs">{row.code ?? '—'}</td>
                <td className="px-3 py-2">
                  <ToneBadge tone={PLACE_STATUS_TONE[row.status]}>{PLACE_STATUS_LABEL[row.status]}</ToneBadge>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {subtree.hasMore && <LoadMoreButton onClick={subtree.loadMore} isLoading={subtree.isLoadingMore} />}
    </div>
  )
}
