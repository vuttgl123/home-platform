import type { ReactNode } from 'react'

import { AspectRatio } from '@/components/ui/aspect-ratio'

import { photoAlt } from '../photoText'
import type { Photo } from '../types'
import { thumbnailUrl } from '../urls'

/**
 * Lưới ảnh thu nhỏ vuông, chú thích dưới ảnh. `onOpen` mở ảnh lớn; `badge` và `actions` gắn thêm
 * nhãn, menu cho từng ảnh.
 */
export function PhotoGrid({
  ws,
  photos,
  onOpen,
  badge,
  actions,
}: {
  ws: string
  photos: Photo[]
  onOpen?: (index: number) => void
  badge?: (photo: Photo) => ReactNode
  actions?: (photo: Photo, index: number) => ReactNode
}) {
  return (
    <ul aria-label="Ảnh" className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
      {photos.map((photo, index) => (
        <li key={photo.id} className="flex flex-col gap-1.5">
          <div className="relative overflow-hidden rounded-lg border bg-muted">
            <AspectRatio ratio={1}>
              <button
                type="button"
                className="size-full cursor-zoom-in outline-none focus-visible:ring-2 focus-visible:ring-ring"
                onClick={() => onOpen?.(index)}
                aria-label={`Xem ${photoAlt(photo, index)}`}
              >
                <img
                  src={thumbnailUrl(ws, photo.fileId)}
                  alt={photoAlt(photo, index)}
                  loading="lazy"
                  className="size-full object-cover"
                />
              </button>
            </AspectRatio>
            {badge && <div className="absolute top-1.5 left-1.5">{badge(photo)}</div>}
            {actions && <div className="absolute top-1.5 right-1.5">{actions(photo, index)}</div>}
          </div>
          {photo.caption && <p className="line-clamp-2 text-sm">{photo.caption}</p>}
        </li>
      ))}
    </ul>
  )
}
