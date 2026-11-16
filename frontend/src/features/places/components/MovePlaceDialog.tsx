import { useState } from 'react'

import { FormError } from '@/components/common/FormError'
import { VersionConflictAlert } from '@/components/common/VersionConflictAlert'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { toast } from '@/components/ui/toast'
import { useDebouncedValue } from '@/hooks/use-debounced-value'

import { useFetchFreshPlace, useMovePlace, usePlaceSearch } from '../queries'
import type { PlaceDetail } from '../types'

const TOP = 'top'

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
  const [term, setTerm] = useState('')
  const [target, setTarget] = useState<string | null>(null)
  const searchFor = useDebouncedValue(term.trim(), 300)
  const results = usePlaceSearch(ws, term.trim() === '' ? '' : searchFor)
  const move = useMovePlace(ws)
  const fetchFresh = useFetchFreshPlace(ws)
  const [reloading, setReloading] = useState(false)

  // Không gợi ý chính nó, cũng không gợi ý chỗ đang chứa nó (chuyển vào đó là không đổi gì).
  const candidates = results.items.filter((item) => item.id !== place.id && item.id !== place.parentId)
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
      { place, parentId: target === TOP ? null : target },
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
      <Input
        type="search"
        aria-label="Tìm nơi chuyển tới"
        placeholder="Tìm nơi chuyển tới theo tên…"
        value={term}
        onChange={(event) => setTerm(event.target.value)}
        autoFocus
      />
      <div role="radiogroup" aria-label="Nơi chuyển tới" className="flex max-h-64 flex-col overflow-y-auto">
        {place.parentId != null && (
          <Choice value={TOP} selected={target} onSelect={setTarget}>
            Lên cấp cao nhất
          </Choice>
        )}
        {candidates.map((candidate) => (
          <Choice key={candidate.id} value={candidate.id} selected={target} onSelect={setTarget} note={candidate.code}>
            {candidate.name}
          </Choice>
        ))}
        {searchFor !== '' && results.isLoaded && candidates.length === 0 && (
          <p className="px-2 py-1.5 text-sm text-muted-foreground">Không có nơi nào có tên bắt đầu bằng “{searchFor}”.</p>
        )}
      </div>
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

/** Một lựa chọn nơi chuyển tới. `note` (mã) đứng ngoài nhãn để tên đọc lên vẫn gọn. */
function Choice({
  value,
  selected,
  onSelect,
  note,
  children,
}: {
  value: string
  selected: string | null
  onSelect: (value: string) => void
  note?: string | null
  children: React.ReactNode
}) {
  return (
    <div className="flex items-center gap-2 rounded-md px-2 hover:bg-muted has-[:checked]:bg-muted">
      <label className="flex min-w-0 flex-1 cursor-pointer items-center gap-2 py-1.5 text-sm">
        <input
          type="radio"
          name="move-target"
          value={value}
          checked={selected === value}
          onChange={() => onSelect(value)}
          className="accent-primary"
        />
        <span className="truncate">{children}</span>
      </label>
      {note && <span className="shrink-0 text-xs text-muted-foreground">{note}</span>}
    </div>
  )
}
