import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { emptyTodoSummary, todo } from '@/test/fixtures'
import { mockShell, recordRequests } from '@/test/handlers'
import { mockItemBackend } from '@/test/itemBackend'
import { mockPlaceBackend } from '@/test/placeBackend'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

import type { TodoSummary } from './types'

/** Như backend: sau `POST /todos/seen` không còn việc nào mới. */
function mockSummary(summary: Partial<TodoSummary>) {
  let seen = false
  server.use(
    http.get('/api/registry/todos/summary', () => {
      const current = { ...emptyTodoSummary(), ...summary }
      return HttpResponse.json(
        seen ? { ...current, newCount: 0, latest: current.latest.map((row) => ({ ...row, isNew: false })) } : current,
      )
    }),
    http.post('/api/registry/todos/seen', () => {
      seen = true
      return new HttpResponse(null, { status: 204 })
    }),
  )
}

/** Lần tìm đầu chờ lâu hơn: cả khung trang (thanh bên, cây địa điểm, tổng quan) tải trước chuông. */
async function bell() {
  return screen.findByRole('button', { name: /^Thông báo/ }, { timeout: 3000 })
}

test('showsNewCountAndCapsAtNine', async () => {
  mockShell()
  mockSummary({ total: 3, newCount: 3 })
  renderApp({ route: '/w/7', userId: '42' })

  await waitFor(async () => expect(await bell()).toHaveTextContent('3'), { timeout: 3000 })
})

test('capsAtNine', async () => {
  mockShell()
  mockSummary({ total: 12, newCount: 12 })
  renderApp({ route: '/w/7', userId: '42' })

  await waitFor(async () => expect(await bell()).toHaveTextContent('9+'), { timeout: 3000 })
})

test('noDotWithoutNews', async () => {
  mockShell()
  mockSummary({ total: 2, newCount: 0 })
  renderApp({ route: '/w/7', userId: '42' })

  expect(await bell()).toHaveTextContent(/^$/)
})

test('openingListsLatestAndMarksSeen', async () => {
  mockShell()
  mockSummary({
    total: 2,
    newCount: 1,
    latest: [
      todo({ kind: 'WARRANTY', subjectId: '901', subjectName: 'Máy giặt', isNew: true }),
      todo({ kind: 'BROKEN', subjectId: '900', subjectName: 'Tủ lạnh', isNew: false }),
    ],
  })
  const requests = recordRequests()
  const { user } = renderApp({ route: '/w/7', userId: '42' })
  await waitFor(async () => expect(await bell()).toHaveTextContent('1'), { timeout: 3000 })

  await user.click(await bell())

  const panel = within(await screen.findByRole('dialog', { name: 'Thông báo' }))
  const rows = panel.getAllByRole('listitem')
  expect(rows).toHaveLength(2)
  expect(within(rows[0]).getByLabelText('Mới')).toBeInTheDocument()
  expect(within(rows[1]).queryByLabelText('Mới')).not.toBeInTheDocument()
  await waitFor(() => expect(requests).toContain('POST /api/registry/todos/seen'))
  expect(await bell()).toHaveTextContent(/^$/)
})

test('emptyPopover', async () => {
  mockShell()
  mockSummary({})
  const { user } = renderApp({ route: '/w/7', userId: '42' })

  await user.click(await bell())

  expect(await within(await screen.findByRole('dialog', { name: 'Thông báo' })).findByText('Chưa có gì mới')).toBeInTheDocument()
})

test('seeAllGoesToTheTodoPage', async () => {
  mockShell()
  mockSummary({ total: 1, latest: [todo({ subjectId: '900', subjectName: 'Tủ lạnh' })] })
  mockPlaceBackend([{ id: '301', name: 'P301' }])
  mockItemBackend({ items: [{ id: '900', name: 'Tủ lạnh' }] })
  server.use(http.get('/api/registry/item-categories', () => HttpResponse.json([])))
  const { user, router } = renderApp({ route: '/w/7', userId: '42' })

  await user.click(await bell())
  const panel = within(await screen.findByRole('dialog', { name: 'Thông báo' }))
  expect(panel.getByRole('link', { name: 'Xem tất cả việc cần làm' })).toHaveAttribute('href', '/w/7/todos')
  await user.click(panel.getByRole('link', { name: 'Tủ lạnh' }))

  await waitFor(() => expect(router.state.location.pathname).toBe('/w/7/items/900'))
  await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Thông báo' })).not.toBeInTheDocument())
})

test('olderRegistryHidesTheBell', async () => {
  mockShell()
  server.use(http.get('/api/registry/todos/summary', () => HttpResponse.json({ status: 404, detail: 'Not found' }, { status: 404 })))
  renderApp({ route: '/w/7', userId: '42' })

  expect(await screen.findByRole('link', { name: 'Người' }, { timeout: 3000 })).toBeInTheDocument()
  await waitFor(() => expect(screen.queryByRole('button', { name: /^Thông báo/ })).not.toBeInTheDocument())
})
