import { ChevronLeftIcon, ChevronRightIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'

import { photoAlt } from '../photoText'
import type { Photo } from '../types'
import { photoUrl } from '../urls'

/**
 * Xem ảnh gốc, cỡ lớn: nút "Ảnh trước", "Ảnh sau" và phím mũi tên trái, phải để lật. `index` là ảnh
 * đang xem; `null` là đóng.
 */
export function PhotoViewer({
  ws,
  photos,
  index,
  onIndexChange,
}: {
  ws: string
  photos: Photo[]
  index: number | null
  onIndexChange: (index: number | null) => void
}) {
  const current = index != null ? photos[index] : undefined
  const go = (delta: number) => {
    if (index == null) return
    const next = index + delta
    if (next >= 0 && next < photos.length) onIndexChange(next)
  }
  return (
    <Dialog open={current != null} onOpenChange={(open) => !open && onIndexChange(null)}>
      <DialogContent
        className="max-h-[95svh] gap-3 sm:max-w-4xl"
        onKeyDown={(event) => {
          if (event.key === 'ArrowRight') go(1)
          if (event.key === 'ArrowLeft') go(-1)
        }}
      >
        {current && index != null && (
          <>
            <DialogHeader>
              <DialogTitle className="tabular-nums">{`Ảnh ${index + 1} / ${photos.length}`}</DialogTitle>
            </DialogHeader>
            <div className="flex min-h-0 items-center justify-center rounded-lg bg-muted">
              <img
                src={photoUrl(ws, current.fileId)}
                alt={photoAlt(current, index)}
                className="max-h-[70svh] w-auto max-w-full object-contain"
              />
            </div>
            <div className="flex items-center justify-between gap-3">
              <Button variant="outline" size="icon" aria-label="Ảnh trước" disabled={index === 0} onClick={() => go(-1)}>
                <ChevronLeftIcon aria-hidden="true" />
              </Button>
              <p className="min-w-0 flex-1 text-center text-sm">{current.caption}</p>
              <Button
                variant="outline"
                size="icon"
                aria-label="Ảnh sau"
                disabled={index === photos.length - 1}
                onClick={() => go(1)}
              >
                <ChevronRightIcon aria-hidden="true" />
              </Button>
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  )
}
