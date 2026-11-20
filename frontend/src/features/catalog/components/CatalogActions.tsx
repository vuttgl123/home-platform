import { ArchiveIcon, ArchiveRestoreIcon, MoreHorizontalIcon, PencilIcon, Trash2Icon } from 'lucide-react'

import { WithTooltip } from '@/components/common/WithTooltip'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

/** Menu của một dòng danh mục: sửa, lưu trữ hoặc bỏ lưu trữ, và xoá (nếu loại danh mục cho xoá). */
export function CatalogActions({
  label,
  archived,
  onEdit,
  onToggleArchive,
  onDelete,
}: {
  label: string
  archived: boolean
  onEdit: () => void
  onToggleArchive: () => void
  onDelete?: () => void
}) {
  return (
    <DropdownMenu>
      <WithTooltip label={`Thao tác với ${label}`}>
        <DropdownMenuTrigger render={<Button variant="ghost" size="icon-sm" aria-label={`Thao tác với ${label}`} />}>
          <MoreHorizontalIcon aria-hidden="true" />
        </DropdownMenuTrigger>
      </WithTooltip>
      <DropdownMenuContent align="end" className="min-w-40">
        <DropdownMenuItem onClick={onEdit}>
          <PencilIcon aria-hidden="true" />
          Sửa
        </DropdownMenuItem>
        <DropdownMenuItem onClick={onToggleArchive}>
          {archived ? <ArchiveRestoreIcon aria-hidden="true" /> : <ArchiveIcon aria-hidden="true" />}
          {archived ? 'Bỏ lưu trữ' : 'Lưu trữ'}
        </DropdownMenuItem>
        {onDelete && (
          <>
            <DropdownMenuSeparator />
            <DropdownMenuItem variant="destructive" onClick={onDelete}>
              <Trash2Icon aria-hidden="true" />
              Xoá
            </DropdownMenuItem>
          </>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
