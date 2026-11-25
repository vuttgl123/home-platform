import type { ChangeAction } from '@/features/history'

import type { ActivityFilter, ActivityType } from './types'

const TYPES: ActivityType[] = ['PLACE', 'PERSON', 'ITEM', 'MAINTENANCE']
const ACTIONS: ChangeAction[] = ['CREATE', 'UPDATE', 'DELETE', 'RESTORE']
const DATE = /^(\d{4})-(\d{2})-(\d{2})$/

/** Tham số `GET /activity` của một bộ lọc; khoá không lọc thì vắng. */
export type ActivityQuery = { entityType?: ActivityType; action?: ChangeAction; changedBy?: string; from?: string; to?: string }

/** 00:00 theo giờ máy của ngày `yyyy-MM-dd`, cộng thêm `days` ngày; `null` nếu không có ngày đó. */
function localMidnight(value: string, days = 0): Date | null {
  const match = DATE.exec(value)
  if (!match) {
    return null
  }
  const [year, month, day] = [Number(match[1]), Number(match[2]) - 1, Number(match[3])]
  const date = new Date(year, month, day)
  // `Date` tự chuyển 30/02 thành 02/03: ngày không còn y nguyên là ngày không có trên lịch.
  if (date.getFullYear() !== year || date.getMonth() !== month || date.getDate() !== day) {
    return null
  }
  return days === 0 ? date : new Date(year, month, day + days)
}

function dateOrNull(value: string | null): string | null {
  return value != null && localMidnight(value) ? value : null
}

/** Bộ lọc trên URL (`?type=&action=&mine=1&from=&to=`); giá trị lạ bị bỏ qua như không có. */
export function readFilter(params: URLSearchParams): ActivityFilter {
  const type = params.get('type')
  const action = params.get('action')
  return {
    type: TYPES.includes(type as ActivityType) ? (type as ActivityType) : null,
    action: ACTIONS.includes(action as ChangeAction) ? (action as ChangeAction) : null,
    mine: params.get('mine') === '1',
    from: dateOrNull(params.get('from')),
    to: dateOrNull(params.get('to')),
  }
}

/** Ngược lại với `readFilter`; khoá không lọc thì bỏ khỏi URL. */
export function writeFilter(filter: ActivityFilter): URLSearchParams {
  const params = new URLSearchParams()
  if (filter.type) params.set('type', filter.type)
  if (filter.action) params.set('action', filter.action)
  if (filter.mine) params.set('mine', '1')
  if (filter.from) params.set('from', filter.from)
  if (filter.to) params.set('to', filter.to)
  return params
}

/**
 * Tham số gửi lên: `from` = 00:00 giờ máy của ngày `from`; `to` = 00:00 giờ máy của ngày sau
 * `to` (backend không tính mốc `to`, nên cả ngày `to` được tính); ISO-8601.
 * @param me người dùng đang chọn; "chỉ việc tôi làm" lọc theo người này
 */
export function toQuery(filter: ActivityFilter, me: string | null): ActivityQuery {
  const query: ActivityQuery = {}
  if (filter.type) query.entityType = filter.type
  if (filter.action) query.action = filter.action
  if (filter.mine && me) query.changedBy = me
  const from = filter.from ? localMidnight(filter.from) : null
  const to = filter.to ? localMidnight(filter.to, 1) : null
  if (from) query.from = from.toISOString()
  if (to) query.to = to.toISOString()
  return query
}

/** Có lọc nào ngoài tab loại bản ghi ("Bỏ lọc" giữ tab). */
export function hasFilters(filter: ActivityFilter): boolean {
  return filter.action != null || filter.mine || filter.from != null || filter.to != null
}

/** `from` không được sau `to`; bằng nhau là lọc đúng một ngày. */
export function rangeIsValid(filter: ActivityFilter): boolean {
  // Cùng dạng yyyy-MM-dd nên so chuỗi là so ngày.
  return filter.from == null || filter.to == null || filter.from <= filter.to
}
