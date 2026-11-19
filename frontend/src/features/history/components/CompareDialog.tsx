import { cn } from 'cn'
import { useState } from 'react'

import { currentUserId } from '@/api'
import { FormError } from '@/components/common/FormError'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Skeleton } from '@/components/ui/skeleton'
import { Switch } from '@/components/ui/switch'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatDateTime } from '@/lib/format'

import { blankAsDash } from '../blank'
import { compareRows } from '../compare'
import { useVersion } from '../queries'
import type { EntityKind, SnapshotRow, VersionSummary } from '../types'

/**
 * Hai version của một bản ghi đặt cạnh nhau: version nhỏ luôn ở cột trái, dù chọn theo thứ tự nào.
 * Mặc định chỉ hiện trường khác nhau; mục tuỳ biến so từng mục một.
 */
export function CompareDialog<Snapshot>({
  ws,
  kind,
  id,
  versionNos,
  snapshotRows,
  onOpenChange,
}: {
  ws: string
  kind: EntityKind
  id: string
  versionNos: [number, number]
  /** Các trường của một bản; `null` khi danh mục để hiện chúng còn đang tải. */
  snapshotRows: ((snapshot: Snapshot) => SnapshotRow[]) | null
  onOpenChange: (open: boolean) => void
}) {
  const olderNo = Math.min(...versionNos)
  const newerNo = Math.max(...versionNos)
  const older = useVersion<Snapshot>(ws, kind, id, olderNo)
  const newer = useVersion<Snapshot>(ws, kind, id, newerNo)
  const [onlyChanged, setOnlyChanged] = useState(true)
  const me = currentUserId()

  const error = older.error ?? newer.error
  const lines =
    older.data && newer.data && snapshotRows ? compareRows(snapshotRows(older.data.snapshot), snapshotRows(newer.data.snapshot)) : null
  const shown = lines?.filter((line) => line.changed || !onlyChanged) ?? []

  const heading = (version: VersionSummary) =>
    `v${version.versionNo} · ${formatDateTime(version.changedAt)} · ${version.changedBy === me ? 'Bạn' : 'Người dùng khác'}`

  return (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle>
            So sánh v{olderNo} và v{newerNo}
          </DialogTitle>
        </DialogHeader>
        <label className="flex items-center gap-2 text-sm">
          <Switch checked={onlyChanged} onCheckedChange={setOnlyChanged} aria-label="Chỉ hiện trường khác" />
          Chỉ hiện trường khác
        </label>
        {error ? (
          <FormError messages={[error.detail]} />
        ) : lines == null || older.data == null || newer.data == null ? (
          <div className="flex flex-col gap-2" aria-label="Đang tải">
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-full" />
          </div>
        ) : (
          <>
            {lines.every((line) => !line.changed) && <p className="text-sm text-muted-foreground">Hai version này giống nhau</p>}
            {shown.length > 0 && (
              <Table className="text-sm">
                <TableHeader>
                  <TableRow>
                    <TableHead>Trường</TableHead>
                    <TableHead>{heading(older.data)}</TableHead>
                    <TableHead>{heading(newer.data)}</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {shown.map((line) => (
                    <TableRow
                      key={line.key}
                      data-changed={line.changed || undefined}
                      className={cn('align-top', line.changed && 'bg-amber-50 font-semibold dark:bg-amber-950/30')}
                    >
                      <TableCell className="text-muted-foreground">{line.label}</TableCell>
                      <TableCell className="whitespace-normal">{blankAsDash(line.older)}</TableCell>
                      <TableCell className="whitespace-normal">{blankAsDash(line.newer)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </>
        )}
        <DialogFooter>
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
            Đóng
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
