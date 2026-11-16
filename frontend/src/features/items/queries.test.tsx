import { QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { ReactNode } from 'react'
import { expect, test } from 'vitest'

import { createQueryClient, setCurrentUser } from '@/api'
import { useItemCategories } from '@/features/catalog'
import { itemCategory } from '@/test/fixtures'
import { mockItemBackend } from '@/test/itemBackend'
import { server } from '@/test/server'

import { useItemsAtPlace, useItemsInSubtree, useMoveItem } from './queries'

function wrapper({ children }: { children: ReactNode }) {
  return <QueryClientProvider client={createQueryClient()}>{children}</QueryClientProvider>
}

test('itemWritesRefreshEveryItemList', async () => {
  setCurrentUser('42')
  const backend = mockItemBackend({
    items: [{ id: '900', placeId: '301', name: 'Tủ lạnh' }],
    placesUnder: { '1': ['301', '302'], '2': ['201'] },
  })
  const { result } = renderHook(
    () => ({
      here: useItemsAtPlace('7', '301', false),
      there: useItemsAtPlace('7', '201', false),
      buildingA: useItemsInSubtree('7', '1', true),
      buildingB: useItemsInSubtree('7', '2', true),
      move: useMoveItem('7'),
    }),
    { wrapper },
  )
  await waitFor(() => expect(result.current.here.items).toHaveLength(1))

  await act(() => result.current.move.mutateAsync({ item: backend.items.get('900')!, placeId: '201' }))

  // Một lệnh chuyển chạm hai địa điểm và hai toà: cả bốn danh sách đều tải lại.
  await waitFor(() => expect(result.current.here.items).toHaveLength(0))
  await waitFor(() => expect(result.current.there.items.map((item) => item.name)).toEqual(['Tủ lạnh']))
  await waitFor(() => expect(result.current.buildingA.items).toHaveLength(0))
  await waitFor(() => expect(result.current.buildingB.items).toHaveLength(1))
})

test('removedListAsksForRemoved', async () => {
  setCurrentUser('42')
  let query: URLSearchParams | null = null
  server.use(
    http.get('/api/registry/items', ({ request }) => {
      query = new URL(request.url).searchParams
      return HttpResponse.json({ items: [], nextCursor: null })
    }),
  )

  renderHook(() => useItemsAtPlace('7', '301', true), { wrapper })

  await waitFor(() => expect(query).not.toBeNull())
  expect(query!.get('placeId')).toBe('301')
  expect(query!.get('removed')).toBe('true')
})

test('categoriesAreSortedForPickers', async () => {
  setCurrentUser('42')
  server.use(
    http.get('/api/registry/item-categories', () =>
      HttpResponse.json([
        itemCategory({ id: '2', name: 'Điện tử', sortOrder: 20 }),
        itemCategory({ id: '1', name: 'Nội thất', sortOrder: 10 }),
      ]),
    ),
  )

  const { result } = renderHook(() => useItemCategories('7'), { wrapper })

  await waitFor(() => expect(result.current.data?.map((category) => category.name)).toEqual(['Nội thất', 'Điện tử']))
})
