import { QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { expect, test } from 'vitest'

import { fetchAllPages, useCursorList } from './paging'
import { createQueryClient } from './queryClient'
import type { CursorPage } from './types'

type Room = { id: string; name: string }

const a: Room = { id: '1', name: 'P101' }
const b: Room = { id: '2', name: 'P102' }
const c: Room = { id: '3', name: 'P103' }

/** Một nguồn trang giả: trang đầu ở khoá 'first', các trang sau ở khoá là cursor của chúng. */
function source(pages: Record<string, CursorPage<Room>>) {
  const cursors: (string | undefined)[] = []
  const fetchPage = async (cursor?: string) => {
    cursors.push(cursor)
    return pages[cursor ?? 'first']
  }
  return { cursors, fetchPage }
}

function wrapper({ children }: { children: ReactNode }) {
  return <QueryClientProvider client={createQueryClient()}>{children}</QueryClientProvider>
}

test('loadMoreAppendsPages', async () => {
  const { cursors, fetchPage } = source({
    first: { items: [a, b], nextCursor: 'c1' },
    c1: { items: [c], nextCursor: null },
  })
  const { result } = renderHook(() => useCursorList({ queryKey: ['rooms'], fetchPage }), { wrapper })

  await waitFor(() => expect(result.current.items).toEqual([a, b]))
  expect(result.current.hasMore).toBe(true)

  act(() => result.current.loadMore())

  await waitFor(() => expect(result.current.items).toEqual([a, b, c]))
  expect(result.current.hasMore).toBe(false)
  expect(cursors).toEqual([undefined, 'c1'])
})

test('loadMoreSkipsRepeatedItems', async () => {
  // Có người thêm một phòng đứng trước P102 giữa hai lần tải: trang hai bắt đầu lại từ P102.
  const { fetchPage } = source({
    first: { items: [a, b], nextCursor: 'c1' },
    c1: { items: [b, c], nextCursor: null },
  })
  const { result } = renderHook(() => useCursorList({ queryKey: ['rooms'], fetchPage }), { wrapper })
  await waitFor(() => expect(result.current.items).toEqual([a, b]))

  act(() => result.current.loadMore())

  await waitFor(() => expect(result.current.hasMore).toBe(false))
  expect(result.current.items).toEqual([a, b, c])
})

test('disabledListDoesNotFetch', async () => {
  const { cursors, fetchPage } = source({ first: { items: [a], nextCursor: null } })

  const { result } = renderHook(() => useCursorList({ queryKey: ['rooms'], fetchPage, enabled: false }), {
    wrapper,
  })

  expect(result.current.isLoading).toBe(false)
  expect(result.current.isLoaded).toBe(false)
  expect(cursors).toEqual([])
})

test('fetchAllPagesFollowsEveryCursor', async () => {
  const { cursors, fetchPage } = source({
    first: { items: [a], nextCursor: 'c1' },
    c1: { items: [b], nextCursor: 'c2' },
    c2: { items: [c], nextCursor: null },
  })

  expect(await fetchAllPages(fetchPage)).toEqual([a, b, c])
  expect(cursors).toEqual([undefined, 'c1', 'c2'])
})
