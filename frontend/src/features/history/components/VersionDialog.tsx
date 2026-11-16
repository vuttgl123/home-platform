import { useId, useState, type ReactNode } from 'react'

import { FormError } from '@/components/common/FormError'
import { VersionConflictAlert } from '@/components/common/VersionConflictAlert'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { toast } from '@/components/ui/toast'

import { useRestoreVersion, useVersion } from '../queries'
import type { EntityKind } from '../types'

/**
 * Nội dung của một version, và lối khôi phục về nó (trừ version đang hiện hành). Khôi phục chỉ
 * đổi nội dung; chỗ đứng (cha của địa điểm, chỗ đặt và trạng thái gỡ của đồ đạc) giữ nguyên.
 */
export function VersionDialog<Snapshot>({
  ws,
  kind,
  id,
  versionNo,
  currentVersion,
  renderSnapshot,
  onRestored,
  onReload,
  onOpenChange,
}: {
  ws: string
  kind: EntityKind
  id: string
  versionNo: number
  currentVersion: number
  renderSnapshot: (snapshot: Snapshot) => ReactNode
  onRestored: (restored: unknown) => void
  onReload: () => Promise<unknown>
  onOpenChange: (open: boolean) => void
}) {
  const version = useVersion<Snapshot>(ws, kind, id, versionNo)
  const restore = useRestoreVersion(ws, kind, id)
  const [confirming, setConfirming] = useState(false)
  const [note, setNote] = useState('')
  const [reloading, setReloading] = useState(false)
  const noteId = useId()
  const isCurrent = versionNo === currentVersion
  const conflict = restore.error?.isVersionConflict === true

  async function reload() {
    setReloading(true)
    try {
      await onReload()
      restore.reset()
    } finally {
      setReloading(false)
    }
  }

  function confirm() {
    restore.mutate(
      { versionNo, currentVersion, note: note.trim() || undefined },
      {
        onSuccess: (restored) => {
          toast.add({ title: `Đã khôi phục về v${versionNo}`, type: 'success' })
          onRestored(restored)
          onOpenChange(false)
        },
      },
    )
  }

  const failure =
    restore.error && !conflict
      ? restore.error.status === 400
        ? [restore.error.detail, 'Nội dung cũ không còn hợp lệ với danh mục hiện tại. Sửa tay bản hiện tại thay vì khôi phục.']
        : [restore.error.detail]
      : []

  return (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Bản v{versionNo}</DialogTitle>
          {isCurrent && <DialogDescription>Đây là bản đang dùng.</DialogDescription>}
        </DialogHeader>
        {version.data ? (
          <div className="text-sm">{renderSnapshot(version.data.snapshot)}</div>
        ) : version.error ? (
          <FormError messages={[version.error.detail]} />
        ) : (
          <div className="flex flex-col gap-2" aria-label="Đang tải">
            <Skeleton className="h-4 w-48" />
            <Skeleton className="h-4 w-64" />
          </div>
        )}
        {!isCurrent && confirming && (
          <div className="flex flex-col gap-3 rounded-lg border p-3">
            <p className="text-sm">Chỉ nội dung được khôi phục; chỗ đứng giữ nguyên.</p>
            <Field>
              <FieldLabel htmlFor={noteId}>Ghi chú</FieldLabel>
              <Input id={noteId} maxLength={255} value={note} onChange={(event) => setNote(event.target.value)} />
            </Field>
          </div>
        )}
        {conflict && <VersionConflictAlert onReload={() => void reload()} isReloading={reloading} />}
        <FormError messages={failure} />
        <DialogFooter>
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
            Đóng
          </Button>
          {!isCurrent &&
            (confirming ? (
              <Button type="button" disabled={restore.isPending || conflict} onClick={confirm}>
                Khôi phục
              </Button>
            ) : (
              <Button type="button" disabled={!version.data} onClick={() => setConfirming(true)}>
                Khôi phục bản này
              </Button>
            ))}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
