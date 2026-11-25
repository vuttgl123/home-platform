import { BellIcon } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'

import { WithTooltip } from '@/components/common/WithTooltip'
import { Button } from '@/components/ui/button'
import { Popover, PopoverContent, PopoverTitle, PopoverTrigger } from '@/components/ui/popover'

import { useMarkTodosSeen, useTodoSummary } from '../queries'
import type { Todo } from '../types'
import { TodoRow } from './TodoRow'

/** Chấm số trên chuông dừng ở 9+, để chấm luôn nhỏ. */
const MAX_COUNT = 9

/**
 * Chuông ở đầu trang (spec Đợt 8, L6): chấm số là số việc mới từ lần cuối mở chuông. Mở chuông thì
 * đánh dấu đã xem; danh sách trong popover giữ nguyên lúc mở, nên chấm "mới" vẫn còn tới khi đóng.
 * Registry cũ chưa có việc cần làm thì không hiện chuông.
 */
export function TodoBell({ ws }: { ws: string }) {
  const summary = useTodoSummary(ws).data
  const markSeen = useMarkTodosSeen(ws)
  const [open, setOpen] = useState(false)
  const [shown, setShown] = useState<Todo[]>([])

  if (!summary) {
    return null
  }
  const count = summary.newCount
  const label = count > 0 ? `Thông báo, ${count} việc mới` : 'Thông báo'

  function change(next: boolean) {
    setOpen(next)
    if (next && summary) {
      setShown(summary.latest)
      markSeen.mutate()
    }
  }

  return (
    <Popover open={open} onOpenChange={change}>
      <WithTooltip label="Thông báo">
        <PopoverTrigger render={<Button variant="ghost" size="icon" aria-label={label} className="relative shrink-0" />}>
          <BellIcon aria-hidden="true" />
          {count > 0 && (
            <span
              aria-hidden="true"
              className="absolute -top-0.5 -right-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-danger px-1 text-[10px] font-semibold text-white tabular-nums"
            >
              {count > MAX_COUNT ? `${MAX_COUNT}+` : count}
            </span>
          )}
        </PopoverTrigger>
      </WithTooltip>
      <PopoverContent align="end" className="w-[min(22rem,calc(100vw-2rem))] gap-0 p-0">
        <PopoverTitle className="border-b px-3 py-2 text-sm font-semibold">Thông báo</PopoverTitle>
        {shown.length === 0 ? (
          <p className="px-3 py-6 text-center text-sm text-muted-foreground">Chưa có gì mới</p>
        ) : (
          <ul className="flex max-h-96 flex-col divide-y overflow-y-auto">
            {shown.map((todo) => (
              <TodoRow key={todo.id} ws={ws} todo={todo} showNew onOpen={() => setOpen(false)} />
            ))}
          </ul>
        )}
        <div className="border-t px-3 py-2">
          <Link to={`/w/${ws}/todos`} onClick={() => setOpen(false)} className="text-sm font-medium text-primary hover:underline">
            Xem tất cả việc cần làm
          </Link>
        </div>
      </PopoverContent>
    </Popover>
  )
}
