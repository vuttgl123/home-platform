import { cn } from 'cn'
import { useId, type ReactNode } from 'react'

import { Card } from '@/components/ui/card'

/**
 * Một khối nội dung có tiêu đề trên trang chi tiết (Thông tin, Địa chỉ, Mục tuỳ biến…). Là một
 * vùng có tên (role region) để công cụ đọc màn hình và test tìm được theo tiêu đề.
 */
export function InfoCard({ title, actions, children, className }: { title: string; actions?: ReactNode; children: ReactNode; className?: string }) {
  const headingId = useId()
  return (
    <section aria-labelledby={headingId} className={className}>
      <Card className="h-full gap-3 px-5 py-4">
        <div className="flex items-center justify-between gap-2">
          <h2 id={headingId} className="font-semibold">
            {title}
          </h2>
          {actions}
        </div>
        {children}
      </Card>
    </section>
  )
}

/** Các dòng nhãn và giá trị trong một khối; giá trị trống hiện "—". */
export function InfoList({ rows, className }: { rows: { label: string; value: ReactNode }[]; className?: string }) {
  return (
    <dl className={cn('grid gap-x-6 gap-y-2 text-sm sm:grid-cols-[minmax(7rem,max-content)_1fr]', className)}>
      {rows.map(({ label, value }) => (
        <div key={label} className="contents">
          <dt className="text-muted-foreground">{label}</dt>
          <dd className="mb-2 sm:mb-0">{value == null || value === '' ? '—' : value}</dd>
        </div>
      ))}
    </dl>
  )
}
