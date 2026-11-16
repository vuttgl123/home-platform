import { FormError } from '@/components/common/FormError'
import { Button } from '@/components/ui/button'
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
import { toast } from '@/components/ui/toast'

import { useCatalogWrite } from '../queries'

/**
 * Xoá hẳn một loại địa điểm hay nhóm đồ. Chỉ được khi chưa bản ghi nào dùng nó; nếu đang dùng,
 * backend trả lý do và hộp thoại mời lưu trữ thay vì xoá.
 */
export function DeleteCatalogDialog({
  ws,
  name,
  remove,
  archive,
  onClose,
}: {
  ws: string
  name: string
  remove: () => Promise<unknown>
  archive: () => Promise<unknown>
  onClose: () => void
}) {
  const deletion = useCatalogWrite(ws, remove)
  const archiving = useCatalogWrite(ws, archive)
  const inUse = deletion.error?.status === 409

  return (
    <AlertDialog open onOpenChange={(open) => !open && onClose()}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Xoá {name}?</AlertDialogTitle>
          <AlertDialogDescription>
            Chỉ xoá được khi chưa bản ghi nào dùng nó. Mục tuỳ biến riêng của nó bị xoá theo.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <FormError messages={deletion.error ? [deletion.error.detail] : archiving.error ? [archiving.error.detail] : []} />
        <AlertDialogFooter>
          <AlertDialogCancel>Huỷ</AlertDialogCancel>
          {inUse ? (
            <Button
              disabled={archiving.isPending}
              onClick={() =>
                archiving.mutate(undefined, {
                  onSuccess: () => {
                    toast.add({ title: `Đã lưu trữ ${name}`, type: 'success' })
                    onClose()
                  },
                })
              }
            >
              Lưu trữ thay vì xoá
            </Button>
          ) : (
            <AlertDialogAction
              variant="destructive"
              disabled={deletion.isPending}
              onClick={() =>
                deletion.mutate(undefined, {
                  onSuccess: () => {
                    toast.add({ title: `Đã xoá ${name}`, type: 'success' })
                    onClose()
                  },
                })
              }
            >
              Xoá
            </AlertDialogAction>
          )}
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
