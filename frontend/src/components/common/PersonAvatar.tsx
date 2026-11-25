import { useState } from 'react'

import { cn } from 'cn'

import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { initials } from '@/lib/initials'

// Sắc nhạt cho nền avatar; cùng một tên luôn ra cùng một màu.
const SHADES = [
  'bg-primary/15 text-primary',
  'bg-success/15 text-success-foreground',
  'bg-warning/18 text-warning-foreground',
  'bg-info/15 text-info-foreground',
  'bg-violet/15 text-violet-foreground',
  'bg-danger/12 text-danger-foreground',
]

function shadeOf(name: string): string {
  let hash = 0
  for (const char of name) {
    hash = (hash * 31 + (char.codePointAt(0) ?? 0)) >>> 0
  }
  return SHADES[hash % SHADES.length]
}

/**
 * Ô tròn ảnh đại diện của một người, hay chữ cái đầu khi chưa có ảnh hoặc ảnh không tải được. Chỉ để
 * nhìn: tên luôn có chữ bên cạnh, nên ô này ẩn với công cụ đọc màn hình. Ảnh là thẻ `img` thường
 * (không phải `AvatarImage`, vốn chỉ hiện sau khi tải xong), để hiện ngay và test được.
 */
export function PersonAvatar({
  name,
  photoUrl,
  size = 'default',
  className,
}: {
  name: string | null | undefined
  photoUrl?: string | null
  size?: 'sm' | 'default' | 'lg'
  className?: string
}) {
  const [failed, setFailed] = useState<string | null>(null)
  const showPhoto = photoUrl != null && failed !== photoUrl
  return (
    <Avatar size={size} aria-hidden="true" className={className}>
      {showPhoto ? (
        <img src={photoUrl} alt="" onError={() => setFailed(photoUrl)} className="size-full rounded-full object-cover" />
      ) : (
        <AvatarFallback className={cn('font-semibold', size === 'sm' ? 'text-[10px]' : 'text-xs', shadeOf(name ?? ''))}>
          {initials(name)}
        </AvatarFallback>
      )}
    </Avatar>
  )
}
