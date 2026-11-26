import { useMutation, useQueryClient } from '@tanstack/react-query'

import type { ApiError } from '@/api'

import { previewImport, runImport, type UnreadableFileError } from './api'
import type { ImportReport } from './types'

/** Gửi file để xem trước; báo cáo nằm ở `data` tới khi `reset`. */
export function usePreviewImport(ws: string) {
  return useMutation<ImportReport, ApiError | UnreadableFileError, File>({ mutationFn: (file) => previewImport(ws, file) })
}

/**
 * Nhập file. Đã lưu (`committed`) thì mọi dữ liệu đã tải đều cũ: lần nhập chạm tới mọi loại hồ sơ,
 * nên làm mới cả cache thay vì từng khoá.
 */
export function useRunImport(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<ImportReport, ApiError | UnreadableFileError, File>({
    mutationFn: (file) => runImport(ws, file),
    onSuccess: (report) => {
      if (report.committed) {
        void queryClient.invalidateQueries()
      }
    },
  })
}
