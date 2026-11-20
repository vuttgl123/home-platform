import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { mockDownloads } from '@/test/downloads'
import { itemCategory, placeType } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { mockItemBackend } from '@/test/itemBackend'
import { mockPlaceBackend } from '@/test/placeBackend'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

function setUp() {
  mockShell({ placeTypes: [placeType()] })
  mockPlaceBackend([
    { id: '301', name: 'P301' },
    { id: '302', name: 'P302' },
  ])
  server.use(
    http.get('/api/registry/item-categories', () =>
      HttpResponse.json([itemCategory({ id: '600', name: 'Điện tử' }), itemCategory({ id: '601', name: 'Nội thất' })]),
    ),
  )
  return mockItemBackend({
    items: [
      { id: '901', placeId: '301', name: 'Tủ lạnh', categoryId: '600' },
      { id: '902', placeId: '302', name: 'Bàn', categoryId: '601' },
      { id: '903', placeId: '301', name: 'Quạt', status: 'BROKEN', categoryId: '600' },
      { id: '904', placeId: '302', name: 'Ghế', status: 'REMOVED', removedAt: '2026-02-01' },
    ],
    placeNames: { '301': 'P301', '302': 'P302' },
    categoryNames: { '600': 'Điện tử', '601': 'Nội thất' },
  })
}

async function main() {
  return within(await screen.findByRole('main'))
}

function names(view: Awaited<ReturnType<typeof main>>) {
  return within(view.getByRole('table'))
    .getAllByRole('row')
    .slice(1)
    .map((row) => within(row).getAllByRole('cell')[0].querySelector('a')?.textContent)
}

test('listsEveryCurrentItemWithItsPlace', async () => {
  setUp()
  renderApp({ route: '/w/7/items', userId: '42' })

  const view = await main()
  expect(await view.findByRole('heading', { name: 'Đồ đạc' })).toBeInTheDocument()
  await view.findByRole('link', { name: 'Bàn' })
  expect(names(view)).toEqual(['Bàn', 'Quạt', 'Tủ lạnh'])
  expect(view.getAllByRole('link', { name: 'P302' })[0]).toHaveAttribute('href', '/w/7/places/302')
})

test('filteringByStatusUpdatesTheUrlAndTheRequest', async () => {
  setUp()
  const requests: string[] = []
  server.events.on('request:start', ({ request }) => requests.push(request.url))
  const { user, router } = renderApp({ route: '/w/7/items', userId: '42' })
  const view = await main()
  await view.findByRole('link', { name: 'Bàn' })

  await user.selectOptions(view.getByRole('combobox', { name: 'Trạng thái' }), 'Hỏng')

  await waitFor(() => expect(router.state.location.search).toBe('?status=BROKEN'))
  await waitFor(() => expect(names(view)).toEqual(['Quạt']))
  expect(requests.some((url) => url.includes('/items?') && url.includes('status=BROKEN'))).toBe(true)
})

test('filteringByCategory', async () => {
  setUp()
  const { user, router } = renderApp({ route: '/w/7/items', userId: '42' })
  const view = await main()
  await view.findByRole('link', { name: 'Bàn' })

  await user.selectOptions(view.getByRole('combobox', { name: 'Nhóm' }), 'Nội thất')

  await waitFor(() => expect(router.state.location.search).toBe('?category=601'))
  await waitFor(() => expect(names(view)).toEqual(['Bàn']))
})

test('removedFilterShowsRemovalDates', async () => {
  setUp()
  renderApp({ route: '/w/7/items?status=REMOVED', userId: '42' })

  const view = await main()
  expect(await view.findByRole('link', { name: 'Ghế' })).toBeInTheDocument()
  expect(view.getByText('01/02/2026')).toBeInTheDocument()
})

test('openingAFilteredUrlAppliesTheFilter', async () => {
  setUp()
  renderApp({ route: '/w/7/items?status=BROKEN&category=600', userId: '42' })

  const view = await main()
  expect(await view.findByRole('link', { name: 'Quạt' })).toBeInTheDocument()
  expect(view.getByRole('combobox', { name: 'Trạng thái' })).toHaveValue('BROKEN')
  expect(view.getByRole('combobox', { name: 'Nhóm' })).toHaveValue('600')
})

test('unknownStatusInTheUrlIsIgnored', async () => {
  setUp()
  const requests: string[] = []
  server.events.on('request:start', ({ request }) => requests.push(request.url))
  renderApp({ route: '/w/7/items?status=XYZ&category=abc', userId: '42' })

  const view = await main()
  await view.findByRole('link', { name: 'Bàn' })
  expect(view.getByRole('combobox', { name: 'Trạng thái' })).toHaveValue('')
  expect(requests.filter((url) => url.includes('/registry/items?')).every((url) => !url.includes('status=') && !url.includes('categoryId='))).toBe(true)
})

test('emptyResultSaysWhy', async () => {
  setUp()
  renderApp({ route: '/w/7/items?status=STORED', userId: '42' })

  expect(await (await main()).findByText('Không có món nào khớp bộ lọc.')).toBeInTheDocument()
})

test('sidebarLinksToTheItemsPage', async () => {
  setUp()
  renderApp({ route: '/w/7', userId: '42' })

  expect(await screen.findByRole('link', { name: 'Đồ đạc' })).toHaveAttribute('href', '/w/7/items')
})

test('itemPageStillOpensUnderTheSamePrefix', async () => {
  setUp()
  renderApp({ route: '/w/7/items/901', userId: '42' })

  expect(await (await main()).findByRole('heading', { name: 'Tủ lạnh' })).toBeInTheDocument()
})

test('itemsExportSendsTheCurrentFilter', async () => {
  setUp()
  const saved = mockDownloads()
  let sent = ''
  server.use(
    http.get('/api/registry/exports/items.xlsx', ({ request }) => {
      sent = new URL(request.url).search
      return new HttpResponse('file')
    }),
  )
  const { user } = renderApp({ route: '/w/7/items?status=BROKEN&category=600', userId: '42' })
  const view = await main()
  await view.findByRole('link', { name: 'Quạt' })

  await user.click(view.getByRole('button', { name: 'Xuất Excel' }))

  await waitFor(() => expect(saved).toHaveLength(1))
  expect(sent).toBe('?status=BROKEN&categoryId=600')
  expect(saved[0].name).toMatch(/^nha-an-do-dac-\d{4}-\d{2}-\d{2}\.xlsx$/)
})

test('itemsExportWithoutAFilterSendsNoParameters', async () => {
  setUp()
  const saved = mockDownloads()
  let sent: string | null = null
  server.use(
    http.get('/api/registry/exports/items.xlsx', ({ request }) => {
      sent = new URL(request.url).search
      return new HttpResponse('file')
    }),
  )
  const { user } = renderApp({ route: '/w/7/items', userId: '42' })
  const view = await main()
  await view.findByRole('link', { name: 'Bàn' })

  await user.click(view.getByRole('button', { name: 'Xuất Excel' }))

  await waitFor(() => expect(saved).toHaveLength(1))
  expect(sent).toBe('')
})

test('brokenItemIsMarkedDanger', async () => {
  setUp()
  renderApp({ route: '/w/7/items', userId: '42' })
  const view = await main()
  await view.findByRole('link', { name: 'Quạt' })

  const row = (name: string) => within(view.getByRole('link', { name }).closest('tr') as HTMLElement)
  // Màu đi kèm chữ: nhãn vẫn ghi đủ, `data-tone` cho biết nghĩa màu.
  expect(row('Quạt').getByText('Hỏng')).toHaveAttribute('data-tone', 'danger')
  expect(row('Tủ lạnh').getByText('Đang dùng')).toHaveAttribute('data-tone', 'success')
})
