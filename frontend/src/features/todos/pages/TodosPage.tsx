import { ListChecksIcon } from 'lucide-react'
import { useId } from 'react'
import { useParams, useSearchParams } from 'react-router'

import { ErrorState } from '@/components/common/ErrorState'
import { LoadMoreButton } from '@/components/common/LoadMoreButton'
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia } from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'

import { TodoRow } from '../components/TodoRow'
import { TODO_KIND_LABEL, TODO_KINDS } from '../labels'
import { useTodos, useTodoSummary } from '../queries'
import type { TodoKind } from '../types'

const ALL = 'all'
/** Nút đang chọn tô màu chính, như bộ lọc của trang Chỗ trống. */
const PRESSED = 'aria-pressed:border-primary aria-pressed:bg-primary aria-pressed:text-primary-foreground aria-pressed:hover:bg-primary/90'

/**
 * Trang `/w/:workspaceId/todos`: đồ hỏng, bảo hành sắp hết, bảo trì tới hạn, người sắp chuyển đi và các
 * hạn tuỳ biến, gấp nhất trước (spec Đợt 8, L5). Loại đang lọc nằm trên URL (`?kind=`).
 */
export function TodosPage() {
  const { workspaceId = '' } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const requested = searchParams.get('kind')
  const kind = TODO_KINDS.includes(requested as TodoKind) ? (requested as TodoKind) : null
  const list = useTodos(workspaceId, kind)
  const summary = useTodoSummary(workspaceId).data
  const filterLabelId = useId()

  function pick(next: TodoKind | null) {
    setSearchParams(next ? { kind: next } : {}, { replace: true })
  }

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">Việc cần làm</h1>
        <p className="text-muted-foreground">Những thứ cần để ý trong không gian này, việc gấp nhất trước.</p>
      </header>
      <div className="flex flex-col gap-2">
        <span id={filterLabelId} className="text-sm font-medium">
          Loại
        </span>
        <ToggleGroup
          aria-labelledby={filterLabelId}
          variant="outline"
          size="sm"
          spacing={0}
          className="flex-wrap"
          value={[kind ?? ALL]}
          onValueChange={(values: string[]) => {
            const picked = values.at(-1)
            pick(picked && picked !== ALL ? (picked as TodoKind) : null)
          }}
        >
          <ToggleGroupItem value={ALL} className={PRESSED}>
            Tất cả
            {summary && <span className="tabular-nums opacity-70">{summary.total}</span>}
          </ToggleGroupItem>
          {TODO_KINDS.map((value) => (
            <ToggleGroupItem key={value} value={value} className={PRESSED}>
              {TODO_KIND_LABEL[value]}
              {summary && <span className="tabular-nums opacity-70">{summary.byKind[value]}</span>}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
      </div>
      {list.isLoading ? (
        <div className="flex flex-col gap-2" aria-label="Đang tải">
          <Skeleton className="h-12" />
          <Skeleton className="h-12" />
        </div>
      ) : list.error ? (
        <ErrorState error={list.error} onRetry={list.refetch} />
      ) : list.items.length === 0 ? (
        <Empty className="border">
          <EmptyHeader>
            <EmptyMedia variant="icon">
              <ListChecksIcon aria-hidden="true" />
            </EmptyMedia>
            <EmptyDescription>Không có việc gì cần làm</EmptyDescription>
          </EmptyHeader>
        </Empty>
      ) : (
        <div className="flex flex-col gap-2">
          <ul className="flex flex-col divide-y rounded-lg border bg-card">
            {list.items.map((todo) => (
              <TodoRow key={todo.id} ws={workspaceId} todo={todo} />
            ))}
          </ul>
          {list.hasMore && <LoadMoreButton onClick={list.loadMore} isLoading={list.isLoadingMore} />}
        </div>
      )}
    </div>
  )
}
