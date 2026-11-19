import { cn } from 'cn'

import { blankAsDash } from '../blank'
import type { SnapshotRow } from '../types'

/** Các trường của một bản, dạng nhãn và giá trị; giá trị trống hiện "—". */
export function SnapshotFields({ rows }: { rows: SnapshotRow[] }) {
  return (
    <dl className="grid gap-x-6 gap-y-2 sm:grid-cols-[minmax(7rem,max-content)_1fr]">
      {rows.map((row) => (
        <div key={row.key} data-muted={row.muted || undefined} className={cn('contents', row.muted && '[&>*]:text-muted-foreground')}>
          <dt className="text-muted-foreground">{row.label}</dt>
          <dd className="mb-2 sm:mb-0">{blankAsDash(row.value)}</dd>
        </div>
      ))}
    </dl>
  )
}
