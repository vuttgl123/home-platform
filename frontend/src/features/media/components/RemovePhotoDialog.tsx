import { FormError } from '@/components/common/FormError'
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

import { useRemovePhoto } from '../queries'
import type { Photo, PhotoOwner } from '../types'

/** Hỏi lại trước khi gỡ một ảnh; `onRemoved` chạy sau khi gỡ xong (bỏ ảnh bìa nếu cần). */
export function RemovePhotoDialog({
  ws,
  owner,
  photo,
  open,
  onOpenChange,
  onRemoved,
}: {
  ws: string
  owner: PhotoOwner
  photo: Photo
  open: boolean
  onOpenChange: (open: boolean) => void
  onRemoved: () => Promise<void> | void
}) {
  const remove = useRemovePhoto(ws, owner)
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
          <AlertDialogTitle>Gỡ ảnh này?</AlertDialogTitle>
          <AlertDialogDescription>Ảnh không còn hiện ở đây. Bản ghi vẫn giữ dấu vết ai đã gỡ và lúc nào.</AlertDialogDescription>
        </AlertDialogHeader>
        <FormError messages={remove.error ? [remove.error.detail] : []} />
        <AlertDialogFooter>
          <AlertDialogCancel>Huỷ</AlertDialogCancel>
          <AlertDialogAction
            variant="destructive"
            disabled={remove.isPending}
            onClick={() =>
              remove.mutate(photo.id, {
                onSuccess: async () => {
                  await onRemoved()
                  onOpenChange(false)
                },
              })
            }
          >
            Gỡ ảnh
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
