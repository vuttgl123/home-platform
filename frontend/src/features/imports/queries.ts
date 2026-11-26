import { useMutation } from '@tanstack/react-query'

import type { ApiError } from '@/api'

import { previewImport } from './api'
import type { ImportReport } from './types'

/** Gửi file để xem trước; báo cáo nằm ở `data` tới khi `reset`. */
export function usePreviewImport(ws: string) {
  return useMutation<ImportReport, ApiError, File>({ mutationFn: (file) => previewImport(ws, file) })
}
