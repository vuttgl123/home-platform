import { CheckIcon, LoaderCircleIcon, TriangleAlertIcon } from 'lucide-react'

import { cn } from 'cn'

/** Một file trong lượt tải lên: đang chờ, đang gửi, xong, hay lỗi (kèm lý do). */
export type QueuedUpload = { key: string; name: string; state: 'waiting' | 'uploading' | 'done' | 'error'; message?: string }

const LABEL: Record<QueuedUpload['state'], string> = { waiting: 'Đang chờ', uploading: 'Đang tải…', done: 'Xong', error: '' }

/** Danh sách các file của lượt tải lên gần nhất, mỗi file một dòng trạng thái. */
export function UploadQueue({ items }: { items: QueuedUpload[] }) {
  if (items.length === 0) return null
  return (
    <ul aria-label="Ảnh đang tải lên" className="flex flex-col gap-1 rounded-lg border bg-card p-3 text-sm">
      {items.map((item) => (
        <li key={item.key} className="flex items-center justify-between gap-3">
          <span className="min-w-0 truncate">{item.name}</span>
          <span
            className={cn(
              'inline-flex shrink-0 items-center gap-1',
              item.state === 'error' ? 'text-danger-foreground' : item.state === 'done' ? 'text-success-foreground' : 'text-muted-foreground',
            )}
          >
            {item.state === 'uploading' && <LoaderCircleIcon aria-hidden="true" className="size-3.5 animate-spin" />}
            {item.state === 'done' && <CheckIcon aria-hidden="true" className="size-3.5" />}
            {item.state === 'error' && <TriangleAlertIcon aria-hidden="true" className="size-3.5" />}
            {item.state === 'error' ? item.message : LABEL[item.state]}
          </span>
        </li>
      ))}
    </ul>
  )
}
