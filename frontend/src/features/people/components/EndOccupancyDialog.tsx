import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'

import { FormError } from '@/components/common/FormError'
import { TextField } from '@/components/common/TextField'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { toast } from '@/components/ui/toast'
import { todayInBusinessZone } from '@/lib/dates'

import { refreshOccupancies, useEndOccupancy } from '../queries'
import type { Occupancy } from '../types'
import { notBefore, STALE_ROW } from './occupancyForm'

/** "Chuyển đi": đặt ngày kết thúc của một dòng cư trú. Dòng vẫn còn trong lịch sử. */
export function EndOccupancyDialog({
  ws,
  occupancy,
  open,
  onOpenChange,
}: {
  ws: string
  occupancy: Occupancy
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>{occupancy.personFullName} chuyển đi</DialogTitle>
          <DialogDescription>Rời {occupancy.placeName}. Dòng cư trú vẫn được giữ trong lịch sử.</DialogDescription>
        </DialogHeader>
        <EndForm ws={ws} occupancy={occupancy} onDone={() => onOpenChange(false)} />
      </DialogContent>
    </Dialog>
  )
}

function EndForm({ ws, occupancy, onDone }: { ws: string; occupancy: Occupancy; onDone: () => void }) {
  const queryClient = useQueryClient()
  const end = useEndOccupancy(ws)
  const [date, setDate] = useState(todayInBusinessZone())
  const [error, setError] = useState<string | null>(null)

  function submit(event: React.FormEvent) {
    event.preventDefault()
    if (date === '') {
      setError('Chọn ngày chuyển đi')
      return
    }
    if (date < occupancy.startDate) {
      setError(notBefore(occupancy.startDate))
      return
    }
    setError(null)
    end.mutate(
      { occupancy, endDate: date },
      {
        onSuccess: () => {
          toast.add({ title: `${occupancy.personFullName} đã chuyển đi`, type: 'success' })
          onDone()
        },
        onError: (failure) => {
          if (failure.isVersionConflict) refreshOccupancies(queryClient, ws, occupancy.personId)
        },
      },
    )
  }

  const serverError = end.error ? (end.error.isVersionConflict ? STALE_ROW : end.error.detail) : null
  return (
    <form noValidate onSubmit={submit} className="flex flex-col gap-4">
      <TextField
        label="Ngày chuyển đi"
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
          Chuyển đi
        </Button>
      </DialogFooter>
    </form>
  )
}
