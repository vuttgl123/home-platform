import { useQueryClient } from '@tanstack/react-query'
import { MoreHorizontalIcon, PencilIcon, PlusIcon, Trash2Icon, WrenchIcon } from 'lucide-react'
import { useState } from 'react'

import { deletedToast } from '@/components/common/deletedToast'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadMoreButton } from '@/components/common/LoadMoreButton'
import { ToneBadge } from '@/components/common/ToneBadge'
import { WithTooltip } from '@/components/common/WithTooltip'
import { Button } from '@/components/ui/button'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia } from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import { toast } from '@/components/ui/toast'
import type { ItemDetail } from '@/features/items'
import { formatDate } from '@/lib/format'

import { MAINTENANCE_KIND_LABEL, MAINTENANCE_KIND_TONE } from '../labels'
import { undoDeleteMaintenance, useDeleteMaintenance, useMaintenance } from '../queries'
import type { MaintenanceEntry } from '../types'
import { MaintenanceDialog } from './MaintenanceDialog'

/** Tab "Bảo trì" của một món: nhật ký sửa chữa, bảo trì, mới làm trước (spec Đợt 8, mục 4.3). */
export function MaintenanceTab({ ws, item }: { ws: string; item: ItemDetail }) {
  const log = useMaintenance(ws, item.id)
  const [adding, setAdding] = useState(false)
  const [editing, setEditing] = useState<MaintenanceEntry | null>(null)

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-2">
        <p className="text-sm text-muted-foreground">Những lần sửa chữa và bảo trì định kỳ của món này.</p>
        <Button onClick={() => setAdding(true)}>
          <PlusIcon aria-hidden="true" />
          Ghi bảo trì
        </Button>
      </div>
      {log.error ? (
        <ErrorState error={log.error} onRetry={log.refetch} />
      ) : log.isLoading ? (
        <Skeleton className="h-16 w-full" />
      ) : log.items.length === 0 ? (
        <Empty className="border">
          <EmptyHeader>
            <EmptyMedia variant="icon">
              <WrenchIcon aria-hidden="true" />
            </EmptyMedia>
            <EmptyDescription>Chưa có lần sửa hay bảo trì nào</EmptyDescription>
          </EmptyHeader>
        </Empty>
      ) : (
        <ul className="flex flex-col divide-y rounded-lg border bg-card">
          {log.items.map((entry) => (
            <EntryRow key={entry.id} ws={ws} entry={entry} onEdit={() => setEditing(entry)} />
          ))}
        </ul>
      )}
      {log.hasMore && <LoadMoreButton onClick={log.loadMore} isLoading={log.isLoadingMore} />}
      <MaintenanceDialog ws={ws} item={item} open={adding} onOpenChange={setAdding} />
      {editing && (
        <MaintenanceDialog ws={ws} item={item} entry={editing} open onOpenChange={(open) => !open && setEditing(null)} />
      )}
    </div>
  )
}

function EntryRow({ ws, entry, onEdit }: { ws: string; entry: MaintenanceEntry; onEdit: () => void }) {
  const queryClient = useQueryClient()
  const remove = useDeleteMaintenance(ws)

  function deleteEntry() {
    remove.mutate(entry, {
      onSuccess: () => deletedToast({ name: entry.work, undo: () => undoDeleteMaintenance(queryClient, ws, entry) }),
      onError: (error) => toast.add({ title: error.detail ?? 'Không xoá được', type: 'error' }),
    })
  }

  return (
    <li className="flex items-start gap-3 px-4 py-3">
      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <div className="flex flex-wrap items-center gap-2 text-sm">
          <span className="tabular-nums text-muted-foreground">{formatDate(entry.doneOn)}</span>
          <ToneBadge tone={MAINTENANCE_KIND_TONE[entry.kind]}>{MAINTENANCE_KIND_LABEL[entry.kind]}</ToneBadge>
        </div>
        <p className="font-medium break-words">{entry.work}</p>
        {entry.doneBy && <p className="text-sm text-muted-foreground">Người làm: {entry.doneBy}</p>}
        {entry.note && <p className="text-sm whitespace-pre-line text-muted-foreground">{entry.note}</p>}
      </div>
      <DropdownMenu>
        <WithTooltip label={`Thao tác với ${entry.work}`}>
          <DropdownMenuTrigger render={<Button variant="ghost" size="icon-sm" aria-label={`Thao tác với ${entry.work}`} />}>
            <MoreHorizontalIcon aria-hidden="true" />
          </DropdownMenuTrigger>
        </WithTooltip>
        <DropdownMenuContent align="end" className="min-w-36">
          <DropdownMenuItem onClick={onEdit}>
            <PencilIcon aria-hidden="true" />
            Sửa
          </DropdownMenuItem>
          <DropdownMenuItem variant="destructive" onClick={deleteEntry}>
            <Trash2Icon aria-hidden="true" />
            Xoá
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </li>
  )
}
