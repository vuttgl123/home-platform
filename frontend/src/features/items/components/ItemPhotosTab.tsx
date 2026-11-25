import { PhotosTab } from '@/features/media'

import { useSetItemCover } from '../queries'
import type { ItemDetail } from '../types'

/** Tab "Ảnh" của một món đồ. Món đã gỡ chỉ đọc được, nên không đặt ảnh bìa được nữa. */
export function ItemPhotosTab({ ws, item }: { ws: string; item: ItemDetail }) {
  const setCover = useSetItemCover(ws)
  return (
    <PhotosTab
      ws={ws}
      owner={{ type: 'ITEM', id: item.id }}
      highlight={
        item.removedAt
          ? undefined
          : {
              fileId: item.coverFileId ?? null,
              label: 'Ảnh bìa',
              setLabel: 'Đặt làm ảnh bìa',
              onSet: async (fileId) => {
                await setCover(item, fileId)
              },
            }
      }
    />
  )
}
