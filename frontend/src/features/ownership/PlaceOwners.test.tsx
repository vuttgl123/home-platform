import { screen, waitFor, within } from '@testing-library/react'
import { expect, test } from 'vitest'

import { todayInBusinessZone } from '@/lib/dates'
import { placeType } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { mockOwnershipBackend } from '@/test/ownershipBackend'
import { mockPeopleBackend } from '@/test/peopleBackend'
import { mockPlaceBackend } from '@/test/placeBackend'
import { renderApp } from '@/test/render'

// Ngày thật của hôm nay: giao diện tự điền "hôm nay", nên một ngày ghi cứng sẽ sai từ hôm sau.
const TODAY = todayInBusinessZone()

function setUp(ownerships: Parameters<typeof mockOwnershipBackend>[0]['ownerships'] = []) {
  mockShell({ placeTypes: [placeType()] })
  mockPlaceBackend([
    { id: '1', name: 'Toà A' },
    { id: '301', parentId: '1', name: 'P301' },
    { id: '302', parentId: '1', name: 'P302' },
  ])
  mockPeopleBackend({ today: TODAY, people: [{ id: '55', fullName: 'Nguyễn An' }, { id: '56', fullName: 'Trần Bình' }] })
  return mockOwnershipBackend({
    ownerships,
    today: TODAY,
    placesUnder: { '1': ['301', '302'] },
    placeNames: { '1': 'Toà A', '301': 'P301', '302': 'P302' },
    personNames: { '55': 'Nguyễn An', '56': 'Trần Bình', '8000': 'Phạm Mới' },
  })
}

async function main() {
  return within(await screen.findByRole('main'))
}

async function group(name: string) {
  return within(await (await main()).findByRole('region', { name }))
}

async function openTab(placeId = '301') {
  return renderApp({ route: `/w/7/places/${placeId}?tab=owners`, userId: '42' })
}

async function openAdd() {
  const app = await openTab()
  await app.user.click(await (await main()).findByRole('button', { name: 'Thêm chủ' }))
  return { ...app, form: within(await screen.findByRole('dialog', { name: 'Thêm chủ' })) }
}

async function rowAction(user: Awaited<ReturnType<typeof openTab>>['user'], person: string, action: string) {
  await user.click(await (await group('Đang sở hữu')).findByRole('button', { name: `Thao tác với ${person}` }))
  await user.click(await screen.findByRole('menuitem', { name: action }))
}

test('listsCurrentOwnersWithSharesAndTotal', async () => {
  setUp([
    { id: '1', personId: '55', sharePercent: 50, documentNo: 'CS 123456' },
    { id: '2', personId: '56', sharePercent: 50 },
  ])
  await openTab()

  const current = await group('Đang sở hữu')
  expect(await current.findByRole('link', { name: 'Nguyễn An' })).toBeInTheDocument()
  expect(current.getByText('CS 123456')).toBeInTheDocument()
  expect(current.getAllByText('50%')).toHaveLength(2)
  expect(current.getByText('100%')).toBeInTheDocument()
})

test('totalIsHiddenWhenNoShareIsRecorded', async () => {
  setUp([{ id: '1', personId: '55' }])
  await openTab()

  const current = await group('Đang sở hữu')
  await current.findByRole('link', { name: 'Nguyễn An' })
  expect(current.queryByText('Tổng')).not.toBeInTheDocument()
})

test('pastOwnersLoadOnlyWhenOpened', async () => {
  setUp([{ id: '1', personId: '55', startDate: '2020-01-01', endDate: '2024-01-01' }])
  const { user } = await openTab()

  await (await group('Đang sở hữu')).findByText('Chưa có ai đang sở hữu.')
  expect(screen.queryByRole('region', { name: 'Đã thôi' })).not.toBeInTheDocument()
  await user.click((await main()).getByRole('button', { name: 'Đã thôi' }))

  expect(await (await group('Đã thôi')).findByRole('link', { name: 'Nguyễn An' })).toBeInTheDocument()
})

test('wholeBuildingShowsWhoOwnsEachPlace', async () => {
  setUp([
    { id: '1', placeId: '301', personId: '55' },
    { id: '2', placeId: '302', personId: '56' },
  ])
  const { user } = await openTab('1')

  await user.click(await (await main()).findByRole('switch', { name: 'Cả bên trong' }))

  const inside = await group('Đang sở hữu bên trong')
  expect(await inside.findByRole('link', { name: 'P302' })).toBeInTheDocument()
  expect(inside.getByRole('link', { name: 'Trần Bình' })).toBeInTheDocument()
})

test('addsAnOwnerWithAShare', async () => {
  const backend = setUp()
  const { user, form } = await openAdd()

  await user.type(form.getByRole('searchbox', { name: 'Tìm người' }), 'Nguyễn')
  await user.click(await form.findByRole('radio', { name: 'Nguyễn An' }))
  await user.type(form.getByRole('textbox', { name: 'Tỷ lệ (%)' }), '50')
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  expect(await (await group('Đang sở hữu')).findByRole('link', { name: 'Nguyễn An' })).toBeInTheDocument()
  expect(backend.writes[0].body).toEqual({ placeId: '301', personId: '55', sharePercent: 50, startDate: TODAY })
})

test('addsAnOwnerWhoIsANewPerson', async () => {
  const backend = setUp()
  const { user, form } = await openAdd()

  await user.type(form.getByRole('searchbox', { name: 'Tìm người' }), 'Phạm Mới')
  await user.click(await form.findByRole('button', { name: 'Thêm người mới “Phạm Mới”' }))
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  expect(await (await group('Đang sở hữu')).findByRole('link', { name: 'Phạm Mới' })).toBeInTheDocument()
  expect(backend.writes[0].body).toMatchObject({ placeId: '301', personId: '8000' })
})

test('shareMustBeBetweenZeroAndOneHundred', async () => {
  const backend = setUp()
  const { user, form } = await openAdd()
  await user.type(form.getByRole('searchbox', { name: 'Tìm người' }), 'Nguyễn')
  await user.click(await form.findByRole('radio', { name: 'Nguyễn An' }))
  const share = form.getByRole('textbox', { name: 'Tỷ lệ (%)' })

  for (const value of ['0', '101', '33,333']) {
    await user.clear(share)
    await user.type(share, value)
    await user.click(form.getByRole('button', { name: 'Thêm' }))
    expect(await form.findByText('Từ 0,01 đến 100, tối đa hai chữ số thập phân')).toBeInTheDocument()
  }
  expect(backend.writes).toEqual([])
  await user.clear(share)
  await user.type(share, '33,33')
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toMatchObject({ sharePercent: 33.33 })
})

test('refusalAboutTheTotalIsShownAsIs', async () => {
  setUp([{ id: '1', personId: '56', sharePercent: 60 }])
  const { user, form } = await openAdd()

  await user.type(form.getByRole('searchbox', { name: 'Tìm người' }), 'Nguyễn')
  await user.click(await form.findByRole('radio', { name: 'Nguyễn An' }))
  await user.type(form.getByRole('textbox', { name: 'Tỷ lệ (%)' }), '50')
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  expect(await form.findByText(`Ownership shares of place 301 would total 110% on ${TODAY}`)).toBeInTheDocument()
})

test('endsAnOwnership', async () => {
  const backend = setUp([{ id: '1', personId: '55' }])
  const { user } = await openTab()

  await rowAction(user, 'Nguyễn An', 'Thôi sở hữu')
  const dialog = within(await screen.findByRole('dialog', { name: 'Nguyễn An thôi sở hữu' }))
  await user.click(dialog.getByRole('button', { name: 'Thôi sở hữu' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0]).toEqual({ method: 'POST', path: '/ownerships/1/end', body: { version: 1, endDate: TODAY } })
})

test('editsShareAndDocument', async () => {
  const backend = setUp([{ id: '1', personId: '55', sharePercent: 50 }])
  const { user } = await openTab()

  await rowAction(user, 'Nguyễn An', 'Sửa')
  const form = within(await screen.findByRole('dialog', { name: 'Sửa sở hữu' }))
  const share = form.getByRole('textbox', { name: 'Tỷ lệ (%)' })
  expect(share).toHaveValue('50')
  await user.clear(share)
  await user.type(share, '40')
  await user.type(form.getByRole('textbox', { name: 'Số giấy tờ' }), 'CS 1')
  await user.click(form.getByRole('button', { name: 'Lưu' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toEqual({ version: 1, sharePercent: 40, startDate: '2026-01-01', documentNo: 'CS 1' })
})

test('deletesAMistakenRow', async () => {
  const backend = setUp([{ id: '1', personId: '55' }])
  const { user } = await openTab()

  await rowAction(user, 'Nguyễn An', 'Xoá dòng nhập nhầm')
  const dialog = within(await screen.findByRole('alertdialog', { name: 'Xoá dòng sở hữu của Nguyễn An tại P301?' }))
  await user.click(dialog.getByRole('button', { name: 'Xoá' }))

  await waitFor(() => expect(backend.writes).toEqual([{ method: 'DELETE', path: '/ownerships/1', body: null }]))
  expect(await (await group('Đang sở hữu')).findByText('Chưa có ai đang sở hữu.')).toBeInTheDocument()
})

test('endedRowCannotBeEndedAgain', async () => {
  setUp([{ id: '1', personId: '55', endDate: '2027-01-01' }])
  const { user } = await openTab()

  await user.click(await (await group('Đang sở hữu')).findByRole('button', { name: 'Thao tác với Nguyễn An' }))
  await screen.findByRole('menuitem', { name: 'Sửa' })
  expect(screen.queryByRole('menuitem', { name: 'Thôi sở hữu' })).not.toBeInTheDocument()
})

test('emptyTabInvitesToAddAnOwner', async () => {
  setUp()
  await openTab()

  expect(await (await group('Đang sở hữu')).findByText('Chưa có ai đang sở hữu.')).toBeInTheDocument()
  expect((await main()).getByRole('button', { name: 'Thêm chủ' })).toBeInTheDocument()
})

test('currentOwnersShowTheirTotalShare', async () => {
  setUp([
    { id: '1', personId: '55', sharePercent: 50 },
    { id: '2', personId: '56', sharePercent: 30 },
  ])
  await openTab()

  const current = await group('Đang sở hữu')

  expect(await current.findByRole('progressbar', { name: 'Tổng tỷ lệ sở hữu' })).toHaveAttribute('aria-valuenow', '80')
})
