import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { MemoryRouter, Route, Routes } from 'react-router'
import { expect, test } from 'vitest'

import { setCurrentUser } from '@/api'
import { page } from '@/test/fixtures'
import { mockShell, recordRequests } from '@/test/handlers'
import { mockPeopleBackend } from '@/test/peopleBackend'
import { renderApp, renderWithProviders } from '@/test/render'
import { server } from '@/test/server'

import { PersonPage } from './pages/PersonPage'

async function main() {
  return within(await screen.findByRole('main'))
}

const an = {
  id: '55',
  fullName: 'Nguyễn An',
  gender: 'FEMALE' as const,
  dateOfBirth: '1990-05-02',
  phone: '0909',
  email: 'an@example.com',
  idType: 'CCCD',
  idNumber: '001',
  note: 'Thuê dài hạn',
}

test('showsHeaderAndInfo', async () => {
  mockShell()
  mockPeopleBackend({
    people: [an],
    occupancies: [{ id: '1', personId: '55', placeId: '301', placeName: 'P301', startDate: '2026-01-01' }],
  })

  renderApp({ route: '/w/7/people/55', userId: '42' })

  const view = await main()
  expect(await view.findByRole('heading', { name: 'Nguyễn An' })).toBeInTheDocument()
  expect(view.getByText('Nữ')).toBeInTheDocument()
  expect(view.getByText('02/05/1990')).toBeInTheDocument()
  expect(view.getByText('0909')).toBeInTheDocument()
  expect(view.getByText('an@example.com')).toBeInTheDocument()
  expect(view.getByText('CCCD 001')).toBeInTheDocument()
  expect(view.getByText('Thuê dài hạn')).toBeInTheDocument()
  // Nơi đang ở hiện ngay ở phần thông tin.
  expect(view.getByRole('link', { name: 'P301' })).toHaveAttribute('href', '/w/7/places/301')
})

test('placesTabListsEveryStayWithStatus', async () => {
  mockShell()
  mockPeopleBackend({
    people: [an],
    occupancies: [
      { id: '1', personId: '55', placeId: '301', placeName: 'P301', startDate: '2026-01-01' },
      { id: '2', personId: '55', placeId: '205', placeName: 'P205', relation: 'GUEST', startDate: '2025-01-01', endDate: '2025-06-30' },
    ],
  })

  renderApp({ route: '/w/7/people/55?tab=places', userId: '42' })

  const view = await main()
  const rows = await view.findAllByRole('row')
  // Hàng đầu là tiêu đề; mới nhất trước.
  expect(rows.slice(1).map((row) => within(row).getAllByRole('cell')[0].textContent)).toEqual(['P301', 'P205'])
  expect(within(rows[1]).getByText('Đang ở')).toBeInTheDocument()
  expect(within(rows[2]).getByText('Đã rời')).toBeInTheDocument()
  expect(within(rows[2]).getByText('Khách')).toBeInTheDocument()
  expect(within(rows[2]).getByText('30/06/2025')).toBeInTheDocument()
})

test('editRenamesTheHeader', async () => {
  mockShell()
  mockPeopleBackend({ people: [an] })
  const { user } = renderApp({ route: '/w/7/people/55', userId: '42' })

  await user.click(await (await main()).findByRole('button', { name: 'Sửa' }))
  const form = within(await screen.findByRole('dialog', { name: 'Sửa hồ sơ' }))
  const name = await form.findByRole('textbox', { name: 'Họ tên' })
  await user.clear(name)
  await user.type(name, 'Nguyễn Thị An')
  await user.click(form.getByRole('button', { name: 'Lưu' }))

  expect(await (await main()).findByRole('heading', { name: 'Nguyễn Thị An' })).toBeInTheDocument()
})

test('deleteIsBlockedWhileTheyLiveSomewhere', async () => {
  mockShell()
  mockPeopleBackend({
    people: [an],
    occupancies: [{ id: '1', personId: '55', placeId: '301', startDate: '2026-01-01' }],
  })
  const { user, router } = renderApp({ route: '/w/7/people/55', userId: '42' })

  await user.click(await (await main()).findByRole('button', { name: 'Thao tác khác' }))
  await user.click(await screen.findByRole('menuitem', { name: 'Xoá' }))
  const dialog = within(await screen.findByRole('alertdialog', { name: 'Xoá Nguyễn An?' }))
  await user.click(dialog.getByRole('button', { name: 'Xoá' }))

  expect(await dialog.findByText('Person 55 still has 1 current or upcoming occupancies')).toBeInTheDocument()
  expect(router.state.location.pathname).toBe('/w/7/people/55')
})

test('deleteReturnsToThePeopleList', async () => {
  mockShell()
  const backend = mockPeopleBackend({ people: [an] })
  const { user, router } = renderApp({ route: '/w/7/people/55', userId: '42' })
  await user.click(await (await main()).findByRole('button', { name: 'Thao tác khác' }))
  await user.click(await screen.findByRole('menuitem', { name: 'Xoá' }))
  const dialog = within(await screen.findByRole('alertdialog', { name: 'Xoá Nguyễn An?' }))
  const requests = recordRequests()

  await user.click(dialog.getByRole('button', { name: 'Xoá' }))

  await waitFor(() => expect(router.state.location.pathname).toBe('/w/7/people'))
  expect(backend.people.has('55')).toBe(false)
  expect(requests.filter((request) => request === 'GET /api/registry/people/55')).toEqual([])
})

test('unknownPersonShowsNotFound', async () => {
  mockShell()
  mockPeopleBackend({})

  renderApp({ route: '/w/7/people/999', userId: '42' })

  expect(await (await main()).findByText('Không tìm thấy')).toBeInTheDocument()
})

test('pageWithoutSlotsHasNoItemsTab', async () => {
  setCurrentUser('42')
  mockPeopleBackend({ people: [an] })
  // Trang được render rời, không qua khung ứng dụng (mockShell): mục tuỳ biến của người phải tự có.
  server.use(http.get('/api/registry/field-defs', () => HttpResponse.json([])))
  renderWithProviders(
    <MemoryRouter initialEntries={['/w/7/people/55']}>
      <Routes>
        <Route path="/w/:workspaceId/people/:personId" element={<PersonPage />} />
      </Routes>
    </MemoryRouter>,
  )

  expect(await screen.findByRole('heading', { name: 'Nguyễn An' })).toBeInTheDocument()
  expect(screen.queryByRole('tab', { name: 'Đồ đạc' })).not.toBeInTheDocument()
  expect(screen.queryByRole('tab', { name: 'Sở hữu' })).not.toBeInTheDocument()
})

test('itemsTabIsOnTheUrl', async () => {
  mockShell()
  mockPeopleBackend({ people: [an] })
  server.use(http.get('/api/registry/items', () => HttpResponse.json(page([]))))
  const { user, router } = renderApp({ route: '/w/7/people/55', userId: '42' })

  await user.click(await (await main()).findByRole('tab', { name: 'Đồ đạc' }))

  await waitFor(() => expect(router.state.location.search).toBe('?tab=items'))
})

test('undoRestoresTheDeletedPerson', async () => {
  mockShell()
  const backend = mockPeopleBackend({ people: [an] })
  const { user, router } = renderApp({ route: '/w/7/people/55', userId: '42' })
  await user.click(await (await main()).findByRole('button', { name: 'Thao tác khác' }))
  await user.click(await screen.findByRole('menuitem', { name: 'Xoá' }))
  await user.click(within(await screen.findByRole('alertdialog', { name: 'Xoá Nguyễn An?' })).getByRole('button', { name: 'Xoá' }))
  await waitFor(() => expect(router.state.location.pathname).toBe('/w/7/people'))

  await user.click(await screen.findByRole('button', { name: 'Hoàn tác' }))

  await waitFor(() => expect(router.state.location.pathname).toBe('/w/7/people/55'))
  expect(backend.people.has('55')).toBe(true)
  expect(await (await main()).findByRole('heading', { name: 'Nguyễn An' })).toBeInTheDocument()
})
