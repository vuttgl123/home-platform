import { formatDate } from '@/lib/format'

import { RELATION_LABEL } from './labels'
import type { Occupancy, OccupancyStatus } from './types'

/** Số ngày đệm thêm ở hai đầu trục, để thanh không dính mép. */
const PAD_DAYS = 7
/** Số vạch tháng nhiều nhất; trục dài hơn thì cách vài tháng mới có một vạch. */
const MAX_TICKS = 12
const DAY_MS = 86_400_000

export type TimelineBar = { id: string; left: number; width: number; open: boolean; status: OccupancyStatus; title: string }
export type TimelineLane = { key: string; label: string; bars: TimelineBar[] }
export type TimelineLayout = {
  start: string
  end: string
  months: { label: string; left: number }[]
  todayLeft: number
  lanes: TimelineLane[]
}

/** Ngày `YYYY-MM-DD` thành số ngày kể từ 1970-01-01 (theo lịch, không theo múi giờ). */
function dayNumber(date: string): number {
  const [year, month, day] = date.split('-').map(Number)
  return Date.UTC(year, month - 1, day) / DAY_MS
}

function dateOf(day: number): string {
  return new Date(day * DAY_MS).toISOString().slice(0, 10)
}

/**
 * Bố cục dòng thời gian: trục từ ngày bắt đầu sớm nhất tới `max(hôm nay, ngày cuối muộn nhất)`, mỗi
 * đầu đệm {@link PAD_DAYS} ngày; mỗi người (hay mỗi địa điểm) một hàng, theo thứ tự xuất hiện trong
 * `rows`; vị trí tính theo % chiều rộng. Ngày kết thúc vẫn là ngày còn ở, nên thanh phủ hết ngày đó.
 */
export function timelineLayout(rows: Occupancy[], groupBy: 'person' | 'place', today: string): TimelineLayout {
  const todayDay = dayNumber(today)
  const starts = rows.map((row) => dayNumber(row.startDate))
  const lasts = rows.map((row) => dayNumber(row.endDate ?? row.startDate))
  const first = (rows.length > 0 ? Math.min(...starts) : todayDay) - PAD_DAYS
  const last = Math.max(todayDay, ...lasts) + PAD_DAYS
  const total = last - first + 1
  const at = (day: number) => ((day - first) / total) * 100

  const lanes = new Map<string, TimelineLane>()
  for (const row of rows) {
    const key = groupBy === 'person' ? row.personId : row.placeId
    const label = groupBy === 'person' ? row.personFullName : row.placeName
    const lane = lanes.get(key) ?? { key, label, bars: [] }
    lanes.set(key, lane)
    const left = at(dayNumber(row.startDate))
    const open = row.endDate == null
    const right = open ? 100 : at(dayNumber(row.endDate!) + 1)
    lane.bars.push({
      id: row.id,
      left,
      width: right - left,
      open,
      status: row.status,
      title: `${RELATION_LABEL[row.relation]} · ${formatDate(row.startDate)} → ${row.endDate ? formatDate(row.endDate) : 'nay'}`,
    })
  }

  const months: { label: string; left: number }[] = []
  const firstDate = new Date(first * DAY_MS)
  let year = firstDate.getUTCFullYear()
  let month = firstDate.getUTCMonth() + 1
  for (;;) {
    month += 1
    if (month > 12) {
      month = 1
      year += 1
    }
    const day = Date.UTC(year, month - 1, 1) / DAY_MS
    if (day > last) break
    months.push({ label: `T${month}/${year}`, left: at(day) })
  }

  // Gõ nhầm năm (2999) làm trục dài hàng nghìn tháng: chỉ giữ mỗi `step` tháng một vạch.
  const step = Math.ceil(months.length / MAX_TICKS)
  const ticks = months.filter((_, index) => index % step === 0)
  return { start: dateOf(first), end: dateOf(last), months: ticks, todayLeft: at(todayDay), lanes: [...lanes.values()] }
}
