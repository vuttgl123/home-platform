import { useId } from 'react'
import { Controller, type UseFormReturn } from 'react-hook-form'

import { TextField } from '@/components/common/TextField'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'
import { Textarea } from '@/components/ui/textarea'
import { AttributeFields, useItemCategories, type FieldDef } from '@/features/catalog'

import { ITEM_STATUS_LABEL } from '../labels'
import type { ItemDetail } from '../types'
import type { ItemFormValues } from './itemForm'

const UNITS = ['cái', 'chiếc', 'bộ', 'đôi', 'thùng']

/**
 * Các ô của một món đồ, dùng chung cho form thêm, sửa và thay.
 * @param keep món đang sửa: nhóm đã lưu trữ của nó vẫn hiện trong danh sách chọn
 */
export function ItemFields({
  ws,
  form,
  defs,
  keep,
}: {
  ws: string
  form: UseFormReturn<ItemFormValues>
  defs: FieldDef[]
  keep?: ItemDetail | null
}) {
  const categories = (useItemCategories(ws).data ?? []).filter(
    (category) => category.status === 'ACTIVE' || category.id === keep?.categoryId,
  )
  const { errors } = form.formState
  const categoryId = useId()
  const statusId = useId()
  const descriptionId = useId()
  const unitList = useId()

  return (
    <FieldGroup className="gap-4">
      <Field>
        <FieldLabel htmlFor={categoryId}>Nhóm</FieldLabel>
        {/* Có điều khiển: danh sách nhóm thường tải xong sau khi form dựng; ô không điều khiển
            sẽ rơi về "Không nhóm" và gửi đi giá trị đó. */}
        <Controller
          name="categoryId"
          control={form.control}
          render={({ field }) => (
            <NativeSelect id={categoryId} className="w-full" {...field}>
              <NativeSelectOption value="">Không nhóm</NativeSelectOption>
              {categories.map((category) => (
                <NativeSelectOption key={category.id} value={category.id}>
                  {category.status === 'ARCHIVED' ? `${category.name} (đã lưu trữ)` : category.name}
                </NativeSelectOption>
              ))}
            </NativeSelect>
          )}
        />
      </Field>
      <TextField label="Tên" required autoComplete="off" error={errors.name?.message} {...form.register('name')} />
      <div className="grid gap-4 sm:grid-cols-3">
        <TextField label="Số lượng" inputMode="numeric" error={errors.quantity?.message} {...form.register('quantity')} />
        <TextField label="Đơn vị" list={unitList} autoComplete="off" error={errors.unit?.message} {...form.register('unit')} />
        <TextField label="Mã" autoComplete="off" error={errors.code?.message} {...form.register('code')} />
      </div>
      <datalist id={unitList}>
        {UNITS.map((unit) => (
          <option key={unit} value={unit} />
        ))}
      </datalist>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field>
          <FieldLabel htmlFor={statusId}>Trạng thái</FieldLabel>
          <NativeSelect id={statusId} className="w-full" {...form.register('status')}>
            {(['IN_USE', 'BROKEN', 'STORED'] as const).map((status) => (
              <NativeSelectOption key={status} value={status}>
                {ITEM_STATUS_LABEL[status]}
              </NativeSelectOption>
            ))}
          </NativeSelect>
        </Field>
        <TextField label="Ngày đặt" type="date" error={errors.placedAt?.message} {...form.register('placedAt')} />
      </div>
      <Field data-invalid={errors.description ? true : undefined}>
        <FieldLabel htmlFor={descriptionId}>Mô tả</FieldLabel>
        <Textarea id={descriptionId} rows={2} {...form.register('description')} />
        {errors.description && <FieldError>{errors.description.message}</FieldError>}
      </Field>
      <AttributeFields defs={defs} control={form.control} stored={keep?.attributes} />
    </FieldGroup>
  )
}
