import { Link } from 'react-router'

import { InfoCard } from '@/components/common/InfoCard'
import { WithTooltip } from '@/components/common/WithTooltip'
import { todayInBusinessZone } from '@/lib/dates'
import { cn } from 'cn'

import { useOccupancyTimeline } from '../queries'
import { timelineLayout } from '../timeline'
import type { Occupancy, OccupancyStatus } from '../types'

/** Màu thanh theo trạng thái, như nhãn trạng thái cư trú (Đợt 5): đang ở, sắp đến, đã đi. */
const BAR_CLASS: Record<OccupancyStatus, { solid: string; open: string }> = {
  CURRENT: { solid: 'bg-success', open: 'bg-linear-to-r from-success from-60% to-success/15' },
  UPCOMING: { solid: 'bg-warning', open: 'bg-linear-to-r from-warning from-60% to-warning/15' },
  PAST: { solid: 'bg-muted-foreground/45', open: 'bg-linear-to-r from-muted-foreground/45 from-60% to-transparent' },
}

/**
 * Dòng thời gian cư trú: mỗi người (`groupBy="person"`, ở một địa điểm) hay mỗi địa điểm
 * (`groupBy="place"`, của một người) một hàng, mỗi lần ở một thanh; có vạch hôm nay. Không có lần ở
 * nào thì không hiện gì. Trên màn hẹp thì cuộn ngang.
 */
export function OccupancyTimeline({
  ws,
  groupBy,
  rows,
  truncated,
  today = todayInBusinessZone(),
}: {
  ws: string
  groupBy: 'person' | 'place'
  rows: Occupancy[]
  truncated: boolean
  today?: string
}) {
  if (rows.length === 0) return null
  const layout = timelineLayout(rows, groupBy, today)
  const href = (key: string) => (groupBy === 'person' ? `/w/${ws}/people/${key}` : `/w/${ws}/places/${key}`)
  return (
    <InfoCard title="Dòng thời gian">
      <div className="overflow-x-auto">
        <div className="flex min-w-[640px] flex-col gap-1.5 text-sm">
          <div className="grid grid-cols-[9rem_1fr] gap-3">
            <span />
            <div className="relative h-5 text-xs text-muted-foreground tabular-nums">
              {layout.months.map((month) => (
                <span key={month.label} className="absolute -translate-x-1/2 whitespace-nowrap" style={{ left: `${month.left}%` }}>
                  {month.label}
                </span>
              ))}
            </div>
          </div>
          {layout.lanes.map((lane) => (
            <div key={lane.key} className="grid grid-cols-[9rem_1fr] items-center gap-3">
              <Link to={href(lane.key)} className="truncate font-medium hover:underline">
                {lane.label}
              </Link>
              <div className="relative h-6 rounded-md bg-muted/50">
                {layout.months.map((month) => (
                  <span key={month.label} aria-hidden="true" className="absolute inset-y-0 w-px bg-border" style={{ left: `${month.left}%` }} />
                ))}
                {lane.bars.map((bar) => (
                  <WithTooltip key={bar.id} label={bar.title} side="top">
                    <div
                      role="img"
                      aria-label={bar.title}
                      tabIndex={0}
                      data-status={bar.status}
                      className={cn(
                        'absolute inset-y-1 min-w-1 rounded-sm outline-none focus-visible:ring-2 focus-visible:ring-ring',
                        bar.open ? BAR_CLASS[bar.status].open : BAR_CLASS[bar.status].solid,
                      )}
                      style={{ left: `${bar.left}%`, width: `${bar.width}%` }}
                    />
                  </WithTooltip>
                ))}
                <span aria-hidden="true" className="absolute -inset-y-1 w-0.5 bg-primary" style={{ left: `${layout.todayLeft}%` }} />
              </div>
            </div>
          ))}
          <div className="grid grid-cols-[9rem_1fr] gap-3">
            <span />
            <div className="relative h-4 text-xs font-medium text-primary">
              <span className="absolute -translate-x-1/2 whitespace-nowrap" style={{ left: `${layout.todayLeft}%` }}>
                Hôm nay
              </span>
            </div>
          </div>
        </div>
      </div>
      {truncated && <p className="mt-2 text-xs text-muted-foreground">Chỉ hiện 200 lần ở gần nhất</p>}
    </InfoCard>
  )
}

/** Dòng thời gian của một địa điểm (`placeId`) hay của một người (`personId`), tự tải dữ liệu. */
export function OccupancyTimelineCard({ ws, owner }: { ws: string; owner: { placeId: string } | { personId: string } }) {
  const timeline = useOccupancyTimeline(ws, owner)
  if (!timeline.data) return null
  return (
    <OccupancyTimeline
      ws={ws}
      groupBy={'placeId' in owner ? 'person' : 'place'}
      rows={timeline.data.rows}
      truncated={timeline.data.truncated}
    />
  )
}
