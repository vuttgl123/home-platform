import { useState } from 'react'

import { FormError } from '@/components/common/FormError'
import { VersionConflictAlert } from '@/components/common/VersionConflictAlert'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { toast } from '@/components/ui/toast'

import { useFetchFreshPlace, useMovePlace } from '../queries'
import type { PlaceDetail } from '../types'
import { PlacePicker, type PlaceChoice } from './PlacePicker'

/**
 * Chuyển một địa điểm (cùng mọi thứ bên trong) sang chỗ khác: tìm chỗ mới theo tên, hoặc đưa
 * lên cấp cao nhất. Chuyển vào chính nó hay con cháu của nó bị backend chặn, lỗi hiện nguyên văn.
 */
export function MovePlaceDialog({
  ws,
  place,
  open,
  onOpenChange,
}: {
  ws: string
  place: PlaceDetail
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Di chuyển {place.name}</DialogTitle>
          <DialogDescription>Mọi chỗ, người và đồ đạc bên trong đi theo nó.</DialogDescription>
        </DialogHeader>
        <MoveForm ws={ws} place={place} onDone={() => onOpenChange(false)} />
      </DialogContent>
    </Dialog>
  )
}

function MoveForm({ ws, place, onDone }: { ws: string; place: PlaceDetail; onDone: () => void }) {
  const [target, setTarget] = useState<PlaceChoice | null>(null)
  const move = useMovePlace(ws)
  const fetchFresh = useFetchFreshPlace(ws)
  const [reloading, setReloading] = useState(false)
  const conflict = move.error?.isVersionConflict === true

  async function reload() {
    setReloading(true)
    try {
      await fetchFresh(place.id)
      move.reset()
    } finally {
      setReloading(false)
    }
  }

  function submit(event: React.FormEvent) {
    event.preventDefault()
    if (target == null) return
    move.mutate(
      { place, parentId: target.kind === 'top' ? null : target.place.id },
      {
        onSuccess: (moved) => {
          toast.add({ title: `Đã chuyển ${moved.name}`, type: 'success' })
          onDone()
        },
      },
    )
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-3">
      {/* Không gợi ý chính nó, cũng không gợi ý chỗ đang chứa nó (chuyển vào đó là không đổi gì). */}
      <PlacePicker
        ws={ws}
        exclude={[place.id, place.parentId]}
        allowTop={place.parentId != null}
        value={target}
        onChange={setTarget}
      />
      {conflict && <VersionConflictAlert onReload={() => void reload()} isReloading={reloading} />}
      <FormError messages={move.error && !conflict ? [move.error.detail] : []} />
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onDone}>
          Huỷ
        </Button>
        <Button type="submit" disabled={target == null || move.isPending || conflict}>
          Chuyển
        </Button>
      </DialogFooter>
    </form>
  )
}
