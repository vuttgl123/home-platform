import { useState } from 'react'

import { FormError } from '@/components/common/FormError'
import { VersionConflictAlert } from '@/components/common/VersionConflictAlert'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { toast } from '@/components/ui/toast'
import { PlacePicker, type PlaceChoice } from '@/features/places'

import { useMoveItem } from '../queries'
import type { ItemDetail } from '../types'
import { useItemReload } from './useItemReload'

/** Chuyển một món sang địa điểm khác; vị trí cũ còn trong lịch sử. */
export function MoveItemDialog({
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
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Chuyển {item.name}</DialogTitle>
        </DialogHeader>
        <MoveForm ws={ws} item={item} onDone={() => onOpenChange(false)} />
      </DialogContent>
    </Dialog>
  )
}

function MoveForm({ ws, item, onDone }: { ws: string; item: ItemDetail; onDone: () => void }) {
  const [target, setTarget] = useState<PlaceChoice | null>(null)
  const move = useMoveItem(ws)
  const conflict = move.error?.isVersionConflict === true
  const { reload, reloading } = useItemReload(ws, item, move.reset)

  function submit(event: React.FormEvent) {
    event.preventDefault()
    if (target?.kind !== 'place') return
    move.mutate(
      { item, placeId: target.place.id },
      {
        onSuccess: (moved) => {
          toast.add({ title: `Đã chuyển ${moved.name} tới ${moved.placeName}`, type: 'success' })
          onDone()
        },
      },
    )
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-3">
      <PlacePicker ws={ws} exclude={[item.placeId]} value={target} onChange={setTarget} label="Tìm địa điểm" />
      {conflict && <VersionConflictAlert onReload={reload} isReloading={reloading} />}
      <FormError messages={move.error && !conflict ? [move.error.detail] : []} />
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onDone}>
          Huỷ
        </Button>
        <Button type="submit" disabled={target?.kind !== 'place' || move.isPending || conflict}>
          Chuyển
        </Button>
      </DialogFooter>
    </form>
  )
}
