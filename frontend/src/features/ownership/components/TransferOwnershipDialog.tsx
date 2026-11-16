import { useState } from 'react'

import type { ApiError } from '@/api'
import { FormError } from '@/components/common/FormError'
import { TextField } from '@/components/common/TextField'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { toast } from '@/components/ui/toast'
import { PersonChoiceField, usePersonChoice } from '@/features/people'
import { todayInBusinessZone } from '@/lib/dates'
import { formatDate } from '@/lib/format'

import { useTransferOwnership } from '../queries'
import type { Ownership } from '../types'
import { formatShare, STALE_ROW } from './ownershipForm'

/** Ngày liền trước của một ngày `YYYY-MM-DD`. */
function dayBefore(date: string): string {
  const day = new Date(`${date}T00:00:00Z`)
  day.setUTCDate(day.getUTCDate() - 1)
  return day.toISOString().slice(0, 10)
}

/**
 * Chuyển nhượng cả dòng sở hữu cho người khác: chủ cũ thôi từ hết ngày hôm trước, người nhận sở hữu
 * cùng tỷ lệ từ ngày chuyển. Dòng tóm tắt nói trước kết quả.
 */
export function TransferOwnershipDialog({
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
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Chuyển nhượng phần của {ownership.personFullName}</DialogTitle>
        </DialogHeader>
        <TransferForm ws={ws} ownership={ownership} onDone={() => onOpenChange(false)} />
      </DialogContent>
    </Dialog>
  )
}

function TransferForm({ ws, ownership, onDone }: { ws: string; ownership: Ownership; onDone: () => void }) {
  const receiver = usePersonChoice(ws)
  const transfer = useTransferOwnership(ws)
  const [day, setDay] = useState(todayInBusinessZone())
  const [documentNo, setDocumentNo] = useState('')
  const [note, setNote] = useState('')
  const [errors, setErrors] = useState<string[]>([])
  const [dateError, setDateError] = useState<string | null>(null)

  const choice = receiver.choice
  const receiverName = choice == null ? null : choice.kind === 'existing' ? choice.person.fullName : choice.fullName
  const sameAsOwner = choice?.kind === 'existing' && choice.person.id === ownership.personId
  const validDay = day !== '' && day > ownership.startDate
  const summary =
    receiverName && validDay && !sameAsOwner
      ? `${ownership.personFullName} thôi sở hữu từ hết ngày ${formatDate(dayBefore(day))}; ${receiverName} sở hữu ${
          ownership.sharePercent != null ? `${formatShare(ownership.sharePercent)} ` : ''
        }từ ${formatDate(day)}.`
      : null

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    const problems = [choice == null && 'Chọn người nhận', sameAsOwner && 'Người nhận phải khác chủ hiện tại'].filter(
      (message): message is string => Boolean(message),
    )
    const badDay = validDay ? null : `Phải sau ngày bắt đầu sở hữu (${formatDate(ownership.startDate)})`
    setErrors(problems)
    setDateError(badDay)
    if (problems.length > 0 || badDay) return
    try {
      const person = (await receiver.resolve())!
      await transfer.mutateAsync({
        ownership,
        body: { toPersonId: person.id, transferDate: day, documentNo: documentNo.trim() || undefined, note: note.trim() || undefined },
      })
      toast.add({ title: `Đã chuyển nhượng cho ${person.fullName}`, type: 'success' })
      onDone()
    } catch (error) {
      const apiError = error as ApiError
      setErrors([apiError.isVersionConflict ? STALE_ROW : (apiError.detail ?? String(error))])
    }
  }

  return (
    <form noValidate onSubmit={(event) => void submit(event)} className="flex flex-col gap-4">
      <p className="text-sm text-muted-foreground">
        {ownership.placeName}
        {ownership.sharePercent != null && ` · ${formatShare(ownership.sharePercent)}`}
      </p>
      <PersonChoiceField ws={ws} state={receiver} label="Chuyển cho" />
      <TextField
        label="Ngày chuyển nhượng"
        type="date"
        value={day}
        onChange={(event) => setDay(event.target.value)}
        error={dateError ?? undefined}
      />
      <TextField label="Số giấy tờ mới" autoComplete="off" value={documentNo} onChange={(event) => setDocumentNo(event.target.value)} />
      <TextField label="Ghi chú" autoComplete="off" value={note} onChange={(event) => setNote(event.target.value)} />
      {summary && <p className="rounded-lg bg-muted px-3 py-2 text-sm">{summary}</p>}
      <FormError messages={errors} />
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onDone}>
          Huỷ
        </Button>
        <Button type="submit" disabled={transfer.isPending || receiver.isCreating}>
          Chuyển nhượng
        </Button>
      </DialogFooter>
    </form>
  )
}
