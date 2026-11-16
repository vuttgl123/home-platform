import type { ReactNode } from 'react'

/** Các trường của một bản, dạng nhãn và giá trị; giá trị trống hiện "—". */
export function SnapshotFields({ rows }: { rows: [string, ReactNode][] }) {
  return (
    <dl className="grid gap-x-6 gap-y-2 sm:grid-cols-[minmax(7rem,max-content)_1fr]">
      {rows.map(([label, value]) => (
        <div key={label} className="contents">
          <dt className="text-muted-foreground">{label}</dt>
          <dd className="mb-2 sm:mb-0">{value == null || value === '' ? '—' : value}</dd>
        </div>
      ))}
    </dl>
  )
}
