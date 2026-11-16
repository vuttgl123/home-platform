import { useQueryClient } from '@tanstack/react-query'

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

import { refreshOccupancies, useDeleteOccupancy } from '../queries'
import type { Occupancy } from '../types'
import { STALE_ROW } from './occupancyForm'

/** Xoá một dòng cư trú nhập nhầm. Chuyển đi bình thường thì dùng "Chuyển đi" để giữ lịch sử. */
export function DeleteOccupancyDialog({
  ws,
  occupancy,
  open,
  onOpenChange,
}: {
  ws: string
  occupancy: Occupancy
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const queryClient = useQueryClient()
  const remove = useDeleteOccupancy(ws)
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
            Xoá dòng cư trú của {occupancy.personFullName} tại {occupancy.placeName}?
          </AlertDialogTitle>
          <AlertDialogDescription>
            Chỉ dùng cho dòng nhập nhầm. Người này chuyển đi thật thì dùng “Chuyển đi” để giữ lại lịch sử.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <FormError messages={error ? [error] : []} />
        <AlertDialogFooter>
          <AlertDialogCancel>Huỷ</AlertDialogCancel>
          <AlertDialogAction
            variant="destructive"
            disabled={remove.isPending}
            onClick={() =>
              remove.mutate(occupancy, {
                onSuccess: () => {
                  toast.add({ title: 'Đã xoá dòng cư trú', type: 'success' })
                  onOpenChange(false)
                },
                onError: (failure) => {
                  if (failure.isVersionConflict) refreshOccupancies(queryClient, ws, occupancy.personId)
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
