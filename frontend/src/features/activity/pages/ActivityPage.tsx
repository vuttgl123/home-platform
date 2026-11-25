import { useId } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'

import { ErrorState } from '@/components/common/ErrorState'
import { LoadMoreButton } from '@/components/common/LoadMoreButton'
import { TextField } from '@/components/common/TextField'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'
import { Skeleton } from '@/components/ui/skeleton'
import { Switch } from '@/components/ui/switch'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { ACTION_LABEL, type ChangeAction } from '@/features/history'

import { ActivityRow } from '../components/ActivityRow'
import { hasFilters, rangeIsValid, readFilter, writeFilter } from '../filters'
import { ENTITY_LABEL } from '../labels'
import { useActivity } from '../queries'
import type { ActivityFilter, ActivityType } from '../types'

const TABS: (ActivityType | 'ALL')[] = ['ALL', 'PLACE', 'PERSON', 'ITEM', 'MAINTENANCE']
const ACTIONS: ChangeAction[] = ['CREATE', 'UPDATE', 'DELETE', 'RESTORE']

/**
 * Trang `/w/:workspaceId/activity`: mọi thay đổi của không gian, mới nhất trước. Lọc theo loại bản
 * ghi (tab), hành động, khoảng ngày và "chỉ việc tôi làm"; mọi lọc nằm trên URL
 * (`?type=&action=&mine=1&from=&to=`) để chép gửi người khác. Cũng là chỗ khôi phục thứ đã xoá.
 */
export function ActivityPage() {
  const { workspaceId = '' } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const filter = readFilter(searchParams)
  const validRange = rangeIsValid(filter)
  const filtered = hasFilters(filter)
  const activity = useActivity(workspaceId, filter)
  const actionId = useId()

  function update(change: Partial<ActivityFilter>) {
    setSearchParams(writeFilter({ ...filter, ...change }), { replace: true })
  }

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">Hoạt động</h1>
        <p className="text-muted-foreground">Mọi lần tạo, sửa, xoá, khôi phục trong không gian này. Thứ đã xoá khôi phục được từ đây.</p>
        <p className="text-sm text-muted-foreground">
          Chỉ cần lấy lại thứ đã xoá?{' '}
          <Link to={`/w/${workspaceId}/trash`} className="underline underline-offset-4">
            Mở Thùng rác
          </Link>
          .
        </p>
      </header>
      <Tabs value={filter.type ?? 'ALL'} onValueChange={(value) => update({ type: value === 'ALL' ? null : (value as ActivityType) })}>
        <TabsList variant="line" className="w-full justify-start overflow-x-auto">
          {TABS.map((tab) => (
            <TabsTrigger key={tab} value={tab} className="flex-none">
              {tab === 'ALL' ? 'Tất cả' : ENTITY_LABEL[tab]}
            </TabsTrigger>
          ))}
        </TabsList>
      </Tabs>
      <div className="flex flex-col gap-3">
        <div className="grid gap-4 sm:grid-cols-3">
          <Field>
            <FieldLabel htmlFor={actionId}>Hành động</FieldLabel>
            <NativeSelect
              id={actionId}
              className="w-full"
              value={filter.action ?? ''}
              onChange={(event) => update({ action: (event.target.value || null) as ChangeAction | null })}
            >
              <NativeSelectOption value="">Tất cả</NativeSelectOption>
              {ACTIONS.map((action) => (
                <NativeSelectOption key={action} value={action}>
                  {ACTION_LABEL[action]}
                </NativeSelectOption>
              ))}
            </NativeSelect>
          </Field>
          <TextField
            label="Từ ngày"
            type="date"
            value={filter.from ?? ''}
            max={filter.to ?? undefined}
            onChange={(event) => update({ from: event.target.value || null })}
          />
          <TextField
            label="Đến ngày"
            type="date"
            value={filter.to ?? ''}
            min={filter.from ?? undefined}
            onChange={(event) => update({ to: event.target.value || null })}
          />
        </div>
        {!validRange && <FieldError>Từ ngày phải trước hoặc bằng Đến ngày</FieldError>}
        <div className="flex min-h-8 flex-wrap items-center gap-4">
          <label className="flex items-center gap-2 text-sm">
            <Switch checked={filter.mine} onCheckedChange={(mine) => update({ mine })} aria-label="Chỉ việc tôi làm" />
            Chỉ việc tôi làm
          </label>
          {filtered && (
            <Button variant="ghost" size="sm" onClick={() => update({ action: null, mine: false, from: null, to: null })}>
              Bỏ lọc
            </Button>
          )}
        </div>
      </div>
      {!validRange ? null : activity.isLoading ? (
        <div className="flex flex-col gap-2" aria-label="Đang tải">
          <Skeleton className="h-12" />
          <Skeleton className="h-12" />
        </div>
      ) : activity.error ? (
        <ErrorState error={activity.error} onRetry={activity.refetch} />
      ) : activity.items.length === 0 ? (
        <p className="text-muted-foreground">{filtered ? 'Không có hoạt động khớp bộ lọc.' : 'Chưa có hoạt động nào.'}</p>
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
