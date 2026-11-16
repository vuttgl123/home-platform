import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { todayInBusinessZone } from '@/lib/dates'
import { formatDate } from '@/lib/format'
import { placeType, problem } from '@/test/fixtures'
import { mockShell, recordRequests } from '@/test/handlers'
import { mockPeopleBackend } from '@/test/peopleBackend'
import { mockPlaceBackend } from '@/test/placeBackend'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

const TODAY = todayInBusinessZone()
const LATER = '2999-01-01'

function setUp(occupancies: Parameters<typeof mockPeopleBackend>[0]['occupancies'] = []) {
  mockShell({ placeTypes: [placeType()] })
  mockPlaceBackend([
    { id: '1', name: 'Toà A' },
    { id: '13', parentId: '1', name: 'Tầng 3' },
    { id: '301', parentId: '13', name: 'P301' },
  ])
  return mockPeopleBackend({
    today: TODAY,
    people: [
      { id: '55', fullName: 'Nguyễn An' },
      { id: '56', fullName: 'Trần Bình' },
      { id: '57', fullName: 'Lê Châu' },
    ],
    occupancies,
    placesUnder: { '1': ['13', '301'], '13': ['301'] },
    placeNames: { '1': 'Toà A', '13': 'Tầng 3', '301': 'P301' },
  })
}

async function main() {
  return within(await screen.findByRole('main'))
}

/** Bảng của một nhóm ("Đang ở", "Sắp đến", "Đã rời", "Đang ở bên trong"). */
async function group(name: string) {
  return within(await (await main()).findByRole('region', { name }))
}

async function openAdd(placeId = '301') {
  const app = renderApp({ route: `/w/7/places/${placeId}?tab=people`, userId: '42' })
  await app.user.click(await (await main()).findByRole('button', { name: 'Thêm người ở' }))
  return { ...app, form: within(await screen.findByRole('dialog', { name: 'Thêm người ở' })) }
}

test('groupsOccupantsByStatus', async () => {
  setUp([
    { id: '1', personId: '55', placeId: '301', startDate: '2026-01-01' },
    { id: '2', personId: '56', placeId: '301', startDate: LATER },
    { id: '3', personId: '57', placeId: '301', startDate: '2020-01-01', endDate: '2020-12-31' },
  ])
  const { user } = renderApp({ route: '/w/7/places/301?tab=people', userId: '42' })

  expect(await (await group('Đang ở')).findByRole('link', { name: 'Nguyễn An' })).toBeInTheDocument()
  expect(await (await group('Sắp đến')).findByRole('link', { name: 'Trần Bình' })).toBeInTheDocument()
  await user.click((await main()).getByRole('button', { name: /Đã rời/ }))
  expect(await (await group('Đã rời')).findByRole('link', { name: 'Lê Châu' })).toBeInTheDocument()
})

test('pastGroupLoadsOnlyWhenOpened', async () => {
  setUp()
  const requests: string[] = []
  server.events.on('request:start', ({ request }) => {
    const url = new URL(request.url)
    if (url.pathname.endsWith('/occupancies')) requests.push(url.searchParams.get('status') ?? '')
  })
  const { user } = renderApp({ route: '/w/7/places/301?tab=people', userId: '42' })
  await (await group('Sắp đến')).findByText('Không có ai sắp đến.')

  expect(requests.sort()).toEqual(['CURRENT', 'UPCOMING'])
  await user.click((await main()).getByRole('button', { name: /Đã rời/ }))
  await waitFor(() => expect(requests).toContain('PAST'))
})

test('wholeBuildingShowsWhereEachPersonLives', async () => {
  setUp([{ id: '1', personId: '55', placeId: '301', startDate: '2026-01-01' }])
  const { user } = renderApp({ route: '/w/7/places/1?tab=people', userId: '42' })

  await user.click(await (await main()).findByRole('switch', { name: 'Cả bên trong' }))

  const inside = await group('Đang ở bên trong')
  const row = (await inside.findByRole('link', { name: 'Nguyễn An' })).closest('tr') as HTMLElement
  expect(within(row).getByRole('link', { name: 'P301' })).toHaveAttribute('href', '/w/7/places/301')
})

test('addsAnExistingPerson', async () => {
  const backend = setUp()
  const { user, form } = await openAdd()

  await user.type(form.getByRole('searchbox', { name: 'Tìm người' }), 'Nguyễn')
  await user.click(await form.findByRole('radio', { name: 'Nguyễn An' }))
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  expect(await (await group('Đang ở')).findByRole('link', { name: 'Nguyễn An' })).toBeInTheDocument()
  expect(backend.writes).toEqual([
    { method: 'POST', path: '/occupancies', body: { placeId: '301', personId: '55', relation: 'RESIDENT', startDate: TODAY } },
  ])
})

test('addsANewPersonInOneGo', async () => {
  const backend = setUp()
  const { user, form } = await openAdd()

  await user.type(form.getByRole('searchbox', { name: 'Tìm người' }), 'Phạm Mới')
  await user.click(await form.findByRole('button', { name: 'Thêm người mới “Phạm Mới”' }))
  await user.selectOptions(form.getByRole('combobox', { name: 'Quan hệ' }), 'Chủ')
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  expect(await (await group('Đang ở')).findByRole('link', { name: 'Phạm Mới' })).toBeInTheDocument()
  expect(backend.writes.map((write) => [write.path, write.body])).toEqual([
    ['/people', { fullName: 'Phạm Mới' }],
    ['/occupancies', { placeId: '301', personId: '8000', relation: 'OWNER', startDate: TODAY }],
  ])
})

test('retryAfterOccupancyFailureReusesTheNewPerson', async () => {
  const backend = setUp()
  server.use(
    http.post('/api/registry/occupancies', () => HttpResponse.json(problem(400, 'Invalid data'), { status: 400 }), {
      once: true,
    }),
  )
  const { user, form } = await openAdd()
  await user.type(form.getByRole('searchbox', { name: 'Tìm người' }), 'Phạm Mới')
  await user.click(await form.findByRole('button', { name: 'Thêm người mới “Phạm Mới”' }))
  await user.click(form.getByRole('button', { name: 'Thêm' }))
  expect(await form.findByText('Invalid data')).toBeInTheDocument()

  await user.click(form.getByRole('button', { name: 'Thêm' }))

  expect(await (await group('Đang ở')).findByRole('link', { name: 'Phạm Mới' })).toBeInTheDocument()
  expect(backend.writes.filter((write) => write.path === '/people')).toHaveLength(1)
})

test('addingAnOccupantRefreshesEveryView', async () => {
  setUp()
  const { user, form } = await openAdd('301')
  await user.type(form.getByRole('searchbox', { name: 'Tìm người' }), 'Nguyễn')
  await user.click(await form.findByRole('radio', { name: 'Nguyễn An' }))
  await user.click(form.getByRole('button', { name: 'Thêm' }))
  await user.click(await (await group('Đang ở')).findByRole('link', { name: 'Nguyễn An' }))

  // Trang của người đó: "Đang ở" ở phần thông tin, và dòng mới ở tab Nơi ở.
  expect(await (await main()).findByRole('link', { name: 'P301' })).toBeInTheDocument()
  await user.click((await main()).getByRole('tab', { name: 'Nơi ở' }))
  const rows = await (await main()).findAllByRole('row')
  expect(within(rows[1]).getByText(formatDate(TODAY))).toBeInTheDocument()
})

test('endsAnOccupancy', async () => {
  const backend = setUp([{ id: '1', personId: '55', placeId: '301', startDate: '2026-01-01' }])
  const { user } = renderApp({ route: '/w/7/places/301?tab=people', userId: '42' })
  const current = await group('Đang ở')

  await user.click(await current.findByRole('button', { name: 'Thao tác với Nguyễn An' }))
  await user.click(await screen.findByRole('menuitem', { name: 'Chuyển đi' }))
  const dialog = within(await screen.findByRole('dialog', { name: 'Nguyễn An chuyển đi' }))
  expect(dialog.getByLabelText('Ngày chuyển đi')).toHaveValue(TODAY)
  await user.click(dialog.getByRole('button', { name: 'Chuyển đi' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0]).toEqual({ method: 'POST', path: '/occupancies/1/end', body: { version: 1, endDate: TODAY } })
  expect(await current.findByText(formatDate(TODAY))).toBeInTheDocument()
})

test('endDateCannotPrecedeStart', async () => {
  const backend = setUp([{ id: '1', personId: '55', placeId: '301', startDate: '2026-01-01' }])
  const { user } = renderApp({ route: '/w/7/places/301?tab=people', userId: '42' })
  await user.click(await (await group('Đang ở')).findByRole('button', { name: 'Thao tác với Nguyễn An' }))
  await user.click(await screen.findByRole('menuitem', { name: 'Chuyển đi' }))
  const dialog = within(await screen.findByRole('dialog', { name: 'Nguyễn An chuyển đi' }))

  const date = dialog.getByLabelText('Ngày chuyển đi')
  await user.clear(date)
  await user.type(date, '2025-12-31')
  await user.click(dialog.getByRole('button', { name: 'Chuyển đi' }))

  expect(await dialog.findByText('Không được trước ngày bắt đầu (01/01/2026)')).toBeInTheDocument()
  expect(backend.writes).toEqual([])
})

test('overlapConflictIsShown', async () => {
  setUp([{ id: '1', personId: '55', placeId: '301', startDate: '2026-01-01' }])
  const { user, form } = await openAdd()

  await user.type(form.getByRole('searchbox', { name: 'Tìm người' }), 'Nguyễn')
  await user.click(await form.findByRole('radio', { name: 'Nguyễn An' }))
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  expect(await form.findByText('Person 55 already has an overlapping occupancy at this place')).toBeInTheDocument()
})

test('deletesAMistakenRow', async () => {
  const backend = setUp([{ id: '1', personId: '55', placeId: '301', startDate: '2026-01-01' }])
  const { user } = renderApp({ route: '/w/7/places/301?tab=people', userId: '42' })
  const current = await group('Đang ở')
  await user.click(await current.findByRole('button', { name: 'Thao tác với Nguyễn An' }))
  await user.click(await screen.findByRole('menuitem', { name: 'Xoá dòng nhập nhầm' }))
  const dialog = within(await screen.findByRole('alertdialog'))

  await user.click(dialog.getByRole('button', { name: 'Xoá' }))

  await waitFor(() => expect(current.queryByRole('link', { name: 'Nguyễn An' })).not.toBeInTheDocument())
  expect(backend.writes).toEqual([{ method: 'DELETE', path: '/occupancies/1', body: null }])
})

test('editsAStay', async () => {
  const backend = setUp([{ id: '1', personId: '55', placeId: '301', startDate: '2026-01-01' }])
  const { user } = renderApp({ route: '/w/7/places/301?tab=people', userId: '42' })
  await user.click(await (await group('Đang ở')).findByRole('button', { name: 'Thao tác với Nguyễn An' }))
  await user.click(await screen.findByRole('menuitem', { name: 'Sửa' }))
  const form = within(await screen.findByRole('dialog', { name: 'Sửa cư trú' }))

  await user.selectOptions(form.getByRole('combobox', { name: 'Quan hệ' }), 'Khách')
  await user.click(form.getByRole('button', { name: 'Lưu' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toEqual({ version: 1, relation: 'GUEST', startDate: '2026-01-01' })
})

test('renamingAPersonRenamesTheirRows', async () => {
  setUp([{ id: '1', personId: '55', placeId: '301', startDate: '2026-01-01' }])
  const { user } = renderApp({ route: '/w/7/places/301?tab=people', userId: '42' })
  await user.click(await (await group('Đang ở')).findByRole('link', { name: 'Nguyễn An' }))
  await user.click(await (await main()).findByRole('button', { name: 'Sửa' }))
  const form = within(await screen.findByRole('dialog', { name: 'Sửa hồ sơ' }))
  const name = await form.findByRole('textbox', { name: 'Họ tên' })
  await user.clear(name)
  await user.type(name, 'Nguyễn Thị An')
  await user.click(form.getByRole('button', { name: 'Lưu' }))
  await (await main()).findByRole('heading', { name: 'Nguyễn Thị An' })

  await user.click((await main()).getByRole('link', { name: 'P301' }))
  await user.click(await (await main()).findByRole('tab', { name: 'Người ở' }))

  expect(await (await group('Đang ở')).findByRole('link', { name: 'Nguyễn Thị An' })).toBeInTheDocument()
})

test('personPageAddsAStayWithThePlacePicker', async () => {
  const backend = setUp()
  const requests = recordRequests()
  const { user } = renderApp({ route: '/w/7/people/55?tab=places', userId: '42' })

  await user.click(await (await main()).findByRole('button', { name: 'Thêm nơi ở' }))
  const form = within(await screen.findByRole('dialog', { name: 'Thêm nơi ở' }))
  await user.type(form.getByRole('searchbox', { name: 'Tìm địa điểm' }), 'P3')
  await user.click(await form.findByRole('radio', { name: 'P301' }))
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toEqual({ placeId: '301', personId: '55', relation: 'RESIDENT', startDate: TODAY })
  expect(requests).toContain('GET /api/registry/places/search')
})
