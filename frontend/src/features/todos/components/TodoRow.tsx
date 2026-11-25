import { Link } from 'react-router'

import { ToneBadge } from '@/components/common/ToneBadge'
import { todayInBusinessZone } from '@/lib/dates'

import { TODO_KIND_ICON, TODO_KIND_LABEL, dueLabel, todoContext, todoHref } from '../labels'
import type { Todo } from '../types'

/** Một việc: biểu tượng loại, tên hồ sơ (mở được), chữ phụ, nhãn hạn. `onOpen` chạy khi bấm vào tên. */
export function TodoRow({ ws, todo, onOpen, showNew = false }: { ws: string; todo: Todo; onOpen?: () => void; showNew?: boolean }) {
  const Icon = TODO_KIND_ICON[todo.kind]
  const due = dueLabel(todo, todayInBusinessZone())
  const context = todoContext(todo)
  return (
    <li className="flex items-start gap-3 px-3 py-2.5 text-sm">
      <span className="relative mt-0.5 flex size-7 shrink-0 items-center justify-center rounded-md bg-muted text-muted-foreground">
        <Icon aria-hidden="true" className="size-4" />
        <span className="sr-only">{TODO_KIND_LABEL[todo.kind]}</span>
        {/* Chấm "mới" ở góc biểu tượng, để tên dài không bị đẩy xuống dòng. */}
        {showNew && todo.isNew && (
          <span aria-label="Mới" className="absolute -top-1 -right-1 size-2.5 rounded-full bg-primary ring-2 ring-popover" />
        )}
      </span>
      <div className="flex min-w-0 flex-1 flex-col gap-0.5">
        <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
          <Link to={todoHref(ws, todo)} onClick={onOpen} className="min-w-0 font-medium break-words hover:underline">
            {todo.subjectName}
          </Link>
          {todo.subjectCode && <span className="text-xs tabular-nums text-muted-foreground">{todo.subjectCode}</span>}
        </div>
        {context && <span className="text-muted-foreground">{context}</span>}
      </div>
      <ToneBadge tone={due.tone} className="shrink-0">
        {due.text}
      </ToneBadge>
    </li>
  )
}
