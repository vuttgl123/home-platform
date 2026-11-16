import { screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { itemCategory, placeType } from '@/test/fixtures'
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
