import { Link, useParams, useSearchParams } from 'react-router'

import { currentUserId } from '@/api'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadMoreButton } from '@/components/common/LoadMoreButton'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { formatDateTime } from '@/lib/format'

import { RestoreButton } from '../components/RestoreButton'
import { ENTITY_LABEL } from '../labels'
import { useTrash } from '../queries'
import type { EntityType, TrashEntry } from '../types'

const TYPES: EntityType[] = ['PLACE', 'PERSON', 'ITEM']

function isEntityType(value: string | null): value is EntityType {
  return TYPES.includes(value as EntityType)
}

/** Ngữ cảnh của một dòng: địa điểm nằm trong đâu, món đồ ở đâu. */
function contextOf(entry: TrashEntry): string | null {
  if (!entry.context) return null
  return entry.entityType === 'ITEM' ? `ở ${entry.context}` : `trong ${entry.context}`
}

/**
 * Trang `/w/:workspaceId/trash`: những gì đang bị xoá, mỗi loại một tab (trên URL `?type=`), mới xoá
 * trước. Khôi phục đưa bản ghi về đúng chỗ cũ; không có xoá hẳn.
 */
export function TrashPage() {
  const { workspaceId = '' } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const requested = searchParams.get('type')
  const type: EntityType = isEntityType(requested) ? requested : 'PLACE'
  const trash = useTrash(workspaceId, type)

  return (
    <div className="flex max-w-3xl flex-col gap-6">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">Thùng rác</h1>
        <p className="text-muted-foreground">Những thứ đang bị xoá. Khôi phục để đưa chúng trở lại đúng chỗ cũ.</p>
      </header>
      <Tabs value={type} onValueChange={(value) => setSearchParams(value === 'PLACE' ? {} : { type: value as string }, { replace: true })}>
        <TabsList variant="line" className="w-full justify-start overflow-x-auto">
          {TYPES.map((value) => (
            <TabsTrigger key={value} value={value} className="flex-none">
              {ENTITY_LABEL[value]}
            </TabsTrigger>
          ))}
        </TabsList>
      </Tabs>
      {trash.isLoading ? (
        <div className="flex flex-col gap-2" aria-label="Đang tải">
          <Skeleton className="h-12" />
          <Skeleton className="h-12" />
        </div>
      ) : trash.error ? (
        <ErrorState error={trash.error} onRetry={trash.refetch} />
      ) : trash.items.length === 0 ? (
        <p className="text-muted-foreground">Không có gì đang bị xoá.</p>
      ) : (
        <div className="flex flex-col gap-3">
          <ul className="flex flex-col divide-y rounded-lg border">
            {trash.items.map((entry) => (
              <li key={entry.id} className="flex flex-wrap items-center gap-x-2 gap-y-1 px-3 py-2.5 text-sm">
                <span className="font-medium">{entry.name}</span>
                {entry.code && <span className="tabular-nums text-xs text-muted-foreground">{entry.code}</span>}
                {contextOf(entry) && <span className="text-muted-foreground">{contextOf(entry)}</span>}
                <span className="text-muted-foreground">
                  · xoá lúc {formatDateTime(entry.deletedAt)} · {entry.deletedBy === currentUserId() ? 'Bạn' : 'Người dùng khác'}
                </span>
                <RestoreButton ws={workspaceId} entityType={entry.entityType} entityId={entry.id} version={entry.version} name={entry.name} />
              </li>
            ))}
          </ul>
          {trash.hasMore && <LoadMoreButton onClick={trash.loadMore} isLoading={trash.isLoadingMore} />}
        </div>
      )}
      <p className="text-sm text-muted-foreground">
        Muốn xem mọi thay đổi? <Link to={`/w/${workspaceId}/activity`} className="underline underline-offset-4">Mở Hoạt động</Link>.
      </p>
    </div>
  )
}
