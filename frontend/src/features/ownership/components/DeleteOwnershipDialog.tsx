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
import { toast } from '@/components/ui/toast'

import { useDeleteOwnership } from '../queries'
import type { Ownership } from '../types'
import { STALE_ROW } from './ownershipForm'

/** Xoá một dòng sở hữu nhập nhầm. Bán hay hết sở hữu thật thì dùng "Thôi sở hữu" hoặc "Chuyển nhượng". */
export function DeleteOwnershipDialog({
  ws,
  ownership,
  open,
  onOpenChange,
}: {
  ws: string
  ownership: Ownership
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const remove = useDeleteOwnership(ws)
  const error = remove.error ? (remove.error.isVersionConflict ? STALE_ROW : remove.error.detail) : null
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
          <AlertDialogTitle>
            Xoá dòng sở hữu của {ownership.personFullName} tại {ownership.placeName}?
          </AlertDialogTitle>
          <AlertDialogDescription>
            Chỉ dùng cho dòng nhập nhầm. Hết sở hữu thật thì dùng “Thôi sở hữu” hoặc “Chuyển nhượng” để giữ lại lịch sử.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <FormError messages={error ? [error] : []} />
        <AlertDialogFooter>
          <AlertDialogCancel>Huỷ</AlertDialogCancel>
          <AlertDialogAction
            variant="destructive"
            disabled={remove.isPending}
            onClick={() =>
              remove.mutate(ownership, {
                onSuccess: () => {
                  toast.add({ title: 'Đã xoá dòng sở hữu', type: 'success' })
                  onOpenChange(false)
                },
              })
            }
          >
            Xoá
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
