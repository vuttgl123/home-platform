import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'

import type { ApiError } from '@/api'
import { FormError } from '@/components/common/FormError'
import { TextField } from '@/components/common/TextField'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Field, FieldLabel } from '@/components/ui/field'
import { toast } from '@/components/ui/toast'
import { PersonChoiceField, usePersonChoice } from '@/features/people'
import { PlacePicker, type PlaceChoice } from '@/features/places'
import { todayInBusinessZone } from '@/lib/dates'

import { useCreateOwnership, useUpdateOwnership } from '../queries'
import type { Ownership } from '../types'
import { formatShare, ownershipFormSchema, parseShare, STALE_ROW, type OwnershipFormValues } from './ownershipForm'

type Mode =
  | { mode: 'create'; place?: { id: string; name: string }; person?: { id: string; fullName: string } }
  | { mode: 'edit'; ownership: Ownership }

function titleOf(mode: Mode): string {
  if (mode.mode === 'edit') return 'Sửa sở hữu'
  return mode.place ? 'Thêm chủ' : 'Thêm chỗ sở hữu'
}

/**
 * Hộp thoại thêm hoặc sửa một dòng sở hữu. Thêm từ một địa điểm thì chọn người (hoặc tạo người mới
 * ngay trong form); thêm từ trang một người thì chọn địa điểm.
 */
export function OwnershipFormDialog({
  ws,
  open,
  onOpenChange,
  ...mode
}: { ws: string; open: boolean; onOpenChange: (open: boolean) => void } & Mode) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{titleOf(mode)}</DialogTitle>
        </DialogHeader>
        <OwnershipForm ws={ws} onDone={() => onOpenChange(false)} {...mode} />
      </DialogContent>
    </Dialog>
  )
}

function OwnershipForm({ ws, onDone, ...mode }: { ws: string; onDone: () => void } & Mode) {
  const editing = mode.mode === 'edit' ? mode.ownership : null
  const fixedPlace = mode.mode === 'create' ? mode.place : null
  const fixedPerson = mode.mode === 'create' ? mode.person : null
  const person = usePersonChoice(ws)
  const [place, setPlace] = useState<PlaceChoice | null>(null)
  const [formErrors, setFormErrors] = useState<string[]>([])
  const form = useForm<OwnershipFormValues>({
    resolver: zodResolver(ownershipFormSchema),
    defaultValues: editing
      ? {
          sharePercent: editing.sharePercent != null ? formatShare(editing.sharePercent).replace('%', '') : '',
          startDate: editing.startDate,
          endDate: editing.endDate ?? '',
          documentNo: editing.documentNo ?? '',
          note: editing.note ?? '',
        }
      : { sharePercent: '', startDate: todayInBusinessZone(), endDate: '', documentNo: '', note: '' },
  })
  const create = useCreateOwnership(ws)
  const update = useUpdateOwnership(ws)
  const pending = person.isCreating || create.isPending || update.isPending

  function fail(error: unknown) {
    const apiError = error as ApiError
    setFormErrors([apiError.isVersionConflict ? STALE_ROW : (apiError.detail ?? String(error))])
  }

  const submit = form.handleSubmit(async (values) => {
    setFormErrors([])
    const share = parseShare(values.sharePercent)
    const common = {
      sharePercent: share === 'invalid' || share == null ? undefined : share,
      startDate: values.startDate,
      endDate: values.endDate || undefined,
      documentNo: values.documentNo.trim() || undefined,
      note: values.note.trim() || undefined,
    }
    try {
      if (editing) {
        await update.mutateAsync({ id: editing.id, body: { ...common, version: editing.version } })
        toast.add({ title: 'Đã lưu thay đổi', type: 'success' })
        onDone()
        return
      }
      const placeId = fixedPlace?.id ?? (place?.kind === 'place' ? place.place.id : null)
      const missing = [
        placeId == null && 'Chọn một địa điểm',
        !fixedPerson && person.choice == null && 'Chọn một người, hoặc thêm người mới',
      ].filter((message): message is string => Boolean(message))
      if (missing.length > 0) {
        setFormErrors(missing)
        return
      }
      const personId = fixedPerson?.id ?? (await person.resolve())!.id
      const created = await create.mutateAsync({ ...common, placeId: placeId!, personId })
      toast.add({ title: `Đã thêm ${created.personFullName} sở hữu ${created.placeName}`, type: 'success' })
      onDone()
    } catch (error) {
      fail(error)
    }
  })

  const { errors } = form.formState
  return (
    <form noValidate onSubmit={submit} className="flex flex-col gap-4">
      {editing && (
        <p className="text-sm text-muted-foreground">
          {editing.personFullName} tại {editing.placeName}
        </p>
      )}
      {fixedPlace && <p className="text-sm text-muted-foreground">Tại {fixedPlace.name}</p>}
      {mode.mode === 'create' && !fixedPerson && <PersonChoiceField ws={ws} state={person} />}
      {mode.mode === 'create' && !fixedPlace && (
        <Field>
          <FieldLabel>Địa điểm</FieldLabel>
          <PlacePicker ws={ws} exclude={[]} value={place} onChange={setPlace} label="Tìm địa điểm" />
        </Field>
      )}
      <TextField
        label="Tỷ lệ (%)"
        inputMode="decimal"
        autoComplete="off"
        description="Để trống nếu không ghi tỷ lệ."
        error={errors.sharePercent?.message}
        {...form.register('sharePercent')}
      />
      <div className="grid gap-4 sm:grid-cols-2">
        <TextField label="Từ ngày" type="date" required error={errors.startDate?.message} {...form.register('startDate')} />
        <TextField
          label="Tới ngày"
          type="date"
          description="Để trống nếu vẫn đang sở hữu"
          error={errors.endDate?.message}
          {...form.register('endDate')}
        />
      </div>
      <TextField label="Số giấy tờ" autoComplete="off" error={errors.documentNo?.message} {...form.register('documentNo')} />
      <TextField label="Ghi chú" autoComplete="off" error={errors.note?.message} {...form.register('note')} />
      <FormError messages={formErrors} />
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onDone}>
          Huỷ
        </Button>
        <Button type="submit" disabled={pending}>
          {editing ? 'Lưu' : 'Thêm'}
        </Button>
      </DialogFooter>
    </form>
  )
}
