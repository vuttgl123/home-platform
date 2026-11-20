import { cn } from 'cn'
import type { ReactNode } from 'react'

import { Badge } from '@/components/ui/badge'

/**
 * Nghĩa màu của một nhãn (docs/superpowers/specs/2026-11-20-ui-refresh-design.md mục 3.2). Bảng
 * "trạng thái nào mang nghĩa nào" nằm cạnh bảng nhãn của từng feature (`*_TONE`).
 */
export type Tone = 'success' | 'warning' | 'danger' | 'info' | 'violet' | 'neutral' | 'primary'

const TONE_CLASS: Record<Tone, string> = {
  success: 'bg-success/12 text-success-foreground',
  warning: 'bg-warning/16 text-warning-foreground',
  danger: 'bg-danger/12 text-danger-foreground',
  info: 'bg-info/12 text-info-foreground',
  violet: 'bg-violet/12 text-violet-foreground',
  neutral: 'bg-muted text-muted-foreground',
  primary: 'bg-primary/12 text-primary',
}

/**
 * Nhãn trạng thái hay hành động có màu theo nghĩa, kèm một chấm màu để dễ quét mắt. Chữ của nhãn
 * vẫn nói đủ nghĩa; màu chỉ giúp nhận ra nhanh hơn.
 */
export function ToneBadge({ tone, children, className }: { tone: Tone; children: ReactNode; className?: string }) {
  return (
    <Badge data-tone={tone} className={cn('border-transparent', TONE_CLASS[tone], className)}>
      <span aria-hidden="true" className="size-1.5 shrink-0 rounded-full bg-current opacity-80" />
      {children}
    </Badge>
  )
}
