import { Link } from 'react-router'

import { ToneBadge } from '@/components/common/ToneBadge'
import { Badge } from '@/components/ui/badge'
import { ACTION_LABEL, ACTION_TONE } from '@/features/history'
import { UserChip } from '@/features/users'
import { formatDateTime } from '@/lib/format'

import { ENTITY_LABEL, ENTITY_PATH, FIELD_LABEL } from '../labels'
import type { ActivityEntry } from '../types'
import { RestoreButton } from './RestoreButton'

/** Trang mở từ một dòng: bản ghi đó, hoặc tab Bảo trì của món đồ chứa dòng bảo trì; `null` khi không mở được. */
function hrefOf(ws: string, entry: ActivityEntry): string | null {
  if (entry.entityType === 'MAINTENANCE') {
    return entry.parentId ? `/w/${ws}/items/${entry.parentId}?tab=maintenance` : null
  }
  return entry.action === 'DELETE' ? null : `/w/${ws}/${ENTITY_PATH[entry.entityType]}/${entry.entityId}`
}

/**
 * Một dòng hoạt động. Dòng "Xoá" không có liên kết (bản ghi không mở được) mà có nút "Khôi phục":
 * gửi `version` bằng số version của dòng đó, vì lần xoá là lần ghi cuối của bản ghi. Dòng bảo trì luôn
 * mở tab Bảo trì của món đồ, kể cả khi đã xoá: lấy lại dòng đó bằng Hoàn tác, không khôi phục từ đây.
 */
export function ActivityRow({ ws, entry }: { ws: string; entry: ActivityEntry }) {
  const name = entry.entityName ?? `#${entry.entityId}`
  const fields = (entry.changedFields ?? []).map((field) => FIELD_LABEL[field] ?? field)
  const deleted = entry.action === 'DELETE'
  const href = hrefOf(ws, entry)
  const entityType = entry.entityType

  return (
    <li className="flex flex-col gap-1 px-3 py-2.5 text-sm">
      <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
        <Badge variant="outline">{ENTITY_LABEL[entry.entityType]}</Badge>
        {href ? (
          <Link to={href} className="font-medium hover:underline">
            {name}
          </Link>
        ) : (
          <span className="font-medium">{name}</span>
        )}
        <ToneBadge tone={ACTION_TONE[entry.action]}>{ACTION_LABEL[entry.action]}</ToneBadge>
        <span className="inline-flex flex-wrap items-center gap-x-2 text-muted-foreground">
          <span className="tabular-nums">
            v{entry.versionNo} · {formatDateTime(entry.changedAt)}
          </span>
          <UserChip userId={entry.changedBy} />
        </span>
        {deleted && entityType !== 'MAINTENANCE' && (
          <RestoreButton ws={ws} entityType={entityType} entityId={entry.entityId} version={entry.versionNo} name={name} />
        )}
      </div>
      {fields.length > 0 && <span className="text-muted-foreground">Đã đổi: {fields.join(', ')}</span>}
      {entry.note && <span className="italic">{entry.note}</span>}
    </li>
  )
}
