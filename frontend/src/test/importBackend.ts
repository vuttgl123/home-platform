import { http, HttpResponse } from 'msw'

import type { ImportReport } from '@/features/imports'

import { importReport } from './fixtures'
import { server } from './server'

/** Một lần gửi file tới backend nhập giả. */
export type ImportCall = { path: 'preview' | 'commit'; fileName: string; size: number }

/** Báo cáo trả về, hoặc một response tuỳ ý (lỗi HTTP), hoặc hàm dựng một trong hai (có thể chờ). */
export type ImportResponder = ImportReport | Response | (() => ImportReport | Response | Promise<ImportReport | Response>)

/**
 * Backend nhập giả: `POST /imports/preview` và `POST /imports` đọc phần `file` của form, ghi lại tên
 * và cỡ file, rồi trả báo cáo. Mặc định xem trước trả `importReport()`, nhập trả cùng báo cáo đó với
 * `committed: true`.
 */
export function mockImportBackend(options: { preview?: ImportResponder; commit?: ImportResponder } = {}): ImportCall[] {
  const calls: ImportCall[] = []
  const handle = (path: ImportCall['path'], responder: ImportResponder) => async ({ request }: { request: Request }) => {
    const file = (await request.formData()).get('file') as File
    calls.push({ path, fileName: file.name, size: file.size })
    const result = typeof responder === 'function' ? await responder() : responder
    return result instanceof Response ? result : HttpResponse.json(result)
  }
  server.use(
    http.post('/api/registry/imports/preview', handle('preview', options.preview ?? importReport())),
    http.post('/api/registry/imports', handle('commit', options.commit ?? importReport({ committed: true }))),
  )
  return calls
}
