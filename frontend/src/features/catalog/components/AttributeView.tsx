import { cn } from 'cn'

import { attributeRows } from '../attributes'
import type { FieldDef } from '../types'

/** Giá trị các mục tuỳ biến của một bản ghi (xem `attributeRows`); mục đã lưu trữ chữ mờ. */
export function AttributeView({ defs, values }: { defs: FieldDef[]; values: Record<string, unknown> | null }) {
  const rows = attributeRows(defs, values)
  if (rows.length === 0) {
    return null
  }
  return (
    <dl className="grid gap-x-6 gap-y-2 sm:grid-cols-[minmax(8rem,max-content)_1fr]">
      {rows.map((row) => (
        <div key={row.key} data-archived={row.archived || undefined} className={cn('contents', row.archived && '[&>*]:text-muted-foreground')}>
          <dt className="text-muted-foreground">{row.label}</dt>
          <dd className="mb-2 sm:mb-0">{row.display}</dd>
        </div>
      ))}
    </dl>
  )
}
