import { Link } from 'react-router'

import { ErrorState } from '@/components/common/ErrorState'
import { LoadMoreButton } from '@/components/common/LoadMoreButton'
import { Button } from '@/components/ui/button'
import { Progress } from '@/components/ui/progress'
import { Skeleton } from '@/components/ui/skeleton'
import type { CursorList } from '@/api'
import { capacityText } from '@/features/places'

import type { Vacancy } from '../types'

/** Trên điện thoại, ô phụ thành một dòng chữ nhỏ có nhãn đứng trước (như danh sách đồ đạc). */
const PHONE_LABEL = "max-sm:p-0 max-sm:col-span-2 max-sm:text-xs max-sm:text-muted-foreground max-sm:before:mr-1 max-sm:before:content-[attr(data-label)_':']"

/** Bảng chỗ trống; mỗi dòng có nút "Xếp người" mở form thêm người ở tại đúng chỗ đó. */
export function VacancyList({
  ws,
  list,
  onSeat,
}: {
  ws: string
  list: CursorList<Vacancy>
  onSeat: (vacancy: Vacancy) => void
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
    return <p className="py-2 text-sm text-muted-foreground">Không còn chỗ trống nào khớp bộ lọc</p>
  }
  return (
    <div className="flex flex-col gap-2">
      <div className="overflow-x-auto rounded-lg border bg-card">
        <table className="w-full text-sm max-sm:block">
          <thead className="bg-muted/50 text-left text-muted-foreground max-sm:sr-only">
            <tr>
              <th className="px-3 py-2 font-medium">Tên</th>
              <th className="px-3 py-2 font-medium">Nằm trong</th>
              <th className="px-3 py-2 font-medium">Loại</th>
              <th className="px-3 py-2 font-medium">Sức chứa</th>
              <th className="px-3 py-2">
                <span className="sr-only">Thao tác</span>
              </th>
            </tr>
          </thead>
          <tbody className="divide-y max-sm:block">
            {list.items.map((vacancy) => (
              <tr key={vacancy.id} className="max-sm:grid max-sm:grid-cols-[1fr_auto] max-sm:gap-x-3 max-sm:gap-y-1 max-sm:px-3 max-sm:py-3">
                <td className="px-3 py-2 max-sm:p-0">
                  <Link to={`/w/${ws}/places/${vacancy.id}`} className="font-medium hover:underline">
                    {vacancy.name}
                  </Link>
                  {vacancy.code && <span className="ml-2 text-xs whitespace-nowrap text-muted-foreground tabular-nums">{vacancy.code}</span>}
                </td>
                <td data-label="Nằm trong" className={`px-3 py-2 ${PHONE_LABEL}`}>
                  {vacancy.ancestors.length > 0 ? vacancy.ancestors.map((ancestor) => ancestor.name).join(' › ') : '—'}
                </td>
                <td data-label="Loại" className={`px-3 py-2 ${PHONE_LABEL}`}>
                  {vacancy.placeTypeName ?? '—'}
                </td>
                <td className="px-3 py-2 max-sm:col-span-2 max-sm:p-0">
                  <Room vacancy={vacancy} />
                </td>
                <td className="px-3 py-2 text-right max-sm:col-start-2 max-sm:row-start-1 max-sm:p-0">
                  <Button size="sm" variant="outline" aria-label={`Xếp người vào ${vacancy.name}`} onClick={() => onSeat(vacancy)}>
                    Xếp người
                  </Button>
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

/** Còn mấy chỗ: thanh đang ở/sức chứa kèm chữ; chưa đặt sức chứa thì chỉ ghi là đang trống. */
function Room({ vacancy }: { vacancy: Vacancy }) {
  if (vacancy.capacity == null) {
    return <span className="text-muted-foreground">Trống, chưa đặt sức chứa</span>
  }
  const percent = vacancy.capacity === 0 ? 100 : Math.round((vacancy.occupantCount / vacancy.capacity) * 100)
  return (
    <div className="flex min-w-40 flex-col gap-1">
      <Progress value={percent} aria-label={`Đã ở ${percent}%`} className="[&_[data-slot=progress-track]]:h-1.5" />
      <span className="tabular-nums">{capacityText(vacancy.capacity, vacancy.occupantCount)}</span>
    </div>
  )
}
