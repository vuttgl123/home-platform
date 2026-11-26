import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { useCallback } from 'react'

import { useCursorList, type ApiError, type CursorList } from '@/api'
import { useDebouncedValue } from '@/hooks/use-debounced-value'
import { isId } from '@/lib/ids'

import {
  checkCapacity,
  createOccupancy,
  createPerson,
  deleteOccupancy,
  deletePerson,
  endOccupancy,
  getOccupancyTimeline,
  getPerson,
  listOccupanciesAtPlace,
  listOccupanciesInSubtree,
  listOccupanciesOfPerson,
  listPeople,
  quickFindPeople,
  restorePerson,
  searchPeople,
  updateOccupancy,
  updatePerson,
} from './api'
import type {
  CapacityRange,
  Occupancy,
  OccupancyBody,
  OccupancyStatus,
  OccupancyUpdateBody,
  PersonBody,
  PersonDetail,
  PersonSearchField,
  PersonSummary,
  PersonUpdateBody,
} from './types'

export const personKeys = {
  all: (ws: string) => ['people', ws] as const,
  lists: (ws: string) => ['people', ws, 'list'] as const,
  list: (ws: string) => ['people', ws, 'list', 'all'] as const,
  search: (ws: string, by: PersonSearchField, term: string) => ['people', ws, 'list', 'search', by, term] as const,
  details: (ws: string) => ['people', ws, 'detail'] as const,
  detail: (ws: string, id: string) => ['people', ws, 'detail', id] as const,
}

export const occupancyKeys = {
  all: (ws: string) => ['occupancies', ws] as const,
  atPlace: (ws: string, placeId: string, status: OccupancyStatus) => ['occupancies', ws, 'place', placeId, status] as const,
  inSubtree: (ws: string, placeId: string) => ['occupancies', ws, 'subtree', placeId] as const,
  ofPerson: (ws: string, personId: string) => ['occupancies', ws, 'person', personId] as const,
  timeline: (ws: string, kind: 'place' | 'person', id: string) => ['occupancies', ws, 'timeline', kind, id] as const,
}

// Người

export function usePeople(ws: string): CursorList<PersonSummary> {
  return useCursorList({ queryKey: personKeys.list(ws), fetchPage: (cursor) => listPeople(ws, cursor) })
}

/** Tìm người; `term` rỗng thì không gọi. */
export function usePeopleSearch(ws: string, by: PersonSearchField, term: string): CursorList<PersonSummary> {
  return useCursorList({
    queryKey: personKeys.search(ws, by, term),
    fetchPage: (cursor) => searchPeople(ws, by, term, cursor),
    enabled: term !== '',
  })
}

/** Một người đầy đủ; `id` trống hoặc sai dạng thì không gọi. */
export function usePerson(ws: string, id: string | undefined) {
  return useQuery<PersonDetail, ApiError>({
    queryKey: personKeys.detail(ws, id ?? ''),
    queryFn: () => getPerson(ws, id as string),
    enabled: isId(id),
  })
}

/** Lấy bản mới nhất của một người (bỏ qua cache) và cập nhật cache; dùng sau 409 "đã có người sửa". */
export function useFetchFreshPerson(ws: string): (id: string) => Promise<PersonDetail> {
  const queryClient = useQueryClient()
  return useCallback(
    (id: string) =>
      queryClient.fetchQuery({ queryKey: personKeys.detail(ws, id), queryFn: () => getPerson(ws, id), staleTime: 0 }),
    [queryClient, ws],
  )
}

export function useCreatePerson(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<PersonDetail, ApiError, PersonBody>({
    mutationFn: (body) => createPerson(ws, body),
    onSuccess: (created) => {
      queryClient.setQueryData(personKeys.detail(ws, created.id), created)
      void queryClient.invalidateQueries({ queryKey: personKeys.lists(ws) })
    },
  })
}

/** Sau khi hồ sơ một người đổi (sửa, khôi phục version): chi tiết, danh sách người, các dòng cư trú mang tên họ. */
export function refreshAfterPersonWrite(queryClient: QueryClient, ws: string, person: PersonDetail): void {
  queryClient.setQueryData(personKeys.detail(ws, person.id), person)
  void queryClient.invalidateQueries({ queryKey: personKeys.lists(ws) })
  void queryClient.invalidateQueries({ queryKey: occupancyKeys.all(ws) })
}

export function useUpdatePerson(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<PersonDetail, ApiError, { id: string; body: PersonUpdateBody }>({
    mutationFn: ({ id, body }) => updatePerson(ws, id, body),
    onSuccess: (updated) => refreshAfterPersonWrite(queryClient, ws, updated),
  })
}

/**
 * Xoá một người. Chi tiết của họ vẫn nằm trong cache: nơi gọi rời trang rồi mới bỏ
 * (`forgetPerson`), để trang đang mở không tải lại một người đã xoá.
 */
export function useDeletePerson(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<void, ApiError, PersonDetail>({
    mutationFn: (person) => deletePerson(ws, person.id, person.version),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: personKeys.lists(ws) })
      void queryClient.invalidateQueries({ queryKey: occupancyKeys.all(ws) })
    },
  })
}

export function forgetPerson(queryClient: QueryClient, ws: string, id: string): void {
  queryClient.removeQueries({ queryKey: personKeys.detail(ws, id), exact: true })
}

// Cư trú

export function useOccupanciesAtPlace(
  ws: string,
  placeId: string,
  status: OccupancyStatus,
  options: { enabled?: boolean } = {},
): CursorList<Occupancy> {
  return useCursorList({
    queryKey: occupancyKeys.atPlace(ws, placeId, status),
    fetchPage: (cursor) => listOccupanciesAtPlace(ws, placeId, status, cursor),
    enabled: options.enabled,
  })
}

export function useOccupanciesInSubtree(ws: string, placeId: string, enabled: boolean): CursorList<Occupancy> {
  return useCursorList({
    queryKey: occupancyKeys.inSubtree(ws, placeId),
    fetchPage: (cursor) => listOccupanciesInSubtree(ws, placeId, cursor),
    enabled,
  })
}

export function useOccupanciesOfPerson(ws: string, personId: string): CursorList<Occupancy> {
  return useCursorList({
    queryKey: occupancyKeys.ofPerson(ws, personId),
    fetchPage: (cursor) => listOccupanciesOfPerson(ws, personId, cursor),
  })
}

/**
 * Sau một lệnh ghi cư trú: mọi danh sách cư trú (của địa điểm, của cả toà, của người) và chi tiết
 * của người đó (`currentOccupancies`) đều có thể đã đổi.
 */
export function refreshOccupancies(queryClient: QueryClient, ws: string, personId: string): void {
  void queryClient.invalidateQueries({ queryKey: occupancyKeys.all(ws) })
  void queryClient.invalidateQueries({ queryKey: personKeys.detail(ws, personId) })
  // Trang Chỗ trống (features/vacancies) đếm người đang ở; khoá viết thẳng để không import vòng.
  void queryClient.invalidateQueries({ queryKey: ['vacancies', ws] })
  // Bảng địa điểm (features/places) có cột Đang ở; khoá viết thẳng như trên.
  void queryClient.invalidateQueries({ queryKey: ['places', ws, 'filtered'] })
}

export function useCreateOccupancy(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<Occupancy, ApiError, OccupancyBody>({
    mutationFn: (body) => createOccupancy(ws, body),
    onSuccess: (created) => refreshOccupancies(queryClient, ws, created.personId),
  })
}

export function useUpdateOccupancy(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<Occupancy, ApiError, { id: string; body: OccupancyUpdateBody }>({
    mutationFn: ({ id, body }) => updateOccupancy(ws, id, body),
    onSuccess: (updated) => refreshOccupancies(queryClient, ws, updated.personId),
  })
}

export function useEndOccupancy(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<Occupancy, ApiError, { occupancy: Occupancy; endDate: string }>({
    mutationFn: ({ occupancy, endDate }) => endOccupancy(ws, occupancy.id, occupancy.version, endDate),
    onSuccess: (ended) => refreshOccupancies(queryClient, ws, ended.personId),
  })
}

export function useDeleteOccupancy(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<void, ApiError, Occupancy>({
    mutationFn: (occupancy) => deleteOccupancy(ws, occupancy.id, occupancy.version),
    onSuccess: (_, occupancy) => refreshOccupancies(queryClient, ws, occupancy.personId),
  })
}

/** "Hoàn tác" sau khi xoá một người: khôi phục với `version` lúc xoá cộng 1. */
export async function undoDeletePerson(queryClient: QueryClient, ws: string, id: string, deletedVersion: number): Promise<PersonDetail> {
  const restored = await restorePerson(ws, id, deletedVersion + 1)
  refreshAfterPersonWrite(queryClient, ws, restored)
  return restored
}

/** Bỏ xoá một người. */
export function useRestorePerson(ws: string) {
  const queryClient = useQueryClient()
  return useMutation<PersonDetail, ApiError, { id: string; version: number }>({
    mutationFn: ({ id, version }) => restorePerson(ws, id, version),
    onSuccess: (restored) => refreshAfterPersonWrite(queryClient, ws, restored),
  })
}

/** Tìm nhanh cho ô tìm chung; `term` rỗng thì không gọi. */
export function usePersonQuickFind(ws: string, term: string) {
  return useQuery<PersonSummary[], ApiError>({
    queryKey: [...personKeys.lists(ws), 'quick', term],
    queryFn: () => quickFindPeople(ws, term),
    enabled: term !== '',
  })
}

/**
 * Kiểm tra sức chứa cho khoảng ngày đang nhập, 300 ms sau lần đổi cuối. Chỉ chạy khi đã có địa
 * điểm và ngày bắt đầu, và ngày kết thúc (nếu có) không đứng trước ngày bắt đầu.
 */
export function useCapacityCheck(ws: string, placeId: string | null, range: CapacityRange) {
  // Debounce từng giá trị, không debounce cả object: form tạo object mới mỗi lần render, và một
  // object mới sau mỗi 300 ms sẽ làm form render lại mãi.
  const startDate = useDebouncedValue(range.startDate, 300)
  const endDate = useDebouncedValue(range.endDate, 300)
  const excludeOccupancyId = range.excludeOccupancyId
  const valid = placeId != null && startDate !== '' && (endDate == null || endDate >= startDate)
  return useQuery({
    queryKey: ['places', ws, 'capacity-check', placeId, startDate, endDate ?? '', excludeOccupancyId ?? ''],
    queryFn: () => checkCapacity(ws, placeId!, { startDate, endDate, excludeOccupancyId }),
    enabled: valid,
  })
}

/** Dòng thời gian của một địa điểm hay một người; nằm dưới `occupancyKeys.all` nên ghi cư trú là tải lại. */
export function useOccupancyTimeline(ws: string, owner: { placeId: string } | { personId: string }) {
  const key = 'placeId' in owner ? occupancyKeys.timeline(ws, 'place', owner.placeId) : occupancyKeys.timeline(ws, 'person', owner.personId)
  return useQuery({ queryKey: key, queryFn: () => getOccupancyTimeline(ws, owner) })
}

/** Đặt hay bỏ (`null`) ảnh đại diện; gửi lại đủ các trường đang có vì lệnh sửa ghi đè cả bản ghi. */
export function useSetPersonAvatar(ws: string) {
  const update = useUpdatePerson(ws)
  return (person: PersonDetail, fileId: string | null) =>
    update.mutateAsync({
      id: person.id,
      body: {
        version: person.version,
        fullName: person.fullName,
        phone: person.phone ?? undefined,
        email: person.email ?? undefined,
        idType: person.idType ?? undefined,
        idNumber: person.idNumber ?? undefined,
        dateOfBirth: person.dateOfBirth ?? undefined,
        gender: person.gender ?? undefined,
        note: person.note ?? undefined,
        attributes: (person.attributes ?? undefined) as PersonUpdateBody['attributes'],
        avatarFileId: fileId ?? undefined,
      },
    })
}
