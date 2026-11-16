import { screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { page, person } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { mockItemBackend } from '@/test/itemBackend'
import { mockPeopleBackend } from '@/test/peopleBackend'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

function setUp(items: Parameters<typeof mockItemBackend>[0]['items']) {
  mockShell()
  mockPeopleBackend({ people: [{ id: '8000', fullName: 'An' }] })
  server.use(
    http.get('/api/registry/item-categories', () => HttpResponse.json([])),
    http.get('/api/registry/people/search', () => HttpResponse.json(page([person({ id: '8000', fullName: 'An' })]))),
  )
  return mockItemBackend({ items, placeNames: { '301': 'P301', '302': 'P302' }, ownerNames: { '8000': 'An' } })
}

async function itemsTab() {
  return within(await screen.findByRole('tabpanel', { name: 'Đồ đạc' }))
}

test('listsTheItemsThisPersonOwnsWithTheirPlaces', async () => {
  setUp([
    { id: '901', placeId: '301', name: 'Tủ lạnh', ownerPersonId: '8000' },
    { id: '902', placeId: '302', name: 'Bàn', ownerPersonId: '8000' },
    { id: '903', placeId: '301', name: 'Ghế' },
  ])
  renderApp({ route: '/w/7/people/8000?tab=items', userId: '42' })

  const tab = await itemsTab()
  expect(await tab.findByRole('link', { name: 'Tủ lạnh' })).toBeInTheDocument()
  expect(tab.getByRole('link', { name: 'Bàn' })).toBeInTheDocument()
  expect(tab.queryByRole('link', { name: 'Ghế' })).not.toBeInTheDocument()
  expect(tab.getByRole('link', { name: 'P302' })).toHaveAttribute('href', '/w/7/places/302')
})

test('emptyOwnerTabSaysSo', async () => {
  setUp([])
  renderApp({ route: '/w/7/people/8000?tab=items', userId: '42' })

  expect(await (await itemsTab()).findByText('Không có món đồ nào là của riêng người này.')).toBeInTheDocument()
})

test('changingAnOwnerRefreshesThePersonsList', async () => {
  setUp([{ id: '901', placeId: '301', name: 'Tủ lạnh' }])
  const { user, router } = renderApp({ route: '/w/7/people/8000?tab=items', userId: '42' })
  await (await itemsTab()).findByText('Không có món đồ nào là của riêng người này.')

  await router.navigate('/w/7/items/901')
  const main = within(await screen.findByRole('main'))
  await main.findByRole('heading', { name: 'Tủ lạnh' })
  await user.click(main.getByRole('button', { name: 'Thao tác khác' }))
  await user.click(await screen.findByRole('menuitem', { name: 'Đổi chủ' }))
  const dialog = within(await screen.findByRole('dialog', { name: 'Đổi chủ Tủ lạnh' }))
  await user.click(dialog.getByRole('radio', { name: 'Của một người' }))
  await user.type(dialog.getByRole('searchbox', { name: 'Tìm người' }), 'An')
  await user.click(await dialog.findByRole('radio', { name: 'An' }))
  await user.click(dialog.getByRole('button', { name: 'Lưu' }))
  expect(await screen.findByText('Đã đổi chủ')).toBeInTheDocument()

  await router.navigate('/w/7/people/8000?tab=items')
  expect(await (await itemsTab()).findByRole('link', { name: 'Tủ lạnh' })).toBeInTheDocument()
})
