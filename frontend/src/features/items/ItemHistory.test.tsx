import { screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { itemCategory, itemDetail, personDetail, placeDetail, placeType } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { mockItemBackend } from '@/test/itemBackend'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

test('itemHistoryRestoresContentOnly', async () => {
  mockShell({ placeTypes: [placeType()] })
  server.use(http.get('/api/registry/item-categories', () => HttpResponse.json([itemCategory({ id: '600', name: 'Nội thất' })])))
  // Món đã gỡ: khôi phục nội dung không đưa nó lại.
  const backend = mockItemBackend({
    items: [{ id: '900', name: 'Tủ lạnh hỏng', removedAt: '2026-03-01', status: 'REMOVED', version: 3 }],
  })
  const summary = (no: number) => ({
    versionNo: no,
    action: 'UPDATE',
    changedFields: ['name'],
    changedBy: '42',
    changedAt: '2026-11-01T03:00:00Z',
    note: null,
  })
  server.use(
    http.get('/api/registry/items/900/versions', () => HttpResponse.json({ items: [summary(3), summary(2)], nextCursor: null })),
    http.get('/api/registry/items/900/versions/2', () =>
      HttpResponse.json({ ...summary(2), snapshot: { id: '900', name: 'Tủ lạnh', categoryId: '600', quantity: 2, unit: 'cái', status: 'IN_USE' } }),
    ),
    http.post('/api/registry/items/900/versions/2/restore', () => {
      const item = backend.items.get('900')!
      const restored = { ...item, name: 'Tủ lạnh', version: 4 }
      backend.items.set('900', restored)
      return HttpResponse.json(restored)
    }),
  )
  const { user } = renderApp({ route: '/w/7/items/900?tab=history', userId: '42' })
  const main = within(await screen.findByRole('main'))

  await user.click(await main.findByRole('button', { name: 'v2' }))
  const dialog = within(await screen.findByRole('dialog', { name: 'Bản v2' }))
  expect(await dialog.findByText('2 cái')).toBeInTheDocument()
  expect(dialog.getByText('Nội thất')).toBeInTheDocument()
  await user.click(dialog.getByRole('button', { name: 'Khôi phục bản này' }))
  await user.click(dialog.getByRole('button', { name: 'Khôi phục' }))

  expect(await main.findByRole('heading', { name: 'Tủ lạnh' })).toBeInTheDocument()
  expect(main.getByText('Đã gỡ ngày 01/03/2026.')).toBeInTheDocument()
})

test('ownerChangeIsLabelled', async () => {
  mockShell({ placeTypes: [placeType()] })
  server.use(http.get('/api/registry/item-categories', () => HttpResponse.json([])))
  mockItemBackend({ items: [{ id: '900', name: 'Tủ lạnh', version: 2 }] })
  server.use(
    http.get('/api/registry/items/900/versions', () =>
      HttpResponse.json({
        items: [
          { versionNo: 2, action: 'UPDATE', changedFields: ['ownerPersonId'], changedBy: '42', changedAt: '2026-11-01T03:00:00Z', note: null },
        ],
        nextCursor: null,
      }),
    ),
  )
  renderApp({ route: '/w/7/items/900?tab=history', userId: '42' })

  expect(await screen.findByText('Đã đổi: Chủ')).toBeInTheDocument()
})

test('comparingShowsAnOwnerChange', async () => {
  mockShell({ placeTypes: [placeType()] })
  server.use(http.get('/api/registry/item-categories', () => HttpResponse.json([])))
  mockItemBackend({ items: [{ id: '900', name: 'Tủ lạnh', version: 2 }] })
  const summary = (no: number) => ({
    versionNo: no,
    action: no === 1 ? 'CREATE' : 'UPDATE',
    changedFields: no === 1 ? null : ['ownerPersonId'],
    changedBy: '42',
    changedAt: '2026-11-01T03:00:00Z',
    note: null,
  })
  const snapshot = (owner: string) => ({ id: '900', name: 'Tủ lạnh', placeId: '1', ownerPersonId: owner, status: 'IN_USE' })
  server.use(
    http.get('/api/registry/items/900/versions', () => HttpResponse.json({ items: [summary(2), summary(1)], nextCursor: null })),
    http.get('/api/registry/items/900/versions/1', () => HttpResponse.json({ ...summary(1), snapshot: snapshot('55') })),
    http.get('/api/registry/items/900/versions/2', () => HttpResponse.json({ ...summary(2), snapshot: snapshot('56') })),
    http.get('/api/registry/people/:id', ({ params }) =>
      HttpResponse.json(personDetail({ id: params.id as string, fullName: params.id === '55' ? 'Nguyễn An' : 'Trần Bình' })),
    ),
    http.get('/api/registry/places/1', () => HttpResponse.json(placeDetail({ id: '1', name: 'Toà A' }))),
  )
  const { user } = renderApp({ route: '/w/7/items/900?tab=history', userId: '42' })
  const main = within(await screen.findByRole('main'))
  await user.click(await main.findByRole('checkbox', { name: 'Chọn v1 để so sánh' }))
  await user.click(main.getByRole('checkbox', { name: 'Chọn v2 để so sánh' }))

  await user.click(main.getByRole('button', { name: 'So sánh' }))

  const dialog = within(await screen.findByRole('dialog', { name: 'So sánh v1 và v2' }))
  await dialog.findByText('Trần Bình')
  // Chỉ chủ đổi; chỗ đặt giống nhau nên không hiện.
  expect(
    dialog
      .getAllByRole('row')
      .slice(1)
      .map((row) => within(row).getAllByRole('cell').map((cell) => cell.textContent)),
  ).toEqual([['Chủ', 'Nguyễn An', 'Trần Bình']])
})

test('comparingTwoRemovalsShowsTheRemovalDate', async () => {
  mockShell({ placeTypes: [placeType()] })
  server.use(http.get('/api/registry/item-categories', () => HttpResponse.json([])))
  mockItemBackend({ items: [{ id: '900', name: 'Tủ lạnh', removedAt: '2026-03-05', status: 'REMOVED', version: 4 }] })
  const summary = (no: number) => ({
    versionNo: no,
    action: 'UPDATE',
    changedFields: no === 2 ? ['status', 'removedAt'] : ['removedAt', 'replacedByItemId'],
    changedBy: '42',
    changedAt: '2026-11-01T03:00:00Z',
    note: null,
  })
  // Gỡ nhầm ngày (v2), đưa lại (v3), gỡ lại đúng ngày và thay bằng món mới (v4): trạng thái hai bản đều là "Đã gỡ".
  const snapshot = (removedAt: string, replacedByItemId: string | null) => ({
    id: '900',
    name: 'Tủ lạnh',
    status: 'REMOVED',
    removedAt,
    replacedByItemId,
  })
  server.use(
    http.get('/api/registry/items/900/versions', () => HttpResponse.json({ items: [summary(4), summary(2)], nextCursor: null })),
    http.get('/api/registry/items/900/versions/2', () => HttpResponse.json({ ...summary(2), snapshot: snapshot('2026-03-01', null) })),
    http.get('/api/registry/items/900/versions/4', () => HttpResponse.json({ ...summary(4), snapshot: snapshot('2026-03-05', '905') })),
    http.get('/api/registry/items/905', () => HttpResponse.json(itemDetail({ id: '905', name: 'Tủ lạnh mới' }))),
  )
  const { user } = renderApp({ route: '/w/7/items/900?tab=history', userId: '42' })
  const main = within(await screen.findByRole('main'))
  await user.click(await main.findByRole('checkbox', { name: 'Chọn v4 để so sánh' }))
  await user.click(main.getByRole('checkbox', { name: 'Chọn v2 để so sánh' }))

  await user.click(main.getByRole('button', { name: 'So sánh' }))

  const dialog = within(await screen.findByRole('dialog', { name: 'So sánh v2 và v4' }))
  await dialog.findByText('Tủ lạnh mới')
  expect(
    dialog
      .getAllByRole('row')
      .slice(1)
      .map((row) => within(row).getAllByRole('cell').map((cell) => cell.textContent)),
  ).toEqual([
    ['Ngày gỡ', '01/03/2026', '05/03/2026'],
    ['Món thay', '—', 'Tủ lạnh mới'],
  ])
})
