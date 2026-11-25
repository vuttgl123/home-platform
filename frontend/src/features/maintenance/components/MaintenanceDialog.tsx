import { zodResolver } from '@hookform/resolvers/zod'
import { useId, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'

import type { ApiError } from '@/api'
import { FormError } from '@/components/common/FormError'
import { TextField } from '@/components/common/TextField'
import { VersionConflictAlert } from '@/components/common/VersionConflictAlert'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Field, FieldLabel } from '@/components/ui/field'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Textarea } from '@/components/ui/textarea'
import { toast } from '@/components/ui/toast'
import { useFetchFreshItem, type ItemDetail } from '@/features/items'
import { todayInBusinessZone } from '@/lib/dates'

import { MAINTENANCE_KIND_LABEL } from '../labels'
import { useCreateMaintenance, useUpdateMaintenance } from '../queries'
import type { MaintenanceEntry, MaintenanceKind } from '../types'
import { maintenanceFormSchema, STALE_ENTRY, type MaintenanceFormValues } from './maintenanceForm'

const KINDS: MaintenanceKind[] = ['REPAIR', 'SERVICE']

/**
 * Hộp thoại ghi một lần sửa hay bảo trì, hoặc sửa một dòng đã ghi. Món đang hỏng thì mặc định là sửa chữa
 * và đánh sẵn "Đã sửa xong", để một lần lưu vừa ghi nhật ký vừa đưa món về dùng lại (spec Đợt 8, L2).
 */
export function MaintenanceDialog({
  ws,
  item,
  entry,
  open,
  onOpenChange,
}: {
  ws: string
  item: ItemDetail
  entry?: MaintenanceEntry
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{entry ? 'Sửa dòng bảo trì' : 'Ghi bảo trì'}</DialogTitle>
        </DialogHeader>
        {open && <MaintenanceForm ws={ws} item={item} entry={entry} onDone={() => onOpenChange(false)} />}
      </DialogContent>
    </Dialog>
  )
}

function MaintenanceForm({ ws, item, entry, onDone }: { ws: string; item: ItemDetail; entry?: MaintenanceEntry; onDone: () => void }) {
  const kindLabelId = useId()
  const noteId = useId()
  const [formErrors, setFormErrors] = useState<string[]>([])
  const [itemVersion, setItemVersion] = useState(item.version)
  const [stale, setStale] = useState(false)
  const [reloading, setReloading] = useState(false)
  const fetchFresh = useFetchFreshItem(ws)
  const create = useCreateMaintenance(ws)
  const update = useUpdateMaintenance(ws)
  const form = useForm<MaintenanceFormValues>({
    resolver: zodResolver(maintenanceFormSchema),
    defaultValues: entry
      ? { kind: entry.kind, doneOn: entry.doneOn, work: entry.work, doneBy: entry.doneBy ?? '', note: entry.note ?? '', markFixed: false }
      : { kind: item.status === 'BROKEN' ? 'REPAIR' : 'SERVICE', doneOn: todayInBusinessZone(), work: '', doneBy: '', note: '', markFixed: true },
  })
  const kind = useWatch({ control: form.control, name: 'kind' })
  const markFixed = useWatch({ control: form.control, name: 'markFixed' })
  const canMarkFixed = !entry && item.status === 'BROKEN' && kind === 'REPAIR'

  async function reload() {
    setReloading(true)
    try {
      setItemVersion((await fetchFresh(item.id)).version)
      setStale(false)
    } finally {
      setReloading(false)
    }
  }

  const submit = form.handleSubmit(async (values) => {
    setFormErrors([])
    const content = {
      kind: values.kind,
      doneOn: values.doneOn,
      work: values.work.trim(),
      ...(values.doneBy.trim() ? { doneBy: values.doneBy.trim() } : {}),
      ...(values.note.trim() ? { note: values.note.trim() } : {}),
    }
    try {
      if (entry) {
        await update.mutateAsync({ id: entry.id, body: { version: entry.version, ...content } })
        toast.add({ title: 'Đã lưu thay đổi', type: 'success' })
      } else {
        const fixed = canMarkFixed && values.markFixed
        await create.mutateAsync({ itemId: item.id, ...content, ...(fixed ? { markFixed: true, itemVersion } : {}) })
        toast.add({ title: fixed ? `Đã ghi và chuyển ${item.name} về Đang dùng` : 'Đã ghi bảo trì', type: 'success' })
      }
      onDone()
    } catch (error) {
      const apiError = error as ApiError
      if (apiError.isVersionConflict && !entry) {
        setStale(true)
      } else {
        setFormErrors([apiError.isVersionConflict ? STALE_ENTRY : (apiError.detail ?? String(error))])
      }
    }
  })

  const { errors } = form.formState
  return (
    <form noValidate onSubmit={submit} className="flex flex-col gap-4">
      <Field>
        <FieldLabel id={kindLabelId}>Loại</FieldLabel>
        <RadioGroup
          aria-labelledby={kindLabelId}
          value={kind}
          onValueChange={(value) => form.setValue('kind', value as MaintenanceKind)}
          className="flex flex-wrap gap-4"
        >
          {KINDS.map((value) => (
            <label key={value} className="flex items-center gap-2 text-sm">
              <RadioGroupItem value={value} />
              {MAINTENANCE_KIND_LABEL[value]}
            </label>
          ))}
        </RadioGroup>
      </Field>
      <TextField label="Ngày làm" type="date" required error={errors.doneOn?.message} {...form.register('doneOn')} />
      <TextField label="Việc đã làm" required autoComplete="off" error={errors.work?.message} {...form.register('work')} />
      <TextField label="Người làm" autoComplete="off" error={errors.doneBy?.message} {...form.register('doneBy')} />
      <Field data-invalid={errors.note ? true : undefined}>
        <FieldLabel htmlFor={noteId}>Ghi chú</FieldLabel>
        <Textarea id={noteId} rows={2} {...form.register('note')} />
      </Field>
      {canMarkFixed && (
        <label className="flex items-center gap-2 text-sm">
          <Checkbox checked={markFixed} onCheckedChange={(checked) => form.setValue('markFixed', checked === true)} />
          Đã sửa xong, chuyển về Đang dùng
        </label>
      )}
      {stale && <VersionConflictAlert onReload={() => void reload()} isReloading={reloading} />}
      <FormError messages={formErrors} />
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onDone}>
          Huỷ
        </Button>
        <Button type="submit" disabled={create.isPending || update.isPending}>
          Lưu
        </Button>
      </DialogFooter>
    </form>
  )
}
