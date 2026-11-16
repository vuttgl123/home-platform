import { useState } from 'react'

import { FormError } from '@/components/common/FormError'
import { TextField } from '@/components/common/TextField'
import { VersionConflictAlert } from '@/components/common/VersionConflictAlert'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { toast } from '@/components/ui/toast'
import { todayInBusinessZone } from '@/lib/dates'

import { useRemoveItem } from '../queries'
import type { ItemDetail } from '../types'
import { removalDateError } from './itemForm'
import { useItemReload } from './useItemReload'

/** Gỡ một món: nó vẫn hiện trong danh sách "đã gỡ" của địa điểm. */
export function RemoveItemDialog({
  ws,
  item,
  open,
  onOpenChange,
}: {
  ws: string
  item: ItemDetail
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>Gỡ {item.name}</DialogTitle>
          <DialogDescription>Món vẫn hiện trong danh sách đã gỡ của {item.placeName}.</DialogDescription>
        </DialogHeader>
        <RemoveForm ws={ws} item={item} onDone={() => onOpenChange(false)} />
      </DialogContent>
    </Dialog>
  )
}

function RemoveForm({ ws, item, onDone }: { ws: string; item: ItemDetail; onDone: () => void }) {
  const remove = useRemoveItem(ws)
  const [date, setDate] = useState(todayInBusinessZone())
  const [error, setError] = useState<string | null>(null)
  const conflict = remove.error?.isVersionConflict === true
  const { reload, reloading } = useItemReload(ws, item, remove.reset)

  function submit(event: React.FormEvent) {
    event.preventDefault()
    const problem = removalDateError(date, item)
    setError(problem)
    if (problem) return
    remove.mutate(
      { item, removedAt: date },
      {
        onSuccess: () => {
          toast.add({ title: `Đã gỡ ${item.name}`, type: 'success' })
          onDone()
        },
      },
    )
  }

  return (
    <form noValidate onSubmit={submit} className="flex flex-col gap-4">
      <TextField label="Ngày gỡ" type="date" value={date} onChange={(event) => setDate(event.target.value)} error={error ?? undefined} />
      {conflict && <VersionConflictAlert onReload={reload} isReloading={reloading} />}
      <FormError messages={remove.error && !conflict ? [remove.error.detail] : []} />
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onDone}>
          Huỷ
        </Button>
        <Button type="submit" disabled={remove.isPending || conflict}>
          Gỡ
        </Button>
      </DialogFooter>
    </form>
  )
}
