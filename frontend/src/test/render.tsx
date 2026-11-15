import { QueryClientProvider } from '@tanstack/react-query'
import { render, type RenderResult } from '@testing-library/react'
import userEvent, { type UserEvent } from '@testing-library/user-event'
import type { ReactElement } from 'react'
import { createMemoryRouter } from 'react-router'

import { createQueryClient } from '@/api'
import { App } from '@/app/App'
import { routes } from '@/app/router'
import { sessionStore } from '@/app/session/sessionStore'

/** Cache như của ứng dụng, nhưng không thử lại: test muốn thấy lỗi ngay. */
function createTestQueryClient() {
  const queryClient = createQueryClient()
  queryClient.setDefaultOptions({
    queries: { ...queryClient.getDefaultOptions().queries, retry: false },
    mutations: { retry: false },
  })
  return queryClient
}

/** Render một component rời với các provider nó cần; mỗi lần gọi có cache riêng. */
export function renderWithProviders(ui: ReactElement): RenderResult & { user: UserEvent } {
  const user = userEvent.setup()
  return { user, ...render(<QueryClientProvider client={createTestQueryClient()}>{ui}</QueryClientProvider>) }
}

/**
 * Render cả ứng dụng với router thật, bắt đầu ở `route`.
 * @param options.userId người dùng đã chọn từ trước; bỏ trống là chưa chọn ai
 */
export function renderApp(options: { route: string; userId?: string }) {
  if (options.userId) {
    sessionStore.signIn(options.userId)
  }
  const router = createMemoryRouter(routes, { initialEntries: [options.route] })
  const user = userEvent.setup()
  return { user, router, ...render(<App router={router} queryClient={createTestQueryClient()} />) }
}
