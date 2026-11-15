import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, type RenderResult } from '@testing-library/react'
import userEvent, { type UserEvent } from '@testing-library/user-event'
import type { ReactElement } from 'react'

/** Render một component rời với các provider nó cần; mỗi lần gọi có cache riêng. */
export function renderWithProviders(ui: ReactElement): RenderResult & { user: UserEvent } {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  const user = userEvent.setup()
  return { user, ...render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>) }
}
