import { useState } from 'react'

import { ErrorState } from '@/components/common/ErrorState'
import { ToneBadge } from '@/components/common/ToneBadge'
import { Skeleton } from '@/components/ui/skeleton'

import { usePhotos, useReorderPhotos, useUploadPhoto } from '../queries'
import type { PhotoOwner } from '../types'
import { uploadErrorMessage } from '../uploadErrors'
import { checkPhotoFile } from '../urls'
import { PhotoActions, type PhotoHighlight } from './PhotoActions'
import { PhotoGrid } from './PhotoGrid'
import { PhotoViewer } from './PhotoViewer'
import { UploadButton } from './UploadButton'
import { UploadQueue, type QueuedUpload } from './UploadQueue'

/**
 * Tab "Ảnh" của một địa điểm, người hay món đồ: lưới ảnh thu nhỏ và nút tải nhiều ảnh. Các file được
 * kiểm tra kiểu và cỡ trước, rồi gửi lần lượt, mỗi lần một ảnh; ảnh lỗi không chặn ảnh khác. Bấm
 * một ảnh để xem lớn; menu của ảnh sửa chú thích, đổi chỗ, gỡ, và (khi có `highlight`) đặt làm ảnh
 * bìa hay ảnh đại diện. Gỡ đúng ảnh đang là bìa thì bỏ luôn ảnh bìa.
 */
export function PhotosTab({ ws, owner, highlight }: { ws: string; owner: PhotoOwner; highlight?: PhotoHighlight }) {
  const photos = usePhotos(ws, owner)
  const upload = useUploadPhoto(ws, owner)
  const reorder = useReorderPhotos(ws, owner)
  const [queue, setQueue] = useState<QueuedUpload[]>([])
  const [viewing, setViewing] = useState<number | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const list = photos.data ?? []

  function move(index: number, delta: -1 | 1) {
    const ids = list.map((photo) => photo.id)
    ;[ids[index], ids[index + delta]] = [ids[index + delta], ids[index]]
    setNotice(null)
    reorder.mutate(ids, {
      onError: (error) => setNotice(error.status === 409 ? 'Danh sách ảnh vừa đổi, đã tải lại' : error.detail),
    })
  }

  // Đặt ảnh bìa có thể hỏng (ai đó vừa sửa bản ghi): báo ở đây thay vì để lỗi trôi mất.
  const guarded: PhotoHighlight | undefined = highlight && {
    ...highlight,
    onSet: async (fileId) => {
      setNotice(null)
      try {
        await highlight.onSet(fileId)
      } catch (error) {
        setNotice(`Không lưu được ${highlight.label.toLowerCase()}: ${error instanceof Error ? error.message : String(error)}`)
      }
    },
  }

  async function removed(fileId: string, id: string) {
    // Gỡ ảnh đang là bìa thì bỏ ảnh bìa, trừ khi một ảnh khác còn lại cùng nội dung (cùng file).
    if (guarded && guarded.fileId === fileId && !list.some((other) => other.id !== id && other.fileId === fileId)) {
      await guarded.onSet(null)
    }
  }
  const busy = queue.some((item) => item.state === 'waiting' || item.state === 'uploading')

  async function send(files: File[]) {
    const batch = files.map((file, index) => ({ file, key: `${Date.now()}-${index}`, problem: checkPhotoFile(file) }))
    const set = (key: string, change: Partial<QueuedUpload>) =>
      setQueue((items) => items.map((item) => (item.key === key ? { ...item, ...change } : item)))
    setQueue(
      batch.map(({ file, key, problem }) =>
        problem ? { key, name: file.name, state: 'error', message: problem } : { key, name: file.name, state: 'waiting' },
      ),
    )
    for (const { file, key, problem } of batch) {
      if (problem) continue
      set(key, { state: 'uploading' })
      try {
        await upload.mutateAsync(file)
        set(key, { state: 'done' })
      } catch (error) {
        set(key, { state: 'error', message: uploadErrorMessage(error) })
      }
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex justify-end">
        <UploadButton onFiles={(files) => void send(files)} disabled={busy} />
      </div>
      <UploadQueue items={queue} />
      {notice && (
        <p role="status" className="text-sm text-warning-foreground">
          {notice}
        </p>
      )}
      {photos.isLoading ? (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5" aria-label="Đang tải">
          <Skeleton className="aspect-square" />
          <Skeleton className="aspect-square" />
        </div>
      ) : photos.error ? (
        <ErrorState error={photos.error} onRetry={() => void photos.refetch()} />
      ) : list.length === 0 ? (
        <p className="py-2 text-sm text-muted-foreground">Chưa có ảnh nào.</p>
      ) : (
        <PhotoGrid
          ws={ws}
          photos={list}
          onOpen={setViewing}
          badge={(photo) =>
            highlight && highlight.fileId === photo.fileId ? (
              // Nền đặc: nhãn nằm trên ảnh, nền trong mờ thì không đọc được trên ảnh sáng.
              <ToneBadge tone="primary" className="bg-primary text-primary-foreground shadow-sm">
                {highlight.label}
              </ToneBadge>
            ) : null
          }
          actions={(photo, index) => (
            <PhotoActions
              ws={ws}
              owner={owner}
              photo={photo}
              index={index}
              count={list.length}
              highlight={guarded}
              onMove={(delta) => move(index, delta)}
              onRemoved={() => removed(photo.fileId, photo.id)}
            />
          )}
        />
      )}
      <PhotoViewer ws={ws} photos={list} index={viewing} onIndexChange={setViewing} />
    </div>
  )
}
