import type { ReactNode } from 'react'

import { ToneBadge } from '@/components/common/ToneBadge'

/** Một phần của trang danh mục: tiêu đề, nút thêm, và bảng. */
export function CatalogSection({
  title,
  description,
  action,
  children,
}: {
  title: string
  description: string
  action: ReactNode
  children: ReactNode
}) {
  return (
    <section aria-label={title} className="flex flex-col gap-3">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div className="flex flex-col gap-0.5">
          <h2 className="text-lg font-semibold tracking-tight">{title}</h2>
          <p className="text-sm text-muted-foreground">{description}</p>
        </div>
        {action}
      </div>
      {children}
    </section>
  )
}

/** Bảng của một phần danh mục; dòng đã lưu trữ chữ mờ. */
export function CatalogTable({ headers, children }: { headers: string[]; children: ReactNode }) {
  return (
    <div className="overflow-x-auto rounded-lg border">
      <table className="w-full text-sm">
        <thead className="bg-muted/50 text-left text-muted-foreground">
          <tr>
            {headers.map((header) => (
              <th key={header} className="px-3 py-2 font-medium">
                {header}
              </th>
            ))}
            <th className="px-3 py-2">
              <span className="sr-only">Thao tác</span>
            </th>
          </tr>
        </thead>
        <tbody className="divide-y">{children}</tbody>
      </table>
    </div>
  )
}

export function StatusBadge({ archived }: { archived: boolean }) {
  return <ToneBadge tone={archived ? 'neutral' : 'success'}>{archived ? 'Đã lưu trữ' : 'Đang dùng'}</ToneBadge>
}
