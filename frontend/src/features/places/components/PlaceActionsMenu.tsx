import { FolderInputIcon, HashIcon, MapPinIcon, MoreHorizontalIcon, PrinterIcon, Trash2Icon } from 'lucide-react'
import { Link } from 'react-router'

import { WithTooltip } from '@/components/common/WithTooltip'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

export type PlaceAction = 'move' | 'address' | 'reissueCode' | 'delete'

/**
 * Menu `⋯` trên đầu trang một địa điểm.
 * @param printHref trang in hồ sơ, mở ở thẻ mới
 */
export function PlaceActionsMenu({
  hasCode,
  printHref,
  onSelect,
}: {
  hasCode: boolean
  printHref: string
  onSelect: (action: PlaceAction) => void
}) {
  return (
    <DropdownMenu>
      <WithTooltip label="Thao tác khác">
        <DropdownMenuTrigger render={<Button variant="outline" size="icon" aria-label="Thao tác khác" />}>
          <MoreHorizontalIcon aria-hidden="true" />
        </DropdownMenuTrigger>
      </WithTooltip>
      <DropdownMenuContent align="end" className="min-w-44">
        <DropdownMenuItem onClick={() => onSelect('move')}>
          <FolderInputIcon aria-hidden="true" />
          Di chuyển
        </DropdownMenuItem>
        <DropdownMenuItem onClick={() => onSelect('address')}>
          <MapPinIcon aria-hidden="true" />
          Địa chỉ
        </DropdownMenuItem>
        <DropdownMenuItem onClick={() => onSelect('reissueCode')}>
          <HashIcon aria-hidden="true" />
          {hasCode ? 'Cấp mã mới' : 'Cấp mã'}
        </DropdownMenuItem>
        <DropdownMenuItem render={<Link to={printHref} target="_blank" rel="noopener" />}>
          <PrinterIcon aria-hidden="true" />
          In hồ sơ
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem variant="destructive" onClick={() => onSelect('delete')}>
          <Trash2Icon aria-hidden="true" />
          Xoá
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
