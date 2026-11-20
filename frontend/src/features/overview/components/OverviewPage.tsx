import { cn } from 'cn'
import { Building2Icon, PackageCheckIcon, TriangleAlertIcon, UsersIcon, WrenchIcon, type LucideIcon } from 'lucide-react'
import { type ReactNode, useId } from 'react'
import { Link } from 'react-router'
import { Bar, BarChart, Cell, Pie, PieChart, XAxis, YAxis } from 'recharts'

import type { Tone } from '@/components/common/ToneBadge'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { buttonVariants } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { ChartContainer, ChartLegend, ChartLegendContent, ChartTooltip, ChartTooltipContent, type ChartConfig } from '@/components/ui/chart'
import { RecentActivity } from '@/features/activity'
import { ExportButton } from '@/features/exports'
import { ITEM_STATUS_LABEL, ITEM_STATUS_TONE, type ItemStatus } from '@/features/items'
import { formatNumber } from '@/lib/format'

import type { DayActivity, Overview } from '../types'

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
    <div className="flex flex-col gap-6">
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
      <div className="grid gap-6 xl:grid-cols-3">
        <Section title="Chỗ ở theo loại" className="xl:col-span-2">
          <Card className="gap-3 p-4">
            <OccupancyChart types={overview.placeTypes} />
            <ul className="grid gap-x-6 text-sm sm:grid-cols-2">
              {overview.placeTypes.map((type) => (
                <OccupancyRow key={type.typeId} name={type.name} total={type.total} occupied={type.occupied} />
              ))}
            </ul>
          </Card>
        </Section>
        <Section title="Đồ đạc theo trạng thái">
          <Card className="gap-3 p-4">
            <ItemsDonut counts={overview.itemsByStatus} />
            <ul className="grid gap-1 text-sm sm:grid-cols-2">
              {STATUSES.map((status) => (
                <li key={status}>
                  <Link to={itemsWith(status)} className="flex items-center justify-between gap-4 rounded-md px-2 py-1.5 hover:bg-muted">
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
        <Section title="Hoạt động 14 ngày qua" className="xl:col-span-2">
          <Card className="gap-3 p-4">
            <ActivityChart days={overview.activityByDay} />
          </Card>
        </Section>
        <RecentActivity ws={ws} />
      </div>
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
    <li className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1 border-b py-2 last:border-b-0">
      <span className="font-medium">{name}</span>{' '}
      <span className="inline-flex items-baseline gap-3 text-muted-foreground tabular-nums">
        <span>
          {formatNumber(occupied)} có người · {formatNumber(total - occupied)} trống
        </span>
        <span className="font-semibold text-primary">{percent}%</span>
      </span>
    </li>
  )
}

/** Cột chồng theo loại địa điểm: phần có người và phần trống. Chỉ để nhìn; số nằm ở danh sách. */
function OccupancyChart({ types }: { types: Overview['placeTypes'] }) {
  const config = { occupied: { label: 'Có người', color: 'var(--primary)' }, vacant: { label: 'Trống', color: 'var(--muted-foreground)' } } satisfies ChartConfig
  const data = types.map((type) => ({ name: type.name, occupied: type.occupied, vacant: type.total - type.occupied }))
  return (
    <ChartContainer config={config} aria-hidden="true" className="aspect-auto h-56 w-full">
      <BarChart data={data} margin={{ left: 0, right: 8, top: 8, bottom: 0 }}>
        <XAxis dataKey="name" tickLine={false} axisLine={false} />
        <YAxis allowDecimals={false} width={32} tickLine={false} axisLine={false} />
        <ChartTooltip content={<ChartTooltipContent />} />
        <ChartLegend content={<ChartLegendContent />} />
        <Bar dataKey="occupied" stackId="a" fill="var(--color-occupied)" isAnimationActive={false} />
        <Bar dataKey="vacant" stackId="a" fill="var(--color-vacant)" fillOpacity={0.35} radius={[4, 4, 0, 0]} isAnimationActive={false} />
      </BarChart>
    </ChartContainer>
  )
}

/** Vành khuyên theo trạng thái đồ, tổng số món ở giữa; số từng trạng thái nằm ở danh sách bên dưới. */
function ItemsDonut({ counts }: { counts: Record<ItemStatus, number> }) {
  const data = STATUSES.map((status) => ({ status, label: ITEM_STATUS_LABEL[status], value: counts[status], fill: TONE_FILL[ITEM_STATUS_TONE[status]] }))
  const total = data.reduce((sum, row) => sum + row.value, 0)
  const config = Object.fromEntries(STATUSES.map((status) => [status, { label: ITEM_STATUS_LABEL[status] }])) satisfies ChartConfig
  return (
    <div className="relative">
      <ChartContainer config={config} aria-hidden="true" className="aspect-auto h-48 w-full">
        <PieChart>
          <Pie data={data} dataKey="value" nameKey="label" innerRadius="62%" outerRadius="90%" strokeWidth={2} isAnimationActive={false}>
            {data.map((row) => (
              <Cell key={row.status} fill={row.fill} />
            ))}
          </Pie>
        </PieChart>
      </ChartContainer>
      <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
        <span className="text-2xl font-semibold tabular-nums">{formatNumber(total)}</span>
        <span className="text-xs text-muted-foreground">món đồ</span>
      </div>
    </div>
  )
}

/** Cột chồng theo ngày (14 ngày) và hành động; kèm tổng để đọc nhanh. Backend cũ chưa có số liệu thì báo. */
function ActivityChart({ days }: { days: DayActivity[] | undefined }) {
  if (!days || days.length === 0) {
    return <p className="text-sm text-muted-foreground">Chưa có số liệu hoạt động theo ngày.</p>
  }
  const sum = (day: DayActivity) => day.created + day.updated + day.deleted + day.restored
  const total = days.reduce((acc, day) => acc + sum(day), 0)
  const config = {
    created: { label: 'Tạo', color: TONE_FILL.success },
    updated: { label: 'Sửa', color: TONE_FILL.info },
    deleted: { label: 'Xoá', color: TONE_FILL.danger },
    restored: { label: 'Khôi phục', color: TONE_FILL.violet },
  } satisfies ChartConfig
  const data = days.map((day) => ({ ...day, label: `${day.date.slice(8, 10)}/${day.date.slice(5, 7)}` }))
  return (
    <>
      <p className="flex flex-wrap gap-x-4 text-sm text-muted-foreground">
        <span className="font-semibold text-foreground">{formatNumber(total)} thay đổi</span>
        <span>Hôm nay: {formatNumber(sum(days[days.length - 1]))}</span>
      </p>
      <ChartContainer config={config} aria-hidden="true" className="aspect-auto h-56 w-full">
        <BarChart data={data} margin={{ left: 0, right: 8, top: 8, bottom: 0 }}>
          <XAxis dataKey="label" tickLine={false} axisLine={false} interval="preserveStartEnd" />
          <YAxis allowDecimals={false} width={32} tickLine={false} axisLine={false} />
          <ChartTooltip content={<ChartTooltipContent />} />
          <ChartLegend content={<ChartLegendContent />} />
          {(['created', 'updated', 'deleted', 'restored'] as const).map((key, index, keys) => (
            <Bar key={key} dataKey={key} stackId="a" fill={`var(--color-${key})`} radius={index === keys.length - 1 ? [4, 4, 0, 0] : 0} isAnimationActive={false} />
          ))}
        </BarChart>
      </ChartContainer>
    </>
  )
}

function Section({ title, children, className }: { title: string; children: ReactNode; className?: string }) {
  const headingId = useId()
  return (
    <section aria-labelledby={headingId} className={cn('flex flex-col gap-2', className)}>
      <h2 id={headingId} className="font-semibold">
        {title}
      </h2>
      {children}
    </section>
  )
}
