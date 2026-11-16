import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { todayInBusinessZone } from '@/lib/dates'
import { formatDate } from '@/lib/format'
import { itemCategory, placeType } from '@/test/fixtures'
import { mockShell, recordRequests } from '@/test/handlers'
import { mockItemBackend } from '@/test/itemBackend'
import { mockPlaceBackend } from '@/test/placeBackend'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

const TODAY = todayInBusinessZone()

function setUp(items: Parameters<typeof mockItemBackend>[0]['items']) {
  mockShell({ placeTypes: [placeType()] })
  mockPlaceBackend([
    { id: '1', name: 'Toà A' },
    { id: '301', parentId: '1', name: 'P301' },
    { id: '302', parentId: '1', name: 'P302' },
  ])
  server.use(http.get('/api/registry/item-categories', () => HttpResponse.json([itemCategory({ id: '600', name: 'Nội thất' })])))
  return mockItemBackend({
    items,
    today: TODAY,
    placeNames: { '1': 'Toà A', '301': 'P301', '302': 'P302' },
    categoryNames: { '600': 'Nội thất' },
  })
}

async function main() {
  return within(await screen.findByRole('main'))
}

async function openMenuItem(user: ReturnType<typeof renderApp>['user'], name: string) {
  await user.click(await (await main()).findByRole('button', { name: 'Thao tác khác' }))
  await user.click(await screen.findByRole('menuitem', { name }))
}

const fridge = { id: '900', placeId: '301', name: 'Tủ lạnh', categoryId: '600', quantity: 1, unit: 'cái', placedAt: '2026-01-10' }

test('showsHeaderWithPlaceLink', async () => {
  setUp([fridge])

  renderApp({ route: '/w/7/items/900', userId: '42' })

  const view = await main()
  expect(await view.findByRole('heading', { name: 'Tủ lạnh' })).toBeInTheDocument()
  // Từ trang món đồ, liên kết mở thẳng tab Đồ đạc của địa điểm.
  expect(view.getByRole('link', { name: 'P301' })).toHaveAttribute('href', '/w/7/places/301?tab=items')
  expect(view.getByText('Nội thất')).toBeInTheDocument()
  expect(view.getByText('Đang dùng')).toBeInTheDocument()
  expect(view.getByText('1 cái')).toBeInTheDocument()
  expect(view.getByText('10/01/2026')).toBeInTheDocument()
})

test('movesToAnotherPlace', async () => {
  const backend = setUp([fridge])
  const { user } = renderApp({ route: '/w/7/items/900', userId: '42' })

  await openMenuItem(user, 'Chuyển chỗ')
  const dialog = within(await screen.findByRole('dialog', { name: 'Chuyển Tủ lạnh' }))
  await user.type(dialog.getByRole('searchbox', { name: 'Tìm địa điểm' }), 'P30')
  expect(await dialog.findByRole('radio', { name: 'P302' })).toBeInTheDocument()
  // Chỗ hiện tại không được gợi ý.
  expect(dialog.queryByRole('radio', { name: 'P301' })).not.toBeInTheDocument()
  await user.click(dialog.getByRole('radio', { name: 'P302' }))
  await user.click(dialog.getByRole('button', { name: 'Chuyển' }))

  expect(await (await main()).findByRole('link', { name: 'P302' })).toBeInTheDocument()
  expect(backend.writes).toEqual([{ method: 'POST', path: '/items/900/move', body: { version: 1, placeId: '302' } }])
})

test('removesWithADate', async () => {
  const backend = setUp([fridge])
  const { user } = renderApp({ route: '/w/7/items/900', userId: '42' })

  await openMenuItem(user, 'Gỡ')
  const dialog = within(await screen.findByRole('dialog', { name: 'Gỡ Tủ lạnh' }))
  expect(dialog.getByLabelText('Ngày gỡ')).toHaveValue(TODAY)
  await user.click(dialog.getByRole('button', { name: 'Gỡ' }))

  expect(await (await main()).findByText(`Đã gỡ ngày ${formatDate(TODAY)}.`)).toBeInTheDocument()
  expect(backend.writes[0]).toEqual({ method: 'POST', path: '/items/900/remove', body: { version: 1, removedAt: TODAY } })
})

test('removeDateCannotPrecedePlacement', async () => {
  const backend = setUp([fridge])
  const { user } = renderApp({ route: '/w/7/items/900', userId: '42' })
  await openMenuItem(user, 'Gỡ')
  const dialog = within(await screen.findByRole('dialog', { name: 'Gỡ Tủ lạnh' }))

  const date = dialog.getByLabelText('Ngày gỡ')
  await user.clear(date)
  await user.type(date, '2026-01-01')
  await user.click(dialog.getByRole('button', { name: 'Gỡ' }))

  expect(await dialog.findByText('Không được trước ngày đặt (10/01/2026)')).toBeInTheDocument()
  expect(backend.writes).toEqual([])
})

test('removedItemIsReadOnly', async () => {
  setUp([{ ...fridge, removedAt: '2026-03-01', status: 'REMOVED' }])
  const { user } = renderApp({ route: '/w/7/items/900', userId: '42' })
  const view = await main()

  expect(await view.findByText('Đã gỡ ngày 01/03/2026.')).toBeInTheDocument()
  expect(view.queryByRole('button', { name: 'Sửa' })).not.toBeInTheDocument()
  expect(view.getByRole('button', { name: 'Đưa lại' })).toBeEnabled()
  await user.click(view.getByRole('button', { name: 'Thao tác khác' }))
  const items = await screen.findAllByRole('menuitem')
  expect(items.map((item) => item.textContent)).toEqual(['Xoá'])
})

test('replaceMovesTheOldOneToRemovedAndLinksTheNewOne', async () => {
  const backend = setUp([fridge])
  const { user, router } = renderApp({ route: '/w/7/items/900', userId: '42' })

  await openMenuItem(user, 'Thay')
  const dialog = within(await screen.findByRole('dialog', { name: 'Thay Tủ lạnh' }))
  const name = dialog.getByRole('textbox', { name: 'Tên' })
  expect(name).toHaveValue('Tủ lạnh')
  await user.clear(name)
  await user.type(name, 'Tủ lạnh mới')
  await waitFor(() => expect(dialog.getByRole('button', { name: 'Thay' })).toBeEnabled())
  await user.click(dialog.getByRole('button', { name: 'Thay' }))

  await waitFor(() => expect(router.state.location.pathname).toBe('/w/7/items/9500'))
  expect(await (await main()).findByRole('heading', { name: 'Tủ lạnh mới' })).toBeInTheDocument()
  expect(backend.writes[0].body).toMatchObject({
    version: 1,
    removedAt: TODAY,
    replacement: { name: 'Tủ lạnh mới', categoryId: '600', quantity: 1, unit: 'cái' },
  })
  // Món cũ: đã gỡ, có liên kết tới món mới.
  await router.navigate('/w/7/items/900')
  const old = await main()
  expect(await old.findByRole('link', { name: 'Tủ lạnh mới' })).toHaveAttribute('href', '/w/7/items/9500')
  expect(old.getByRole('button', { name: 'Đưa lại' })).toBeDisabled()
})

test('replaceFormHasNoCodeBox', async () => {
  setUp([{ ...fridge, code: 'TS-000001' }])
  const { user } = renderApp({ route: '/w/7/items/900', userId: '42' })

  await openMenuItem(user, 'Thay')
  const dialog = within(await screen.findByRole('dialog', { name: 'Thay Tủ lạnh' }))

  expect(dialog.queryByRole('textbox', { name: 'Mã' })).not.toBeInTheDocument()
})

test('replaceAtAnotherPlace', async () => {
  const backend = setUp([fridge])
  const { user } = renderApp({ route: '/w/7/items/900', userId: '42' })
  await openMenuItem(user, 'Thay')
  const dialog = within(await screen.findByRole('dialog', { name: 'Thay Tủ lạnh' }))

  await user.click(dialog.getByRole('switch', { name: 'Đặt món mới ở chỗ khác' }))
  await user.type(dialog.getByRole('searchbox', { name: 'Tìm địa điểm' }), 'P302')
  await user.click(await dialog.findByRole('radio', { name: 'P302' }))
  await waitFor(() => expect(dialog.getByRole('button', { name: 'Thay' })).toBeEnabled())
  await user.click(dialog.getByRole('button', { name: 'Thay' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toMatchObject({ replacement: { placeId: '302' } })
})

test('putBackIsBlockedWhileTheReplacementExists', async () => {
  setUp([
    { ...fridge, removedAt: '2026-03-01', status: 'REMOVED', replacedByItemId: '901' },
    { id: '901', placeId: '301', name: 'Tủ lạnh mới' },
  ])

  renderApp({ route: '/w/7/items/900', userId: '42' })

  const view = await main()
  expect(await view.findByRole('link', { name: 'Tủ lạnh mới' })).toBeInTheDocument()
  expect(view.getByRole('button', { name: 'Đưa lại' })).toBeDisabled()
  expect(view.getByText('Món thay nó vẫn còn, nên chưa đưa lại được.')).toBeInTheDocument()
})

test('putBackWorksOnceTheReplacementIsDeleted', async () => {
  const backend = setUp([{ ...fridge, removedAt: '2026-03-01', status: 'REMOVED', replacedByItemId: '901' }])
  const { user } = renderApp({ route: '/w/7/items/900', userId: '42' })
  const view = await main()
  await waitFor(() => expect(view.getByRole('button', { name: 'Đưa lại' })).toBeEnabled())

  await user.click(view.getByRole('button', { name: 'Đưa lại' }))
  const dialog = within(await screen.findByRole('alertdialog', { name: 'Đưa Tủ lạnh lại?' }))
  await user.click(dialog.getByRole('button', { name: 'Đưa lại' }))

  expect(await (await main()).findByRole('button', { name: 'Sửa' })).toBeInTheDocument()
  expect(backend.writes[0]).toEqual({ method: 'POST', path: '/items/900/put-back', body: { version: 1 } })
})

test('deleteReturnsToThePlace', async () => {
  const backend = setUp([fridge])
  const { user, router } = renderApp({ route: '/w/7/items/900', userId: '42' })
  await openMenuItem(user, 'Xoá')
  const dialog = within(await screen.findByRole('alertdialog', { name: 'Xoá Tủ lạnh?' }))
  const requests = recordRequests()

  await user.click(dialog.getByRole('button', { name: 'Xoá' }))

  await waitFor(() => expect(router.state.location.pathname).toBe('/w/7/places/301'))
  expect(router.state.location.search).toBe('?tab=items')
  expect(backend.items.has('900')).toBe(false)
  expect(requests.filter((request) => request === 'GET /api/registry/items/900')).toEqual([])
})

test('versionConflictOffersReload', async () => {
  const backend = setUp([fridge])
  const { user } = renderApp({ route: '/w/7/items/900', userId: '42' })
  await (await main()).findByRole('heading', { name: 'Tủ lạnh' })
  backend.editElsewhere('900')
  await openMenuItem(user, 'Gỡ')
  const dialog = within(await screen.findByRole('dialog', { name: 'Gỡ Tủ lạnh' }))

  await user.click(dialog.getByRole('button', { name: 'Gỡ' }))
  await user.click(await dialog.findByRole('button', { name: 'Tải bản mới' }))
  await user.click(await dialog.findByRole('button', { name: 'Gỡ' }))

  await waitFor(() => expect(backend.items.get('900')?.removedAt).toBe(TODAY))
  expect(backend.writes.map((write) => (write.body as { version: number }).version)).toEqual([1, 2])
})

test('unknownItemShowsNotFound', async () => {
  setUp([])

  renderApp({ route: '/w/7/items/999', userId: '42' })

  expect(await (await main()).findByText('Không tìm thấy')).toBeInTheDocument()
})

test('reissuesAnItemCode', async () => {
  const backend = setUp([{ ...fridge, code: 'TS-000007' }])
  const { user } = renderApp({ route: '/w/7/items/900', userId: '42' })

  await openMenuItem(user, 'Cấp mã mới')
  const dialog = within(await screen.findByRole('alertdialog', { name: 'Cấp mã mới cho Tủ lạnh?' }))
  expect(dialog.getByText(/Mã hiện tại TS-000007/)).toBeInTheDocument()
  await user.click(dialog.getByRole('button', { name: 'Cấp mã mới' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0]).toMatchObject({ path: '/items/900/reissue-code', body: { version: 1 } })
  expect(await screen.findByText('Mã mới: TS-000001')).toBeInTheDocument()
})

test('removedItemHasNoReissueAction', async () => {
  setUp([{ ...fridge, removedAt: '2026-03-01', status: 'REMOVED' }])
  const { user } = renderApp({ route: '/w/7/items/900', userId: '42' })

  await user.click(await (await main()).findByRole('button', { name: 'Thao tác khác' }))
  await screen.findByRole('menuitem', { name: 'Xoá' })
  expect(screen.queryByRole('menuitem', { name: /Cấp mã/ })).not.toBeInTheDocument()
})
