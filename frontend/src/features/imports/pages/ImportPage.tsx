import { useRef, useState } from 'react'
import { useParams } from 'react-router'

import { Button } from '@/components/ui/button'
import { Progress } from '@/components/ui/progress'

import { MAX_FILE_BYTES } from '../api'
import { FilePicker } from '../components/FilePicker'
import { ImportResult } from '../components/ImportResult'
import { ImportSteps } from '../components/ImportSteps'
import { ReportView } from '../components/ReportView'
import { importErrorMessage, TOO_LARGE } from '../messages'
import { usePreviewImport, useRunImport } from '../queries'
import { blockingErrors } from '../report'

/**
 * Trang `/w/:workspaceId/import` (spec Đợt 9, mục 4.3): chọn file, xem trước từng dòng, rồi nhập
 * đúng file đó (gửi lại, backend kiểm lại từ đầu). Báo cáo chỉ nằm trong trang; rời trang là chọn
 * lại file.
 */
export function ImportPage() {
  const { workspaceId = '' } = useParams()
  const preview = usePreviewImport(workspaceId)
  const run = useRunImport(workspaceId)
  const [file, setFile] = useState<File | null>(null)
  const [pickError, setPickError] = useState<string | null>(null)
  // Chặn bấm hai lần trước khi nút kịp khoá: hai lần nhập cùng một file sinh người trùng.
  const submitted = useRef(false)

  const committed = run.data?.committed ? run.data : null
  // Lần nhập bị huỷ vì có lỗi (dữ liệu đổi từ lúc xem trước): báo cáo mới thay cho báo cáo cũ.
  const changed = run.data && !run.data.committed ? run.data : null
  const report = changed ?? preview.data ?? null

  function pick(next: File) {
    preview.reset()
    run.reset()
    if (next.size > MAX_FILE_BYTES) {
      setFile(null)
      setPickError(TOO_LARGE)
      return
    }
    setPickError(null)
    setFile(next)
    preview.mutate(next)
  }

  function startOver() {
    preview.reset()
    run.reset()
    setFile(null)
    setPickError(null)
  }

  function startImport() {
    if (!file || submitted.current) return
    submitted.current = true
    run.mutate(file, {
      onSettled: () => {
        submitted.current = false
      },
    })
  }

  const step = committed || run.isPending ? 3 : report ? 2 : 1
  const blocking = report ? blockingErrors(report) : 0

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">Nhập từ Excel</h1>
        <p className="text-muted-foreground">Thêm địa điểm, người, cư trú, sở hữu và đồ đạc từ một file Excel.</p>
      </header>
      <ImportSteps current={step} />
      {committed ? (
        <ImportResult ws={workspaceId} report={committed} onAgain={startOver} />
      ) : run.isPending ? (
        <div className="flex flex-col gap-3 rounded-xl border p-6" aria-live="polite">
          <p className="font-medium">Đang nhập, đừng đóng trang</p>
          <Progress value={null} aria-label="Đang nhập" />
        </div>
      ) : report ? (
        <>
          {changed ? (
            <p role="status" className="rounded-lg border border-amber-500/40 px-4 py-3 text-sm text-amber-700 dark:text-amber-400">
              Dữ liệu đã thay đổi từ lúc xem trước
            </p>
          ) : null}
          <ReportView key={`${changed ? 'run' : 'preview'}-${report.fileName}-${report.totals.rows}`} report={report} />
          <div className="flex flex-col-reverse gap-3 sm:flex-row sm:items-start sm:justify-end">
            <Button variant="outline" onClick={startOver}>
              Chọn file khác
            </Button>
            <div className="flex flex-col gap-1 sm:items-end">
              <Button disabled={blocking > 0 || !file} onClick={startImport}>
                Nhập {report.totals.rows} dòng
              </Button>
              {blocking > 0 ? (
                <p className="text-sm text-muted-foreground">Sửa {blocking} lỗi trong file rồi chọn lại file</p>
              ) : null}
              {run.isError ? (
                <p role="alert" className="text-sm text-destructive">
                  {importErrorMessage(run.error)}
                </p>
              ) : null}
            </div>
          </div>
        </>
      ) : (
        <FilePicker
          ws={workspaceId}
          pending={preview.isPending}
          error={pickError ?? (preview.isError ? importErrorMessage(preview.error) : null)}
          onPick={pick}
        />
      )}
    </div>
  )
}
