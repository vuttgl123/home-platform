import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { mockDownloads } from '@/test/downloads'
import { overview, page, placeDetail, problem } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

function entry(overrides: Record<string, unknown>) {
  return {
    entityType: 'PLACE',
    entityId: '1',
    versionNo: 1,
    action: 'CREATE',
    changedFields: null,
    changedBy: '42',
    changedAt: '2026-11-16T03:00:00Z',
    note: null,
    entityName: 'Toà A',
    ...overrides,
  }
}

/** Trả `limit` dòng đầu của `entries`, như backend; ghi lại tham số của mỗi lần hỏi. */
function mockActivity(entries: Record<string, unknown>[]) {
  const asked: string[] = []
  server.use(
    http.get('/api/registry/activity', ({ request }) => {
      const params = new URL(request.url).searchParams
      asked.push(params.toString())
      return HttpResponse.json(page(entries.slice(0, Number(params.get('limit') ?? 20))))
    }),
  )
  return asked
}

function setUp(overrides: Parameters<typeof overview>[0] = {}) {
  mockShell({ overview: overview(overrides) })
  return mockActivity([])
}

async function main() {
  const view = within(await screen.findByRole('main'))
  await view.findByRole('heading', { name: 'Tổng quan' })
  return view
}

test('showsTheHeadlineNumbers', async () => {
  setUp({ places: 1042 })
  renderApp({ route: '/w/7', userId: '42' })

  const view = await main()
  const tiles = within(view.getByRole('list', { name: 'Số liệu chính' })).getAllByRole('listitem')

  expect(tiles).toHaveLength(4)
  expect(tiles[0]).toHaveTextContent(/^1\.042\s*địa điểm$/)
  expect(tiles[1]).toHaveTextContent(/^18\s*người đang ở \/ 25 hồ sơ$/)
  expect(tiles[2]).toHaveTextContent(/^230\s*món đồ đang dùng$/)
  expect(tiles[3]).toHaveTextContent(/^3\s*món hỏng$/)
})

test('statusNumbersLinkToTheFilteredItemsPage', async () => {
  setUp()
  renderApp({ route: '/w/7', userId: '42' })

  const view = await main()

  expect(view.getByRole('link', { name: '230 món đồ đang dùng' })).toHaveAttribute('href', '/w/7/items?status=IN_USE')
  expect(view.getByRole('link', { name: '3 món hỏng' })).toHaveAttribute('href', '/w/7/items?status=BROKEN')
  const statuses = within(view.getByRole('region', { name: 'Đồ đạc theo trạng thái' }))
  expect(statuses.getByRole('link', { name: 'Đang dùng 230' })).toHaveAttribute('href', '/w/7/items?status=IN_USE')
  expect(statuses.getByRole('link', { name: 'Hỏng 3' })).toHaveAttribute('href', '/w/7/items?status=BROKEN')
  expect(statuses.getByRole('link', { name: 'Cất kho 12' })).toHaveAttribute('href', '/w/7/items?status=STORED')
  expect(statuses.getByRole('link', { name: 'Đã gỡ 40' })).toHaveAttribute('href', '/w/7/items?status=REMOVED')
})

test('placeTypesShowOccupiedAndVacant', async () => {
  setUp({
    placeTypes: [
      { typeId: '500', name: 'Phòng', total: 3, occupied: 2 },
      { typeId: '501', name: 'Tầng', total: 1, occupied: 0 },
    ],
  })
  renderApp({ route: '/w/7', userId: '42' })

  const types = within((await main()).getByRole('region', { name: 'Chỗ ở theo loại' }))
  const rows = types.getAllByRole('listitem')

  expect(rows).toHaveLength(2)
  expect(rows[0]).toHaveTextContent(/^Phòng\s*2 có người · 1 trống$/)
  expect(rows[1]).toHaveTextContent(/^Tầng\s*0 có người · 1 trống$/)
})

test('todoSectionHiddenWhenNothingIsMissing', async () => {
  setUp({ missingCodes: { places: 0, items: 0 } })
  renderApp({ route: '/w/7', userId: '42' })

  const view = await main()

  expect(view.queryByRole('region', { name: 'Cần xử lý' })).not.toBeInTheDocument()
  expect(view.queryByText(/chưa có mã/)).not.toBeInTheDocument()
})

test('todoSectionLinksToCatalog', async () => {
  setUp({ missingCodes: { places: 4, items: 0 } })
  renderApp({ route: '/w/7', userId: '42' })

  const todo = within((await main()).getByRole('region', { name: 'Cần xử lý' }))

  expect(todo.getByText('4 địa điểm chưa có mã')).toBeInTheDocument()
  // Dòng có số 0 không hiện.
  expect(todo.queryByText(/đồ chưa có mã/)).not.toBeInTheDocument()
  expect(todo.getByRole('link', { name: 'Cấp mã' })).toHaveAttribute('href', '/w/7/catalog')
})

test('todoSectionCountsItemsToo', async () => {
  setUp({ missingCodes: { places: 0, items: 2 } })
  renderApp({ route: '/w/7', userId: '42' })

  const todo = within((await main()).getByRole('region', { name: 'Cần xử lý' }))

  expect(todo.getByText('2 đồ chưa có mã')).toBeInTheDocument()
  expect(todo.queryByText(/địa điểm chưa có mã/)).not.toBeInTheDocument()
})

test('recentActivityShowsFiveRows', async () => {
  mockShell({ overview: overview() })
  const asked = mockActivity(
    [1, 2, 3, 4, 5, 6, 7].map((n) => entry({ entityId: String(n), entityName: `P${n}`, changedAt: `2026-11-16T03:0${n}:00Z` })),
  )
  renderApp({ route: '/w/7', userId: '42' })

  const recent = within((await main()).getByRole('region', { name: 'Hoạt động gần đây' }))
  await recent.findByRole('link', { name: 'P1' })

  expect(recent.getAllByRole('listitem')).toHaveLength(5)
  expect(recent.getByRole('link', { name: 'P5' })).toHaveAttribute('href', '/w/7/places/5')
  expect(recent.queryByRole('link', { name: 'P6' })).not.toBeInTheDocument()
  expect(asked).toEqual(['limit=5'])
  expect(recent.getByRole('link', { name: 'Xem tất cả' })).toHaveAttribute('href', '/w/7/activity')
})

test('recentActivityCanBeEmpty', async () => {
  setUp()
  renderApp({ route: '/w/7', userId: '42' })

  const recent = within((await main()).getByRole('region', { name: 'Hoạt động gần đây' }))

  expect(await recent.findByText('Chưa có hoạt động nào.')).toBeInTheDocument()
})

test('downloadButtonSavesTheWholeWorkspace', async () => {
  setUp()
  const saved = mockDownloads()
  server.use(http.get('/api/registry/exports/workspace.xlsx', () => new HttpResponse('file')))
  const { user } = renderApp({ route: '/w/7', userId: '42' })

  await user.click((await main()).getByRole('button', { name: 'Tải Excel' }))

  await waitFor(() => expect(saved).toHaveLength(1))
  expect(saved[0].name).toMatch(/^nha-an-toan-bo-\d{4}-\d{2}-\d{2}\.xlsx$/)
})

/** Khôi phục ngay trên trang tổng quan: các con số phải theo kịp, không đợi mở lại trang. */
test('restoringFromRecentActivityRefreshesTheNumbers', async () => {
  let places = 41
  mockShell()
  server.use(
    http.get('/api/registry/overview', () => HttpResponse.json(overview({ places }))),
    http.post('/api/registry/places/9/restore', () => {
      places = 42
      return HttpResponse.json(placeDetail({ id: '9', name: 'P302' }))
    }),
  )
  mockActivity([entry({ entityId: '9', entityName: 'P302', action: 'DELETE', versionNo: 3 })])
  const { user } = renderApp({ route: '/w/7', userId: '42' })
  const view = await main()
  const tiles = () => within(view.getByRole('list', { name: 'Số liệu chính' })).getAllByRole('listitem')
  expect(tiles()[0]).toHaveTextContent(/^41\s*địa điểm$/)

  await user.click(await view.findByRole('button', { name: 'Khôi phục P302' }))

  await waitFor(() => expect(tiles()[0]).toHaveTextContent(/^42\s*địa điểm$/))
})

test('failedOverviewShowsAnErrorWithRetry', async () => {
  mockShell()
  let fail = true
  server.use(
    http.get('/api/registry/overview', () =>
      fail ? HttpResponse.json(problem(400, 'boom'), { status: 400 }) : HttpResponse.json(overview()),
    ),
  )
  mockActivity([])
  const { user } = renderApp({ route: '/w/7', userId: '42' })
  const view = within(await screen.findByRole('main'))
  expect(await view.findByText('boom')).toBeInTheDocument()

  fail = false
  await user.click(view.getByRole('button', { name: 'Thử lại' }))

  expect(await view.findByRole('heading', { name: 'Tổng quan' })).toBeInTheDocument()
})
