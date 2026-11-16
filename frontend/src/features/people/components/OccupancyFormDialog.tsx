import { zodResolver } from '@hookform/resolvers/zod'
import { useQueryClient } from '@tanstack/react-query'
import { useId, useState } from 'react'
import { useForm } from 'react-hook-form'

import type { ApiError } from '@/api'
import { FormError } from '@/components/common/FormError'
import { TextField } from '@/components/common/TextField'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'
import { Textarea } from '@/components/ui/textarea'
import { toast } from '@/components/ui/toast'
import { PlacePicker, type PlaceChoice } from '@/features/places'
import { todayInBusinessZone } from '@/lib/dates'

import { RELATION_LABEL } from '../labels'
import { refreshOccupancies, useCreateOccupancy, useUpdateOccupancy } from '../queries'
import type { Occupancy, Relation } from '../types'
import { occupancyFormSchema, STALE_ROW, type OccupancyFormValues } from './occupancyForm'
import { PersonChoiceField } from './PersonChoiceField'
import { usePersonChoice } from './personChoice'

type Mode =
  | { mode: 'create'; place?: { id: string; name: string }; person?: { id: string; fullName: string } }
  | { mode: 'edit'; occupancy: Occupancy }

function titleOf(mode: Mode): string {
  if (mode.mode === 'edit') return 'Sửa cư trú'
  return mode.place ? 'Thêm người ở' : 'Thêm nơi ở'
}

/**
 * Hộp thoại thêm hoặc sửa một dòng cư trú. Thêm từ một địa điểm thì chọn người (hoặc tạo người
 * mới ngay trong form); thêm từ trang một người thì chọn địa điểm.
 */
export function OccupancyFormDialog({
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
        <OccupancyForm ws={ws} onDone={() => onOpenChange(false)} {...mode} />
      </DialogContent>
    </Dialog>
  )
}

function OccupancyForm({ ws, onDone, ...mode }: { ws: string; onDone: () => void } & Mode) {
  const queryClient = useQueryClient()
  const editing = mode.mode === 'edit' ? mode.occupancy : null
  const fixedPlace = mode.mode === 'create' ? mode.place : null
  const fixedPerson = mode.mode === 'create' ? mode.person : null
  const person = usePersonChoice(ws)
  const [place, setPlace] = useState<PlaceChoice | null>(null)
  const [formErrors, setFormErrors] = useState<string[]>([])
  const form = useForm<OccupancyFormValues>({
    resolver: zodResolver(occupancyFormSchema),
    defaultValues: editing
      ? { relation: editing.relation, startDate: editing.startDate, endDate: editing.endDate ?? '', note: editing.note ?? '' }
      : { relation: 'RESIDENT', startDate: todayInBusinessZone(), endDate: '', note: '' },
  })
  const create = useCreateOccupancy(ws)
  const update = useUpdateOccupancy(ws)
  const relationId = useId()
  const noteId = useId()
  const pending = person.isCreating || create.isPending || update.isPending
  // "Chủ" ghi ở phần Sở hữu; chỉ dòng cư trú cũ đang là "Chủ" còn giữ lựa chọn này khi sửa.
  const relations = (Object.keys(RELATION_LABEL) as Relation[]).filter(
    (relation) => relation !== 'OWNER' || editing?.relation === 'OWNER',
  )

  function fail(error: unknown) {
    const apiError = error as ApiError
    if (apiError.isVersionConflict) {
      setFormErrors([STALE_ROW])
      if (editing) refreshOccupancies(queryClient, ws, editing.personId)
    } else {
      setFormErrors([apiError.detail ?? String(error)])
    }
  }

  async function personIdForCreate(): Promise<string | null> {
    if (fixedPerson) return fixedPerson.id
    return (await person.resolve())?.id ?? null
  }

  const submit = form.handleSubmit(async (values) => {
    setFormErrors([])
    const common = {
      relation: values.relation,
      startDate: values.startDate,
      endDate: values.endDate || undefined,
      note: values.note || undefined,
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
      const personId = await personIdForCreate()
      const created = await create.mutateAsync({ ...common, placeId: placeId!, personId: personId! })
      toast.add({ title: `Đã thêm ${created.personFullName} ở ${created.placeName}`, type: 'success' })
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
      <Field>
        <FieldLabel htmlFor={relationId}>Quan hệ</FieldLabel>
        <NativeSelect id={relationId} className="w-full" {...form.register('relation')}>
          {relations.map((relation) => (
            <NativeSelectOption key={relation} value={relation}>
              {RELATION_LABEL[relation]}
            </NativeSelectOption>
          ))}
        </NativeSelect>
      </Field>
      <div className="grid gap-4 sm:grid-cols-2">
        <TextField label="Từ ngày" type="date" required error={errors.startDate?.message} {...form.register('startDate')} />
        <TextField
          label="Tới ngày"
          type="date"
          description="Để trống nếu vẫn đang ở"
          error={errors.endDate?.message}
          {...form.register('endDate')}
        />
      </div>
      <Field data-invalid={errors.note ? true : undefined}>
        <FieldLabel htmlFor={noteId}>Ghi chú</FieldLabel>
        <Textarea id={noteId} rows={2} {...form.register('note')} />
        {errors.note && <FieldError>{errors.note.message}</FieldError>}
      </Field>
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
