import { useParams, useSearchParams } from 'react-router'

import { ErrorState } from '@/components/common/ErrorState'
import { LoadMoreButton } from '@/components/common/LoadMoreButton'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'

import { ActivityRow } from '../components/ActivityRow'
import { ENTITY_LABEL } from '../labels'
import { useActivity } from '../queries'
import type { EntityType } from '../types'

const FILTERS: (EntityType | 'ALL')[] = ['ALL', 'PLACE', 'PERSON', 'ITEM']

function isEntityType(value: string | null): value is EntityType {
  return value === 'PLACE' || value === 'PERSON' || value === 'ITEM'
}

/**
 * Trang `/w/:workspaceId/activity`: mọi thay đổi của không gian, mới nhất trước, lọc theo loại
 * bản ghi (trên URL `?type=`). Cũng là chỗ khôi phục thứ đã xoá.
 */
export function ActivityPage() {
  const { workspaceId = '' } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const requested = searchParams.get('type')
  const type = isEntityType(requested) ? requested : null
  const activity = useActivity(workspaceId, type)

  return (
    <div className="flex max-w-3xl flex-col gap-6">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">Hoạt động</h1>
        <p className="text-muted-foreground">Mọi lần tạo, sửa, xoá, khôi phục trong không gian này. Thứ đã xoá khôi phục được từ đây.</p>
      </header>
      <Tabs value={type ?? 'ALL'} onValueChange={(value) => setSearchParams(value === 'ALL' ? {} : { type: value as string }, { replace: true })}>
        <TabsList variant="line" className="w-full justify-start overflow-x-auto">
          {FILTERS.map((filter) => (
            <TabsTrigger key={filter} value={filter} className="flex-none">
              {filter === 'ALL' ? 'Tất cả' : ENTITY_LABEL[filter]}
            </TabsTrigger>
          ))}
        </TabsList>
      </Tabs>
      {activity.isLoading ? (
        <div className="flex flex-col gap-2" aria-label="Đang tải">
          <Skeleton className="h-12" />
          <Skeleton className="h-12" />
        </div>
      ) : activity.error ? (
        <ErrorState error={activity.error} onRetry={activity.refetch} />
      ) : activity.items.length === 0 ? (
        <p className="text-muted-foreground">Chưa có hoạt động nào.</p>
      ) : (
        <div className="flex flex-col gap-3">
          <ol className="flex flex-col divide-y rounded-lg border">
            {activity.items.map((entry) => (
              <ActivityRow key={entry.id} ws={workspaceId} entry={entry} />
            ))}
          </ol>
          {activity.hasMore && <LoadMoreButton onClick={activity.loadMore} isLoading={activity.isLoadingMore} />}
        </div>
      )}
    </div>
  )
}
