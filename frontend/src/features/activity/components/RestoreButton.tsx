import { useQueryClient } from '@tanstack/react-query'

import type { ApiError } from '@/api'
import { Button } from '@/components/ui/button'
import { toast } from '@/components/ui/toast'
import { useRestoreItem } from '@/features/items'
import { useRestorePerson } from '@/features/people'
import { useRestorePlace } from '@/features/places'

import { activityKeys, trashKeys } from '../queries'
import type { EntityType } from '../types'

/**
 * Nút "Khôi phục" một bản ghi đã xoá, dùng ở trang Hoạt động và Thùng rác. `version` là version hiện
 * tại của bản ghi (lần xoá là lần ghi cuối). Xong, hoặc gặp "đã có người sửa", thì làm cũ cả hai trang.
 */
export function RestoreButton({
  ws,
  entityType,
  entityId,
  version,
  name,
}: {
  ws: string
  entityType: EntityType
  entityId: string
  version: number
  name: string
}) {
  const queryClient = useQueryClient()
  const place = useRestorePlace(ws)
  const person = useRestorePerson(ws)
  const item = useRestoreItem(ws)
  const restore = entityType === 'PLACE' ? place : entityType === 'PERSON' ? person : item
  const error = restore.error as ApiError | null

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: activityKeys.all(ws) })
    void queryClient.invalidateQueries({ queryKey: trashKeys.all(ws) })
  }

  function run() {
    restore.mutate(
      { id: entityId, version },
      {
        onSuccess: () => {
          toast.add({ title: `Đã khôi phục ${name}`, type: 'success' })
          refresh()
        },
        onError: (failure) => {
          // Đã có thay đổi sau lần xoá (thường là đã được khôi phục): danh sách đang cũ.
          if (failure.isVersionConflict) refresh()
        },
      },
    )
  }

  return (
    <>
      <Button variant="outline" size="xs" className="ml-auto" disabled={restore.isPending} onClick={run} aria-label={`Khôi phục ${name}`}>
        Khôi phục
      </Button>
      {error && (
        <p role="alert" className="basis-full text-sm text-destructive">
          {error.isVersionConflict ? 'Đã được khôi phục hoặc thay đổi sau lần xoá này.' : error.detail}
        </p>
      )}
    </>
  )
}
