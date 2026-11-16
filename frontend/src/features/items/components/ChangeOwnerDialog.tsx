import { useState } from 'react'

import { FormError } from '@/components/common/FormError'
import { VersionConflictAlert } from '@/components/common/VersionConflictAlert'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { toast } from '@/components/ui/toast'
import type { PersonSummary } from '@/features/people'

import { useChangeItemOwner } from '../queries'
import type { ItemDetail } from '../types'
import { OwnerField } from './OwnerField'
import { ownerIdOf, type OwnerChoice } from './ownerChoice'
import { useItemReload } from './useItemReload'

/** Đổi chủ của một món chưa gỡ, hoặc trả nó về cho địa điểm. */
export function ChangeOwnerDialog({
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
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Đổi chủ {item.name}</DialogTitle>
        </DialogHeader>
        <ChangeOwnerForm ws={ws} item={item} onDone={() => onOpenChange(false)} />
      </DialogContent>
    </Dialog>
  )
}

function ChangeOwnerForm({ ws, item, onDone }: { ws: string; item: ItemDetail; onDone: () => void }) {
  const [choice, setChoice] = useState<OwnerChoice>(
    item.ownerPersonId
      ? { kind: 'person', person: { id: item.ownerPersonId, fullName: item.ownerName ?? '', version: 0 } as PersonSummary }
      : { kind: 'place' },
  )
  const change = useChangeItemOwner(ws)
  const conflict = change.error?.isVersionConflict === true
  const { reload, reloading } = useItemReload(ws, item, change.reset)
  const chosen = ownerIdOf(choice)
  const unchanged = chosen === (item.ownerPersonId ?? null) || (choice.kind === 'person' && chosen == null)

  return (
    <form
      noValidate
      className="flex flex-col gap-4"
      onSubmit={(event) => {
        event.preventDefault()
        change.mutate(
          { item, ownerPersonId: chosen },
          {
            onSuccess: () => {
              toast.add({ title: 'Đã đổi chủ', type: 'success' })
              onDone()
            },
          },
        )
      }}
    >
      <OwnerField ws={ws} value={choice} onChange={setChoice} />
      {conflict && <VersionConflictAlert onReload={reload} isReloading={reloading} />}
      <FormError messages={change.error && !conflict ? [change.error.detail] : []} />
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onDone}>
          Huỷ
        </Button>
        <Button type="submit" disabled={unchanged || change.isPending || conflict}>
          Lưu
        </Button>
      </DialogFooter>
    </form>
  )
}
