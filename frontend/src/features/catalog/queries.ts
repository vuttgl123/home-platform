import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import type { ApiError } from '@/api'

import { listFieldDefs, listItemCategories, listPlaceTypes, setupWorkspace } from './api'
import type { FieldDef, FieldTarget, ItemCategory, PlaceType } from './types'

export const catalogKeys = {
  all: (ws: string) => ['catalog', ws] as const,
  placeTypes: (ws: string) => ['catalog', ws, 'place-types'] as const,
  itemCategories: (ws: string) => ['catalog', ws, 'item-categories'] as const,
  fieldDefs: (ws: string, target: FieldTarget, typeId: string) => ['catalog', ws, 'field-defs', target, typeId] as const,
}

export function usePlaceTypes(ws: string) {
  return useQuery<PlaceType[], ApiError>({ queryKey: catalogKeys.placeTypes(ws), queryFn: () => listPlaceTypes(ws) })
}

/** Nhóm đồ, theo thứ tự hiển thị (thứ tự rồi tên). */
export function useItemCategories(ws: string) {
  return useQuery<ItemCategory[], ApiError>({
    queryKey: catalogKeys.itemCategories(ws),
    queryFn: async () =>
      (await listItemCategories(ws)).sort((a, b) => a.sortOrder - b.sortOrder || a.name.localeCompare(b.name)),
  })
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
