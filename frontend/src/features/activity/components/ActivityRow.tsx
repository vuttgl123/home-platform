import { useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router'

import { currentUserId, type ApiError } from '@/api'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { toast } from '@/components/ui/toast'
import { ACTION_LABEL } from '@/features/history'
import { useRestoreItem } from '@/features/items'
import { useRestorePerson } from '@/features/people'
import { useRestorePlace } from '@/features/places'
import { formatDateTime } from '@/lib/format'

import { ENTITY_LABEL, ENTITY_PATH, FIELD_LABEL } from '../labels'
import { activityKeys } from '../queries'
import type { ActivityEntry } from '../types'

/**
 * Một dòng hoạt động. Dòng "Xoá" không có liên kết (bản ghi không mở được) mà có nút "Khôi phục":
 * gửi `version` bằng số version của dòng đó, vì lần xoá là lần ghi cuối của bản ghi.
 */
export function ActivityRow({ ws, entry }: { ws: string; entry: ActivityEntry }) {
  const name = entry.entityName ?? `#${entry.entityId}`
  const fields = (entry.changedFields ?? []).map((field) => FIELD_LABEL[field] ?? field)
  const deleted = entry.action === 'DELETE'

  return (
    <li className="flex flex-col gap-1 px-3 py-2.5 text-sm">
      <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
        <Badge variant="outline">{ENTITY_LABEL[entry.entityType]}</Badge>
        {deleted ? (
          <span className="font-medium">{name}</span>
        ) : (
          <Link to={`/w/${ws}/${ENTITY_PATH[entry.entityType]}/${entry.entityId}`} className="font-medium hover:underline">
            {name}
          </Link>
        )}
        <Badge variant={deleted ? 'destructive' : 'secondary'}>{ACTION_LABEL[entry.action]}</Badge>
        <span className="text-muted-foreground">
          v{entry.versionNo} · {formatDateTime(entry.changedAt)} · {entry.changedBy === currentUserId() ? 'Bạn' : 'Người dùng khác'}
        </span>
        {deleted && <RestoreButton ws={ws} entry={entry} name={name} />}
      </div>
      {fields.length > 0 && <span className="text-muted-foreground">Đã đổi: {fields.join(', ')}</span>}
      {entry.note && <span className="italic">{entry.note}</span>}
    </li>
  )
}

function RestoreButton({ ws, entry, name }: { ws: string; entry: ActivityEntry; name: string }) {
  const queryClient = useQueryClient()
  const place = useRestorePlace(ws)
  const person = useRestorePerson(ws)
  const item = useRestoreItem(ws)
  const restore = entry.entityType === 'PLACE' ? place : entry.entityType === 'PERSON' ? person : item
  const error = restore.error as ApiError | null

  function run() {
    restore.mutate(
      { id: entry.entityId, version: entry.versionNo },
      {
        onSuccess: () => {
          toast.add({ title: `Đã khôi phục ${name}`, type: 'success' })
          void queryClient.invalidateQueries({ queryKey: activityKeys.all(ws) })
        },
        onError: (failure) => {
          // Đã có thay đổi sau lần xoá (thường là đã được khôi phục): danh sách đang cũ.
          if (failure.isVersionConflict) void queryClient.invalidateQueries({ queryKey: activityKeys.all(ws) })
        },
      },
    )
  }

  return (
    <>
      <Button variant="outline" size="xs" className="ml-auto" disabled={restore.isPending} onClick={run} aria-label={`Khôi phục ${name}`}>
        Khôi phục
      </Button>
      {error && (
        <p role="alert" className="basis-full text-sm text-destructive">
          {error.isVersionConflict ? 'Đã được khôi phục hoặc thay đổi sau lần xoá này.' : error.detail}
        </p>
      )}
    </>
  )
}
