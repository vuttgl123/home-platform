import { useState } from 'react'

import { cn } from 'cn'

/**
 * Ảnh bìa nhỏ ở đầu trang một địa điểm hay món đồ. Không có `src`, hay ảnh không tải được, thì không
 * hiện gì: đầu trang vẫn đủ nghĩa với tên.
 */
export function CoverImage({ src, alt, className }: { src: string | null; alt: string; className?: string }) {
  const [failed, setFailed] = useState<string | null>(null)
  if (!src || failed === src) return null
  return (
    <img
      src={src}
      alt={alt}
      onError={() => setFailed(src)}
      className={cn('size-14 shrink-0 rounded-lg border bg-muted object-cover', className)}
    />
  )
}
