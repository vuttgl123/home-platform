import type { MediaSchemas, Present } from '@/api'

type S = MediaSchemas

/** Bản ghi có ảnh: địa điểm, người hay món đồ. */
export type OwnerType = 'PLACE' | 'PERSON' | 'ITEM'

/** Bản ghi đang xem ảnh. */
export type PhotoOwner = { type: OwnerType; id: string }

export type PhotoFile = Present<S['FileInfo'], 'originalName' | 'contentType' | 'sizeBytes' | 'width' | 'height'>

/** Một ảnh đã gắn; `width`, `height` của file theo chiều hiển thị. */
export type Photo = Omit<
  Present<S['AttachmentResponse'], 'id' | 'ownerType' | 'ownerId' | 'fileId' | 'sortOrder' | 'addedBy' | 'addedAt'>,
  'file'
> & { file: PhotoFile }
