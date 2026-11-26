import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test, vi } from 'vitest'

import { setCurrentUser } from '@/api'
import { attributeRows } from '@/features/catalog'
import { fieldDef, page } from '@/test/fixtures'
import { renderWithProviders } from '@/test/render'
import { server } from '@/test/server'

import { HistoryTab } from './components/HistoryTab'
import type { SnapshotRow } from './types'

type Snapshot = { name: string; code?: string; attributes?: Record<string, unknown> }

/** Như một tab thật: dòng cố định, rồi tới mục tuỳ biến. */
function snapshotRows(snapshot: Snapshot): SnapshotRow[] {
  const defs = [
    fieldDef({ id: '1', fieldKey: 'area_m2', label: 'Diện tích', sortOrder: 1 }),
    fieldDef({ id: '2', fieldKey: 'floor', label: 'Tầng', sortOrder: 2 }),
  ]
  return [
    { key: 'name', label: 'Tên', value: snapshot.name, raw: snapshot.name },
    { key: 'code', label: 'Mã', value: snapshot.code, raw: snapshot.code },
    ...attributeRows(defs, snapshot.attributes ?? null).map((row) => ({ ...row, value: row.display })),
  ]
}

function version(versionNo: number, overrides: Record<string, unknown> = {}) {
  return {
    versionNo,
    action: versionNo === 1 ? 'CREATE' : 'UPDATE',
    // Như backend: version tạo không có danh sách trường đã đổi (null).
    changedFields: versionNo === 1 ? null : ['name'],
    changedBy: '42',
    changedAt: `2026-11-0${versionNo}T03:00:00Z`,
    note: null,
    ...overrides,
  }
}

/**
 * @param snapshots nội dung theo số version; thiếu thì `{ name: "Tên ở v<n>", code: "R1" }`
 */
function mockVersions(
  pages: Record<string, ReturnType<typeof version>[]>,
  next: Record<string, string | null> = {},
  snapshots: Record<number, Snapshot> = {},
) {
  server.use(
    http.get('/api/registry/places/1/versions', ({ request }) => {
      const cursor = new URL(request.url).searchParams.get('cursor') ?? 'first'
      return HttpResponse.json(page(pages[cursor], next[cursor] ?? null))
    }),
    http.get('/api/registry/places/1/versions/:no', ({ params }) => {
      const no = Number(params.no)
      // Cùng phần tóm tắt như dòng trong danh sách (người sửa, giờ sửa).
      const summary = Object.values(pages).flat().find((item) => item.versionNo === no) ?? version(no)
      return HttpResponse.json({ ...summary, snapshot: snapshots[no] ?? { name: `Tên ở v${no}`, code: 'R1' } })
    }),
  )
}

function renderTab(options: { currentVersion?: number; onRestored?: (restored: unknown) => void } = {}) {
  setCurrentUser('42')
  return renderWithProviders(
    <HistoryTab<Snapshot>
      ws="7"
      kind="places"
      id="1"
      currentVersion={options.currentVersion ?? 3}
      fieldLabels={{ name: 'Tên', 'attributes.area_m2': 'Diện tích' }}
      snapshotRows={snapshotRows}
      onRestored={options.onRestored ?? (() => {})}
      onReload={async () => {}}
    />,
  )
}

test('listsVersionsNewestFirst', async () => {
  mockVersions({ first: [version(3), version(2), version(1)] })

  renderTab()

  const rows = await screen.findAllByRole('button', { name: /^v\d/ })
  expect(rows.map((row) => row.getAttribute('aria-label'))).toEqual(['v3', 'v2', 'v1'])
  expect(within(rows[2]).getByText('Tạo')).toBeInTheDocument()
  expect(within(rows[0]).getByText('Hiện tại')).toBeInTheDocument()
})

test('versionsLoadPageByPage', async () => {
  mockVersions({ first: [version(3), version(2)], c1: [version(1)] }, { first: 'c1' })
  const { user } = renderTab()
  await screen.findByRole('button', { name: 'v2' })
  expect(screen.queryByRole('button', { name: 'v1' })).not.toBeInTheDocument()

  await user.click(screen.getByRole('button', { name: 'Tải thêm' }))

  expect(await screen.findByRole('button', { name: 'v1' })).toBeInTheDocument()
})

test('changedFieldsUseLabels', async () => {
  mockVersions({ first: [version(2, { changedFields: ['name', 'attributes.area_m2', 'coverFileId'] })] })

  renderTab({ currentVersion: 2 })

  // Trường không có nhãn hiện nguyên khoá.
  expect(await screen.findByText('Đã đổi: Tên, Diện tích, coverFileId')).toBeInTheDocument()
})

test('changedByIsYouForTheSignedInUser', async () => {
  mockVersions({ first: [version(2), version(1, { changedBy: '99' })] })

  renderTab({ currentVersion: 2 })

  expect(within(await screen.findByRole('button', { name: 'v2' })).getByText(/Bạn/)).toBeInTheDocument()
  expect(within(screen.getByRole('button', { name: 'v1' })).getByText(/Người dùng khác/)).toBeInTheDocument()
})

test('opensAVersionWithItsSnapshot', async () => {
  mockVersions({ first: [version(3), version(2), version(1)] })
  const { user } = renderTab()

  await user.click(await screen.findByRole('button', { name: 'v2' }))

  const dialog = within(await screen.findByRole('dialog', { name: 'Bản v2' }))
  expect(await dialog.findByText('Tên ở v2')).toBeInTheDocument()
  expect(dialog.getByRole('button', { name: 'Khôi phục bản này' })).toBeInTheDocument()
})

test('latestVersionCannotBeRestored', async () => {
  mockVersions({ first: [version(3), version(2), version(1)] })
  const { user } = renderTab()

  await user.click(await screen.findByRole('button', { name: 'v3' }))

  const dialog = within(await screen.findByRole('dialog', { name: 'Bản v3' }))
  await dialog.findByText('Tên ở v3')
  expect(dialog.queryByRole('button', { name: 'Khôi phục bản này' })).not.toBeInTheDocument()
})

test('restoreSendsTheCurrentVersion', async () => {
  mockVersions({ first: [version(3), version(2), version(1)] })
  let body: unknown = null
  server.use(
    http.post('/api/registry/places/1/versions/2/restore', async ({ request }) => {
      body = await request.json()
      return HttpResponse.json({ id: '1', version: 4 })
    }),
  )
  const onRestored = vi.fn()
  const { user } = renderTab({ currentVersion: 3, onRestored })
  await user.click(await screen.findByRole('button', { name: 'v2' }))
  const dialog = within(await screen.findByRole('dialog', { name: 'Bản v2' }))

  await user.click(await dialog.findByRole('button', { name: 'Khôi phục bản này' }))
  expect(dialog.getByText('Chỉ nội dung được khôi phục; chỗ đứng giữ nguyên.')).toBeInTheDocument()
  await user.type(dialog.getByRole('textbox', { name: 'Ghi chú' }), 'Đặt lại tên cũ')
  await user.click(dialog.getByRole('button', { name: 'Khôi phục' }))

  await waitFor(() => expect(onRestored).toHaveBeenCalledWith({ id: '1', version: 4 }))
  expect(body).toEqual({ version: 3, note: 'Đặt lại tên cũ' })
})

type User = ReturnType<typeof renderTab>['user']

async function choose(user: User, ...versionNos: number[]) {
  for (const no of versionNos) {
    await user.click(await screen.findByRole('checkbox', { name: `Chọn v${no} để so sánh` }))
  }
}

async function compare(user: User, ...versionNos: number[]) {
  await choose(user, ...versionNos)
  await user.click(screen.getByRole('button', { name: 'So sánh' }))
  return within(await screen.findByRole('dialog', { name: /^So sánh v\d+ và v\d+$/ }))
}

/** Các dòng của bảng so sánh, mỗi dòng là chữ của từng ô. */
function lines(dialog: ReturnType<typeof within>) {
  return dialog
    .getAllByRole('row')
    .slice(1)
    .map((row: HTMLElement) => within(row).getAllByRole('cell').map((cell) => cell.textContent))
}

test('compareNeedsExactlyTwo', async () => {
  mockVersions({ first: [version(3), version(2), version(1)] })
  const { user } = renderTab()
  await screen.findByRole('button', { name: 'v3' })
  expect(screen.getByRole('button', { name: 'So sánh' })).toBeDisabled()

  await choose(user, 3)
  expect(screen.getByRole('button', { name: 'So sánh' })).toBeDisabled()
  await choose(user, 1)
  expect(screen.getByRole('button', { name: 'So sánh' })).toBeEnabled()
  // Bỏ một dòng thì lại thiếu.
  await choose(user, 3)

  expect(screen.getByRole('button', { name: 'So sánh' })).toBeDisabled()
})

test('thirdChoiceDropsTheFirstChosen', async () => {
  mockVersions({ first: [version(3), version(2), version(1)] })
  const { user } = renderTab()

  await choose(user, 2, 3, 1)

  expect(screen.getByRole('checkbox', { name: 'Chọn v2 để so sánh' })).not.toBeChecked()
  expect(screen.getByRole('checkbox', { name: 'Chọn v3 để so sánh' })).toBeChecked()
  expect(screen.getByRole('checkbox', { name: 'Chọn v1 để so sánh' })).toBeChecked()
})

test('olderVersionIsAlwaysOnTheLeft', async () => {
  mockVersions({ first: [version(3), version(2), version(1, { changedBy: '99' })] })
  const { user } = renderTab()

  // Chọn bản mới trước, bản cũ sau.
  const dialog = await compare(user, 3, 1)

  // Giờ hiện theo múi giờ của máy chạy test, nên chỉ so phần đầu và người sửa.
  const headers = dialog.getAllByRole('columnheader').map((header) => header.textContent)
  expect(headers).toHaveLength(3)
  expect(headers[0]).toBe('Trường')
  expect(headers[1]).toMatch(/^v1 · Tạo · \d{2}\/\d{2}\/2026 \d{2}:\d{2} · Người dùng khác$/)
  expect(headers[2]).toMatch(/^v3 · Sửa · \d{2}\/\d{2}\/2026 \d{2}:\d{2} · Bạn$/)
  expect(await dialog.findByText('Tên ở v1')).toBeInTheDocument()
  expect(lines(dialog)).toEqual([['Tên', 'Tên ở v1', 'Tên ở v3']])
})

test('onlyChangedFieldsByDefault', async () => {
  mockVersions({ first: [version(2), version(1)] })
  const { user } = renderTab({ currentVersion: 2 })

  const dialog = await compare(user, 1, 2)

  await dialog.findByText('Tên ở v2')
  expect(dialog.getByRole('switch', { name: 'Chỉ hiện trường khác' })).toBeChecked()
  // "Mã" cùng là R1 ở hai bản nên không hiện.
  expect(lines(dialog)).toEqual([['Tên', 'Tên ở v1', 'Tên ở v2']])
  expect(dialog.getByRole('row', { name: /Tên/ })).toHaveAttribute('data-changed')
})

test('showAllFieldsListsUnchangedToo', async () => {
  mockVersions({ first: [version(2), version(1)] })
  const { user } = renderTab({ currentVersion: 2 })
  const dialog = await compare(user, 1, 2)
  await dialog.findByText('Tên ở v2')

  await user.click(dialog.getByRole('switch', { name: 'Chỉ hiện trường khác' }))

  expect(lines(dialog)).toEqual([
    ['Tên', 'Tên ở v1', 'Tên ở v2'],
    ['Mã', 'R1', 'R1'],
    // Mục tuỳ biến đang dùng luôn có dòng, kể cả khi trống.
    ['Diện tích', '—', '—'],
    ['Tầng', '—', '—'],
  ])
  expect(dialog.getByRole('row', { name: /Mã/ })).not.toHaveAttribute('data-changed')
})

test('identicalVersionsSaySo', async () => {
  // Sửa rồi sửa lại: v3 giống hệt v1.
  mockVersions({ first: [version(3), version(2), version(1)] }, {}, { 1: { name: 'P301', code: 'R1' }, 3: { name: 'P301', code: 'R1' } })
  const { user } = renderTab()

  const dialog = await compare(user, 1, 3)

  expect(await dialog.findByText('Hai version này giống nhau')).toBeInTheDocument()
  expect(dialog.queryByRole('table')).not.toBeInTheDocument()
})

test('attributeChangeIsShownPerField', async () => {
  mockVersions({ first: [version(2), version(1)] }, {}, {
    1: { name: 'P301', code: 'R1', attributes: { area_m2: 20, floor: 3 } },
    2: { name: 'P301', code: 'R1', attributes: { area_m2: 25, floor: 3 } },
  })
  const { user } = renderTab({ currentVersion: 2 })

  const dialog = await compare(user, 2, 1)

  await dialog.findByText('25')
  expect(lines(dialog)).toEqual([['Diện tích', '20', '25']])
})
