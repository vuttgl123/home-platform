import { zodResolver } from '@hookform/resolvers/zod'
import { useId, useState } from 'react'
import { useForm, type Resolver } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'

import type { ApiError } from '@/api'
import { FormError } from '@/components/common/FormError'
import { applyServerErrors } from '@/components/common/serverErrors'
import { TextField } from '@/components/common/TextField'
import { VersionConflictAlert } from '@/components/common/VersionConflictAlert'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'
import { Textarea } from '@/components/ui/textarea'
import { toast } from '@/components/ui/toast'
import { AttributeFields, useFieldDefs, type FieldDef } from '@/features/catalog'

import { GENDER_LABEL } from '../labels'
import { useCreatePerson, useFetchFreshPerson, useUpdatePerson } from '../queries'
import type { PersonDetail } from '../types'
import {
  duplicateDocumentOwner,
  emptyPersonForm,
  personFormSchema,
  personFormValues,
  personPayload,
  type PersonFormValues,
} from './personForm'

type Mode = { mode: 'create'; initialName?: string } | { mode: 'edit'; person: PersonDetail }

const NO_DEFS: FieldDef[] = []
const FIXED_FIELDS = new Set(['fullName', 'phone', 'email', 'gender', 'dateOfBirth', 'idType', 'idNumber', 'note'])
const DOCUMENT_TYPES = ['CCCD', 'CMND', 'Hộ chiếu']

/**
 * Hộp thoại thêm hoặc sửa hồ sơ một người. Thêm xong thì tới trang của người đó, trừ khi nơi gọi
 * tự xử lý (`onSaved`, ví dụ form người ở cần người vừa tạo).
 */
export function PersonFormDialog({
  ws,
  open,
  onOpenChange,
  onSaved,
  ...mode
}: {
  ws: string
  open: boolean
  onOpenChange: (open: boolean) => void
  onSaved?: (person: PersonDetail) => void
} & Mode) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{mode.mode === 'create' ? 'Thêm người' : 'Sửa hồ sơ'}</DialogTitle>
        </DialogHeader>
        <PersonForm ws={ws} onDone={() => onOpenChange(false)} onSaved={onSaved} {...mode} />
      </DialogContent>
    </Dialog>
  )
}

function PersonForm({
  ws,
  onDone,
  onSaved,
  ...mode
}: { ws: string; onDone: () => void; onSaved?: (person: PersonDetail) => void } & Mode) {
  const navigate = useNavigate()
  const [base, setBase] = useState(mode.mode === 'edit' ? mode.person : null)
  const fieldDefs = useFieldDefs(ws, 'PERSON', '0')
  const defs = fieldDefs.data ?? NO_DEFS
  const form = useForm<PersonFormValues>({
    resolver: zodResolver(personFormSchema(defs)) as Resolver<PersonFormValues>,
    defaultValues: base ? personFormValues(base, []) : emptyPersonForm(mode.mode === 'create' ? mode.initialName : ''),
  })
  const create = useCreatePerson(ws)
  const update = useUpdatePerson(ws)
  const fetchFresh = useFetchFreshPerson(ws)
  const [formErrors, setFormErrors] = useState<string[]>([])
  const [duplicateOf, setDuplicateOf] = useState<string | null>(null)
  const [conflict, setConflict] = useState(false)
  const [reloading, setReloading] = useState(false)
  const activeKeys = new Set(defs.filter((def) => def.status === 'ACTIVE').map((def) => def.fieldKey))
  const isField = (name: string) =>
    FIXED_FIELDS.has(name) || (name.startsWith('attributes.') && activeKeys.has(name.slice('attributes.'.length)))

  function onError(error: ApiError) {
    if (error.isVersionConflict) {
      setConflict(true)
      return
    }
    const owner = error.status === 409 ? duplicateDocumentOwner(error.detail) : null
    if (owner) {
      setDuplicateOf(owner)
      return
    }
    setFormErrors(applyServerErrors(error, (name, value) => form.setError(name as keyof PersonFormValues, value), isField))
  }

  async function reload() {
    if (!base) return
    setReloading(true)
    try {
      const fresh = await fetchFresh(base.id)
      setBase(fresh)
      form.reset(personFormValues(fresh, defs), { keepDirtyValues: true })
      setConflict(false)
      update.reset()
    } finally {
      setReloading(false)
    }
  }

  const submit = form.handleSubmit((values) => {
    setFormErrors([])
    setDuplicateOf(null)
    const body = personPayload(values, defs, base)
    const done = (person: PersonDetail, created: boolean) => {
      toast.add({ title: created ? `Đã thêm ${person.fullName}` : 'Đã lưu thay đổi', type: 'success' })
      onDone()
      if (onSaved) {
        onSaved(person)
      } else if (created) {
        void navigate(`/w/${ws}/people/${person.id}`)
      }
    }
    if (base) {
      update.mutate(
        // Lệnh sửa ghi đè cả bản ghi: gửi lại cả ảnh đại diện, thứ form không có ô.
        { id: base.id, body: { ...body, version: base.version, avatarFileId: base.avatarFileId ?? undefined } },
        { onSuccess: (person) => done(person, false), onError },
      )
    } else {
      create.mutate(body, { onSuccess: (person) => done(person, true), onError })
    }
  })

  const { errors } = form.formState
  const pending = create.isPending || update.isPending
  const genderId = useId()
  const noteId = useId()
  const documentListId = useId()

  return (
    <form noValidate onSubmit={submit} className="flex flex-col gap-4">
      <FieldGroup className="gap-4">
        <TextField label="Họ tên" required autoComplete="off" error={errors.fullName?.message} {...form.register('fullName')} />
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField label="Số điện thoại" type="tel" error={errors.phone?.message} {...form.register('phone')} />
          <TextField label="Email" type="email" error={errors.email?.message} {...form.register('email')} />
          <Field>
            <FieldLabel htmlFor={genderId}>Giới tính</FieldLabel>
            <NativeSelect id={genderId} className="w-full" {...form.register('gender')}>
              <NativeSelectOption value="">—</NativeSelectOption>
              {Object.entries(GENDER_LABEL).map(([value, label]) => (
                <NativeSelectOption key={value} value={value}>
                  {label}
                </NativeSelectOption>
              ))}
            </NativeSelect>
          </Field>
          <TextField label="Ngày sinh" type="date" error={errors.dateOfBirth?.message} {...form.register('dateOfBirth')} />
          <TextField
            label="Loại giấy tờ"
            list={documentListId}
            autoComplete="off"
            error={errors.idType?.message}
            {...form.register('idType')}
          />
          <TextField label="Số giấy tờ" autoComplete="off" error={errors.idNumber?.message} {...form.register('idNumber')} />
        </div>
        <datalist id={documentListId}>
          {DOCUMENT_TYPES.map((type) => (
            <option key={type} value={type} />
          ))}
        </datalist>
        <Field data-invalid={errors.note ? true : undefined}>
          <FieldLabel htmlFor={noteId}>Ghi chú</FieldLabel>
          <Textarea id={noteId} rows={3} {...form.register('note')} />
          {errors.note && <FieldError>{errors.note.message}</FieldError>}
        </Field>
        <AttributeFields defs={defs} control={form.control} stored={base?.attributes} />
      </FieldGroup>
      {duplicateOf && (
        <div role="alert" className="rounded-lg border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm text-destructive">
          Đã có người mang giấy tờ này.{' '}
          <Link to={`/w/${ws}/people/${duplicateOf}`} className="font-medium underline underline-offset-4" onClick={onDone}>
            Mở hồ sơ đã có
          </Link>
        </div>
      )}
      {conflict && <VersionConflictAlert onReload={() => void reload()} isReloading={reloading} />}
      <FormError messages={formErrors} />
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onDone}>
          Huỷ
        </Button>
        <Button type="submit" disabled={pending || conflict || !fieldDefs.isSuccess}>
          {base ? 'Lưu' : 'Thêm'}
        </Button>
      </DialogFooter>
    </form>
  )
}
