import { CalendarClockIcon, CalendarIcon, LogOutIcon, ShieldCheckIcon, WrenchIcon, type LucideIcon } from 'lucide-react'

import type { Tone } from '@/components/common/ToneBadge'
import { daysBetween } from '@/lib/dates'
import { formatDate } from '@/lib/format'

import type { Todo, TodoKind } from './types'

/** Thứ tự như backend sắp khi hai việc cùng ngày. */
export const TODO_KINDS: TodoKind[] = ['BROKEN', 'WARRANTY', 'SERVICE', 'MOVE_OUT', 'CUSTOM_DATE']

export const TODO_KIND_LABEL: Record<TodoKind, string> = {
  BROKEN: 'Hỏng',
  WARRANTY: 'Bảo hành',
  SERVICE: 'Bảo trì',
  MOVE_OUT: 'Chuyển đi',
  CUSTOM_DATE: 'Hạn khác',
}

export const TODO_KIND_ICON: Record<TodoKind, LucideIcon> = {
  BROKEN: WrenchIcon,
  WARRANTY: ShieldCheckIcon,
  SERVICE: CalendarClockIcon,
  MOVE_OUT: LogOutIcon,
  CUSTOM_DATE: CalendarIcon,
}

/** Việc trong 7 ngày tới cần để ý ngay; xa hơn thì chỉ cần biết. */
const SOON_DAYS = 7

/** Nhãn hạn của một việc so với `today` (`YYYY-MM-DD`): quá hạn đỏ, trong tuần vàng, xa hơn xám. */
export function dueLabel(todo: Pick<Todo, 'kind' | 'dueOn'>, today: string): { text: string; tone: Tone } {
  if (todo.kind === 'BROKEN') return { text: `hỏng từ ${formatDate(todo.dueOn).slice(0, 5)}`, tone: 'danger' }
  const days = daysBetween(today, todo.dueOn)
  if (days < 0) return { text: `quá hạn ${-days} ngày`, tone: 'danger' }
  if (days === 0) return { text: 'hôm nay', tone: 'warning' }
  return { text: `còn ${days} ngày`, tone: days <= SOON_DAYS ? 'warning' : 'neutral' }
}

const SUBJECT_PATH: Record<string, string> = { ITEM: 'items', PERSON: 'people', PLACE: 'places' }

/** Trang mở từ một việc; việc bảo trì mở thẳng tab Bảo trì của món. */
export function todoHref(ws: string, todo: Pick<Todo, 'kind' | 'subjectType' | 'subjectId'>): string {
  const base = `/w/${ws}/${SUBJECT_PATH[todo.subjectType] ?? 'items'}/${todo.subjectId}`
  return todo.kind === 'SERVICE' ? `${base}?tab=maintenance` : base
}

/** Chữ phụ dưới tên: chỗ của món hay của người, hoặc nhãn của mục tuỳ biến. */
export function todoContext(todo: Pick<Todo, 'kind' | 'context'>): string | null {
  if (!todo.context) return null
  if (todo.kind === 'CUSTOM_DATE') return todo.context
  if (todo.kind === 'MOVE_OUT') return `Chuyển khỏi ${todo.context}`
  return `Ở ${todo.context}`
}
