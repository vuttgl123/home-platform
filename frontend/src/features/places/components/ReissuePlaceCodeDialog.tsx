import { useState } from 'react'

import { ReissueCodeDialog } from '@/components/common/ReissueCodeDialog'
import { toast } from '@/components/ui/toast'

import { useFetchFreshPlace, useReissuePlaceCode } from '../queries'
import type { PlaceDetail } from '../types'

/** "Cấp mã mới" (hoặc "Cấp mã" khi chưa có) cho một địa điểm. */
export function ReissuePlaceCodeDialog({
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
  const reissue = useReissuePlaceCode(ws)
  const fetchFresh = useFetchFreshPlace(ws)
  const [reloading, setReloading] = useState(false)

  async function reload() {
    setReloading(true)
    try {
      await fetchFresh(place.id)
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
      name={place.name}
      code={place.code ?? null}
      isPending={reissue.isPending}
      error={reissue.error}
      onConfirm={() =>
        reissue.mutate(place, {
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
