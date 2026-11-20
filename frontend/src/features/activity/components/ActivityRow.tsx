import { Link } from 'react-router'

import { ToneBadge } from '@/components/common/ToneBadge'
import { currentUserId } from '@/api'
import { Badge } from '@/components/ui/badge'
import { ACTION_LABEL, ACTION_TONE } from '@/features/history'
import { formatDateTime } from '@/lib/format'

import { ENTITY_LABEL, ENTITY_PATH, FIELD_LABEL } from '../labels'
import type { ActivityEntry } from '../types'
import { RestoreButton } from './RestoreButton'

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
        <ToneBadge tone={ACTION_TONE[entry.action]}>{ACTION_LABEL[entry.action]}</ToneBadge>
        <span className="text-muted-foreground">
          v{entry.versionNo} · {formatDateTime(entry.changedAt)} · {entry.changedBy === currentUserId() ? 'Bạn' : 'Người dùng khác'}
        </span>
        {deleted && <RestoreButton ws={ws} entityType={entry.entityType} entityId={entry.entityId} version={entry.versionNo} name={name} />}
      </div>
      {fields.length > 0 && <span className="text-muted-foreground">Đã đổi: {fields.join(', ')}</span>}
      {entry.note && <span className="italic">{entry.note}</span>}
    </li>
  )
}
