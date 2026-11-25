import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'

import { server } from '@/test/server'

import { useCapacityCheck } from './queries'

beforeEach(() => vi.useFakeTimers({ shouldAdvanceTime: true }))
afterEach(() => vi.useRealTimers())

test('idleFormDoesNotKeepRerendering', async () => {
  server.use(http.get('/api/registry/places/:id/capacity-check', () => HttpResponse.json({ capacity: 2, peak: 0 })))
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={client}>{children}</QueryClientProvider>
  let renders = 0
  // Form truyền một object khoảng ngày mới mỗi lần render, như OccupancyFormDialog.
  renderHook(
    () => {
      renders += 1
      return useCapacityCheck('7', '301', { startDate: '2026-12-01', endDate: undefined })
    },
    { wrapper },
  )
  await act(async () => {
    await vi.advanceTimersByTimeAsync(1000)
  })
  const settled = renders
  await act(async () => {
    await vi.advanceTimersByTimeAsync(3000)
  })
  expect(renders).toBe(settled)
})
