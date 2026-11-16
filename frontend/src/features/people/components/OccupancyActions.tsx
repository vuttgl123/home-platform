import { DoorOpenIcon, MoreHorizontalIcon, PencilIcon, Trash2Icon } from 'lucide-react'
import { useState } from 'react'

import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

import type { Occupancy } from '../types'
import { DeleteOccupancyDialog } from './DeleteOccupancyDialog'
import { EndOccupancyDialog } from './EndOccupancyDialog'
import { OccupancyFormDialog } from './OccupancyFormDialog'

/** Menu thao tác của một dòng cư trú: chuyển đi (khi chưa rời), sửa, xoá dòng nhập nhầm. */
export function OccupancyActions({ ws, row }: { ws: string; row: Occupancy }) {
  const [dialog, setDialog] = useState<'end' | 'edit' | 'delete' | null>(null)
  const close = (open: boolean) => !open && setDialog(null)
  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger
          render={<Button variant="ghost" size="icon-sm" aria-label={`Thao tác với ${row.personFullName}`} />}
        >
          <MoreHorizontalIcon aria-hidden="true" />
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="min-w-48">
          {row.status !== 'PAST' && (
            <DropdownMenuItem onClick={() => setDialog('end')}>
              <DoorOpenIcon aria-hidden="true" />
              Chuyển đi
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
      <EndOccupancyDialog ws={ws} occupancy={row} open={dialog === 'end'} onOpenChange={close} />
      <OccupancyFormDialog ws={ws} mode="edit" occupancy={row} open={dialog === 'edit'} onOpenChange={close} />
      <DeleteOccupancyDialog ws={ws} occupancy={row} open={dialog === 'delete'} onOpenChange={close} />
    </>
  )
}
