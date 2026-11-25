import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { todayInBusinessZone, addDays } from '@/lib/dates'
import { emptyTodoSummary, todo } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

import type { Todo } from './types'

const TODAY = todayInBusinessZone()

/** Trả `rows` (lọc theo `kind` như backend); `nextCursor` khi `more`. Ghi lại tham số của mỗi lần hỏi. */
function mockTodos(rows: Todo[], options: { more?: Todo[] } = {}) {
  const asked: Record<string, string>[] = []
  server.use(
    http.get('/api/registry/todos', ({ request }) => {
      const query = Object.fromEntries(new URL(request.url).searchParams)
      asked.push(query)
      if (query.cursor === 'next') return HttpResponse.json({ items: options.more ?? [], nextCursor: null })
      const items = rows.filter((row) => query.kind == null || row.kind === query.kind)
      return HttpResponse.json({ items, nextCursor: options.more ? 'next' : null })
    }),
    http.get('/api/registry/todos/summary', () =>
      HttpResponse.json({
        ...emptyTodoSummary(),
        total: rows.length,
        byKind: { BROKEN: 1, WARRANTY: 2, SERVICE: 0, MOVE_OUT: 1, CUSTOM_DATE: 0 },
      }),
    ),
  )
  return asked
}

async function main() {
  return within(await screen.findByRole('main'))
}

test('listsTodosWithLinksAndDueLabels', async () => {
  mockShell()
  mockTodos([
    todo({ kind: 'BROKEN', subjectId: '900', subjectName: 'Tủ lạnh', dueOn: '2026-11-20', context: 'P301' }),
    todo({ kind: 'WARRANTY', subjectId: '901', subjectName: 'Máy giặt', dueOn: addDays(TODAY, 12), context: 'P302' }),
    todo({ kind: 'MOVE_OUT', subjectType: 'PERSON', subjectId: '55', subjectName: 'Nguyễn An', dueOn: TODAY, context: 'P101' }),
  ])

  renderApp({ route: '/w/7/todos', userId: '42' })

  const view = await main()
  expect(await view.findByRole('heading', { name: 'Việc cần làm' })).toBeInTheDocument()
  const rows = await view.findAllByRole('listitem')
  expect(rows).toHaveLength(3)
  expect(within(rows[0]).getByRole('link', { name: 'Tủ lạnh' })).toHaveAttribute('href', '/w/7/items/900')
  expect(within(rows[0]).getByText('hỏng từ 20/11')).toBeInTheDocument()
  expect(within(rows[0]).getByText('P301', { exact: false })).toBeInTheDocument()
  expect(within(rows[1]).getByText('còn 12 ngày')).toBeInTheDocument()
  expect(within(rows[2]).getByRole('link', { name: 'Nguyễn An' })).toHaveAttribute('href', '/w/7/people/55')
  expect(within(rows[2]).getByText('hôm nay')).toBeInTheDocument()
})

test('kindToggleFiltersAndLandsOnTheUrl', async () => {
  mockShell()
  const asked = mockTodos([todo({ kind: 'BROKEN' }), todo({ kind: 'WARRANTY', subjectId: '901', subjectName: 'Máy giặt' })])

  const { user, router } = renderApp({ route: '/w/7/todos', userId: '42' })
  const view = await main()
  await user.click(await view.findByRole('button', { name: /^Bảo hành/ }))

  await waitFor(() => expect(asked.at(-1)).toMatchObject({ kind: 'WARRANTY' }))
  expect(router.state.location.search).toBe('?kind=WARRANTY')
  expect(await view.findByRole('link', { name: 'Máy giặt' })).toBeInTheDocument()
  expect(view.queryByRole('link', { name: 'Tủ lạnh' })).not.toBeInTheDocument()
  expect(view.getByRole('button', { name: /^Bảo hành/ })).toHaveTextContent('2')

  await user.click(view.getByRole('button', { name: /^Tất cả/ }))
  expect(await view.findByRole('link', { name: 'Tủ lạnh' })).toBeInTheDocument()
  expect(router.state.location.search).toBe('')
})

test('emptyState', async () => {
  mockShell()
  mockTodos([])

  renderApp({ route: '/w/7/todos', userId: '42' })

  expect(await (await main()).findByText('Không có việc gì cần làm')).toBeInTheDocument()
})

test('loadsMore', async () => {
  mockShell()
  mockTodos([todo({ subjectId: '900', subjectName: 'Tủ lạnh' })], { more: [todo({ subjectId: '902', subjectName: 'Quạt' })] })

  const { user } = renderApp({ route: '/w/7/todos', userId: '42' })
  const view = await main()
  await user.click(await view.findByRole('button', { name: 'Tải thêm' }))

  expect(await view.findByRole('link', { name: 'Quạt' })).toBeInTheDocument()
})

test('sidebarShowsTheTodoCount', async () => {
  mockShell()
  server.use(http.get('/api/registry/todos/summary', () => HttpResponse.json({ ...emptyTodoSummary(), total: 4 })))

  renderApp({ route: '/w/7/people', userId: '42' })

  const link = await screen.findByRole('link', { name: /Việc cần làm/ })
  expect(link).toHaveAttribute('href', '/w/7/todos')
  await waitFor(() => expect(link.closest('li')).toHaveTextContent('4'))
})

test('sidebarHidesAZeroCount', async () => {
  mockShell()

  renderApp({ route: '/w/7/people', userId: '42' })

  const link = await screen.findByRole('link', { name: /Việc cần làm/ })
  expect(link.closest('li')).toHaveTextContent(/^Việc cần làm$/)
})

test('olderRegistryHidesTheSidebarEntry', async () => {
  mockShell()
  server.use(http.get('/api/registry/todos/summary', () => HttpResponse.json({ status: 404, detail: 'Not found' }, { status: 404 })))

  renderApp({ route: '/w/7/people', userId: '42' })

  expect(await screen.findByRole('link', { name: 'Người' })).toBeInTheDocument()
  await waitFor(() => expect(screen.queryByRole('link', { name: /Việc cần làm/ })).not.toBeInTheDocument())
})
