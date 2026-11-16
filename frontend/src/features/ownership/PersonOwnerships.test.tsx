import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { placeType, problem } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { mockOwnershipBackend } from '@/test/ownershipBackend'
import { mockPeopleBackend } from '@/test/peopleBackend'
import { mockPlaceBackend } from '@/test/placeBackend'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

const TODAY = '2026-11-16'

function setUp(ownerships: Parameters<typeof mockOwnershipBackend>[0]['ownerships'] = []) {
  mockShell({ placeTypes: [placeType()] })
  mockPlaceBackend([
    { id: '301', name: 'P301' },
    { id: '302', name: 'P302' },
  ])
  mockPeopleBackend({ today: TODAY, people: [{ id: '55', fullName: 'Nguyễn An' }] })
  return mockOwnershipBackend({
    ownerships,
    today: TODAY,
    placeNames: { '301': 'P301', '302': 'P302' },
    personNames: { '55': 'Nguyễn An' },
  })
}

async function tab() {
  return within(await screen.findByRole('tabpanel', { name: 'Sở hữu' }))
}

test('listsWhatAPersonOwnsNewestFirst', async () => {
  setUp([
    { id: '1', placeId: '301', personId: '55', startDate: '2020-01-01', endDate: '2022-01-01' },
    { id: '2', placeId: '302', personId: '55', startDate: '2024-01-01', sharePercent: 50 },
  ])
  renderApp({ route: '/w/7/people/55?tab=ownerships', userId: '42' })

  const panel = await tab()
  await panel.findByRole('link', { name: 'P302' })
  const places = panel.getAllByRole('link').map((link) => link.textContent)
  expect(places).toEqual(['P302', 'P301'])
  expect(panel.getByText('Đã thôi')).toBeInTheDocument()
  expect(panel.getByText('50%')).toBeInTheDocument()
})

test('addsAPlaceThisPersonOwns', async () => {
  const backend = setUp()
  const { user } = renderApp({ route: '/w/7/people/55?tab=ownerships', userId: '42' })

  await user.click(await (await tab()).findByRole('button', { name: 'Thêm chỗ sở hữu' }))
  const form = within(await screen.findByRole('dialog', { name: 'Thêm chỗ sở hữu' }))
  await user.type(form.getByRole('searchbox', { name: 'Tìm địa điểm' }), 'P30')
  await user.click(await form.findByRole('radio', { name: /P302/ }))
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toMatchObject({ placeId: '302', personId: '55', startDate: TODAY })
  expect(await (await tab()).findByRole('link', { name: 'P302' })).toBeInTheDocument()
})

test('deletingAnOwnerShowsWhyItIsBlocked', async () => {
  setUp([{ id: '1', placeId: '301', personId: '55' }])
  server.use(
    http.delete('/api/registry/people/55', () =>
      HttpResponse.json(problem(409, 'Person 55 still has 0 current or upcoming occupancies, 1 ownerships and 0 items'), {
        status: 409,
      }),
    ),
  )
  const { user } = renderApp({ route: '/w/7/people/55', userId: '42' })
  const main = within(await screen.findByRole('main'))
  await main.findByRole('heading', { name: 'Nguyễn An' })

  await user.click(main.getByRole('button', { name: 'Thao tác khác' }))
  await user.click(await screen.findByRole('menuitem', { name: 'Xoá' }))
  const dialog = within(await screen.findByRole('alertdialog'))
  await user.click(dialog.getByRole('button', { name: 'Xoá' }))

  expect(await dialog.findByText('Person 55 still has 0 current or upcoming occupancies, 1 ownerships and 0 items')).toBeInTheDocument()
})
