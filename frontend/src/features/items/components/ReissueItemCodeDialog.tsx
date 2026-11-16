import { useState } from 'react'

import { ReissueCodeDialog } from '@/components/common/ReissueCodeDialog'
import { toast } from '@/components/ui/toast'

import { useFetchFreshItem, useReissueItemCode } from '../queries'
import type { ItemDetail } from '../types'

/** "Cấp mã mới" (hoặc "Cấp mã" khi chưa có) cho một món đồ chưa gỡ. */
export function ReissueItemCodeDialog({
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
  const reissue = useReissueItemCode(ws)
  const fetchFresh = useFetchFreshItem(ws)
  const [reloading, setReloading] = useState(false)

  async function reload() {
    setReloading(true)
    try {
      await fetchFresh(item.id)
      reissue.reset()
    } finally {
      setReloading(false)
    }
  }

  return (
    <ReissueCodeDialog
      open={open}
      onOpenChange={(next) => {
        if (!next) reissue.reset()
        onOpenChange(next)
      }}
      name={item.name}
      code={item.code ?? null}
      isPending={reissue.isPending}
      error={reissue.error}
      onConfirm={() =>
        reissue.mutate(item, {
          onSuccess: (updated) => {
            toast.add({ title: `Mã mới: ${updated.code}`, type: 'success' })
            onOpenChange(false)
          },
        })
      }
      onReload={() => void reload()}
      isReloading={reloading}
    />
  )
}
