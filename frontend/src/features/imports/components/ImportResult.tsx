import { CircleCheckIcon } from 'lucide-react'
import { Link } from 'react-router'

import { Button, buttonVariants } from '@/components/ui/button'

import type { ImportReport } from '../types'

/** Bước ③ xong: đã lưu bao nhiêu hồ sơ, theo sheet, và lối đi tiếp. */
export function ImportResult({ ws, report, onAgain }: { ws: string; report: ImportReport; onAgain: () => void }) {
  const base = `/w/${ws}`
  const links = [
    { to: base, label: 'Tổng quan' },
    { to: `${base}/people`, label: 'Người' },
    { to: `${base}/items`, label: 'Đồ đạc' },
    { to: `${base}/activity`, label: 'Hoạt động' },
  ]
  return (
    <div className="flex flex-col gap-4 rounded-xl border p-6">
      <p className="flex items-center gap-2 text-lg font-semibold">
        <CircleCheckIcon aria-hidden="true" className="size-6 text-emerald-600 dark:text-emerald-400" />
        Đã nhập {report.totals.rows} hồ sơ
      </p>
      <ul className="flex flex-wrap gap-2 text-sm">
        {report.sheets.map((sheet) => (
          <li key={sheet.sheet} className="rounded-full border px-3 py-1">
            {sheet.name}: {sheet.rows}
          </li>
        ))}
      </ul>
      <div className="flex flex-wrap gap-2">
        {links.map((link) => (
          <Link key={link.to} to={link.to} className={buttonVariants({ variant: 'secondary', size: 'sm' })}>
            {link.label}
          </Link>
        ))}
        <Button size="sm" onClick={onAgain}>
          Nhập file khác
        </Button>
      </div>
    </div>
  )
}
