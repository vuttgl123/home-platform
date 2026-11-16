import { useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'

import { deletedToast } from '@/components/common/deletedToast'
import { FormError } from '@/components/common/FormError'
import { VersionConflictAlert } from '@/components/common/VersionConflictAlert'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'

import { forgetItem, undoDeleteItem, useDeleteItem } from '../queries'
import type { ItemDetail } from '../types'
import { useItemReload } from './useItemReload'

/** Xoá một món nhập nhầm; xong thì về tab Đồ đạc của địa điểm chứa nó. */
export function DeleteItemDialog({
  ws,
  item,
  open,
  onOpenChange,
}: {
  ws: string
  item: ItemDetail
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const remove = useDeleteItem(ws)
  const conflict = remove.error?.isVersionConflict === true
  const { reload, reloading } = useItemReload(ws, item, remove.reset)

  function confirm() {
    remove.mutate(item, {
      onSuccess: async () => {
        deletedToast({
          name: item.name,
          undo: async () => {
            await undoDeleteItem(queryClient, ws, item.id, item.version)
            await navigate(`/w/${ws}/items/${item.id}`)
          },
        })
        onOpenChange(false)
        // Rời trang của nó trước rồi mới bỏ khỏi cache, để trang đang mở không tải lại nó.
        await navigate(`/w/${ws}/places/${item.placeId}?tab=items`)
        forgetItem(queryClient, ws, item.id)
      },
    })
  }

  return (
    <AlertDialog
      open={open}
      onOpenChange={(next) => {
        if (!next) remove.reset()
        onOpenChange(next)
      }}
    >
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Xoá {item.name}?</AlertDialogTitle>
          <AlertDialogDescription>
            Chỉ dùng cho món nhập nhầm. Món hỏng hay bỏ đi thì dùng “Gỡ” để giữ lịch sử.
          </AlertDialogDescription>
        </AlertDialogHeader>
        {conflict && <VersionConflictAlert onReload={reload} isReloading={reloading} />}
        <FormError messages={remove.error && !conflict ? [remove.error.detail] : []} />
        <AlertDialogFooter>
          <AlertDialogCancel>Huỷ</AlertDialogCancel>
          <AlertDialogAction variant="destructive" disabled={remove.isPending || conflict} onClick={confirm}>
            Xoá
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
