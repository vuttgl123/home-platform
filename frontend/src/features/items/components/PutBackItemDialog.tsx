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
import { toast } from '@/components/ui/toast'

import { usePutBackItem } from '../queries'
import type { ItemDetail } from '../types'
import { useItemReload } from './useItemReload'

/** Đưa một món đã gỡ trở lại danh sách đang có. */
export function PutBackItemDialog({
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
  const putBack = usePutBackItem(ws)
  const conflict = putBack.error?.isVersionConflict === true
  const { reload, reloading } = useItemReload(ws, item, putBack.reset)
  return (
    <AlertDialog
      open={open}
      onOpenChange={(next) => {
        if (!next) putBack.reset()
        onOpenChange(next)
      }}
    >
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Đưa {item.name} lại?</AlertDialogTitle>
          <AlertDialogDescription>Món trở lại danh sách đang có của {item.placeName}.</AlertDialogDescription>
        </AlertDialogHeader>
        {conflict && <VersionConflictAlert onReload={reload} isReloading={reloading} />}
        <FormError messages={putBack.error && !conflict ? [putBack.error.detail] : []} />
        <AlertDialogFooter>
          <AlertDialogCancel>Huỷ</AlertDialogCancel>
          <AlertDialogAction
            disabled={putBack.isPending || conflict}
            onClick={() =>
              putBack.mutate(item, {
                onSuccess: () => {
                  toast.add({ title: `Đã đưa ${item.name} lại`, type: 'success' })
                  onOpenChange(false)
                },
              })
            }
          >
            Đưa lại
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
