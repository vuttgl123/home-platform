import { ArrowLeftIcon, ArrowRightIcon, ImageIcon, MoreHorizontalIcon, PencilIcon, Trash2Icon } from 'lucide-react'
import { useState } from 'react'

import { WithTooltip } from '@/components/common/WithTooltip'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

import { photoAlt } from '../photoText'
import type { Photo, PhotoOwner } from '../types'
import { CaptionDialog } from './CaptionDialog'
import { RemovePhotoDialog } from './RemovePhotoDialog'

/** Ảnh bìa (địa điểm, món đồ) hay ảnh đại diện (người) của bản ghi, chọn từ các ảnh đã gắn. */
export type PhotoHighlight = {
  fileId: string | null
  label: 'Ảnh bìa' | 'Ảnh đại diện'
  setLabel: 'Đặt làm ảnh bìa' | 'Đặt làm ảnh đại diện'
  onSet: (fileId: string | null) => Promise<void>
}

/** Menu của một ảnh: sửa chú thích, đặt làm ảnh bìa hay đại diện, đổi chỗ, gỡ. */
export function PhotoActions({
  ws,
  owner,
  photo,
  index,
  count,
  highlight,
  onMove,
  onRemoved,
}: {
  ws: string
  owner: PhotoOwner
  photo: Photo
  index: number
  count: number
  highlight?: PhotoHighlight
  onMove: (delta: -1 | 1) => void
  onRemoved: () => Promise<void> | void
}) {
  const [dialog, setDialog] = useState<'caption' | 'remove' | null>(null)
  const close = (open: boolean) => !open && setDialog(null)
  const label = `Thao tác với ${photoAlt(photo, index)}`
  return (
    <>
      <DropdownMenu>
        <WithTooltip label={label}>
          <DropdownMenuTrigger
            render={<Button variant="secondary" size="icon-sm" aria-label={label} className="bg-background/85 shadow-sm" />}
          >
            <MoreHorizontalIcon aria-hidden="true" />
          </DropdownMenuTrigger>
        </WithTooltip>
        <DropdownMenuContent align="end" className="min-w-52">
          <DropdownMenuItem onClick={() => setDialog('caption')}>
            <PencilIcon aria-hidden="true" />
            Sửa chú thích
          </DropdownMenuItem>
          {highlight && highlight.fileId !== photo.fileId && (
            <DropdownMenuItem onClick={() => void highlight.onSet(photo.fileId)}>
              <ImageIcon aria-hidden="true" />
              {highlight.setLabel}
            </DropdownMenuItem>
          )}
          {index > 0 && (
            <DropdownMenuItem onClick={() => onMove(-1)}>
              <ArrowLeftIcon aria-hidden="true" />
              Đưa lên trước
            </DropdownMenuItem>
          )}
          {index < count - 1 && (
            <DropdownMenuItem onClick={() => onMove(1)}>
              <ArrowRightIcon aria-hidden="true" />
              Đưa ra sau
            </DropdownMenuItem>
          )}
          <DropdownMenuSeparator />
          <DropdownMenuItem variant="destructive" onClick={() => setDialog('remove')}>
            <Trash2Icon aria-hidden="true" />
            Gỡ ảnh
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
      {dialog === 'caption' && <CaptionDialog ws={ws} owner={owner} photo={photo} open onOpenChange={close} />}
      <RemovePhotoDialog
        ws={ws}
        owner={owner}
        photo={photo}
        open={dialog === 'remove'}
        onOpenChange={close}
        onRemoved={onRemoved}
      />
    </>
  )
}
