import { http, HttpResponse } from 'msw'
import { expect, test, vi } from 'vitest'

import { mockDownloads } from '@/test/downloads'
import { server } from '@/test/server'

import { downloadFile } from './download'
import { ApiError } from './errors'
import { setCurrentUser } from './headers'

function file(body = 'file') {
  return new HttpResponse(body, {
    headers: { 'Content-Type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' },
  })
}

test('sendsWorkspaceHeadersAndQuery', async () => {
  mockDownloads()
  let sent: Request | null = null
  server.use(
    http.get('/api/registry/exports/items.xlsx', ({ request }) => {
      sent = request
      return file()
    }),
  )
  setCurrentUser('42')

  await downloadFile('7', '/exports/items.xlsx', { status: 'BROKEN', categoryId: undefined }, 'a.xlsx')

  expect(sent!.headers.get('X-Workspace-Id')).toBe('7')
  expect(sent!.headers.get('X-User-Id')).toBe('42')
  expect(new URL(sent!.url).search).toBe('?status=BROKEN')
})

test('savesTheBlobUnderTheGivenName', async () => {
  const saved = mockDownloads()
  server.use(http.get('/api/registry/exports/people.xlsx', () => file('nội dung')))
  setCurrentUser('42')

  await downloadFile('7', '/exports/people.xlsx', {}, 'nha-an-nguoi-2026-11-17.xlsx')

  expect(saved).toHaveLength(1)
  expect(saved[0].name).toBe('nha-an-nguoi-2026-11-17.xlsx')
  expect(await saved[0].blob.text()).toBe('nội dung')
  // Liên kết tạm không ở lại sau khi lưu; địa chỉ blob được thu hồi sau (downloadUrlIsRevokedLater).
  expect(document.querySelector('a[download]')).toBeNull()
})

test('errorBecomesApiError', async () => {
  const saved = mockDownloads()
  server.use(
    http.get('/api/registry/exports/items.xlsx', () =>
      HttpResponse.json(
        { status: 404, title: 'Not Found', detail: 'Item category 9 not found' },
        { status: 404, headers: { 'Content-Type': 'application/problem+json' } },
      ),
    ),
  )
  setCurrentUser('42')

  const error = await downloadFile('7', '/exports/items.xlsx', { categoryId: '9' }, 'a.xlsx').catch((e: unknown) => e)

  expect(error).toBeInstanceOf(ApiError)
  expect((error as ApiError).status).toBe(404)
  expect((error as ApiError).detail).toBe('Item category 9 not found')
  expect(saved).toHaveLength(0)
})

test('errorWithoutAJsonBodyIsStillAnApiError', async () => {
  mockDownloads()
  server.use(http.get('/api/registry/exports/people.xlsx', () => new HttpResponse('<html>Bad Gateway</html>', { status: 502 })))
  setCurrentUser('42')

  const error = await downloadFile('7', '/exports/people.xlsx', {}, 'a.xlsx').catch((e: unknown) => e)

  expect(error).toBeInstanceOf(ApiError)
  expect((error as ApiError).status).toBe(502)
})

test('networkFailureBecomesApiError', async () => {
  const saved = mockDownloads()
  server.use(http.get('/api/registry/exports/people.xlsx', () => HttpResponse.error()))
  setCurrentUser('42')

  const error = await downloadFile('7', '/exports/people.xlsx', {}, 'a.xlsx').catch((e: unknown) => e)

  expect(error).toBeInstanceOf(ApiError)
  expect((error as ApiError).status).toBe(0)
  expect(saved).toHaveLength(0)
})

/** Máy chủ ngắt giữa chừng (lỗi khi đang ghi file): không lưu một file dở. */
test('brokenDownloadIsNotSaved', async () => {
  const saved = mockDownloads()
  server.use(
    http.get('/api/registry/exports/workspace.xlsx', () => {
      const body = new ReadableStream<Uint8Array>({
        start(controller) {
          controller.enqueue(new Uint8Array([80, 75]))
          controller.error(new Error('connection reset'))
        },
      })
      return new HttpResponse(body)
    }),
  )
  setCurrentUser('42')

  const error = await downloadFile('7', '/exports/workspace.xlsx', {}, 'a.xlsx').catch((e: unknown) => e)

  expect(error).toBeInstanceOf(ApiError)
  expect((error as ApiError).status).toBe(0)
  expect(saved).toHaveLength(0)
})

test('downloadUrlIsRevokedLater', async () => {
  mockDownloads()
  server.use(http.get('/api/registry/exports/people.xlsx', () => file()))
  setCurrentUser('42')
  const timers = vi.spyOn(globalThis, 'setTimeout')

  await downloadFile('7', '/exports/people.xlsx', {}, 'a.xlsx')

  // Một số trình duyệt huỷ việc tải nếu link tạm bị thu hồi ngay lúc bấm.
  expect(URL.revokeObjectURL).not.toHaveBeenCalled()
  const revoke = timers.mock.calls.find(([, delay]) => delay === 10_000)
  expect(revoke).toBeDefined()
  ;(revoke![0] as () => void)()
  expect(URL.revokeObjectURL).toHaveBeenCalledTimes(1)
  timers.mockRestore()
})
