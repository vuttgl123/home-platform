import { zodResolver } from '@hookform/resolvers/zod'
import { useId, useState } from 'react'
import { useForm, useWatch, type Resolver } from 'react-hook-form'
import { useNavigate } from 'react-router'

import type { ApiError } from '@/api'
import { applyServerErrors } from '@/components/common/serverErrors'
import { FormError } from '@/components/common/FormError'
import { TextField } from '@/components/common/TextField'
import { CodeNote } from '@/components/common/CodeNote'
import { VersionConflictAlert } from '@/components/common/VersionConflictAlert'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'
import { Textarea } from '@/components/ui/textarea'
import { toast } from '@/components/ui/toast'
import { AttributeFields, attributePayload, useFieldDefs, usePlaceTypes, type FieldDef } from '@/features/catalog'

import { useCreatePlace, useFetchFreshPlace, useUpdatePlace } from '../queries'
import { placeUrl } from '../routes'
import type { PlaceDetail } from '../types'
import { placeFormSchema, placeFormValues, suggestedTypeId, type PlaceFormValues } from './placeForm'

type Mode = { mode: 'create'; parent: PlaceDetail | null } | { mode: 'edit'; place: PlaceDetail }

const NO_DEFS: FieldDef[] = []

const FIXED_FIELDS = new Set(['placeTypeId', 'name', 'status', 'description'])

/**
 * Hộp thoại thêm hoặc sửa một địa điểm.
 *
 * - Thêm: `parent` là địa điểm sẽ chứa nó (`null`: cấp cao nhất). Thêm xong thì tới trang của nó.
 * - Sửa: gửi kèm `version` lúc mở form. Gặp 409 "đã có người sửa" thì form giữ nguyên chữ đã
 *   nhập và cho tải bản mới.
 */
export function PlaceFormDialog({
  ws,
  open,
  onOpenChange,
  ...mode
}: { ws: string; open: boolean; onOpenChange: (open: boolean) => void } & Mode) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{mode.mode === 'create' ? 'Thêm địa điểm' : 'Sửa địa điểm'}</DialogTitle>
        </DialogHeader>
        {/* Nội dung chỉ dựng khi mở, nên mỗi lần mở là một form mới. */}
        <PlaceForm ws={ws} onDone={() => onOpenChange(false)} {...mode} />
      </DialogContent>
    </Dialog>
  )
}

function PlaceForm({ ws, onDone, ...mode }: { ws: string; onDone: () => void } & Mode) {
  const navigate = useNavigate()
  const types = usePlaceTypes(ws).data ?? []
  const editing = mode.mode === 'edit' ? mode.place : null
  // Bản đang sửa; đổi khi người dùng tải bản mới sau một lần 409.
  const [base, setBase] = useState(editing)

  // Mục tuỳ biến phụ thuộc loại đang chọn, nên schema được dựng lại mỗi lần render; React Hook
  // Form dùng `resolver` của lần render gần nhất.
  const [defs, setDefs] = useState<FieldDef[]>([])
  const form = useForm<PlaceFormValues>({
    resolver: zodResolver(placeFormSchema(defs)) as Resolver<PlaceFormValues>,
    defaultValues: base
      ? placeFormValues(base, [])
      : {
          placeTypeId: suggestedTypeId(types, mode.mode === 'create' ? (mode.parent?.placeTypeId ?? null) : null),
          name: '',
          status: 'ACTIVE',
          description: '',
          attributes: {},
        },
  })
  const typeId = useWatch({ control: form.control, name: 'placeTypeId' })
  const fieldDefs = useFieldDefs(ws, 'PLACE', typeId || undefined)
  const loadedDefs = fieldDefs.data ?? NO_DEFS
  if (loadedDefs !== defs) {
    setDefs(loadedDefs)
  }

  const create = useCreatePlace(ws)
  const update = useUpdatePlace(ws)
  const fetchFresh = useFetchFreshPlace(ws)
  const [formErrors, setFormErrors] = useState<string[]>([])
  const [conflict, setConflict] = useState(false)
  const [reloading, setReloading] = useState(false)

  const typeOptions = [...types]
    .filter((type) => type.status === 'ACTIVE' || type.id === base?.placeTypeId)
    .sort((a, b) => a.sortOrder - b.sortOrder)
  const activeKeys = new Set(defs.filter((def) => def.status === 'ACTIVE').map((def) => def.fieldKey))
  const isField = (name: string) =>
    FIXED_FIELDS.has(name) || (name.startsWith('attributes.') && activeKeys.has(name.slice('attributes.'.length)))

  function onError(error: ApiError) {
    if (error.isVersionConflict) {
      setConflict(true)
      return
    }
    setFormErrors(applyServerErrors(error, (name, value) => form.setError(name as keyof PlaceFormValues, value), isField))
  }

  async function reload() {
    if (!base) return
    setReloading(true)
    try {
      const fresh = await fetchFresh(base.id)
      setBase(fresh)
      // Ô người dùng đã sửa giữ nguyên; ô chưa đụng tới nhận giá trị mới.
      form.reset(placeFormValues(fresh, defs), { keepDirtyValues: true })
      setConflict(false)
      update.reset()
    } catch (error) {
      setFormErrors([error instanceof Error ? error.message : String(error)])
    } finally {
      setReloading(false)
    }
  }

  const submit = form.handleSubmit((values) => {
    setFormErrors([])
    const common = {
      placeTypeId: values.placeTypeId,
      name: values.name,
      status: values.status,
      description: values.description || undefined,
      attributes: attributePayload(defs, values.attributes, base?.attributes ?? null) ?? undefined,
    }
    if (base) {
      update.mutate(
        // Lệnh sửa ghi đè cả bản ghi: gửi lại cả những trường form không có ô (ảnh bìa).
        { id: base.id, body: { ...common, version: base.version, coverFileId: base.coverFileId ?? undefined } },
        {
          onSuccess: () => {
            toast.add({ title: 'Đã lưu thay đổi', type: 'success' })
            onDone()
          },
          onError,
        },
      )
    } else {
      const parentId = mode.mode === 'create' ? mode.parent?.id : undefined
      create.mutate(
        { ...common, parentId },
        {
          onSuccess: (created) => {
            toast.add({ title: created.code ? `Đã thêm ${created.name} (${created.code})` : `Đã thêm ${created.name}`, type: 'success' })
            onDone()
            void navigate(placeUrl(ws, created.id))
          },
          onError,
        },
      )
    }
  })

  const { errors } = form.formState
  const pending = create.isPending || update.isPending
  // Chưa biết mục tuỳ biến của loại đang chọn thì chưa gửi được: gửi lúc đó sẽ làm mất giá trị của chúng.
  const defsReady = !typeId || fieldDefs.isSuccess
  const typeFieldId = useId()
  const statusFieldId = useId()
  const descriptionId = useId()

  return (
    <form noValidate onSubmit={submit} className="flex flex-col gap-4">
      {mode.mode === 'create' && mode.parent && (
        <p className="text-sm text-muted-foreground">Nằm trong {mode.parent.name}</p>
      )}
      <FieldGroup className="gap-4">
        <Field data-invalid={errors.placeTypeId ? true : undefined}>
          <FieldLabel htmlFor={typeFieldId}>Loại</FieldLabel>
          <NativeSelect id={typeFieldId} className="w-full" {...form.register('placeTypeId')}>
            {typeOptions.map((type) => (
              <NativeSelectOption key={type.id} value={type.id}>
                {type.status === 'ARCHIVED' ? `${type.name} (đã lưu trữ)` : type.name}
              </NativeSelectOption>
            ))}
          </NativeSelect>
          {errors.placeTypeId && <FieldError>{errors.placeTypeId.message}</FieldError>}
        </Field>
        <TextField label="Tên" required autoComplete="off" error={errors.name?.message} {...form.register('name')} />
        <CodeNote code={editing ? (base?.code ?? null) : undefined} />
        <Field>
          <FieldLabel htmlFor={statusFieldId}>Trạng thái</FieldLabel>
          <NativeSelect id={statusFieldId} className="w-full" {...form.register('status')}>
            <NativeSelectOption value="ACTIVE">Đang dùng</NativeSelectOption>
            <NativeSelectOption value="INACTIVE">Tạm ngưng</NativeSelectOption>
          </NativeSelect>
        </Field>
        <Field data-invalid={errors.description ? true : undefined}>
          <FieldLabel htmlFor={descriptionId}>Mô tả</FieldLabel>
          <Textarea id={descriptionId} rows={3} {...form.register('description')} />
          {errors.description && <FieldError>{errors.description.message}</FieldError>}
        </Field>
        <AttributeFields defs={defs} control={form.control} stored={base?.attributes} />
      </FieldGroup>
      {conflict && <VersionConflictAlert onReload={() => void reload()} isReloading={reloading} />}
      <FormError messages={formErrors} />
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onDone}>
          Huỷ
        </Button>
        <Button type="submit" disabled={pending || conflict || !defsReady}>
          {editing ? 'Lưu' : 'Thêm'}
        </Button>
      </DialogFooter>
    </form>
  )
}
