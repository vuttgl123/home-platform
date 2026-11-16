import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useNavigate } from 'react-router'

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

import { forgetPerson, useDeletePerson, useFetchFreshPerson } from '../queries'
import type { PersonDetail } from '../types'

/** Hỏi lại trước khi xoá hồ sơ; chỉ xoá được khi người đó không còn đang ở hay sắp đến ở đâu. */
export function DeletePersonDialog({
  ws,
  person,
  open,
  onOpenChange,
}: {
  ws: string
  person: PersonDetail
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const remove = useDeletePerson(ws)
  const fetchFresh = useFetchFreshPerson(ws)
  const [reloading, setReloading] = useState(false)
  const conflict = remove.error?.isVersionConflict === true

  function confirm() {
    remove.mutate(person, {
      onSuccess: async () => {
        toast.add({ title: `Đã xoá ${person.fullName}`, type: 'success' })
        onOpenChange(false)
        // Rời trang của họ trước rồi mới bỏ khỏi cache, để trang đang mở không tải lại họ.
        await navigate(`/w/${ws}/people`)
        forgetPerson(queryClient, ws, person.id)
      },
    })
  }

  async function reload() {
    setReloading(true)
    try {
      await fetchFresh(person.id)
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
          <AlertDialogTitle>Xoá {person.fullName}?</AlertDialogTitle>
          <AlertDialogDescription>
            Chỉ xoá được khi người này không còn đang ở hay sắp đến ở đâu. Những nơi đã rời vẫn còn trong lịch sử.
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
