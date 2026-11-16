import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test, vi } from 'vitest'

import { setCurrentUser } from '@/api'
import { page } from '@/test/fixtures'
import { renderWithProviders } from '@/test/render'
import { server } from '@/test/server'

import { HistoryTab } from './components/HistoryTab'

type Snapshot = { name: string }

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

function mockVersions(pages: Record<string, ReturnType<typeof version>[]>, next: Record<string, string | null> = {}) {
  server.use(
    http.get('/api/registry/places/1/versions', ({ request }) => {
      const cursor = new URL(request.url).searchParams.get('cursor') ?? 'first'
      return HttpResponse.json(page(pages[cursor], next[cursor] ?? null))
    }),
    http.get('/api/registry/places/1/versions/:no', ({ params }) =>
      HttpResponse.json({ ...version(Number(params.no)), snapshot: { name: `Tên ở v${params.no}` } }),
    ),
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
      renderSnapshot={(snapshot) => <p>{snapshot.name}</p>}
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
