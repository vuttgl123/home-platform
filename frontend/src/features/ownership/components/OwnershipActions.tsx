import { CalendarXIcon, MoreHorizontalIcon, PencilIcon, Trash2Icon } from 'lucide-react'
import { useState } from 'react'

import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

import type { Ownership } from '../types'
import { DeleteOwnershipDialog } from './DeleteOwnershipDialog'
import { EndOwnershipDialog } from './EndOwnershipDialog'
import { OwnershipFormDialog } from './OwnershipFormDialog'

type Action = 'end' | 'edit' | 'delete'

/** Menu thao tác của một dòng sở hữu: thôi sở hữu (khi chưa có ngày kết thúc), sửa, xoá dòng nhập nhầm. */
export function OwnershipActions({ ws, row }: { ws: string; row: Ownership }) {
  const [dialog, setDialog] = useState<Action | null>(null)
  const close = (open: boolean) => !open && setDialog(null)
  const open = row.endDate == null
  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger render={<Button variant="ghost" size="icon-sm" aria-label={`Thao tác với ${row.personFullName}`} />}>
          <MoreHorizontalIcon aria-hidden="true" />
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="min-w-48">
          {open && (
            <DropdownMenuItem onClick={() => setDialog('end')}>
              <CalendarXIcon aria-hidden="true" />
              Thôi sở hữu
            </DropdownMenuItem>
          )}
          <DropdownMenuItem onClick={() => setDialog('edit')}>
            <PencilIcon aria-hidden="true" />
            Sửa
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem variant="destructive" onClick={() => setDialog('delete')}>
            <Trash2Icon aria-hidden="true" />
            Xoá dòng nhập nhầm
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
      <EndOwnershipDialog ws={ws} ownership={row} open={dialog === 'end'} onOpenChange={close} />
      <OwnershipFormDialog ws={ws} mode="edit" ownership={row} open={dialog === 'edit'} onOpenChange={close} />
      <DeleteOwnershipDialog ws={ws} ownership={row} open={dialog === 'delete'} onOpenChange={close} />
    </>
  )
}
