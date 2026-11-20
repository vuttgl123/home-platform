import { useId, type ComponentProps, type ReactNode } from 'react'

import { Field, FieldDescription, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'

import { DateInput } from './DateInput'

/**
 * Một ô nhập có nhãn và chỗ hiện lỗi. Nhận mọi thuộc tính của `<input>`, nên dùng được với
 * `register(...)` của React Hook Form. Ô `type="date"` có thêm nút mở lịch (`DateInput`).
 */
export function TextField({
  label,
  error,
  description,
  required,
  ...input
}: ComponentProps<'input'> & { label: ReactNode; error?: string; description?: ReactNode }) {
  const id = useId()
  return (
    <Field data-invalid={error ? true : undefined}>
      <FieldLabel htmlFor={id}>
        {label}
        {required && <span aria-hidden="true">*</span>}
      </FieldLabel>
      {input.type === 'date' ? (
        <DateInput
          id={id}
          aria-invalid={error ? true : undefined}
          aria-required={required}
          pickerLabel={typeof label === 'string' ? `Chọn ${label} trên lịch` : 'Chọn ngày trên lịch'}
          {...input}
        />
      ) : (
        <Input id={id} aria-invalid={error ? true : undefined} aria-required={required} {...input} />
      )}
      {description && <FieldDescription>{description}</FieldDescription>}
      {error && <FieldError>{error}</FieldError>}
    </Field>
  )
}
