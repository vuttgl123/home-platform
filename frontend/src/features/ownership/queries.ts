import { useMutation, useQueryClient, type QueryClient } from '@tanstack/react-query'

import { useCursorList, type ApiError, type CursorList } from '@/api'

import {
  createOwnership,
  deleteOwnership,
  endOwnership,
  listOwnershipsAtPlace,
  listOwnershipsInSubtree,
  listOwnershipsOfPerson,
  transferOwnership,
  updateOwnership,
} from './api'
import type { Ownership, OwnershipBody, OwnershipStatus, OwnershipTransferBody, OwnershipUpdateBody } from './types'

export const ownershipKeys = {
  all: (ws: string) => ['ownerships', ws] as const,
  atPlace: (ws: string, placeId: string, status: OwnershipStatus) => ['ownerships', ws, 'place', placeId, status] as const,
  inSubtree: (ws: string, placeId: string) => ['ownerships', ws, 'subtree', placeId] as const,
  ofPerson: (ws: string, personId: string) => ['ownerships', ws, 'person', personId] as const,
}

export function useOwnershipsAtPlace(
  ws: string,
  placeId: string,
  status: OwnershipStatus,
  options: { enabled?: boolean } = {},
): CursorList<Ownership> {
  return useCursorList({
    queryKey: ownershipKeys.atPlace(ws, placeId, status),
    fetchPage: (cursor) => listOwnershipsAtPlace(ws, placeId, status, cursor),
    enabled: options.enabled,
  })
}

export function useOwnershipsInSubtree(ws: string, placeId: string, enabled: boolean): CursorList<Ownership> {
  return useCursorList({
    queryKey: ownershipKeys.inSubtree(ws, placeId),
    fetchPage: (cursor) => listOwnershipsInSubtree(ws, placeId, cursor),
    enabled,
  })
}

export function useOwnershipsOfPerson(ws: string, personId: string): CursorList<Ownership> {
  return useCursorList({
    queryKey: ownershipKeys.ofPerson(ws, personId),
    fetchPage: (cursor) => listOwnershipsOfPerson(ws, personId, cursor),
  })
}

/**
 * Sau một lệnh ghi sở hữu: mọi danh sách sở hữu (của địa điểm, của cả toà, của người) đều có thể đã
 * đổi; một lần chuyển nhượng chạm hai người.
 */
export function refreshOwnerships(queryClient: QueryClient, ws: string): void {
  void queryClient.invalidateQueries({ queryKey: ownershipKeys.all(ws) })
}

export function useCreateOwnership(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<Ownership, ApiError, OwnershipBody>({
    mutationFn: (body) => createOwnership(ws, body),
    onSuccess: () => refreshOwnerships(queryClient, ws),
  })
}

export function useUpdateOwnership(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<Ownership, ApiError, { id: string; body: OwnershipUpdateBody }>({
    mutationFn: ({ id, body }) => updateOwnership(ws, id, body),
    onSuccess: () => refreshOwnerships(queryClient, ws),
  })
}

export function useEndOwnership(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<Ownership, ApiError, { ownership: Ownership; endDate: string }>({
    mutationFn: ({ ownership, endDate }) => endOwnership(ws, ownership.id, ownership.version, endDate),
    onSuccess: () => refreshOwnerships(queryClient, ws),
  })
}

export function useTransferOwnership(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<
    { ended: Ownership; started: Ownership },
    ApiError,
    { ownership: Ownership; body: Omit<OwnershipTransferBody, 'version'> }
  >({
    mutationFn: ({ ownership, body }) => transferOwnership(ws, ownership.id, { ...body, version: ownership.version }),
    onSuccess: () => refreshOwnerships(queryClient, ws),
  })
}

export function useDeleteOwnership(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<void, ApiError, Ownership>({
    mutationFn: (ownership) => deleteOwnership(ws, ownership.id, ownership.version),
    onSuccess: () => refreshOwnerships(queryClient, ws),
  })
}
