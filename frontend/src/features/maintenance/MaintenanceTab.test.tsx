import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import type { ItemDetail } from '@/features/items'
import { addDays, todayInBusinessZone } from '@/lib/dates'
import { itemCategory, page, placeType } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { mockItemBackend } from '@/test/itemBackend'
import { mockMaintenanceBackend } from '@/test/maintenanceBackend'
import { mockPlaceBackend } from '@/test/placeBackend'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

import type { MaintenanceEntry } from './types'

const TODAY = todayInBusinessZone()

function setUp(item: Partial<ItemDetail>, entries: Partial<MaintenanceEntry>[] = []) {
  mockShell({ placeTypes: [placeType()] })
  mockPlaceBackend([{ id: '301', name: 'P301' }])
  server.use(
    http.get('/api/registry/item-categories', () => HttpResponse.json([itemCategory({ id: '600', name: 'Điện máy' })])),
    http.get('/api/registry/people/search', () => HttpResponse.json(page([]))),
  )
  const items = mockItemBackend({ items: [{ id: '900', placeId: '301', name: 'Tủ lạnh', ...item }], today: TODAY })
  const log = mockMaintenanceBackend({ entries, items: items.items })
  const app = renderApp({ route: '/w/7/items/900?tab=maintenance', userId: '42' })
  return { ...app, items, log }
}

async function tab() {
  const main = within(await screen.findByRole('main'))
  return within(await main.findByRole('tabpanel'))
}

async function openLog(user: ReturnType<typeof renderApp>['user']) {
  await user.click(await (await tab()).findByRole('button', { name: 'Ghi bảo trì' }))
  return within(await screen.findByRole('dialog', { name: 'Ghi bảo trì' }))
}

test('listsEntriesNewestFirst', async () => {
  setUp({}, [
    { id: '7001', kind: 'REPAIR', doneOn: '2026-03-01', work: 'Thay ron cửa', doneBy: 'Thợ Minh' },
    { id: '7002', kind: 'SERVICE', doneOn: '2026-05-10', work: 'Vệ sinh dàn nóng' },
  ])

  const panel = await tab()
  const rows = await panel.findAllByRole('listitem')
  expect(rows.map((row) => row.textContent)).toEqual([
    expect.stringContaining('Vệ sinh dàn nóng'),
    expect.stringContaining('Thay ron cửa'),
  ])
  expect(within(rows[0]).getByText('Bảo trì')).toBeInTheDocument()
  expect(within(rows[0]).getByText('10/05/2026')).toBeInTheDocument()
  expect(within(rows[1]).getByText('Sửa chữa')).toBeInTheDocument()
  expect(within(rows[1]).getByText('Thợ Minh', { exact: false })).toBeInTheDocument()
})

test('emptyState', async () => {
  setUp({})

  expect(await (await tab()).findByText('Chưa có lần sửa hay bảo trì nào')).toBeInTheDocument()
})

test('brokenItemDefaultsToRepairAndFixed', async () => {
  const { user, log, items } = setUp({ status: 'BROKEN', version: 3 })
  const dialog = await openLog(user)

  expect(dialog.getByRole('radio', { name: 'Sửa chữa' })).toBeChecked()
  expect(dialog.getByRole('checkbox', { name: 'Đã sửa xong, chuyển về Đang dùng' })).toBeChecked()
  expect(dialog.getByLabelText(/^Ngày làm/)).toHaveValue(TODAY)
  await user.type(dialog.getByRole('textbox', { name: 'Việc đã làm' }), 'Thay block')
  await user.click(dialog.getByRole('button', { name: 'Lưu' }))

  await waitFor(() => expect(log.writes).toHaveLength(1))
  expect(log.writes[0].body).toEqual({
    itemId: '900',
    kind: 'REPAIR',
    doneOn: TODAY,
    work: 'Thay block',
    markFixed: true,
    itemVersion: 3,
  })
  expect(items.items.get('900')!.status).toBe('IN_USE')
  // Món được tải lại: nhãn trạng thái ở đầu trang đổi theo.
  expect(await within(await screen.findByRole('main')).findByText('Đang dùng')).toBeInTheDocument()
})

test('workingItemDefaultsToService', async () => {
  const { user } = setUp({ status: 'IN_USE' })
  const dialog = await openLog(user)

  expect(dialog.getByRole('radio', { name: 'Bảo trì' })).toBeChecked()
  expect(dialog.queryByRole('checkbox', { name: 'Đã sửa xong, chuyển về Đang dùng' })).not.toBeInTheDocument()
})

test('requiresWorkAndNoFutureDate', async () => {
  const { user, log } = setUp({})
  const dialog = await openLog(user)

  await user.click(dialog.getByRole('button', { name: 'Lưu' }))
  expect(await dialog.findByText('Nhập việc đã làm')).toBeInTheDocument()

  await user.type(dialog.getByRole('textbox', { name: 'Việc đã làm' }), 'Vệ sinh')
  const date = dialog.getByLabelText(/^Ngày làm/)
  await user.clear(date)
  await user.type(date, addDays(TODAY, 1))
  await user.click(dialog.getByRole('button', { name: 'Lưu' }))
  expect(await dialog.findByText('Không được sau hôm nay')).toBeInTheDocument()
  expect(log.writes).toHaveLength(0)
})

test('editSendsVersion', async () => {
  const { user, log } = setUp({}, [{ id: '7001', doneOn: '2026-03-01', work: 'Thay ron', version: 2 }])
  const panel = await tab()

  await user.click(await panel.findByRole('button', { name: 'Thao tác với Thay ron' }))
  await user.click(await screen.findByRole('menuitem', { name: 'Sửa' }))
  const dialog = within(await screen.findByRole('dialog', { name: 'Sửa dòng bảo trì' }))
  const work = dialog.getByRole('textbox', { name: 'Việc đã làm' })
  await user.clear(work)
  await user.type(work, 'Thay ron cửa')
  await user.type(dialog.getByRole('textbox', { name: 'Người làm' }), 'Thợ Minh')
  await user.click(dialog.getByRole('button', { name: 'Lưu' }))

  await waitFor(() => expect(log.writes).toHaveLength(1))
  expect(log.writes[0]).toEqual({
    method: 'PUT',
    path: '/maintenance/7001',
    body: { version: 2, kind: 'REPAIR', doneOn: '2026-03-01', work: 'Thay ron cửa', doneBy: 'Thợ Minh' },
  })
  expect(await panel.findByText('Thay ron cửa')).toBeInTheDocument()
})

test('deleteOffersUndo', async () => {
  const { user, log } = setUp({}, [{ id: '7001', work: 'Thay ron', version: 1 }])
  const panel = await tab()

  await user.click(await panel.findByRole('button', { name: 'Thao tác với Thay ron' }))
  await user.click(await screen.findByRole('menuitem', { name: 'Xoá' }))
  expect(await panel.findByText('Chưa có lần sửa hay bảo trì nào')).toBeInTheDocument()

  await user.click(await screen.findByRole('button', { name: 'Hoàn tác' }))
  await waitFor(() => expect(log.writes.at(-1)).toEqual({ method: 'POST', path: '/maintenance/7001/restore', body: { version: 2 } }))
  expect(await panel.findByText('Thay ron')).toBeInTheDocument()
})

test('staleItemVersionShowsConflict', async () => {
  const { user, items, log } = setUp({ status: 'BROKEN', version: 3 })
  const dialog = await openLog(user)
  items.editElsewhere('900', { status: 'BROKEN' })

  await user.type(dialog.getByRole('textbox', { name: 'Việc đã làm' }), 'Thay block')
  await user.click(dialog.getByRole('button', { name: 'Lưu' }))

  expect(await dialog.findByText('Đã có người sửa trước bạn.')).toBeInTheDocument()
  await user.click(dialog.getByRole('button', { name: 'Tải bản mới' }))
  await user.click(dialog.getByRole('button', { name: 'Lưu' }))
  await waitFor(() => expect(log.writes.at(-1)?.body).toMatchObject({ itemVersion: 4 }))
  expect(log.entries.size).toBe(1)
})

test('removedItemStillTakesEntries', async () => {
  const { user } = setUp({ status: 'REMOVED', removedAt: '2026-10-01' })
  const dialog = await openLog(user)

  expect(dialog.getByRole('radio', { name: 'Bảo trì' })).toBeChecked()
  await user.click(dialog.getByRole('radio', { name: 'Sửa chữa' }))
  expect(dialog.queryByRole('checkbox', { name: 'Đã sửa xong, chuyển về Đang dùng' })).not.toBeInTheDocument()
})
