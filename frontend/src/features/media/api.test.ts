import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { setCurrentUser } from '@/api'
import { server } from '@/test/server'

import { uploadPhoto } from './api'
import { checkPhotoFile, photoUrl, thumbnailUrl } from './urls'

test('uploadSendsMultipart', async () => {
  let received: { type: string | null; ownerType: string | null; ownerId: string | null; hasFile: boolean } | null = null
  server.use(
    http.post('/api/media/attachments', async ({ request }) => {
      const form = await request.formData()
      const query = new URL(request.url).searchParams
      received = {
        type: request.headers.get('content-type'),
        ownerType: query.get('ownerType'),
        ownerId: query.get('ownerId'),
        hasFile: form.has('file'),
      }
      return HttpResponse.json({ id: '1', fileId: '9', ownerType: 'PLACE', ownerId: '301', sortOrder: 0 }, { status: 201 })
    }),
  )

  setCurrentUser('42')
  const photo = new File([new Uint8Array([1, 2, 3])], 'phong.jpg', { type: 'image/jpeg' })
  const created = await uploadPhoto('7', { type: 'PLACE', id: '301' }, photo)

  expect(created.fileId).toBe('9')
  expect(received!.type).toMatch(/^multipart\/form-data; boundary=/)
  expect(received!.ownerType).toBe('PLACE')
  expect(received!.ownerId).toBe('301')
  // Nội dung và tên file không đo được ở đây: FormData của jsdom đi qua Request của undici không giữ
  // nguyên File. Tải ảnh thật lên media-service được kiểm bằng trình duyệt (Task 12).
  expect(received!.hasFile).toBe(true)
})

test('urlsCarryTheWorkspace', () => {
  expect(photoUrl('7', '9')).toBe('/api/media/workspaces/7/files/9')
  expect(thumbnailUrl('7', '9')).toBe('/api/media/workspaces/7/files/9/thumbnail')
})

test('fileCheckRejectsTypeAndSize', () => {
  const file = (type: string, size: number) => new File([new Uint8Array(size)], 'a', { type })
  expect(checkPhotoFile(file('image/jpeg', 10))).toBeNull()
  expect(checkPhotoFile(file('image/png', 10))).toBeNull()
  expect(checkPhotoFile(file('image/webp', 10))).toBeNull()
  expect(checkPhotoFile(file('image/gif', 10))).toBe('Chỉ nhận ảnh JPEG, PNG, WebP')
  expect(checkPhotoFile(file('image/jpeg', 10 * 1024 * 1024 + 1))).toBe('Ảnh lớn hơn 10 MB')
})
