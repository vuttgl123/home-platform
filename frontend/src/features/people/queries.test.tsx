import { QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { ReactNode } from 'react'
import { expect, test } from 'vitest'

import { createQueryClient, setCurrentUser } from '@/api'
import { occupancy, page, person } from '@/test/fixtures'
import { server } from '@/test/server'

import {
  useEndOccupancy,
  useOccupanciesAtPlace,
  useOccupanciesInSubtree,
  useOccupanciesOfPerson,
  usePeopleSearch,
} from './queries'

function wrapper({ children }: { children: ReactNode }) {
  return <QueryClientProvider client={createQueryClient()}>{children}</QueryClientProvider>
}

test('occupancyWritesRefreshPlacePersonAndSubtree', async () => {
  setCurrentUser('42')
  const calls: string[] = []
  const row = occupancy({ id: '700', personId: '55', placeId: '301' })
  server.use(
    http.get('/api/registry/occupancies', ({ request }) => {
      const query = new URL(request.url).searchParams
      calls.push(query.has('placeId') ? 'place' : query.has('subtreeOf') ? 'subtree' : 'person')
      return HttpResponse.json(page([row]))
    }),
    http.post('/api/registry/occupancies/700/end', () =>
      HttpResponse.json({ ...row, endDate: '2026-11-16', status: 'PAST', version: 2 }),
    ),
  )
  const { result } = renderHook(
    () => ({
      atPlace: useOccupanciesAtPlace('7', '301', 'CURRENT'),
      inSubtree: useOccupanciesInSubtree('7', '1', true),
      ofPerson: useOccupanciesOfPerson('7', '55'),
      end: useEndOccupancy('7'),
    }),
    { wrapper },
  )
  await waitFor(() => expect(calls.sort()).toEqual(['person', 'place', 'subtree']))

  await act(() => result.current.end.mutateAsync({ occupancy: row, endDate: '2026-11-16' }))

  await waitFor(() => expect(calls).toHaveLength(6))
  expect(calls.slice(3).sort()).toEqual(['person', 'place', 'subtree'])
})

test('searchSendsTheChosenField', async () => {
  setCurrentUser('42')
  let query: URLSearchParams | null = null
  server.use(
    http.get('/api/registry/people/search', ({ request }) => {
      query = new URL(request.url).searchParams
      return HttpResponse.json(page([person()]))
    }),
  )

  const { result } = renderHook(() => usePeopleSearch('7', 'phone', '0909'), { wrapper })

  await waitFor(() => expect(result.current.items).toHaveLength(1))
  expect(query!.get('phone')).toBe('0909')
  expect(query!.has('name')).toBe(false)
})
