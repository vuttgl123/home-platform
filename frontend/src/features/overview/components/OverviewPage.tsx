import { cn } from 'cn'
import { Building2Icon, PackageCheckIcon, TriangleAlertIcon, UsersIcon, WrenchIcon, type LucideIcon } from 'lucide-react'
import { type ReactNode, useId } from 'react'
import { Link } from 'react-router'
import { Bar, BarChart, Cell, XAxis, YAxis } from 'recharts'

import type { Tone } from '@/components/common/ToneBadge'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { buttonVariants } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { ChartContainer, type ChartConfig } from '@/components/ui/chart'
import { Progress } from '@/components/ui/progress'
import { RecentActivity } from '@/features/activity'
import { ExportButton } from '@/features/exports'
import { ITEM_STATUS_LABEL, ITEM_STATUS_TONE, type ItemStatus } from '@/features/items'
import { formatNumber } from '@/lib/format'

import type { Overview } from '../types'

const STATUSES: ItemStatus[] = ['IN_USE', 'BROKEN', 'STORED', 'REMOVED']

/** Màu của ô biểu tượng và của cột biểu đồ theo nghĩa (token trong `index.css`). */
const TONE_CHIP: Record<Tone, string> = {
  primary: 'bg-primary/12 text-primary',
  success: 'bg-success/12 text-success-foreground',
  warning: 'bg-warning/16 text-warning-foreground',
  danger: 'bg-danger/12 text-danger-foreground',
  info: 'bg-info/12 text-info-foreground',
  violet: 'bg-violet/12 text-violet-foreground',
  neutral: 'bg-muted text-muted-foreground',
}
const TONE_FILL: Record<Tone, string> = {
  primary: 'var(--primary)',
  success: 'var(--success)',
  warning: 'var(--warning)',
  danger: 'var(--danger)',
  info: 'var(--info)',
  violet: 'var(--violet)',
  neutral: 'var(--muted-foreground)',
}

/**
 * Trang tổng quan của một không gian đã có địa điểm: các con số chính, chỗ ở theo loại, đồ đạc theo
 * trạng thái, việc cần xử lý và hoạt động gần đây. Mỗi số về đồ đạc là liên kết tới trang Đồ đạc đã
 * lọc đúng trạng thái đó, để số bấm vào khớp với danh sách mở ra.
 */
export function OverviewPage({ ws, overview }: { ws: string; overview: Overview }) {
  const itemsWith = (status: ItemStatus) => `/w/${ws}/items?status=${status}`
  const { missingCodes } = overview

  return (
    <div className="flex max-w-6xl flex-col gap-6">
      <header className="flex flex-wrap items-center gap-3">
        <div className="flex flex-1 flex-col gap-1">
          <h1 className="text-2xl font-semibold tracking-tight">Tổng quan</h1>
          <p className="text-sm text-muted-foreground">Chỗ ở, đồ đạc và những thay đổi mới nhất của không gian này.</p>
        </div>
        <ExportButton ws={ws} kind="workspace" label="Tải Excel" />
      </header>
      <ul aria-label="Số liệu chính" className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Tile icon={Building2Icon} tone="primary" value={overview.places} label="địa điểm" />
        <Tile
          icon={UsersIcon}
          tone="success"
          value={overview.peopleLiving}
          label={`người đang ở / ${formatNumber(overview.people)} hồ sơ`}
        />
        <Tile icon={PackageCheckIcon} tone="success" value={overview.itemsByStatus.IN_USE} label="món đồ đang dùng" to={itemsWith('IN_USE')} />
        <Tile icon={WrenchIcon} tone="danger" value={overview.itemsByStatus.BROKEN} label="món hỏng" to={itemsWith('BROKEN')} />
      </ul>
      {missingCodes.places + missingCodes.items > 0 && (
        <Section title="Cần xử lý">
          <Alert className="border-warning/40 bg-warning/8">
            <TriangleAlertIcon className="text-warning-foreground" aria-hidden="true" />
            <AlertTitle className="sr-only">Bản ghi chưa có mã</AlertTitle>
            <AlertDescription className="flex flex-wrap items-center justify-between gap-3 text-foreground">
              <ul className="flex flex-col gap-1">
                {missingCodes.places > 0 && <li>{formatNumber(missingCodes.places)} địa điểm chưa có mã</li>}
                {missingCodes.items > 0 && <li>{formatNumber(missingCodes.items)} đồ chưa có mã</li>}
              </ul>
              {/* Một liên kết thật (không phải nút): nó mở trang Danh mục, nơi có phần "Mã tự cấp". */}
              <Link to={`/w/${ws}/catalog`} className={buttonVariants({ variant: 'outline', size: 'sm' })}>
                Cấp mã
              </Link>
            </AlertDescription>
          </Alert>
        </Section>
      )}
      <div className="grid gap-6 lg:grid-cols-2">
        <Section title="Chỗ ở theo loại">
          <Card className="gap-0 py-1">
            <ul className="flex flex-col divide-y text-sm">
              {overview.placeTypes.map((type) => (
                <OccupancyRow key={type.typeId} name={type.name} total={type.total} occupied={type.occupied} />
              ))}
            </ul>
          </Card>
        </Section>
        <Section title="Đồ đạc theo trạng thái">
          <Card className="gap-3 p-4">
            <ItemsChart counts={overview.itemsByStatus} />
            <ul className="grid gap-1 text-sm sm:grid-cols-2">
              {STATUSES.map((status) => (
                <li key={status}>
                  <Link
                    to={itemsWith(status)}
                    className="flex items-center justify-between gap-4 rounded-md px-2 py-1.5 hover:bg-muted"
                  >
                    <span className="inline-flex items-center gap-2">
                      <span aria-hidden="true" className={cn('size-2 rounded-full', TONE_CHIP[ITEM_STATUS_TONE[status]], 'bg-current')} />
                      {ITEM_STATUS_LABEL[status]}
                    </span>{' '}
                    <span className="font-medium tabular-nums">{formatNumber(overview.itemsByStatus[status])}</span>
                  </Link>
                </li>
              ))}
            </ul>
          </Card>
        </Section>
      </div>
      <RecentActivity ws={ws} />
    </div>
  )
}

/** Một ô số lớn có biểu tượng; có `to` thì cả ô là một liên kết. */
function Tile({ icon: Icon, tone, value, label, to }: { icon: LucideIcon; tone: Tone; value: number; label: string; to?: string }) {
  const content = (
    <>
      <span aria-hidden="true" className={cn('flex size-10 shrink-0 items-center justify-center rounded-lg', TONE_CHIP[tone])}>
        <Icon className="size-5" />
      </span>
      <span className="flex flex-col">
        <span className="text-2xl font-semibold tracking-tight tabular-nums">{formatNumber(value)}</span>{' '}
        <span className="text-sm text-muted-foreground">{label}</span>
      </span>
    </>
  )
  const box = 'flex h-full items-center gap-4 rounded-xl border bg-card p-4'
  return (
    <li>
      {to ? (
        <Link to={to} className={cn(box, 'transition-colors hover:border-primary/40 hover:bg-accent')}>
          {content}
        </Link>
      ) : (
        <div className={box}>{content}</div>
      )}
    </li>
  )
}

/**
 * Một loại địa điểm: tên, chữ "có người · trống" và thanh tỷ lệ. Thanh mang giá trị phần trăm làm
 * tròn cho công cụ đọc màn hình; chữ bên cạnh đã nói đủ số.
 */
function OccupancyRow({ name, total, occupied }: { name: string; total: number; occupied: number }) {
  const percent = total === 0 ? 0 : Math.round((occupied / total) * 100)
  return (
    <li className="flex flex-col gap-2 px-4 py-3">
      <span className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
        <span className="font-medium">{name}</span>{' '}
        <span className="text-muted-foreground tabular-nums">
          {formatNumber(occupied)} có người · {formatNumber(total - occupied)} trống
        </span>
      </span>
      <Progress value={percent} aria-label={`Tỷ lệ có người của ${name}`} className="[&_[data-slot=progress-track]]:h-2" />
    </li>
  )
}

/** Cột ngang theo trạng thái đồ; chỉ để nhìn (số và liên kết nằm ở danh sách bên dưới). */
function ItemsChart({ counts }: { counts: Record<ItemStatus, number> }) {
  const data = STATUSES.map((status) => ({ status, label: ITEM_STATUS_LABEL[status], value: counts[status] }))
  const config = Object.fromEntries(
    STATUSES.map((status) => [status, { label: ITEM_STATUS_LABEL[status], color: TONE_FILL[ITEM_STATUS_TONE[status]] }]),
  ) satisfies ChartConfig
  return (
    <ChartContainer config={config} aria-hidden="true" className="aspect-auto h-36 w-full">
      <BarChart data={data} layout="vertical" margin={{ left: 0, right: 12, top: 4, bottom: 4 }} barCategoryGap={8}>
        <XAxis type="number" hide allowDecimals={false} />
        <YAxis type="category" dataKey="label" width={88} tickLine={false} axisLine={false} />
        <Bar dataKey="value" radius={4} isAnimationActive={false}>
          {data.map((row) => (
            <Cell key={row.status} fill={`var(--color-${row.status})`} />
          ))}
        </Bar>
      </BarChart>
    </ChartContainer>
  )
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  const headingId = useId()
  return (
    <section aria-labelledby={headingId} className="flex flex-col gap-2">
      <h2 id={headingId} className="font-semibold">
        {title}
      </h2>
      {children}
    </section>
  )
}
