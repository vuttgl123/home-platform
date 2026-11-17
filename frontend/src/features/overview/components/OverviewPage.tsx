import { type ReactNode, useId } from 'react'
import { Link } from 'react-router'

import { buttonVariants } from '@/components/ui/button'
import { RecentActivity } from '@/features/activity'
import { ExportButton } from '@/features/exports'
import { ITEM_STATUS_LABEL, type ItemStatus } from '@/features/items'
import { formatNumber } from '@/lib/format'

import type { Overview } from '../types'

const STATUSES: ItemStatus[] = ['IN_USE', 'BROKEN', 'STORED', 'REMOVED']

/**
 * Trang tổng quan của một không gian đã có địa điểm: các con số chính, chỗ ở theo loại, đồ đạc theo
 * trạng thái, việc cần xử lý và hoạt động gần đây. Mỗi số về đồ đạc là liên kết tới trang Đồ đạc đã
 * lọc đúng trạng thái đó, để số bấm vào khớp với danh sách mở ra.
 */
export function OverviewPage({ ws, overview }: { ws: string; overview: Overview }) {
  const itemsWith = (status: ItemStatus) => `/w/${ws}/items?status=${status}`
  const { missingCodes } = overview

  return (
    <div className="flex max-w-5xl flex-col gap-6">
      <header className="flex flex-wrap items-center gap-3">
        <h1 className="flex-1 text-2xl font-semibold tracking-tight">Tổng quan</h1>
        <ExportButton ws={ws} kind="workspace" label="Tải Excel" />
      </header>
      <ul aria-label="Số liệu chính" className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Tile value={overview.places} label="địa điểm" />
        <Tile value={overview.peopleLiving} label={`người đang ở / ${formatNumber(overview.people)} hồ sơ`} />
        <Tile value={overview.itemsByStatus.IN_USE} label="món đồ đang dùng" to={itemsWith('IN_USE')} />
        <Tile value={overview.itemsByStatus.BROKEN} label="món hỏng" to={itemsWith('BROKEN')} />
      </ul>
      <div className="grid gap-6 lg:grid-cols-2">
        <Section title="Chỗ ở theo loại">
          <ul className="flex flex-col divide-y rounded-lg border text-sm">
            {overview.placeTypes.map((type) => (
              <li key={type.typeId} className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1 px-3 py-2">
                <span className="font-medium">{type.name}</span>{' '}
                <span className="text-muted-foreground">
                  {formatNumber(type.occupied)} có người · {formatNumber(type.total - type.occupied)} trống
                </span>
              </li>
            ))}
          </ul>
        </Section>
        <Section title="Đồ đạc theo trạng thái">
          <ul className="flex flex-col divide-y rounded-lg border text-sm">
            {STATUSES.map((status) => (
              <li key={status}>
                <Link to={itemsWith(status)} className="flex items-baseline justify-between gap-4 px-3 py-2 hover:bg-muted">
                  {ITEM_STATUS_LABEL[status]}{' '}
                  <span className="font-medium tabular-nums">{formatNumber(overview.itemsByStatus[status])}</span>
                </Link>
              </li>
            ))}
          </ul>
        </Section>
        {missingCodes.places + missingCodes.items > 0 && (
          <Section title="Cần xử lý">
            <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border px-3 py-2 text-sm">
              <ul className="flex flex-col gap-1">
                {missingCodes.places > 0 && <li>{formatNumber(missingCodes.places)} địa điểm chưa có mã</li>}
                {missingCodes.items > 0 && <li>{formatNumber(missingCodes.items)} đồ chưa có mã</li>}
              </ul>
              {/* Một liên kết thật (không phải nút): nó mở trang Danh mục, nơi có phần "Mã tự cấp". */}
              <Link to={`/w/${ws}/catalog`} className={buttonVariants({ variant: 'outline', size: 'sm' })}>
                Cấp mã
              </Link>
            </div>
          </Section>
        )}
        <RecentActivity ws={ws} />
      </div>
    </div>
  )
}

/** Một ô số lớn; có `to` thì cả ô là một liên kết. */
function Tile({ value, label, to }: { value: number; label: string; to?: string }) {
  const content = (
    <>
      <span className="text-3xl font-semibold tracking-tight tabular-nums">{formatNumber(value)}</span>{' '}
      <span className="text-sm text-muted-foreground">{label}</span>
    </>
  )
  const box = 'flex h-full flex-col gap-1 rounded-lg border p-4'
  return (
    <li>
      {to ? (
        <Link to={to} className={`${box} hover:bg-muted`}>
          {content}
        </Link>
      ) : (
        <div className={box}>{content}</div>
      )}
    </li>
  )
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  const headingId = useId()
  return (
    <section aria-labelledby={headingId} className="flex flex-col gap-2">
      <h2 id={headingId} className="font-medium">
        {title}
      </h2>
      {children}
    </section>
  )
}
