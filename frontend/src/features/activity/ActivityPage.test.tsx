import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { page, place, placeDetail, problem } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { mockPlaceBackend } from '@/test/placeBackend'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

function entry(overrides: Record<string, unknown>) {
  return {
    entityType: 'PLACE',
    entityId: '1',
    versionNo: 1,
    action: 'CREATE',
    // Như backend: dòng tạo không có danh sách trường đã đổi (null).
    changedFields: null,
    changedBy: '42',
    changedAt: '2026-11-16T03:00:00Z',
    note: null,
    entityName: 'Toà A',
    ...overrides,
  }
}

/** Trả `entries` (lọc theo `entityType` như backend); ghi lại tham số của mỗi lần hỏi. */
function mockActivity(entries: Record<string, unknown>[]) {
  const asked: Record<string, string>[] = []
  server.use(
    http.get('/api/registry/activity', ({ request }) => {
      const query = Object.fromEntries(new URL(request.url).searchParams)
      asked.push(query)
      return HttpResponse.json(page(entries.filter((row) => query.entityType == null || row.entityType === query.entityType)))
    }),
  )
  return asked
}

async function main() {
  return within(await screen.findByRole('main'))
}

async function rows() {
  const view = await main()
  await view.findAllByRole('listitem')
  return view.getAllByRole('listitem')
}

test('listsActivityNewestFirst', async () => {
  mockShell()
  mockActivity([
    entry({ entityId: '1', versionNo: 2, action: 'UPDATE', changedFields: ['name'], entityName: 'Toà Alpha' }),
    entry({ entityType: 'PERSON', entityId: '55', entityName: 'Nguyễn An' }),
  ])

  renderApp({ route: '/w/7/activity', userId: '42' })

  const [first, second] = await rows()
  expect(within(first).getByRole('link', { name: 'Toà Alpha' })).toBeInTheDocument()
  expect(within(first).getByText('Sửa')).toBeInTheDocument()
  expect(within(first).getByText('Đã đổi: Tên')).toBeInTheDocument()
  expect(within(second).getByText('Người')).toBeInTheDocument()
  expect(screen.getByRole('link', { name: 'Hoạt động' })).toHaveAttribute('aria-current', 'page')
})

test('filterFollowsTheUrl', async () => {
  mockShell()
  const asked = mockActivity([entry({}), entry({ entityType: 'PERSON', entityId: '55', entityName: 'Nguyễn An' })])
  const { user, router } = renderApp({ route: '/w/7/activity', userId: '42' })
  await rows()

  await user.click((await main()).getByRole('tab', { name: 'Người' }))

  await waitFor(() => expect(router.state.location.search).toBe('?type=PERSON'))
  await waitFor(() => expect(asked.map((query) => query.entityType)).toContain('PERSON'))
  await waitFor(async () => expect(await rows()).toHaveLength(1))
})

test('filtersAreReadFromTheUrl', async () => {
  mockShell()
  const asked = mockActivity([])
  renderApp({ route: '/w/7/activity?type=ITEM&action=DELETE&mine=1&from=2026-11-01&to=2026-11-17', userId: '42' })
  const view = await main()

  await waitFor(() => expect(asked).not.toHaveLength(0))
  // "Đến ngày" 17/11 gửi 00:00 (giờ máy) ngày 18/11: backend không tính mốc `to`.
  expect(asked.at(-1)).toEqual({
    entityType: 'ITEM',
    action: 'DELETE',
    changedBy: '42',
    from: new Date(2026, 10, 1).toISOString(),
    to: new Date(2026, 10, 18).toISOString(),
  })
  expect(view.getByLabelText('Hành động')).toHaveValue('DELETE')
  expect(view.getByRole('switch', { name: 'Chỉ việc tôi làm' })).toBeChecked()
  expect(view.getByLabelText('Từ ngày')).toHaveValue('2026-11-01')
  expect(view.getByLabelText('Đến ngày')).toHaveValue('2026-11-17')
})

test('changingAFilterUpdatesTheUrl', async () => {
  mockShell()
  const asked = mockActivity([])
  const { user, router } = renderApp({ route: '/w/7/activity', userId: '42' })
  const view = await main()

  await user.selectOptions(await view.findByLabelText('Hành động'), 'Xoá')
  await waitFor(() => expect(router.state.location.search).toBe('?action=DELETE'))
  await user.click(view.getByRole('switch', { name: 'Chỉ việc tôi làm' }))
  await waitFor(() => expect(router.state.location.search).toBe('?action=DELETE&mine=1'))
  await user.type(view.getByLabelText('Từ ngày'), '2026-11-01')
  await waitFor(() => expect(router.state.location.search).toBe('?action=DELETE&mine=1&from=2026-11-01'))
  // Đổi tab loại bản ghi không làm mất các lọc khác.
  await user.click(view.getByRole('tab', { name: 'Đồ đạc' }))

  await waitFor(() => expect(router.state.location.search).toBe('?type=ITEM&action=DELETE&mine=1&from=2026-11-01'))
  await waitFor(() =>
    expect(asked.at(-1)).toEqual({ entityType: 'ITEM', action: 'DELETE', changedBy: '42', from: new Date(2026, 10, 1).toISOString() }),
  )
})

test('clearFiltersKeepsTheTypeTab', async () => {
  mockShell()
  mockActivity([])
  const { user, router } = renderApp({ route: '/w/7/activity?type=PERSON&action=DELETE&mine=1&from=2026-11-01', userId: '42' })
  const view = await main()

  await user.click(await view.findByRole('button', { name: 'Bỏ lọc' }))

  await waitFor(() => expect(router.state.location.search).toBe('?type=PERSON'))
  // Chỉ còn tab loại bản ghi: không còn gì để bỏ.
  expect(view.queryByRole('button', { name: 'Bỏ lọc' })).not.toBeInTheDocument()
  expect(view.getByLabelText('Hành động')).toHaveValue('')
})

test('fromAfterToShowsAMessageAndSendsNothing', async () => {
  // Cây địa điểm tải cùng lúc với danh sách: khi cây đã hiện thì request hoạt động (nếu có) cũng đã đi.
  mockShell({ places: [place({ id: '1', name: 'Toà A' })] })
  const asked = mockActivity([entry({})])
  const { user } = renderApp({ route: '/w/7/activity?from=2026-11-18&to=2026-11-17', userId: '42' })
  const view = await main()

  expect(await view.findByText('Từ ngày phải trước hoặc bằng Đến ngày')).toBeInTheDocument()
  await screen.findByRole('treeitem', { name: 'Toà A' })
  expect(asked).toEqual([])
  expect(view.queryByRole('listitem')).not.toBeInTheDocument()

  // Sửa lại khoảng ngày thì danh sách tải.
  await user.clear(view.getByLabelText('Từ ngày'))

  await waitFor(() => expect(asked).toEqual([{ to: new Date(2026, 10, 18).toISOString() }]))
  expect(await rows()).toHaveLength(1)
  expect(view.queryByText('Từ ngày phải trước hoặc bằng Đến ngày')).not.toBeInTheDocument()
})

test('emptyFilteredListSaysSo', async () => {
  mockShell()
  mockActivity([])
  renderApp({ route: '/w/7/activity?action=RESTORE', userId: '42' })

  expect(await (await main()).findByText('Không có hoạt động khớp bộ lọc.')).toBeInTheDocument()
})

test('emptyListWithoutFiltersKeepsItsMessage', async () => {
  mockShell()
  mockActivity([])
  renderApp({ route: '/w/7/activity?type=ITEM', userId: '42' })

  expect(await (await main()).findByText('Chưa có hoạt động nào.')).toBeInTheDocument()
})

test('rowsLinkToTheirRecords', async () => {
  mockShell()
  mockActivity([
    entry({ entityType: 'PLACE', entityId: '1', entityName: 'Toà A' }),
    entry({ entityType: 'PERSON', entityId: '55', entityName: 'Nguyễn An' }),
    entry({ entityType: 'ITEM', entityId: '900', entityName: 'Tủ lạnh' }),
    entry({ entityType: 'ITEM', entityId: '901', action: 'DELETE', entityName: 'Ghế' }),
  ])

  renderApp({ route: '/w/7/activity', userId: '42' })

  const list = await rows()
  expect(within(list[0]).getByRole('link', { name: 'Toà A' })).toHaveAttribute('href', '/w/7/places/1')
  expect(within(list[1]).getByRole('link', { name: 'Nguyễn An' })).toHaveAttribute('href', '/w/7/people/55')
  expect(within(list[2]).getByRole('link', { name: 'Tủ lạnh' })).toHaveAttribute('href', '/w/7/items/900')
  // Dòng xoá: bản ghi không mở được, nên không có liên kết.
  expect(within(list[3]).queryByRole('link')).not.toBeInTheDocument()
  expect(within(list[3]).getByText('Ghế')).toBeInTheDocument()
})

test('deletedRowsOfferRestore', async () => {
  mockShell()
  mockActivity([entry({ action: 'UPDATE', versionNo: 2 }), entry({ entityId: '9', action: 'DELETE', versionNo: 3, entityName: 'Kho cũ' })])

  renderApp({ route: '/w/7/activity', userId: '42' })

  const list = await rows()
  expect(within(list[0]).queryByRole('button', { name: /Khôi phục/ })).not.toBeInTheDocument()
  expect(within(list[1]).getByRole('button', { name: 'Khôi phục Kho cũ' })).toBeInTheDocument()
})

test('restoringADeletedPlacePutsItBackInTheTree', async () => {
  mockShell()
  const backend = mockPlaceBackend([{ id: '1', name: 'Toà A' }])
  mockActivity([entry({ entityId: '9', action: 'DELETE', versionNo: 2, entityName: 'Kho cũ' })])
  let sent: unknown = null
  server.use(
    http.post('/api/registry/places/9/restore', async ({ request }) => {
      sent = await request.json()
      const restored = placeDetail({ id: '9', name: 'Kho cũ', version: 3 })
      backend.places.set('9', restored)
      return HttpResponse.json(restored)
    }),
  )
  const { user } = renderApp({ route: '/w/7/activity', userId: '42' })
  const tree = within(await screen.findByRole('navigation', { name: 'Cây địa điểm' }))
  await tree.findByRole('treeitem', { name: 'Toà A' })

  await user.click(within((await rows())[0]).getByRole('button', { name: 'Khôi phục Kho cũ' }))

  expect(await tree.findByRole('treeitem', { name: 'Kho cũ' })).toBeInTheDocument()
  expect(sent).toEqual({ version: 2 })
})

test('restoreNeedsTheParentFirst', async () => {
  mockShell()
  mockActivity([entry({ entityId: '9', action: 'DELETE', versionNo: 2, entityName: 'P301' })])
  server.use(
    http.post('/api/registry/places/9/restore', () =>
      HttpResponse.json(problem(409, 'Restore the parent place 5 first'), { status: 409 }),
    ),
  )
  const { user } = renderApp({ route: '/w/7/activity', userId: '42' })
  const row = (await rows())[0]

  await user.click(within(row).getByRole('button', { name: 'Khôi phục P301' }))

  expect(await within(row).findByText('Restore the parent place 5 first')).toBeInTheDocument()
})

test('alreadyRestoredRowExplainsItself', async () => {
  mockShell()
  mockActivity([entry({ entityId: '9', action: 'DELETE', versionNo: 2, entityName: 'Kho cũ' })])
  server.use(
    http.post('/api/registry/places/9/restore', () =>
      HttpResponse.json({ ...problem(409, 'Place 9 was changed by someone else'), code: 'VERSION_CONFLICT' }, { status: 409 }),
    ),
  )
  const { user } = renderApp({ route: '/w/7/activity', userId: '42' })
  const row = (await rows())[0]

  await user.click(within(row).getByRole('button', { name: 'Khôi phục Kho cũ' }))

  expect(await within(row).findByText('Đã được khôi phục hoặc thay đổi sau lần xoá này.')).toBeInTheDocument()
})

test('restoresAPersonAndAnItem', async () => {
  mockShell()
  mockActivity([
    entry({ entityType: 'PERSON', entityId: '55', action: 'DELETE', versionNo: 4, entityName: 'Nguyễn An' }),
    entry({ entityType: 'ITEM', entityId: '900', action: 'DELETE', versionNo: 2, entityName: 'Tủ lạnh' }),
  ])
  const sent: Record<string, unknown> = {}
  server.use(
    http.post('/api/registry/people/55/restore', async ({ request }) => {
      sent.person = await request.json()
      return HttpResponse.json({ id: '55', fullName: 'Nguyễn An', version: 5, currentOccupancies: [] })
    }),
    http.post('/api/registry/items/900/restore', async ({ request }) => {
      sent.item = await request.json()
      return HttpResponse.json({ id: '900', name: 'Tủ lạnh', placeId: '1', placeName: 'Toà A', version: 3 })
    }),
  )
  const { user } = renderApp({ route: '/w/7/activity', userId: '42' })
  const [person, item] = await rows()

  await user.click(within(person).getByRole('button', { name: 'Khôi phục Nguyễn An' }))
  await user.click(within(item).getByRole('button', { name: 'Khôi phục Tủ lạnh' }))

  await waitFor(() => expect(sent).toEqual({ person: { version: 4 }, item: { version: 2 } }))
})

test('linksToTheTrash', async () => {
  mockShell()
  mockActivity([])
  renderApp({ route: '/w/7/activity', userId: '42' })

  expect(await (await main()).findByRole('link', { name: 'Mở Thùng rác' })).toHaveAttribute('href', '/w/7/trash')
})
