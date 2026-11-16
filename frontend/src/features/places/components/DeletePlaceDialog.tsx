import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
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

import { forgetPlace, undoDeletePlace, useDeletePlace, useFetchFreshPlace } from '../queries'
import { placeUrl } from '../routes'
import type { PlaceDetail } from '../types'

/**
 * Hỏi lại trước khi xoá một địa điểm. Backend chỉ cho xoá khi bên trong không còn chỗ con, người
 * đang ở hay đồ đạc; nếu còn thì lý do hiện nguyên văn. Xoá xong thì về trang của cha.
 */
export function DeletePlaceDialog({
  ws,
  place,
  open,
  onOpenChange,
}: {
  ws: string
  place: PlaceDetail
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const remove = useDeletePlace(ws)
  const fetchFresh = useFetchFreshPlace(ws)
  const [reloading, setReloading] = useState(false)
  const conflict = remove.error?.isVersionConflict === true

  function confirm() {
    remove.mutate(place, {
      onSuccess: async () => {
        deletedToast({
          name: place.name,
          undo: async () => {
            await undoDeletePlace(queryClient, ws, place.id, place.version)
            await navigate(placeUrl(ws, place.id))
          },
        })
        onOpenChange(false)
        // Rời trang của nó trước rồi mới bỏ khỏi cache, để trang đang mở không tải lại nó.
        await navigate(place.parentId ? placeUrl(ws, place.parentId) : `/w/${ws}`)
        forgetPlace(queryClient, ws, place.id)
      },
    })
  }

  async function reload() {
    setReloading(true)
    try {
      await fetchFresh(place.id)
      remove.reset()
    } finally {
      setReloading(false)
    }
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
          <AlertDialogTitle>Xoá {place.name}?</AlertDialogTitle>
          <AlertDialogDescription>
            Chỉ xoá được khi bên trong không còn chỗ con, người đang ở hay đồ đạc.
          </AlertDialogDescription>
        </AlertDialogHeader>
        {conflict && <VersionConflictAlert onReload={() => void reload()} isReloading={reloading} />}
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
