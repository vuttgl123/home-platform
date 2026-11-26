import { registry, unwrap, workspaceHeaders } from '@/api'

import type { ImportReport } from './types'

/** Giới hạn cỡ file của registry; file lớn hơn thì không gửi. */
export const MAX_FILE_BYTES = 5 * 1024 * 1024

type ImportPath = '/imports/preview' | '/imports'

async function send(ws: string, path: ImportPath, file: File): Promise<ImportReport> {
  const form = new FormData()
  form.append('file', file, file.name)
  const call = registry.POST(path, {
    params: { header: workspaceHeaders(ws) },
    body: form as never,
    bodySerializer: (body) => body as unknown as FormData,
  })
  return (await unwrap(call)) as ImportReport
}

/** Chạy thử cả file rồi huỷ: kết quả từng dòng, không lưu gì. */
export function previewImport(ws: string, file: File): Promise<ImportReport> {
  return send(ws, '/imports/preview', file)
}

/** Nhập cả file; chỉ lưu khi không có lỗi nào (`committed`). */
export function runImport(ws: string, file: File): Promise<ImportReport> {
  return send(ws, '/imports', file)
}
