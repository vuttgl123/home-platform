import { screen, waitFor, within } from '@testing-library/react'
import { expect, test } from 'vitest'

import { placeType } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { mockOwnershipBackend } from '@/test/ownershipBackend'
import { mockPeopleBackend } from '@/test/peopleBackend'
import { mockPlaceBackend } from '@/test/placeBackend'
import { renderApp } from '@/test/render'

const TODAY = '2026-11-16'

function setUp(ownerships: Parameters<typeof mockOwnershipBackend>[0]['ownerships']) {
  mockShell({ placeTypes: [placeType()] })
  mockPlaceBackend([{ id: '301', name: 'P301' }])
  mockPeopleBackend({
    today: TODAY,
    people: [
      { id: '55', fullName: 'Nguyễn An' },
      { id: '56', fullName: 'Trần Bình' },
      { id: '57', fullName: 'Lê Cường' },
    ],
  })
  return mockOwnershipBackend({
    ownerships,
    today: TODAY,
    placeNames: { '301': 'P301' },
    personNames: { '55': 'Nguyễn An', '56': 'Trần Bình', '57': 'Lê Cường', '8000': 'Phạm Mới' },
  })
}

async function openTransfer() {
  const app = renderApp({ route: '/w/7/places/301?tab=owners', userId: '42' })
  const current = within(await screen.findByRole('region', { name: 'Đang sở hữu' }))
  await app.user.click(await current.findByRole('button', { name: 'Thao tác với Nguyễn An' }))
  await app.user.click(await screen.findByRole('menuitem', { name: 'Chuyển nhượng' }))
  return { ...app, dialog: within(await screen.findByRole('dialog', { name: 'Chuyển nhượng phần của Nguyễn An' })) }
}

async function pick(user: Awaited<ReturnType<typeof openTransfer>>['user'], dialog: Awaited<ReturnType<typeof openTransfer>>['dialog'], name: string) {
  await user.type(dialog.getByRole('searchbox', { name: 'Tìm người' }), name.split(' ')[0])
  await user.click(await dialog.findByRole('radio', { name }))
}

test('transferSaysWhatWillHappen', async () => {
  setUp([{ id: '1', personId: '55', sharePercent: 50, startDate: '2024-03-01' }])
  const { user, dialog } = await openTransfer()

  await pick(user, dialog, 'Lê Cường')

  expect(dialog.getByText('Nguyễn An thôi sở hữu từ hết ngày 15/11/2026; Lê Cường sở hữu 50% từ 16/11/2026.')).toBeInTheDocument()
})

test('transfersToAnotherPerson', async () => {
  const backend = setUp([{ id: '1', personId: '55', sharePercent: 50, startDate: '2024-03-01' }])
  const { user, dialog } = await openTransfer()

  await pick(user, dialog, 'Lê Cường')
  await user.click(dialog.getByRole('button', { name: 'Chuyển nhượng' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0]).toEqual({
    method: 'POST',
    path: '/ownerships/1/transfer',
    body: { version: 1, toPersonId: '57', transferDate: TODAY },
  })
  expect(await screen.findByText('Đã chuyển nhượng cho Lê Cường')).toBeInTheDocument()
  const current = within(await screen.findByRole('region', { name: 'Đang sở hữu' }))
  expect(await current.findByRole('link', { name: 'Lê Cường' })).toBeInTheDocument()
  expect(current.queryByRole('link', { name: 'Nguyễn An' })).not.toBeInTheDocument()
})

test('transfersToANewPerson', async () => {
  const backend = setUp([{ id: '1', personId: '55', startDate: '2024-03-01' }])
  const { user, dialog } = await openTransfer()

  await user.type(dialog.getByRole('searchbox', { name: 'Tìm người' }), 'Phạm Mới')
  await user.click(await dialog.findByRole('button', { name: 'Thêm người mới “Phạm Mới”' }))
  await user.click(dialog.getByRole('button', { name: 'Chuyển nhượng' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toMatchObject({ toPersonId: '8000' })
})

test('receiverMustDifferFromTheOwner', async () => {
  const backend = setUp([{ id: '1', personId: '55', startDate: '2024-03-01' }])
  const { user, dialog } = await openTransfer()

  await pick(user, dialog, 'Nguyễn An')
  await user.click(dialog.getByRole('button', { name: 'Chuyển nhượng' }))

  expect(await dialog.findByText('Người nhận phải khác chủ hiện tại')).toBeInTheDocument()
  expect(backend.writes).toEqual([])
})

test('transferDateMustBeAfterTheStart', async () => {
  const backend = setUp([{ id: '1', personId: '55', startDate: '2024-03-01' }])
  const { user, dialog } = await openTransfer()

  await pick(user, dialog, 'Lê Cường')
  const date = dialog.getByLabelText('Ngày chuyển nhượng')
  await user.clear(date)
  await user.type(date, '2024-03-01')
  await user.click(dialog.getByRole('button', { name: 'Chuyển nhượng' }))

  expect(await dialog.findByText('Phải sau ngày bắt đầu sở hữu (01/03/2024)')).toBeInTheDocument()
  expect(backend.writes).toEqual([])
})

test('refusalBecauseTheReceiverAlreadyOwnsIsShownAsIs', async () => {
  setUp([
    { id: '1', personId: '55', sharePercent: 50, startDate: '2024-03-01' },
    { id: '2', personId: '56', sharePercent: 50, startDate: '2024-03-01' },
  ])
  const { user, dialog } = await openTransfer()

  await pick(user, dialog, 'Trần Bình')
  await user.click(dialog.getByRole('button', { name: 'Chuyển nhượng' }))

  expect(await dialog.findByText('Person 56 already has an overlapping ownership at this place')).toBeInTheDocument()
})
