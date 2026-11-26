import { useState } from 'react'
import { useParams } from 'react-router'

import { Button } from '@/components/ui/button'

import { MAX_FILE_BYTES } from '../api'
import { FilePicker } from '../components/FilePicker'
import { ImportSteps } from '../components/ImportSteps'
import { ReportView } from '../components/ReportView'
import { importErrorMessage, TOO_LARGE } from '../messages'
import { usePreviewImport } from '../queries'
import { blockingErrors } from '../report'

/**
 * Trang `/w/:workspaceId/import` (spec Đợt 9, mục 4.3): chọn file, xem trước từng dòng, rồi nhập.
 * Báo cáo xem trước chỉ nằm trong trang; rời trang là chọn lại file.
 */
export function ImportPage() {
  const { workspaceId = '' } = useParams()
  const preview = usePreviewImport(workspaceId)
  const [pickError, setPickError] = useState<string | null>(null)
  const report = preview.data ?? null

  function pick(file: File) {
    preview.reset()
    if (file.size > MAX_FILE_BYTES) {
      setPickError(TOO_LARGE)
      return
    }
    setPickError(null)
    preview.mutate(file)
  }

  function startOver() {
    preview.reset()
    setPickError(null)
  }

  const blocking = report ? blockingErrors(report) : 0

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">Nhập từ Excel</h1>
        <p className="text-muted-foreground">Thêm địa điểm, người, cư trú, sở hữu và đồ đạc từ một file Excel.</p>
      </header>
      <ImportSteps current={report ? 2 : 1} />
      {report ? (
        <>
          <ReportView key={report.fileName + report.totals.rows} report={report} />
          <div className="flex flex-col-reverse gap-3 sm:flex-row sm:items-center sm:justify-end">
            <Button variant="outline" onClick={startOver}>
              Chọn file khác
            </Button>
            <div className="flex flex-col gap-1 sm:items-end">
              <Button disabled={blocking > 0}>Nhập {report.totals.rows} dòng</Button>
              {blocking > 0 ? (
                <p className="text-sm text-muted-foreground">Sửa {blocking} lỗi trong file rồi chọn lại file</p>
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
