import { registry, unwrap, workspaceHeaders } from '@/api'

import type { ImportReport } from './types'

/** Giới hạn cỡ file của registry; file lớn hơn thì không gửi. */
export const MAX_FILE_BYTES = 5 * 1024 * 1024

type ImportPath = '/imports/preview' | '/imports'

/** Trình duyệt không đọc được file đã chọn nữa: file trên máy đã bị sửa, đổi tên hay xoá. */
export class UnreadableFileError extends Error {
  constructor() {
    super('The chosen file can no longer be read')
    this.name = 'UnreadableFileError'
  }
}

/**
 * Đọc hết file trước khi gửi (tối đa 5 MB): file trên máy đổi sau lúc chọn thì báo rõ, thay vì để
 * request hỏng giữa chừng như một lỗi mạng.
 */
async function send(ws: string, path: ImportPath, file: File): Promise<ImportReport> {
  let content: ArrayBuffer
  try {
    content = await file.arrayBuffer()
  } catch {
    throw new UnreadableFileError()
  }
  const form = new FormData()
  form.append('file', new File([content], file.name, { type: file.type }), file.name)
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
