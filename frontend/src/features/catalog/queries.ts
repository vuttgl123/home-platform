import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import type { ApiError } from '@/api'

import { listFieldDefs, listPlaceTypes, setupWorkspace } from './api'
import type { FieldDef, FieldTarget, PlaceType } from './types'

export const catalogKeys = {
  all: (ws: string) => ['catalog', ws] as const,
  placeTypes: (ws: string) => ['catalog', ws, 'place-types'] as const,
  fieldDefs: (ws: string, target: FieldTarget, typeId: string) => ['catalog', ws, 'field-defs', target, typeId] as const,
}

export function usePlaceTypes(ws: string) {
  return useQuery<PlaceType[], ApiError>({ queryKey: catalogKeys.placeTypes(ws), queryFn: () => listPlaceTypes(ws) })
}

/** Mục tuỳ biến của một loại; chưa có `typeId` thì không gọi. */
export function useFieldDefs(ws: string, target: FieldTarget, typeId: string | undefined) {
  return useQuery<FieldDef[], ApiError>({
    queryKey: catalogKeys.fieldDefs(ws, target, typeId ?? ''),
    queryFn: () => listFieldDefs(ws, target, typeId as string),
    enabled: typeId != null && typeId !== '',
  })
}

/** Chạy lại bước tạo danh mục mặc định, rồi tải lại danh mục. */
export function useSetupWorkspace(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<void, ApiError, void>({
    mutationFn: () => setupWorkspace(ws),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: catalogKeys.all(ws) }),
  })
}
