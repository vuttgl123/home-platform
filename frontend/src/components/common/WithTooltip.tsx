import type { ReactElement } from 'react'

import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip'

/**
 * Chú thích khi rê chuột hay đưa tiêu điểm tới một nút chỉ có biểu tượng. `children` là chính phần
 * tử kích hoạt (nút, `SidebarTrigger`, `DropdownMenuTrigger`…); nó cần tự có `aria-label` cùng chữ
 * với `label`, vì chú thích chỉ hiện cho người nhìn.
 */
export function WithTooltip({ label, children, side = 'bottom' }: { label: string; children: ReactElement; side?: 'top' | 'bottom' | 'left' | 'right' }) {
  return (
    <Tooltip>
      <TooltipTrigger render={children} />
      <TooltipContent side={side}>{label}</TooltipContent>
    </Tooltip>
  )
}
