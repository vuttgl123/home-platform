import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import type { ApiError } from '@/api'

import { listPlaceTypes, setupWorkspace } from './api'
import type { PlaceType } from './types'

export const catalogKeys = {
  all: (ws: string) => ['catalog', ws] as const,
  placeTypes: (ws: string) => ['catalog', ws, 'place-types'] as const,
}

export function usePlaceTypes(ws: string) {
  return useQuery<PlaceType[], ApiError>({ queryKey: catalogKeys.placeTypes(ws), queryFn: () => listPlaceTypes(ws) })
}

/** Chạy lại bước tạo danh mục mặc định, rồi tải lại danh mục. */
export function useSetupWorkspace(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<void, ApiError, void>({
    mutationFn: () => setupWorkspace(ws),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: catalogKeys.all(ws) }),
  })
}
