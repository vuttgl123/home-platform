import { useState } from 'react'

import { ErrorState } from '@/components/common/ErrorState'
import { Skeleton } from '@/components/ui/skeleton'

import { usePhotos, useUploadPhoto } from '../queries'
import type { PhotoOwner } from '../types'
import { uploadErrorMessage } from '../uploadErrors'
import { checkPhotoFile } from '../urls'
import { PhotoGrid } from './PhotoGrid'
import { UploadButton } from './UploadButton'
import { UploadQueue, type QueuedUpload } from './UploadQueue'

/**
 * Tab "Ảnh" của một địa điểm, người hay món đồ: lưới ảnh thu nhỏ và nút tải nhiều ảnh. Các file được
 * kiểm tra kiểu và cỡ trước, rồi gửi lần lượt, mỗi lần một ảnh; ảnh lỗi không chặn ảnh khác.
 */
export function PhotosTab({ ws, owner }: { ws: string; owner: PhotoOwner }) {
  const photos = usePhotos(ws, owner)
  const upload = useUploadPhoto(ws, owner)
  const [queue, setQueue] = useState<QueuedUpload[]>([])
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
      {photos.isLoading ? (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5" aria-label="Đang tải">
          <Skeleton className="aspect-square" />
          <Skeleton className="aspect-square" />
        </div>
      ) : photos.error ? (
        <ErrorState error={photos.error} onRetry={() => void photos.refetch()} />
      ) : (photos.data ?? []).length === 0 ? (
        <p className="py-2 text-sm text-muted-foreground">Chưa có ảnh nào.</p>
      ) : (
        <PhotoGrid ws={ws} photos={photos.data ?? []} />
      )}
    </div>
  )
}
