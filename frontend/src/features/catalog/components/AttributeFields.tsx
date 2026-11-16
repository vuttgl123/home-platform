import { useId } from 'react'
import { Controller, type Control, type FieldValues } from 'react-hook-form'

import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'
import { Switch } from '@/components/ui/switch'

import { attributeDefaults } from '../attributes'
import type { FieldDef } from '../types'

/**
 * Các ô nhập của mục tuỳ biến, đặt trong một form của React Hook Form dưới khoá `attributes`.
 * Chỉ mục đang dùng, theo thứ tự hiển thị.
 *
 * @param stored giá trị đã lưu của bản ghi, làm giá trị ban đầu của các ô. Cần vì danh sách mục
 *   thường tải xong sau khi form đã dựng.
 */
export function AttributeFields<T extends FieldValues>({
  defs,
  control,
  stored = null,
}: {
  defs: FieldDef[]
  control: Control<T>
  stored?: Record<string, unknown> | null
}) {
  const defaults = attributeDefaults(defs, stored)
  const shown = defs.filter((def) => def.status === 'ACTIVE').sort((a, b) => a.sortOrder - b.sortOrder)
  return (
    <>
      {shown.map((def) => (
        <AttributeField
          key={def.id}
          def={def}
          defaultValue={defaults[def.fieldKey]}
          control={control as unknown as Control<FieldValues>}
        />
      ))}
    </>
  )
}

function AttributeField({
  def,
  defaultValue,
  control,
}: {
  def: FieldDef
  defaultValue: string | boolean | undefined
  control: Control<FieldValues>
}) {
  const id = useId()
  return (
    <Controller
      name={`attributes.${def.fieldKey}`}
      control={control}
      defaultValue={defaultValue}
      render={({ field, fieldState }) => {
        const invalid = fieldState.error ? true : undefined
        const label = (
          <FieldLabel htmlFor={id}>
            {def.label}
            {def.required && <span aria-hidden="true">*</span>}
          </FieldLabel>
        )
        if (def.dataType === 'BOOLEAN') {
          return (
            <Field orientation="horizontal" data-invalid={invalid}>
              <Switch id={id} checked={field.value === true} onCheckedChange={field.onChange} onBlur={field.onBlur} />
              {label}
              {fieldState.error && <FieldError>{fieldState.error.message}</FieldError>}
            </Field>
          )
        }
        const common = {
          id,
          name: field.name,
          ref: field.ref,
          value: (field.value as string | undefined) ?? '',
          onChange: field.onChange,
          onBlur: field.onBlur,
          'aria-invalid': invalid,
          'aria-required': def.required || undefined,
        }
        return (
          <Field data-invalid={invalid}>
            {label}
            {def.dataType === 'SELECT' ? (
              <NativeSelect {...common} className="w-full">
                <NativeSelectOption value="">—</NativeSelectOption>
                {(def.options ?? []).map((option) => (
                  <NativeSelectOption key={option} value={option}>
                    {option}
                  </NativeSelectOption>
                ))}
              </NativeSelect>
            ) : (
              <Input
                {...common}
                type={def.dataType === 'DATE' ? 'date' : 'text'}
                inputMode={def.dataType === 'NUMBER' ? 'decimal' : undefined}
                placeholder={def.dataType === 'NUMBER' ? 'Ví dụ: 25 hoặc 25.5' : undefined}
              />
            )}
            {fieldState.error && <FieldError>{fieldState.error.message}</FieldError>}
          </Field>
        )
      }}
    />
  )
}
