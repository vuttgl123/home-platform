import { PhotosTab } from '@/features/media'
import { useSetPlaceCover, type PlaceDetail } from '@/features/places'

/** Tab "Ảnh" của một địa điểm: ghép ảnh (media) với ảnh bìa (registry, qua lệnh sửa địa điểm). */
export function PlacePhotosTab({ ws, place }: { ws: string; place: PlaceDetail }) {
  const setCover = useSetPlaceCover(ws)
  return (
    <PhotosTab
      ws={ws}
      owner={{ type: 'PLACE', id: place.id }}
      highlight={{
        fileId: place.coverFileId ?? null,
        label: 'Ảnh bìa',
        setLabel: 'Đặt làm ảnh bìa',
        onSet: async (fileId) => {
          await setCover(place, fileId)
        },
      }}
    />
  )
}
