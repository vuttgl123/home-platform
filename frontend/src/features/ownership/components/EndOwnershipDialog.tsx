import { useState } from 'react'

import { FormError } from '@/components/common/FormError'
import { TextField } from '@/components/common/TextField'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { toast } from '@/components/ui/toast'
import { todayInBusinessZone } from '@/lib/dates'
import { formatDate } from '@/lib/format'

import { useEndOwnership } from '../queries'
import type { Ownership } from '../types'
import { STALE_ROW } from './ownershipForm'

/** "Thôi sở hữu": đặt ngày cuối cùng còn sở hữu. Dòng vẫn còn trong lịch sử. */
export function EndOwnershipDialog({
  ws,
  ownership,
  open,
  onOpenChange,
}: {
  ws: string
  ownership: Ownership
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>{ownership.personFullName} thôi sở hữu</DialogTitle>
          <DialogDescription>Không còn sở hữu {ownership.placeName}. Dòng vẫn được giữ trong lịch sử.</DialogDescription>
        </DialogHeader>
        <EndForm ws={ws} ownership={ownership} onDone={() => onOpenChange(false)} />
      </DialogContent>
    </Dialog>
  )
}

function EndForm({ ws, ownership, onDone }: { ws: string; ownership: Ownership; onDone: () => void }) {
  const end = useEndOwnership(ws)
  const [date, setDate] = useState(todayInBusinessZone())
  const [error, setError] = useState<string | null>(null)

  function submit(event: React.FormEvent) {
    event.preventDefault()
    if (date === '') {
      setError('Chọn ngày')
      return
    }
    if (date < ownership.startDate) {
      setError(`Không được trước ngày bắt đầu (${formatDate(ownership.startDate)})`)
      return
    }
    setError(null)
    end.mutate(
      { ownership, endDate: date },
      {
        onSuccess: () => {
          toast.add({ title: `${ownership.personFullName} đã thôi sở hữu`, type: 'success' })
          onDone()
        },
      },
    )
  }

  const serverError = end.error ? (end.error.isVersionConflict ? STALE_ROW : end.error.detail) : null
  return (
    <form noValidate onSubmit={submit} className="flex flex-col gap-4">
      <TextField
        label="Ngày cuối cùng còn sở hữu"
        type="date"
        value={date}
        onChange={(event) => setDate(event.target.value)}
        error={error ?? undefined}
      />
      <FormError messages={serverError ? [serverError] : []} />
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onDone}>
          Huỷ
        </Button>
        <Button type="submit" disabled={end.isPending}>
          Thôi sở hữu
        </Button>
      </DialogFooter>
    </form>
  )
}
