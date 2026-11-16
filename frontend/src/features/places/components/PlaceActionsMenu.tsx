import { FolderInputIcon, MapPinIcon, MoreHorizontalIcon, Trash2Icon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

export type PlaceAction = 'move' | 'address' | 'delete'

/** Menu `⋯` trên đầu trang một địa điểm. */
export function PlaceActionsMenu({ onSelect }: { onSelect: (action: PlaceAction) => void }) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger render={<Button variant="outline" size="icon" aria-label="Thao tác khác" />}>
        <MoreHorizontalIcon aria-hidden="true" />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="min-w-44">
        <DropdownMenuItem onClick={() => onSelect('move')}>
          <FolderInputIcon aria-hidden="true" />
          Di chuyển
        </DropdownMenuItem>
        <DropdownMenuItem onClick={() => onSelect('address')}>
          <MapPinIcon aria-hidden="true" />
          Địa chỉ
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
