import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { placeType, problem } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { mockPlaceBackend } from '@/test/placeBackend'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

const address = {
  countryCode: 'VN',
  provinceCode: null,
  provinceName: null,
  districtCode: null,
  districtName: null,
  wardCode: null,
  wardName: null,
  street: null,
  fullAddress: '1 Tràng Tiền',
  latitude: null,
  longitude: null,
  geohash: null,
}

/** P301 ở v3; v1 tên "Phòng 301" có địa chỉ riêng, v2, v3 đổi tên. */
function setUp() {
  mockShell({ placeTypes: [placeType({ id: '100', name: 'Phòng' })] })
  const backend = mockPlaceBackend([
    { id: '1', name: 'Toà A' },
    { id: '301', parentId: '1', name: 'P301', version: 3 },
  ])
  const snapshots: Record<number, Record<string, unknown>> = {
    1: { id: '301', name: 'Phòng 301', code: 'R1', placeTypeId: '100', status: 'ACTIVE', description: null, attributes: null, address },
    2: { id: '301', name: 'P-301', code: 'R1', placeTypeId: '100', status: 'ACTIVE', description: null, attributes: null, address: null },
    3: { id: '301', name: 'P301', code: 'R1', placeTypeId: '100', status: 'ACTIVE', description: null, attributes: null, address: null },
  }
  const summary = (no: number) => ({
    versionNo: no,
    action: no === 1 ? 'CREATE' : 'UPDATE',
    changedFields: no === 1 ? [] : ['name'],
    changedBy: '42',
    changedAt: '2026-11-01T03:00:00Z',
    note: null,
  })
  server.use(
    http.get('/api/registry/places/301/versions', () =>
      HttpResponse.json({ items: [3, 2, 1].map(summary), nextCursor: null }),
    ),
    http.get('/api/registry/places/301/versions/:no', ({ params }) =>
      HttpResponse.json({ ...summary(Number(params.no)), snapshot: snapshots[Number(params.no)] }),
    ),
    http.post('/api/registry/places/301/versions/:no/restore', async ({ request, params }) => {
      const body = (await request.json()) as { version: number }
      const place = backend.places.get('301')!
      if (body.version !== place.version) {
        return HttpResponse.json({ ...problem(409, 'Place 301 was changed by someone else'), code: 'VERSION_CONFLICT' }, { status: 409 })
      }
      const snapshot = snapshots[Number(params.no)]
      backend.places.set('301', { ...place, name: snapshot.name as string, version: place.version + 1 })
      return HttpResponse.json({ ...place, name: snapshot.name, version: place.version + 1, ancestors: [{ id: '1', name: 'Toà A' }] })
    }),
  )
  return backend
}

async function main() {
  return within(await screen.findByRole('main'))
}

async function openVersion(no: number) {
  const app = renderApp({ route: '/w/7/places/301?tab=history', userId: '42' })
  await app.user.click(await (await main()).findByRole('button', { name: `v${no}` }))
  return { ...app, dialog: within(await screen.findByRole('dialog', { name: `Bản v${no}` })) }
}

test('placeHistoryShowsAddressOfThatVersion', async () => {
  setUp()
  const { user } = renderApp({ route: '/w/7/places/301?tab=history', userId: '42' })
  const view = await main()
  expect((await view.findAllByText('Đã đổi: Tên')).length).toBe(2)

  await user.click(view.getByRole('button', { name: 'v1' }))

  const dialog = within(await screen.findByRole('dialog', { name: 'Bản v1' }))
  expect(await dialog.findByText('Phòng 301')).toBeInTheDocument()
  expect(dialog.getByText('1 Tràng Tiền')).toBeInTheDocument()
  expect(dialog.getByText('Phòng')).toBeInTheDocument()
})

test('restoringAPlaceVersionRenamesItInTheTree', async () => {
  setUp()
  const { user } = renderApp({ route: '/w/7/places/301?tab=history', userId: '42' })
  // Xem cây trước khi mở hộp thoại: hộp thoại ẩn phần còn lại của trang với công cụ đọc màn hình.
  const tree = within(await screen.findByRole('navigation', { name: 'Cây địa điểm' }))
  await tree.findByRole('link', { name: 'P301' })
  await user.click(await (await main()).findByRole('button', { name: 'v2' }))
  const dialog = within(await screen.findByRole('dialog', { name: 'Bản v2' }))

  await user.click(await dialog.findByRole('button', { name: 'Khôi phục bản này' }))
  await user.click(dialog.getByRole('button', { name: 'Khôi phục' }))

  expect(await (await main()).findByRole('heading', { name: 'P-301' })).toBeInTheDocument()
  await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Bản v2' })).not.toBeInTheDocument())
  expect(await tree.findByRole('link', { name: 'P-301' })).toBeInTheDocument()
})

test('restoreWithStaleVersionOffersReload', async () => {
  const backend = setUp()
  const { user, dialog } = await openVersion(1)
  backend.editElsewhere('301')

  await user.click(await dialog.findByRole('button', { name: 'Khôi phục bản này' }))
  await user.click(dialog.getByRole('button', { name: 'Khôi phục' }))
  await user.click(await dialog.findByRole('button', { name: 'Tải bản mới' }))
  await user.click(await dialog.findByRole('button', { name: 'Khôi phục' }))

  expect(await (await main()).findByRole('heading', { name: 'Phòng 301' })).toBeInTheDocument()
})

test('invalidOldContentExplainsWhy', async () => {
  setUp()
  server.use(
    http.post('/api/registry/places/301/versions/:no/restore', () =>
      HttpResponse.json(problem(400, 'Place type 100 is archived'), { status: 400 }),
    ),
  )
  const { user, dialog } = await openVersion(1)

  await user.click(await dialog.findByRole('button', { name: 'Khôi phục bản này' }))
  await user.click(dialog.getByRole('button', { name: 'Khôi phục' }))

  expect(await dialog.findByText('Place type 100 is archived')).toBeInTheDocument()
  expect(dialog.getByText(/Sửa tay bản hiện tại thay vì khôi phục/)).toBeInTheDocument()
})
