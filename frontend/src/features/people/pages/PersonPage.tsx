import { MoreHorizontalIcon, PencilIcon, Trash2Icon } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'

import { ComingSoonState } from '@/components/common/ComingSoonState'
import { ErrorState } from '@/components/common/ErrorState'
import { NotFoundState } from '@/components/common/NotFoundState'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { formatDate } from '@/lib/format'
import { isId } from '@/lib/ids'

import { DeletePersonDialog } from '../components/DeletePersonDialog'
import { OccupancyList } from '../components/OccupancyList'
import { PersonFormDialog } from '../components/PersonFormDialog'
import { PersonInfoTab } from '../components/PersonInfoTab'
import { GENDER_LABEL } from '../labels'
import { useOccupanciesOfPerson, usePerson } from '../queries'
import type { PersonDetail } from '../types'

const TABS = [
  { value: 'info', label: 'Thông tin' },
  { value: 'places', label: 'Nơi ở' },
  { value: 'history', label: 'Lịch sử' },
] as const

type Tab = (typeof TABS)[number]['value']

function isTab(value: string | null): value is Tab {
  return TABS.some((tab) => tab.value === value)
}

/** Trang `/w/:workspaceId/people/:personId`: hồ sơ một người và những nơi họ đã, đang ở. */
export function PersonPage() {
  const { workspaceId = '', personId } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const requested = searchParams.get('tab')
  const tab: Tab = isTab(requested) ? requested : 'info'
  const person = usePerson(workspaceId, personId)
  const [dialog, setDialog] = useState<'edit' | 'delete' | null>(null)

  if (!isId(personId) || person.error?.isNotFound) {
    return <NotFoundState description="Người này không có, hoặc đã bị xoá." homeHref={`/w/${workspaceId}/people`} />
  }
  if (person.error) {
    return <ErrorState error={person.error} onRetry={() => void person.refetch()} />
  }
  if (!person.data) {
    return (
      <div className="flex flex-col gap-3" aria-label="Đang tải">
        <Skeleton className="h-4 w-24" />
        <Skeleton className="h-8 w-64" />
      </div>
    )
  }
  const detail = person.data

  return (
    <div className="flex flex-col gap-6">
      <PersonHeader
        ws={workspaceId}
        person={detail}
        onEdit={() => setDialog('edit')}
        onDelete={() => setDialog('delete')}
      />
      <Tabs
        value={tab}
        onValueChange={(value) => setSearchParams(value === 'info' ? {} : { tab: value as Tab }, { replace: true })}
      >
        <TabsList variant="line" className="w-full justify-start overflow-x-auto">
          {TABS.map(({ value, label }) => (
            <TabsTrigger key={value} value={value} className="flex-none">
              {label}
            </TabsTrigger>
          ))}
        </TabsList>
        <TabsContent value="info" className="pt-4">
          <PersonInfoTab ws={workspaceId} person={detail} />
        </TabsContent>
        <TabsContent value="places" className="pt-4">
          <PersonPlacesTab ws={workspaceId} person={detail} />
        </TabsContent>
        <TabsContent value="history" className="pt-4">
          <ComingSoonState />
        </TabsContent>
      </Tabs>
      <PersonFormDialog
        ws={workspaceId}
        mode="edit"
        person={detail}
        open={dialog === 'edit'}
        onOpenChange={(open) => setDialog(open ? 'edit' : null)}
      />
      <DeletePersonDialog
        ws={workspaceId}
        person={detail}
        open={dialog === 'delete'}
        onOpenChange={(open) => setDialog(open ? 'delete' : null)}
      />
    </div>
  )
}

function PersonHeader({
  ws,
  person,
  onEdit,
  onDelete,
}: {
  ws: string
  person: PersonDetail
  onEdit: () => void
  onDelete: () => void
}) {
  return (
    <header className="flex flex-col gap-3">
      <Link to={`/w/${ws}/people`} className="w-fit text-sm text-muted-foreground hover:text-foreground">
        Người
      </Link>
      <div className="flex flex-wrap items-start gap-x-4 gap-y-2">
        <h1 className="min-w-0 flex-1 text-2xl font-semibold tracking-tight break-words">{person.fullName}</h1>
        <div className="flex shrink-0 gap-2">
          <Button variant="outline" onClick={onEdit}>
            <PencilIcon aria-hidden="true" />
            Sửa
          </Button>
          <DropdownMenu>
            <DropdownMenuTrigger render={<Button variant="outline" size="icon" aria-label="Thao tác khác" />}>
              <MoreHorizontalIcon aria-hidden="true" />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="min-w-40">
              <DropdownMenuItem variant="destructive" onClick={onDelete}>
                <Trash2Icon aria-hidden="true" />
                Xoá
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
      <dl className="flex flex-wrap items-center gap-2 text-sm">
        {person.gender && (
          <>
            <dt className="sr-only">Giới tính</dt>
            <dd>
              <Badge variant="secondary">{GENDER_LABEL[person.gender]}</Badge>
            </dd>
          </>
        )}
        {person.dateOfBirth && (
          <>
            <dt className="sr-only">Ngày sinh</dt>
            <dd className="text-muted-foreground">{formatDate(person.dateOfBirth)}</dd>
          </>
        )}
        <dt className="sr-only">Version</dt>
        <dd className="text-muted-foreground" title="Số lần đã sửa, tính cả lần tạo">
          v{person.version}
        </dd>
      </dl>
    </header>
  )
}

/** Tab "Nơi ở": mọi nơi người này đã và đang ở, mới nhất trước. */
function PersonPlacesTab({ ws, person }: { ws: string; person: PersonDetail }) {
  const stays = useOccupanciesOfPerson(ws, person.id)
  return (
    <div className="flex max-w-4xl flex-col gap-3">
      <OccupancyList ws={ws} list={stays} show="place" empty="Chưa ở đâu." />
    </div>
  )
}
