import { media, unwrap, workspaceHeaders } from '@/api'

import type { Photo, PhotoOwner } from './types'

/** Ảnh đang hiện của một bản ghi, theo thứ tự. */
export async function listPhotos(ws: string, owner: PhotoOwner): Promise<Photo[]> {
  const call = media.GET('/attachments', {
    params: { header: workspaceHeaders(ws), query: { ownerType: owner.type, ownerId: owner.id } },
  })
  return (await unwrap(call)) as Photo[]
}

/** Tải một ảnh lên và gắn vào bản ghi; ảnh mới đứng cuối. `Content-Type` do trình duyệt đặt (có boundary). */
export async function uploadPhoto(ws: string, owner: PhotoOwner, file: File, caption?: string): Promise<Photo> {
  const form = new FormData()
  form.append('file', file, file.name)
  const call = media.POST('/attachments', {
    params: { header: workspaceHeaders(ws), query: { ownerType: owner.type, ownerId: owner.id, caption } },
    body: form as never,
    bodySerializer: (body) => body as unknown as FormData,
  })
  return (await unwrap(call)) as Photo
}

export async function setCaption(ws: string, id: string, caption: string): Promise<Photo> {
  const call = media.PUT('/attachments/{id}', { params: { header: workspaceHeaders(ws), path: { id } }, body: { caption } })
  return (await unwrap(call)) as Photo
}

/** Sắp lại mọi ảnh đang hiện của bản ghi; `ids` phải đủ và đúng, không thì 409. */
export async function reorderPhotos(ws: string, owner: PhotoOwner, ids: string[]): Promise<Photo[]> {
  const call = media.PUT('/attachments/order', {
    params: { header: workspaceHeaders(ws) },
    body: { ownerType: owner.type, ownerId: owner.id, attachmentIds: ids },
  })
  return (await unwrap(call)) as Photo[]
}

/** Gỡ ảnh; dòng và file vẫn còn. */
export async function removePhoto(ws: string, id: string): Promise<void> {
  await unwrap(media.DELETE('/attachments/{id}', { params: { header: workspaceHeaders(ws), path: { id } } }))
}
