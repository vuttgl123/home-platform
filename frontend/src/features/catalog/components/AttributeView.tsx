import { cn } from 'cn'

import { formatDate, formatNumber } from '@/lib/format'

import type { FieldDef } from '../types'

/**
 * Giá trị các mục tuỳ biến của một bản ghi, theo thứ tự hiển thị.
 *
 * Mục đang dùng luôn có một dòng ("—" khi trống), để người xem biết còn thiếu gì. Mục đã lưu
 * trữ chỉ hiện khi bản ghi còn giữ giá trị cũ, chữ mờ. Khoá không ứng với mục nào thì bỏ qua.
 */
export function AttributeView({ defs, values }: { defs: FieldDef[]; values: Record<string, unknown> | null }) {
  const rows = [...defs]
    .sort((a, b) => a.sortOrder - b.sortOrder)
    .filter((def) => def.status === 'ACTIVE' || !isBlank(values?.[def.fieldKey]))

  if (rows.length === 0) {
    return null
  }
  return (
    <dl className="grid gap-x-6 gap-y-2 sm:grid-cols-[minmax(8rem,max-content)_1fr]">
      {rows.map((def) => {
        const archived = def.status === 'ARCHIVED'
        return (
          <div
            key={def.id}
            data-archived={archived || undefined}
            className={cn('contents', archived && '[&>*]:text-muted-foreground')}
          >
            <dt className="text-muted-foreground">{def.label}</dt>
            <dd className="mb-2 sm:mb-0">{display(def, values?.[def.fieldKey])}</dd>
          </div>
        )
      })}
    </dl>
  )
}

function isBlank(value: unknown): boolean {
  return value == null || value === ''
}

function display(def: FieldDef, value: unknown): string {
  if (isBlank(value)) {
    return '—'
  }
  switch (def.dataType) {
    case 'BOOLEAN':
      return value === true ? 'Có' : 'Không'
    case 'NUMBER':
      return typeof value === 'number' ? formatNumber(value) : String(value)
    case 'DATE':
      return typeof value === 'string' ? formatDate(value) : String(value)
    default:
      return String(value)
  }
}
