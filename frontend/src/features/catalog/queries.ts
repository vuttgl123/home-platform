import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'

import type { ApiError } from '@/api'

import { backfillCodes, getCodeStatus, listCodeRules, listFieldDefs, listItemCategories, listPlaceTypes, setupWorkspace } from './api'
import type { CodeRule, CodeStatus, FieldDef, FieldTarget, ItemCategory, PlaceType } from './types'

export const catalogKeys = {
  all: (ws: string) => ['catalog', ws] as const,
  placeTypes: (ws: string) => ['catalog', ws, 'place-types'] as const,
  itemCategories: (ws: string) => ['catalog', ws, 'item-categories'] as const,
  fieldDefs: (ws: string, target: FieldTarget, typeId: string) => ['catalog', ws, 'field-defs', target, typeId] as const,
  codeRules: (ws: string) => ['catalog', ws, 'code-rules'] as const,
  codeStatus: (ws: string) => ['catalog', ws, 'code-status'] as const,
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

/** Mọi mục tuỳ biến của một đích (mọi phạm vi, mọi trạng thái), cho trang danh mục. */
export function useAllFieldDefs(ws: string, target: FieldTarget) {
  return useQuery<FieldDef[], ApiError>({
    queryKey: catalogKeys.fieldDefs(ws, target, 'all'),
    queryFn: async () => (await listFieldDefs(ws, target)).sort((a, b) => a.sortOrder - b.sortOrder || a.label.localeCompare(b.label)),
  })
}

/**
 * Một lệnh ghi danh mục bất kỳ. Danh mục nhỏ và ít đổi, nên ghi xong thì làm cũ cả danh mục của
 * không gian: loại, nhóm, mục tuỳ biến đều được form khác đọc.
 */
export function useCatalogWrite<Variables>(ws: string, write: (variables: Variables) => Promise<unknown>) {
  const queryClient = useQueryClient()
  return useMutation<unknown, ApiError, Variables>({
    mutationFn: write,
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: catalogKeys.all(ws) }),
  })
}

export function useCodeRules(ws: string) {
  return useQuery<CodeRule[], ApiError>({ queryKey: catalogKeys.codeRules(ws), queryFn: () => listCodeRules(ws) })
}

export function useCodeStatus(ws: string) {
  return useQuery<CodeStatus, ApiError>({ queryKey: catalogKeys.codeStatus(ws), queryFn: () => getCodeStatus(ws) })
}

/**
 * Cấp mã cho mọi bản ghi chưa có mã: gọi từng lô tới khi không còn, hoặc một lô không cấp được gì
 * (để không lặp mãi). Xong thì làm cũ mọi cache, vì danh sách và chi tiết của địa điểm, đồ đạc đều mang mã.
 */
export function useBackfillCodes(ws: string) {
  const queryClient = useQueryClient()
  const [state, setState] = useState<{ isRunning: boolean; assigned: number; total: number; error: ApiError | null }>({
    isRunning: false,
    assigned: 0,
    total: 0,
    error: null,
  })

  async function run(total: number): Promise<number> {
    setState({ isRunning: true, assigned: 0, total, error: null })
    let assigned = 0
    try {
      for (;;) {
        const round = await backfillCodes(ws)
        const now = round.placesAssigned + round.itemsAssigned
        assigned += now
        setState((current) => ({ ...current, assigned }))
        if (now === 0 || round.placesRemaining + round.itemsRemaining === 0) break
      }
      return assigned
    } catch (error) {
      setState((current) => ({ ...current, error: error as ApiError }))
      throw error
    } finally {
      setState((current) => ({ ...current, isRunning: false }))
      void queryClient.invalidateQueries()
    }
  }

  return { ...state, run }
}
