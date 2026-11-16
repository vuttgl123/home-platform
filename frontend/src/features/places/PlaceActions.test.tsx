import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { placeType, problem } from '@/test/fixtures'
import { mockShell, recordRequests } from '@/test/handlers'
import { mockPlaceBackend } from '@/test/placeBackend'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

const types = [
  placeType({ id: '100', code: 'BUILDING', name: 'Toà nhà', sortOrder: 10 }),
  placeType({ id: '101', code: 'FLOOR', name: 'Tầng', sortOrder: 20 }),
  placeType({ id: '102', code: 'ROOM', name: 'Phòng', sortOrder: 30 }),
]

const tranTien = {
  countryCode: 'VN',
  provinceCode: '01',
  provinceName: 'Hà Nội',
  districtCode: null,
  districtName: null,
  wardCode: null,
  wardName: 'Tràng Tiền',
  street: '1 Tràng Tiền',
  fullAddress: '1 Tràng Tiền, Hoàn Kiếm, Hà Nội',
  latitude: null,
  longitude: null,
  geohash: null,
}

function setUp(options: { addressOfA?: typeof tranTien } = {}) {
  mockShell({ placeTypes: types })
  return mockPlaceBackend([
    { id: '1', name: 'Toà A', placeTypeId: '100', address: options.addressOfA ?? null },
    { id: '2', name: 'Toà B', placeTypeId: '100' },
    { id: '13', parentId: '1', name: 'Tầng 3', placeTypeId: '101' },
    { id: '301', parentId: '13', name: 'P301', placeTypeId: '102', code: 'DD-000041', version: 3 },
    { id: '302', parentId: '13', name: 'P302', placeTypeId: '102' },
  ])
}

async function main() {
  return within(await screen.findByRole('main'))
}

/** Mở trang của địa điểm `id`, chọn `action` trong menu thao tác, trả về hộp thoại mở ra. */
async function openAction(
  id: string,
  action: 'Di chuyển' | 'Địa chỉ' | 'Xoá' | 'Cấp mã mới' | 'Cấp mã',
  dialogName: string | RegExp,
) {
  const app = renderApp({ route: `/w/7/places/${id}`, userId: '42' })
  await app.user.click(await (await main()).findByRole('button', { name: 'Thao tác khác' }))
  await app.user.click(await screen.findByRole('menuitem', { name: action }))
  const role = action === 'Xoá' || action.startsWith('Cấp mã') ? 'alertdialog' : 'dialog'
  return { ...app, dialog: within(await screen.findByRole(role, { name: dialogName })) }
}

test('movesToAnotherParent', async () => {
  const backend = setUp()
  const { user, dialog } = await openAction('13', 'Di chuyển', 'Di chuyển Tầng 3')

  await user.type(dialog.getByRole('searchbox', { name: 'Tìm nơi chuyển tới' }), 'Toà')
  await user.click(await dialog.findByRole('radio', { name: 'Toà B' }))
  await user.click(dialog.getByRole('button', { name: 'Chuyển' }))

  const trail = within(await (await main()).findByRole('navigation', { name: 'Đường dẫn' }))
  expect(await trail.findByRole('link', { name: 'Toà B' })).toBeInTheDocument()
  expect(backend.writes).toEqual([{ method: 'POST', path: '/places/13/move', body: { version: 1, parentId: '2' } }])
  // Cây hiện Tầng 3 dưới Toà B, không còn dưới Toà A.
  const tree = within(screen.getByRole('navigation', { name: 'Cây địa điểm' }))
  const underB = await waitFor(() => {
    const group = tree.getByRole('link', { name: 'Toà B' }).closest('li')!.querySelector('ul')
    expect(group).not.toBeNull()
    return within(group as HTMLElement)
  })
  expect(await underB.findByRole('link', { name: 'Tầng 3' })).toBeInTheDocument()
})

test('movesToTheTop', async () => {
  const backend = setUp()
  const { user, dialog } = await openAction('13', 'Di chuyển', 'Di chuyển Tầng 3')

  await user.click(dialog.getByRole('radio', { name: 'Lên cấp cao nhất' }))
  await user.click(dialog.getByRole('button', { name: 'Chuyển' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toEqual({ version: 1, parentId: null })
})

test('moveDoesNotOfferItselfOrTheCurrentParent', async () => {
  setUp()
  const { user, dialog } = await openAction('301', 'Di chuyển', 'Di chuyển P301')

  await user.type(dialog.getByRole('searchbox', { name: 'Tìm nơi chuyển tới' }), 'P')

  expect(await dialog.findByRole('radio', { name: 'P302' })).toBeInTheDocument()
  expect(dialog.queryByRole('radio', { name: 'P301' })).not.toBeInTheDocument()
  expect(dialog.getByRole('button', { name: 'Chuyển' })).toBeDisabled()

  // Chuyển vào chính chỗ đang chứa nó là không đổi gì.
  const search = dialog.getByRole('searchbox', { name: 'Tìm nơi chuyển tới' })
  await user.clear(search)
  await user.type(search, 'T')
  expect(await dialog.findByRole('radio', { name: 'Toà B' })).toBeInTheDocument()
  expect(dialog.queryByRole('radio', { name: 'Tầng 3' })).not.toBeInTheDocument()
})

test('moveShowsBusinessConflict', async () => {
  setUp()
  server.use(
    http.post('/api/registry/places/:id/move', () =>
      HttpResponse.json(problem(409, 'A place cannot be moved into itself or its descendants'), { status: 409 }),
    ),
  )
  const { user, dialog } = await openAction('13', 'Di chuyển', 'Di chuyển Tầng 3')

  await user.type(dialog.getByRole('searchbox', { name: 'Tìm nơi chuyển tới' }), 'Toà B')
  await user.click(await dialog.findByRole('radio', { name: 'Toà B' }))
  await user.click(dialog.getByRole('button', { name: 'Chuyển' }))

  expect(await dialog.findByText('A place cannot be moved into itself or its descendants')).toBeInTheDocument()
})

test('deleteShowsBusinessConflict', async () => {
  setUp()
  server.use(
    http.delete('/api/registry/places/13', () =>
      HttpResponse.json(problem(409, 'Place 13 still has 2 child places, 0 occupancies and 0 items'), { status: 409 }),
    ),
  )
  const { user, dialog, router } = await openAction('13', 'Xoá', 'Xoá Tầng 3?')

  await user.click(dialog.getByRole('button', { name: 'Xoá' }))

  expect(await dialog.findByText('Place 13 still has 2 child places, 0 occupancies and 0 items')).toBeInTheDocument()
  expect(router.state.location.pathname).toBe('/w/7/places/13')
})

test('deleteReturnsToParent', async () => {
  const backend = setUp()
  const { user, dialog, router } = await openAction('302', 'Xoá', 'Xoá P302?')
  const requests = recordRequests()

  await user.click(dialog.getByRole('button', { name: 'Xoá' }))

  await waitFor(() => expect(router.state.location.pathname).toBe('/w/7/places/13'))
  expect(await (await main()).findByRole('heading', { name: 'Tầng 3' })).toBeInTheDocument()
  expect(backend.places.has('302')).toBe(false)
  const tree = within(screen.getByRole('navigation', { name: 'Cây địa điểm' }))
  await waitFor(() => expect(tree.queryByRole('link', { name: 'P302' })).not.toBeInTheDocument())
  // Trang của địa điểm đã xoá không bị tải lại (sẽ là một lần 404 thừa).
  expect(requests.filter((request) => request === 'GET /api/registry/places/302')).toEqual([])
})

test('staleVersionCanBeReloaded', async () => {
  const backend = setUp()
  const { user, dialog } = await openAction('302', 'Xoá', 'Xoá P302?')
  backend.editElsewhere('302')

  await user.click(dialog.getByRole('button', { name: 'Xoá' }))
  await user.click(await dialog.findByRole('button', { name: 'Tải bản mới' }))
  await user.click(await dialog.findByRole('button', { name: 'Xoá' }))

  await waitFor(() => expect(backend.places.has('302')).toBe(false))
  expect(backend.writes.map((write) => write.path)).toEqual(['/places/302', '/places/302'])
})

test('setsAnAddress', async () => {
  const backend = setUp()
  const { user } = renderApp({ route: '/w/7/places/1', userId: '42' })
  await user.click(await (await main()).findByRole('button', { name: 'Thêm địa chỉ' }))
  const dialog = within(await screen.findByRole('dialog', { name: 'Địa chỉ của Toà A' }))

  await user.type(dialog.getByRole('textbox', { name: 'Địa chỉ đầy đủ' }), '1 Tràng Tiền, Hoàn Kiếm, Hà Nội')
  await user.type(dialog.getByRole('textbox', { name: 'Tỉnh/thành phố' }), 'Hà Nội')
  await user.click(dialog.getByRole('button', { name: 'Lưu' }))

  expect(await (await main()).findByText('1 Tràng Tiền, Hoàn Kiếm, Hà Nội')).toBeInTheDocument()
  expect(backend.writes).toEqual([
    {
      method: 'PUT',
      path: '/places/1/address',
      body: { version: 1, address: { fullAddress: '1 Tràng Tiền, Hoàn Kiếm, Hà Nội', provinceName: 'Hà Nội' } },
    },
  ])
})

test('editingAnAddressKeepsFieldsTheFormDoesNotShow', async () => {
  const backend = setUp({ addressOfA: tranTien })
  const { user, dialog } = await openAction('1', 'Địa chỉ', 'Địa chỉ của Toà A')

  const street = dialog.getByRole('textbox', { name: 'Số nhà, đường' })
  await user.clear(street)
  await user.type(street, '3 Tràng Tiền')
  await user.click(dialog.getByRole('button', { name: 'Lưu' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toEqual({
    version: 1,
    address: {
      countryCode: 'VN',
      provinceCode: '01',
      provinceName: 'Hà Nội',
      wardName: 'Tràng Tiền',
      street: '3 Tràng Tiền',
      fullAddress: '1 Tràng Tiền, Hoàn Kiếm, Hà Nội',
    },
  })
})

test('childSeesTheNewInheritedAddress', async () => {
  setUp()
  const { user } = renderApp({ route: '/w/7/places/301', userId: '42' })
  expect(await (await main()).findByText('Chưa có địa chỉ')).toBeInTheDocument()
  const trail = within(await (await main()).findByRole('navigation', { name: 'Đường dẫn' }))
  await user.click(trail.getByRole('link', { name: 'Toà A' }))
  await user.click(await (await main()).findByRole('button', { name: 'Thêm địa chỉ' }))
  const dialog = within(await screen.findByRole('dialog', { name: 'Địa chỉ của Toà A' }))
  await user.type(dialog.getByRole('textbox', { name: 'Địa chỉ đầy đủ' }), '1 Tràng Tiền')
  await user.click(dialog.getByRole('button', { name: 'Lưu' }))
  await (await main()).findByText('1 Tràng Tiền')

  await user.click(within(screen.getByRole('navigation', { name: 'Cây địa điểm' })).getByRole('link', { name: 'P301' }))

  expect(await (await main()).findByText(/lấy từ/)).toHaveTextContent('lấy từ Toà A')
})

test('coordinatesMustComeTogether', async () => {
  const backend = setUp()
  const { user, dialog } = await openAction('1', 'Địa chỉ', 'Địa chỉ của Toà A')

  await user.type(dialog.getByRole('textbox', { name: 'Địa chỉ đầy đủ' }), '1 Tràng Tiền')
  await user.type(dialog.getByRole('textbox', { name: 'Vĩ độ' }), '21.0245')
  await user.click(dialog.getByRole('button', { name: 'Lưu' }))

  expect(await dialog.findByText('Nhập cả vĩ độ và kinh độ, hoặc để trống cả hai')).toBeInTheDocument()
  expect(backend.writes).toEqual([])
})

test('coordinatesAreSentAsNumbers', async () => {
  const backend = setUp()
  const { user, dialog } = await openAction('1', 'Địa chỉ', 'Địa chỉ của Toà A')

  await user.type(dialog.getByRole('textbox', { name: 'Địa chỉ đầy đủ' }), '1 Tràng Tiền')
  await user.type(dialog.getByRole('textbox', { name: 'Vĩ độ' }), '21.0245')
  await user.type(dialog.getByRole('textbox', { name: 'Kinh độ' }), '105.8572')
  await user.click(dialog.getByRole('button', { name: 'Lưu' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toMatchObject({ address: { latitude: 21.0245, longitude: 105.8572 } })
})

test('removesTheAddress', async () => {
  const backend = setUp({ addressOfA: tranTien })
  const { user, dialog } = await openAction('1', 'Địa chỉ', 'Địa chỉ của Toà A')

  await user.click(dialog.getByRole('button', { name: 'Gỡ địa chỉ' }))

  expect(await (await main()).findByText('Chưa có địa chỉ')).toBeInTheDocument()
  expect(backend.writes).toEqual([{ method: 'DELETE', path: '/places/1/address', body: null }])
})

test('deletingTheLastChildMakesTheParentALeaf', async () => {
  mockShell({ placeTypes: types })
  mockPlaceBackend([
    { id: '1', name: 'Toà A', placeTypeId: '100' },
    { id: '13', parentId: '1', name: 'Tầng 3', placeTypeId: '101' },
  ])
  const { user } = renderApp({ route: '/w/7/places/13', userId: '42' })
  // Xem cây trước khi mở hộp thoại: hộp thoại ẩn phần còn lại của trang với công cụ đọc màn hình.
  const tree = within(await screen.findByRole('navigation', { name: 'Cây địa điểm' }))
  expect(await tree.findByRole('button', { name: /Toà A/ })).toBeInTheDocument()
  await user.click(await (await main()).findByRole('button', { name: 'Thao tác khác' }))
  await user.click(await screen.findByRole('menuitem', { name: 'Xoá' }))
  const dialog = within(await screen.findByRole('alertdialog', { name: 'Xoá Tầng 3?' }))

  await user.click(dialog.getByRole('button', { name: 'Xoá' }))

  await waitFor(() => expect(tree.queryByRole('button', { name: /Toà A/ })).not.toBeInTheDocument())
  expect(tree.getByRole('link', { name: 'Toà A' })).toBeInTheDocument()
})

test('reissueWarnsAboutTheOldCode', async () => {
  setUp()
  const { dialog } = await openAction('301', 'Cấp mã mới', 'Cấp mã mới cho P301?')

  expect(dialog.getByText(/Mã hiện tại DD-000041 sẽ không còn tìm ra bản ghi này/)).toBeInTheDocument()
  expect(dialog.getByText(/Nhãn đã in theo mã cũ sẽ sai/)).toBeInTheDocument()
})

test('reissueSendsTheVersionAndShowsTheNewCode', async () => {
  const backend = setUp()
  const { user, dialog } = await openAction('301', 'Cấp mã mới', 'Cấp mã mới cho P301?')

  await user.click(dialog.getByRole('button', { name: 'Cấp mã mới' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0]).toEqual({ method: 'POST', path: '/places/301/reissue-code', body: { version: 3 } })
  expect(await screen.findByText('Mã mới: DD-000001')).toBeInTheDocument()
  expect(await (await main()).findByText('DD-000001')).toBeInTheDocument()
})

test('recordWithoutCodeOffersToAssignOne', async () => {
  setUp()
  const { dialog } = await openAction('302', 'Cấp mã', 'Cấp mã cho P302?')

  expect(dialog.getByText(/Hệ thống sẽ cấp một mã theo dạng mã đang đặt trong Danh mục/)).toBeInTheDocument()
})

test('reissueConflictOffersToReload', async () => {
  const backend = setUp()
  const { user, dialog } = await openAction('301', 'Cấp mã mới', 'Cấp mã mới cho P301?')
  backend.editElsewhere('301')

  await user.click(dialog.getByRole('button', { name: 'Cấp mã mới' }))

  expect(await dialog.findByRole('button', { name: 'Tải bản mới' })).toBeInTheDocument()
})

test('undoRestoresTheDeletedPlaceAndReturnsToIt', async () => {
  const backend = setUp()
  const { user, dialog, router } = await openAction('302', 'Xoá', 'Xoá P302?')
  await user.click(dialog.getByRole('button', { name: 'Xoá' }))
  await waitFor(() => expect(router.state.location.pathname).toBe('/w/7/places/13'))

  await user.click(await screen.findByRole('button', { name: 'Hoàn tác' }))

  await waitFor(() => expect(router.state.location.pathname).toBe('/w/7/places/302'))
  expect(backend.writes.at(-1)).toEqual({ method: 'POST', path: '/places/302/restore', body: { version: 2 } })
  expect(await screen.findByText('Đã khôi phục P302')).toBeInTheDocument()
  expect(await (await main()).findByRole('heading', { name: 'P302' })).toBeInTheDocument()
})

test('undoAfterSomeoneElseChangedItSaysSo', async () => {
  setUp()
  const { user, dialog, router } = await openAction('302', 'Xoá', 'Xoá P302?')
  await user.click(dialog.getByRole('button', { name: 'Xoá' }))
  await waitFor(() => expect(router.state.location.pathname).toBe('/w/7/places/13'))
  server.use(
    http.post('/api/registry/places/302/restore', () =>
      HttpResponse.json({ ...problem(409, 'Place 302 was changed by someone else'), code: 'VERSION_CONFLICT' }, { status: 409 }),
    ),
  )

  await user.click(await screen.findByRole('button', { name: 'Hoàn tác' }))

  expect(await screen.findByText('Không hoàn tác được: bản ghi đã thay đổi')).toBeInTheDocument()
  expect(router.state.location.pathname).toBe('/w/7/places/13')
})
