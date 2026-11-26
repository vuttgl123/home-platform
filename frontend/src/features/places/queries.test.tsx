import { QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { ReactNode } from 'react'
import { expect, test } from 'vitest'

import { createQueryClient, setCurrentUser } from '@/api'
import { placeDetail } from '@/test/fixtures'
import { server } from '@/test/server'

import { useDeletePlace } from './queries'

test('deletingAPlaceMarksVacanciesStale', async () => {
  server.use(http.delete('/api/registry/places/:id', () => new HttpResponse(null, { status: 204 })))
  setCurrentUser('42')
  const queryClient = createQueryClient()
  // Trang Chỗ trống (features/vacancies) giữ danh sách dưới khoá này; một địa điểm bị xoá thì danh sách đó cũ.
  queryClient.setQueryData(['vacancies', '7', 'list'], { pages: [], pageParams: [] })
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  const { result } = renderHook(() => useDeletePlace('7'), { wrapper })

  await act(() => result.current.mutateAsync(placeDetail({ id: '301', version: 2 })))

  expect(queryClient.getQueryState(['vacancies', '7', 'list'])?.isInvalidated).toBe(true)
})

test('deletingAPlaceMarksThePlaceTableStale', async () => {
  server.use(http.delete('/api/registry/places/:id', () => new HttpResponse(null, { status: 204 })))
  setCurrentUser('42')
  const queryClient = createQueryClient()
  queryClient.setQueryData(['places', '7', 'filtered', {}], { pages: [], pageParams: [] })
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  const { result } = renderHook(() => useDeletePlace('7'), { wrapper })

  await act(() => result.current.mutateAsync(placeDetail({ id: '301', version: 2 })))

  expect(queryClient.getQueryState(['places', '7', 'filtered', {}])?.isInvalidated).toBe(true)
})
