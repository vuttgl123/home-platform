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
 * Ô tròn chữ cái đầu của một người (chưa có ảnh: ảnh đến ở đợt media). Chỉ để nhìn: tên luôn có
 * chữ bên cạnh, nên ô này ẩn với công cụ đọc màn hình.
 */
export function PersonAvatar({ name, size = 'default', className }: { name: string | null | undefined; size?: 'sm' | 'default' | 'lg'; className?: string }) {
  return (
    <Avatar size={size} aria-hidden="true" className={className}>
      <AvatarFallback className={cn('font-semibold', size === 'sm' ? 'text-[10px]' : 'text-xs', shadeOf(name ?? ''))}>
        {initials(name)}
      </AvatarFallback>
    </Avatar>
  )
}
