import { useId } from 'react'
import { Link } from 'react-router'

import { ErrorState } from '@/components/common/ErrorState'
import { Skeleton } from '@/components/ui/skeleton'

import { useRecentActivity } from '../queries'
import { ActivityRow } from './ActivityRow'

/** Mấy thay đổi mới nhất của không gian, cùng dòng với trang Hoạt động, kèm liên kết mở trang đó. */
export function RecentActivity({ ws }: { ws: string }) {
  const recent = useRecentActivity(ws)
  const headingId = useId()

  return (
    <section aria-labelledby={headingId} className="flex flex-col gap-2">
      <div className="flex items-baseline justify-between gap-3">
        <h2 id={headingId} className="font-semibold">
          Hoạt động gần đây
        </h2>
        <Link to={`/w/${ws}/activity`} className="text-sm font-medium text-primary underline-offset-4 hover:underline">
          Xem tất cả
        </Link>
      </div>
      {recent.isPending ? (
        <div className="flex flex-col gap-2" aria-label="Đang tải">
          <Skeleton className="h-10" />
          <Skeleton className="h-10" />
        </div>
      ) : recent.isError ? (
        <ErrorState error={recent.error} onRetry={() => void recent.refetch()} />
      ) : recent.data.length === 0 ? (
        <p className="text-sm text-muted-foreground">Chưa có hoạt động nào.</p>
      ) : (
        <ol className="flex flex-col divide-y rounded-xl border bg-card">
          {recent.data.map((entry) => (
            <ActivityRow key={`${entry.entityType}:${entry.entityId}:${entry.versionNo}`} ws={ws} entry={entry} />
          ))}
        </ol>
      )}
    </section>
  )
}
