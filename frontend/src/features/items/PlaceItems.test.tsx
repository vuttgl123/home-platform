import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { itemCategory, placeType } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { mockItemBackend } from '@/test/itemBackend'
import { mockPlaceBackend } from '@/test/placeBackend'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

function setUp(items: Parameters<typeof mockItemBackend>[0]['items'] = []) {
  mockShell({ placeTypes: [placeType()] })
  mockPlaceBackend([
    { id: '1', name: 'Toà A' },
    { id: '301', parentId: '1', name: 'P301' },
  ])
  server.use(http.get('/api/registry/item-categories', () => HttpResponse.json([itemCategory({ id: '600', name: 'Nội thất' })])))
  return mockItemBackend({
    items,
    placesUnder: { '1': ['301'] },
    placeNames: { '1': 'Toà A', '301': 'P301' },
    categoryNames: { '600': 'Nội thất' },
  })
}

async function main() {
  return within(await screen.findByRole('main'))
}

async function group(name: string) {
  return within(await (await main()).findByRole('region', { name }))
}

test('listsCurrentItemsByName', async () => {
  setUp([
    { id: '901', placeId: '301', name: 'Tủ lạnh', quantity: 1 },
    { id: '902', placeId: '301', name: 'Bàn', categoryId: '600', quantity: 4, unit: 'cái' },
    { id: '903', placeId: '301', name: 'Quạt', removedAt: '2026-02-01', status: 'REMOVED' },
  ])

  renderApp({ route: '/w/7/places/301?tab=items', userId: '42' })

  const current = await group('Đang có')
  const links = await current.findAllByRole('link')
  expect(links.map((link) => link.textContent)).toEqual(['Bàn', 'Tủ lạnh'])
  expect(links[0]).toHaveAttribute('href', '/w/7/items/902')
  const row = links[0].closest('tr') as HTMLElement
  expect(within(row).getByText('Nội thất')).toBeInTheDocument()
  expect(within(row).getByText('4 cái')).toBeInTheDocument()
  expect(within(row).getByText('Đang dùng')).toBeInTheDocument()
  expect(current.queryByText('Quạt')).not.toBeInTheDocument()
})

test('removedGroupLoadsOnlyWhenOpened', async () => {
  setUp([{ id: '903', placeId: '301', name: 'Quạt', removedAt: '2026-02-01', status: 'REMOVED' }])
  const asked: string[] = []
  server.events.on('request:start', ({ request }) => {
    const url = new URL(request.url)
    if (url.pathname.endsWith('/items')) asked.push(url.searchParams.get('removed') ?? '')
  })
  const { user } = renderApp({ route: '/w/7/places/301?tab=items', userId: '42' })
  await (await group('Đang có')).findByText('Chưa có đồ nào ở đây.')
  expect(asked).toEqual(['false'])

  await user.click((await main()).getByRole('button', { name: /Đã gỡ/ }))

  const removed = await group('Đã gỡ')
  const row = (await removed.findByRole('link', { name: 'Quạt' })).closest('tr') as HTMLElement
  expect(within(row).getByText('01/02/2026')).toBeInTheDocument()
})

test('wholeBuildingShowsWhereEachItemIs', async () => {
  setUp([{ id: '901', placeId: '301', name: 'Tủ lạnh' }])
  const { user } = renderApp({ route: '/w/7/places/1?tab=items', userId: '42' })

  await user.click(await (await main()).findByRole('switch', { name: 'Cả bên trong' }))

  const inside = await group('Đang có bên trong')
  const row = (await inside.findByRole('link', { name: 'Tủ lạnh' })).closest('tr') as HTMLElement
  expect(within(row).getByRole('link', { name: 'P301' })).toHaveAttribute('href', '/w/7/places/301')
})

test('addsAnItemFromTheTab', async () => {
  const backend = setUp()
  const { user } = renderApp({ route: '/w/7/places/301?tab=items', userId: '42' })

  await user.click((await (await main()).findAllByRole('button', { name: 'Thêm đồ' }))[0])
  const form = within(await screen.findByRole('dialog', { name: 'Thêm đồ' }))
  await user.type(form.getByRole('textbox', { name: 'Tên' }), 'Máy giặt')
  await waitFor(() => expect(form.getByRole('button', { name: 'Thêm' })).toBeEnabled())
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  expect(await (await group('Đang có')).findByRole('link', { name: 'Máy giặt' })).toBeInTheDocument()
  expect(backend.writes[0].body).toMatchObject({ placeId: '301', name: 'Máy giặt' })
})

test('emptyTabInvitesToAdd', async () => {
  setUp()

  renderApp({ route: '/w/7/places/301?tab=items', userId: '42' })

  expect(await (await group('Đang có')).findByText('Chưa có đồ nào ở đây.')).toBeInTheDocument()
  expect((await main()).getAllByRole('button', { name: 'Thêm đồ' }).length).toBeGreaterThan(0)
  // P301 không có chỗ con: không có công tắc "Cả bên trong".
  expect((await main()).queryByRole('switch', { name: 'Cả bên trong' })).not.toBeInTheDocument()
})
