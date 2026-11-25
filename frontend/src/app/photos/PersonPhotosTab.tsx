import { PhotosTab } from '@/features/media'
import { useSetPersonAvatar, type PersonDetail } from '@/features/people'

/** Tab "Ảnh" của một người: ghép ảnh (media) với ảnh đại diện (registry, qua lệnh sửa người). */
export function PersonPhotosTab({ ws, person }: { ws: string; person: PersonDetail }) {
  const setAvatar = useSetPersonAvatar(ws)
  return (
    <PhotosTab
      ws={ws}
      owner={{ type: 'PERSON', id: person.id }}
      highlight={{
        fileId: person.avatarFileId ?? null,
        label: 'Ảnh đại diện',
        setLabel: 'Đặt làm ảnh đại diện',
        onSet: async (fileId) => {
          await setAvatar(person, fileId)
        },
      }}
    />
  )
}
