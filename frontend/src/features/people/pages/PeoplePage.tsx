import { PlusIcon, UsersIcon } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router'

import type { CursorList } from '@/api'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadMoreButton } from '@/components/common/LoadMoreButton'
import { Button } from '@/components/ui/button'
import { Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from '@/components/ui/empty'
import { Input } from '@/components/ui/input'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'
import { Skeleton } from '@/components/ui/skeleton'
import { useDebouncedValue } from '@/hooks/use-debounced-value'

import { PersonFormDialog } from '../components/PersonFormDialog'
import { usePeople, usePeopleSearch } from '../queries'
import type { PersonSearchField, PersonSummary } from '../types'

const SEARCH_FIELDS: { value: PersonSearchField; label: string; placeholder: string }[] = [
  { value: 'name', label: 'Tên', placeholder: 'Tên bắt đầu bằng…' },
  { value: 'phone', label: 'Số điện thoại', placeholder: 'Đúng số điện thoại' },
  { value: 'idNumber', label: 'Số giấy tờ', placeholder: 'Đúng số giấy tờ' },
]

/** Trang `/w/:workspaceId/people`: mọi người của không gian, tìm theo tên, số điện thoại hay số giấy tờ. */
export function PeoplePage() {
  const { workspaceId = '' } = useParams()
  const [by, setBy] = useState<PersonSearchField>('name')
  const [term, setTerm] = useState('')
  const settled = useDebouncedValue(term.trim(), 300)
  const searchFor = term.trim() === '' ? '' : settled
  const all = usePeople(workspaceId)
  const found = usePeopleSearch(workspaceId, by, searchFor)
  const [adding, setAdding] = useState(false)
  const field = SEARCH_FIELDS.find((option) => option.value === by)!

  const addButton = (
    <Button onClick={() => setAdding(true)}>
      <PlusIcon aria-hidden="true" />
      Thêm người
    </Button>
  )

  return (
    <div className="flex max-w-3xl flex-col gap-6">
      <header className="flex flex-wrap items-center gap-3">
        <h1 className="flex-1 text-2xl font-semibold tracking-tight">Người</h1>
        {addButton}
      </header>
      <div className="flex gap-2">
        <NativeSelect aria-label="Tìm theo" value={by} onChange={(event) => setBy(event.target.value as PersonSearchField)}>
          {SEARCH_FIELDS.map((option) => (
            <NativeSelectOption key={option.value} value={option.value}>
              {option.label}
            </NativeSelectOption>
          ))}
        </NativeSelect>
        <Input
          type="search"
          aria-label="Tìm người"
          placeholder={field.placeholder}
          value={term}
          onChange={(event) => setTerm(event.target.value)}
          className="flex-1"
        />
      </div>
      {searchFor !== '' ? (
        <PeopleList ws={workspaceId} list={found} empty={<p className="text-muted-foreground">Không tìm thấy ai.</p>} />
      ) : (
        <PeopleList
          ws={workspaceId}
          list={all}
          empty={
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
          }
        />
      )}
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
      <ul className="divide-y rounded-lg border">
        {list.items.map((person) => (
          <li key={person.id}>
            <Link
              to={`/w/${ws}/people/${person.id}`}
              className="flex flex-wrap items-baseline gap-x-4 gap-y-1 px-3 py-2.5 outline-none hover:bg-muted focus-visible:bg-muted"
            >
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
