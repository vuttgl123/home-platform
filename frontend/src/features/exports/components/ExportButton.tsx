import { useMutation } from '@tanstack/react-query'
import { DownloadIcon } from 'lucide-react'

import { type ApiError, downloadFile } from '@/api'
import { Button } from '@/components/ui/button'
import { Spinner } from '@/components/ui/spinner'
import { toast } from '@/components/ui/toast'
import { useWorkspaceName } from '@/features/workspaces'

import { exportFileName, type ExportKind } from '../fileName'

const EXPORT_PATH: Record<ExportKind, string> = {
  workspace: '/exports/workspace.xlsx',
  items: '/exports/items.xlsx',
  people: '/exports/people.xlsx',
  template: '/imports/template.xlsx',
}

/**
 * Nút tải một file Excel của không gian `ws`. Trong lúc tải nút bị khoá và hiện vòng xoay; lỗi hiện
 * thành thông báo.
 * @param query bộ lọc gửi kèm (chỉ file đồ đạc có); khoá `undefined` bị bỏ qua
 */
export function ExportButton({
  ws,
  kind,
  query,
  label,
}: {
  ws: string
  kind: ExportKind
  query?: Record<string, string | undefined>
  label: string
}) {
  const workspaceName = useWorkspaceName(ws)
  const download = useMutation<void, ApiError>({
    mutationFn: () => downloadFile(ws, EXPORT_PATH[kind], query ?? {}, exportFileName(workspaceName, kind, new Date())),
    onError: (error) => toast.add({ title: error.detail, type: 'error' }),
  })

  return (
    <Button variant="outline" disabled={download.isPending} aria-busy={download.isPending} onClick={() => download.mutate()}>
      {download.isPending ? <Spinner aria-hidden="true" /> : <DownloadIcon aria-hidden="true" />}
      {label}
    </Button>
  )
}
