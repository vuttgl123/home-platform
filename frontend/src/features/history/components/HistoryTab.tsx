import { useState, type ReactNode } from 'react'

import { currentUserId } from '@/api'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadMoreButton } from '@/components/common/LoadMoreButton'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { formatDateTime } from '@/lib/format'

import { ACTION_LABEL } from '../labels'
import { useVersions } from '../queries'
import type { EntityKind } from '../types'
import { VersionDialog } from './VersionDialog'

/**
 * Tab "Lịch sử" của một địa điểm, người hay món đồ: các version mới nhất trước; bấm một version để
 * xem nội dung và khôi phục. Không biết gì về nghiệp vụ: nơi dùng cho nhãn của các trường, cách hiện
 * nội dung, và cách cập nhật cache sau khi khôi phục.
 *
 * @param currentVersion `version` hiện tại của bản ghi, gửi kèm lệnh khôi phục
 * @param onReload lấy bản mới nhất của bản ghi sau một lần 409 "đã có người sửa"
 */
export function HistoryTab<Snapshot>({
  ws,
  kind,
  id,
  currentVersion,
  fieldLabels,
  renderSnapshot,
  onRestored,
  onReload,
}: {
  ws: string
  kind: EntityKind
  id: string
  currentVersion: number
  fieldLabels: Record<string, string>
  renderSnapshot: (snapshot: Snapshot) => ReactNode
  onRestored: (restored: unknown) => void
  onReload: () => Promise<unknown>
}) {
  const versions = useVersions(ws, kind, id)
  const [open, setOpen] = useState<number | null>(null)
  const me = currentUserId()

  if (versions.isLoading) {
    return (
      <div className="flex max-w-3xl flex-col gap-2" aria-label="Đang tải">
        <Skeleton className="h-14" />
        <Skeleton className="h-14" />
      </div>
    )
  }
  if (versions.error) {
    return <ErrorState error={versions.error} onRetry={versions.refetch} />
  }

  return (
    <div className="flex max-w-3xl flex-col gap-3">
      <ol className="flex flex-col divide-y rounded-lg border">
        {versions.items.map((version) => {
          const fields = (version.changedFields ?? []).map((field) => fieldLabels[field] ?? field)
          return (
            <li key={version.versionNo}>
              <button
                type="button"
                aria-label={`v${version.versionNo}`}
                onClick={() => setOpen(version.versionNo)}
                className="flex w-full flex-col gap-1 px-3 py-2.5 text-left text-sm outline-none hover:bg-muted focus-visible:bg-muted"
              >
                <span className="flex flex-wrap items-center gap-x-2 gap-y-1">
                  <span className="font-medium">v{version.versionNo}</span>
                  <Badge variant="outline">{ACTION_LABEL[version.action]}</Badge>
                  {version.versionNo === currentVersion && <Badge variant="secondary">Hiện tại</Badge>}
                  <span className="text-muted-foreground">
                    {formatDateTime(version.changedAt)} · {version.changedBy === me ? 'Bạn' : 'Người dùng khác'}
                  </span>
                </span>
                {fields.length > 0 && <span className="text-muted-foreground">Đã đổi: {fields.join(', ')}</span>}
                {version.note && <span className="italic">{version.note}</span>}
              </button>
            </li>
          )
        })}
      </ol>
      {versions.hasMore && <LoadMoreButton onClick={versions.loadMore} isLoading={versions.isLoadingMore} />}
      {open != null && (
        <VersionDialog<Snapshot>
          ws={ws}
          kind={kind}
          id={id}
          versionNo={open}
          currentVersion={currentVersion}
          renderSnapshot={renderSnapshot}
          onRestored={onRestored}
          onReload={onReload}
          onOpenChange={(next) => !next && setOpen(null)}
        />
      )}
    </div>
  )
}
