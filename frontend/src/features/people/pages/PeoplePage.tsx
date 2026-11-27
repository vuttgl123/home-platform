import { PlusIcon, UsersIcon } from 'lucide-react'
import { useId, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'

import { PersonAvatar } from '@/components/common/PersonAvatar'
import type { CursorList } from '@/api'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadMoreButton } from '@/components/common/LoadMoreButton'
import { Button } from '@/components/ui/button'
import { Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from '@/components/ui/empty'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'
import { Skeleton } from '@/components/ui/skeleton'
import { AttributeFilters, filterableDefs, useAllFieldDefs } from '@/features/catalog'
import { ExportButton } from '@/features/exports'
import { useDebouncedValue } from '@/hooks/use-debounced-value'
import { thumbnailUrl } from '@/features/media'
import { useTopLevelPlaces } from '@/features/places'

import { PersonFormDialog } from '../components/PersonFormDialog'
import { isFiltered, readPersonFilter, writePersonFilter } from '../personFilter'
import { usePeopleList } from '../queries'
import type { PersonFilter, PersonSummary } from '../types'

/**
 * Trang `/w/:workspaceId/people`: mọi người của không gian (spec Đợt 11, S4). Một ô tìm trong họ tên,
 * số điện thoại, email, giấy tờ, ghi chú; lọc theo nơi đang ở và mục tuỳ biến. Bộ lọc nằm trên URL;
 * ô gõ đổi URL ngay, danh sách tải lại sau 300 ms.
 */
export function PeoplePage() {
  const { workspaceId = '' } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const fieldDefs = useAllFieldDefs(workspaceId, 'PERSON')
  const defs = fieldDefs.data ?? []
  const filter = readPersonFilter(searchParams, defs)
  const settled = new URLSearchParams(useDebouncedValue(searchParams.toString(), 300))
  const settledFilter = readPersonFilter(settled, defs)
  // Điều kiện trên URL chỉ đọc được khi đã có danh sách mục; chờ để không tải một lần không có chúng.
  const list = usePeopleList(workspaceId, settledFilter, !(settled.has('f') && fieldDefs.isPending))
  const buildings = useTopLevelPlaces(workspaceId).data ?? []
  const [adding, setAdding] = useState(false)
  const livingId = useId()
  const buildingId = useId()

  function update(next: PersonFilter) {
    setSearchParams(writePersonFilter(next), { replace: true })
  }

  const addButton = (
    <Button onClick={() => setAdding(true)}>
      <PlusIcon aria-hidden="true" />
      Thêm người
    </Button>
  )

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-wrap items-center gap-3">
        <h1 className="flex-1 text-2xl font-semibold tracking-tight">Người</h1>
        <ExportButton ws={workspaceId} kind="people" label="Xuất Excel" />
        {addButton}
      </header>
      <div className="flex flex-col gap-4">
        <Input
          type="search"
          aria-label="Tìm người"
          placeholder="Tên, SĐT, email, giấy tờ, ghi chú…"
          value={filter.q}
          onChange={(event) => update({ ...filter, q: event.target.value })}
        />
        <div className="grid gap-4 sm:max-w-xl sm:grid-cols-2">
          <Field>
            <FieldLabel htmlFor={livingId}>Cư trú</FieldLabel>
            <NativeSelect
              id={livingId}
              className="w-full"
              value={filter.living ?? ''}
              onChange={(event) => {
                const living = (event.target.value || null) as PersonFilter['living']
                update({ ...filter, living, rootId: living === 'no' ? null : filter.rootId })
              }}
            >
              <NativeSelectOption value="">Mọi người</NativeSelectOption>
              <NativeSelectOption value="yes">Đang ở</NativeSelectOption>
              <NativeSelectOption value="no">Không ở đâu</NativeSelectOption>
            </NativeSelect>
          </Field>
          <Field>
            <FieldLabel htmlFor={buildingId}>Toà</FieldLabel>
            <NativeSelect
              id={buildingId}
              className="w-full"
              disabled={filter.living === 'no'}
              value={filter.rootId ?? ''}
              // Đang ở trong một toà nghĩa là đang ở: chọn toà thì "Cư trú" thành "Đang ở".
              onChange={(event) => {
                const rootId = event.target.value || null
                update({ ...filter, rootId, living: rootId ? 'yes' : filter.living })
              }}
            >
              <NativeSelectOption value="">Mọi toà</NativeSelectOption>
              {buildings.map((building) => (
                <NativeSelectOption key={building.id} value={building.id}>
                  {building.name}
                </NativeSelectOption>
              ))}
            </NativeSelect>
          </Field>
        </div>
        <AttributeFilters
          defs={filterableDefs(defs, null)}
          value={filter.conditions}
          onChange={(conditions) => update({ ...filter, conditions })}
        />
      </div>
      <PeopleList
        ws={workspaceId}
        list={list}
        empty={
          isFiltered(settledFilter) ? (
            <p className="text-muted-foreground">Không tìm thấy ai.</p>
          ) : (
            <Empty>
              <EmptyHeader>
                <EmptyMedia variant="icon">
                  <UsersIcon aria-hidden="true" />
                </EmptyMedia>
                <EmptyTitle>Chưa có ai</EmptyTitle>
                <EmptyDescription>Hồ sơ người ở, chủ nhà, khách sẽ hiện ở đây.</EmptyDescription>
              </EmptyHeader>
              <EmptyContent>{addButton}</EmptyContent>
            </Empty>
          )
        }
      />
      <PersonFormDialog ws={workspaceId} mode="create" open={adding} onOpenChange={setAdding} />
    </div>
  )
}

function PeopleList({ ws, list, empty }: { ws: string; list: CursorList<PersonSummary>; empty: React.ReactNode }) {
  if (list.isLoading) {
    return (
      <div className="flex flex-col gap-2" aria-label="Đang tải">
        <Skeleton className="h-11" />
        <Skeleton className="h-11" />
      </div>
    )
  }
  if (list.error) {
    return <ErrorState error={list.error} onRetry={list.refetch} />
  }
  if (list.items.length === 0) {
    return <>{empty}</>
  }
  return (
    <div className="flex flex-col gap-3">
      <ul className="divide-y rounded-xl border bg-card">
        {list.items.map((person) => (
          <li key={person.id}>
            <Link
              to={`/w/${ws}/people/${person.id}`}
              className="flex flex-wrap items-center gap-x-4 gap-y-1 px-3 py-2.5 outline-none hover:bg-muted focus-visible:bg-muted"
            >
              <PersonAvatar name={person.fullName} photoUrl={person.avatarFileId ? thumbnailUrl(ws, person.avatarFileId) : null} />
              <span className="min-w-0 flex-1 truncate font-medium">{person.fullName}</span>
              {person.phone && <span className="text-sm text-muted-foreground">{person.phone}</span>}
              {person.idNumber && (
                <span className="text-sm text-muted-foreground">
                  {[person.idType, person.idNumber].filter(Boolean).join(' ')}
                </span>
              )}
            </Link>
          </li>
        ))}
      </ul>
      {list.hasMore && <LoadMoreButton onClick={list.loadMore} isLoading={list.isLoadingMore} />}
    </div>
  )
}
