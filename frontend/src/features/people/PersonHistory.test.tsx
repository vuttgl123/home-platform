import { screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { mockShell } from '@/test/handlers'
import { mockPeopleBackend } from '@/test/peopleBackend'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

test('personHistoryRestores', async () => {
  mockShell()
  const backend = mockPeopleBackend({ people: [{ id: '55', fullName: 'Nguyễn Thị An', version: 2 }] })
  const summary = (no: number) => ({
    versionNo: no,
    action: no === 1 ? 'CREATE' : 'UPDATE',
    changedFields: no === 1 ? null : ['fullName'],
    changedBy: '42',
    changedAt: '2026-11-01T03:00:00Z',
    note: null,
  })
  let sent: unknown = null
  server.use(
    http.get('/api/registry/people/55/versions', () => HttpResponse.json({ items: [summary(2), summary(1)], nextCursor: null })),
    http.get('/api/registry/people/55/versions/1', () =>
      HttpResponse.json({ ...summary(1), snapshot: { id: '55', fullName: 'Nguyễn An', phone: '0909', gender: 'FEMALE' } }),
    ),
    http.post('/api/registry/people/55/versions/1/restore', async ({ request }) => {
      sent = await request.json()
      const person = backend.people.get('55')!
      backend.people.set('55', { ...person, fullName: 'Nguyễn An', version: 3 })
      return HttpResponse.json({ ...person, fullName: 'Nguyễn An', version: 3, currentOccupancies: [] })
    }),
  )
  const { user } = renderApp({ route: '/w/7/people/55?tab=history', userId: '42' })
  const main = within(await screen.findByRole('main'))
  expect(await main.findByText('Đã đổi: Họ tên')).toBeInTheDocument()

  await user.click(main.getByRole('button', { name: 'v1' }))
  const dialog = within(await screen.findByRole('dialog', { name: 'Bản v1' }))
  expect(await dialog.findByText('0909')).toBeInTheDocument()
  expect(dialog.getByText('Nữ')).toBeInTheDocument()
  await user.click(dialog.getByRole('button', { name: 'Khôi phục bản này' }))
  await user.click(dialog.getByRole('button', { name: 'Khôi phục' }))

  expect(await main.findByRole('heading', { name: 'Nguyễn An' })).toBeInTheDocument()
  expect(sent).toEqual({ version: 2 })
})
