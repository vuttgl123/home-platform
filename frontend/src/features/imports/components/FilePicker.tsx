import { FileSpreadsheetIcon, UploadIcon } from 'lucide-react'
import { useId, useState } from 'react'

import { Spinner } from '@/components/ui/spinner'
import { ExportButton } from '@/features/exports'
import { cn } from '@/lib/utils'

const ACCEPT = '.xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'

/**
 * Bước ①: tải file mẫu, rồi chọn (hoặc kéo thả) một file `.xlsx`. Chọn xong là gọi `onPick` ngay.
 * @param error lời nhắn của lần gửi trước, nếu hỏng
 */
export function FilePicker({
  ws,
  pending,
  error,
  onPick,
}: {
  ws: string
  pending: boolean
  error: string | null
  onPick: (file: File) => void
}) {
  const inputId = useId()
  const [dragging, setDragging] = useState(false)

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center gap-3">
        <ExportButton ws={ws} kind="template" label="Tải mẫu" />
        <p className="text-sm text-muted-foreground">
          File mẫu có sẵn các cột của không gian này và một sheet hướng dẫn.
        </p>
      </div>
      <div
        className={cn(
          'flex flex-col items-center justify-center gap-3 rounded-xl border-2 border-dashed px-4 py-10 text-center transition-colors',
          dragging ? 'border-primary bg-primary/5' : 'border-border',
        )}
        onDragOver={(event) => {
          event.preventDefault()
          setDragging(true)
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={(event) => {
          event.preventDefault()
          setDragging(false)
          const file = event.dataTransfer.files[0]
          if (file && !pending) onPick(file)
        }}
      >
        {pending ? (
          <p className="flex items-center gap-2 text-sm text-muted-foreground" aria-live="polite">
            <Spinner aria-hidden="true" />
            Đang đọc file…
          </p>
        ) : (
          <>
            <FileSpreadsheetIcon aria-hidden="true" className="size-10 text-muted-foreground" />
            <label
              htmlFor={inputId}
              className="inline-flex h-9 cursor-pointer items-center gap-2 rounded-lg bg-primary px-4 text-sm font-medium text-primary-foreground hover:bg-primary/90 focus-within:ring-3 focus-within:ring-ring/50"
            >
              <UploadIcon aria-hidden="true" className="size-4" />
              Chọn file .xlsx
              <input
                id={inputId}
                type="file"
                accept={ACCEPT}
                className="sr-only"
                onChange={(event) => {
                  const file = event.target.files?.[0]
                  event.target.value = ''
                  if (file) onPick(file)
                }}
              />
            </label>
            <p className="text-sm text-muted-foreground">hoặc kéo file vào đây</p>
          </>
        )}
        {error ? (
          <p role="alert" className="text-sm text-destructive">
            {error}
          </p>
        ) : null}
      </div>
    </div>
  )
}
