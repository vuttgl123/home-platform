import { useState } from 'react'

import { ToneBadge } from '@/components/common/ToneBadge'
import { currentUserId } from '@/api'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadMoreButton } from '@/components/common/LoadMoreButton'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Skeleton } from '@/components/ui/skeleton'
import { formatDateTime } from '@/lib/format'

import { ACTION_LABEL, ACTION_TONE } from '../labels'
import { useVersions } from '../queries'
import type { EntityKind, SnapshotRow } from '../types'
import { CompareDialog } from './CompareDialog'
import { VersionDialog } from './VersionDialog'

/**
 * Tab "Lịch sử" của một địa điểm, người hay món đồ: các version mới nhất trước; bấm một version để
 * xem nội dung và khôi phục, chọn hai version để so sánh. Không biết gì về nghiệp vụ: nơi dùng cho
 * nhãn của các trường, các trường của một bản, và cách cập nhật cache sau khi khôi phục.
 *
 * @param snapshotRows các trường của một bản, dùng cho cả hộp xem và hộp so sánh; `null` khi danh
 *   mục để hiện chúng (tên loại, mục tuỳ biến) còn đang tải
 * @param currentVersion `version` hiện tại của bản ghi, gửi kèm lệnh khôi phục
 * @param onReload lấy bản mới nhất của bản ghi sau một lần 409 "đã có người sửa"
 */
export function HistoryTab<Snapshot>({
  ws,
  kind,
  id,
  currentVersion,
  fieldLabels,
  snapshotRows,
  onRestored,
  onReload,
}: {
  ws: string
  kind: EntityKind
  id: string
  currentVersion: number
  fieldLabels: Record<string, string>
  snapshotRows: ((snapshot: Snapshot) => SnapshotRow[]) | null
  onRestored: (restored: unknown) => void
  onReload: () => Promise<unknown>
}) {
  const versions = useVersions(ws, kind, id)
  const [open, setOpen] = useState<number | null>(null)
  // Theo thứ tự chọn: chọn dòng thứ ba thì bỏ dòng được chọn sớm nhất.
  const [chosen, setChosen] = useState<number[]>([])
  const [comparing, setComparing] = useState(false)
  const me = currentUserId()

  function toggle(versionNo: number) {
    setChosen((current) =>
      current.includes(versionNo) ? current.filter((no) => no !== versionNo) : [...current, versionNo].slice(-2),
    )
  }

  if (versions.isLoading) {
    return (
      <div className="flex flex-col gap-2" aria-label="Đang tải">
        <Skeleton className="h-14" />
        <Skeleton className="h-14" />
      </div>
    )
  }
  if (versions.error) {
    return <ErrorState error={versions.error} onRetry={versions.refetch} />
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center gap-3">
        <p className="flex-1 text-sm text-muted-foreground">Chọn hai version để so sánh.</p>
        <Button type="button" size="sm" variant="outline" disabled={chosen.length !== 2} onClick={() => setComparing(true)}>
          So sánh
        </Button>
      </div>
      <ol className="flex flex-col divide-y rounded-lg border">
        {versions.items.map((version) => {
          const fields = (version.changedFields ?? []).map((field) => fieldLabels[field] ?? field)
          return (
            <li key={version.versionNo} className="flex items-start">
              <span className="flex pt-3 pl-3">
                <Checkbox
                  checked={chosen.includes(version.versionNo)}
                  onCheckedChange={() => toggle(version.versionNo)}
                  aria-label={`Chọn v${version.versionNo} để so sánh`}
                />
              </span>
              <button
                type="button"
                aria-label={`v${version.versionNo}`}
                onClick={() => setOpen(version.versionNo)}
                className="flex min-w-0 flex-1 flex-col gap-1 px-3 py-2.5 text-left text-sm outline-none hover:bg-muted focus-visible:bg-muted"
              >
                <span className="flex flex-wrap items-center gap-x-2 gap-y-1">
                  <span className="font-medium">v{version.versionNo}</span>
                  <ToneBadge tone={ACTION_TONE[version.action]}>{ACTION_LABEL[version.action]}</ToneBadge>
                  {version.versionNo === currentVersion && <ToneBadge tone="primary">Hiện tại</ToneBadge>}
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
          snapshotRows={snapshotRows}
          onRestored={onRestored}
          onReload={onReload}
          onOpenChange={(next) => !next && setOpen(null)}
        />
      )}
      {comparing && chosen.length === 2 && (
        <CompareDialog<Snapshot>
          ws={ws}
          kind={kind}
          id={id}
          versionNos={[chosen[0], chosen[1]]}
          snapshotRows={snapshotRows}
          onOpenChange={(next) => !next && setComparing(false)}
        />
      )}
    </div>
  )
}
