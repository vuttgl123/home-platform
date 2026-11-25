import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import type { ApiError } from '@/api'

import { listPhotos, removePhoto, reorderPhotos, setCaption, uploadPhoto } from './api'
import type { Photo, PhotoOwner } from './types'

export const photoKeys = {
  all: (ws: string) => ['photos', ws] as const,
  of: (ws: string, owner: PhotoOwner) => ['photos', ws, owner.type, owner.id] as const,
}

export function usePhotos(ws: string, owner: PhotoOwner) {
  return useQuery<Photo[], ApiError>({ queryKey: photoKeys.of(ws, owner), queryFn: () => listPhotos(ws, owner) })
}

/** Mọi lệnh ghi ảnh làm mới danh sách ảnh của bản ghi đó. */
function useRefresh(ws: string, owner: PhotoOwner) {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: photoKeys.of(ws, owner) })
}

export function useUploadPhoto(ws: string, owner: PhotoOwner) {
  const refresh = useRefresh(ws, owner)
  return useMutation<Photo, ApiError, File>({ mutationFn: (file) => uploadPhoto(ws, owner, file), onSuccess: refresh })
}

export function useSetCaption(ws: string, owner: PhotoOwner) {
  const refresh = useRefresh(ws, owner)
  return useMutation<Photo, ApiError, { id: string; caption: string }>({
    mutationFn: ({ id, caption }) => setCaption(ws, id, caption),
    onSuccess: refresh,
  })
}

export function useReorderPhotos(ws: string, owner: PhotoOwner) {
  const refresh = useRefresh(ws, owner)
  return useMutation<Photo[], ApiError, string[]>({
    mutationFn: (ids) => reorderPhotos(ws, owner, ids),
    // Thành công hay 409 (danh sách cũ) đều tải lại.
    onSettled: refresh,
  })
}

export function useRemovePhoto(ws: string, owner: PhotoOwner) {
  const refresh = useRefresh(ws, owner)
  return useMutation<void, ApiError, string>({ mutationFn: (id) => removePhoto(ws, id), onSuccess: refresh })
}
