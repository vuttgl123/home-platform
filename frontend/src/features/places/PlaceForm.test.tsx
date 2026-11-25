import { screen, waitFor, within } from '@testing-library/react'
import { delay, http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { fieldDef, placeType, problem } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { mockPlaceBackend } from '@/test/placeBackend'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

import type { PlaceDetail } from './types'

const types = [
  placeType({ id: '100', code: 'BUILDING', name: 'Toà nhà', sortOrder: 10 }),
  placeType({ id: '101', code: 'FLOOR', name: 'Tầng', sortOrder: 20 }),
  placeType({ id: '102', code: 'ROOM', name: 'Phòng', sortOrder: 30 }),
  placeType({ id: '103', code: 'STORE', name: 'Kho', sortOrder: 40, status: 'ARCHIVED' }),
]

const areaDef = fieldDef({ id: '500', typeId: '102', fieldKey: 'area_m2', label: 'Diện tích (m²)', dataType: 'NUMBER' })

/** Một toà nhà, một tầng, một phòng; phòng có mục "Diện tích". */
function setUp(p301: Partial<PlaceDetail> = {}) {
  mockShell({ placeTypes: types })
  const backend = mockPlaceBackend([
    { id: '1', name: 'Toà A', placeTypeId: '100' },
    { id: '13', parentId: '1', name: 'Tầng 3', placeTypeId: '101' },
    { id: '301', parentId: '13', name: 'P301', placeTypeId: '102', version: 4, code: 'R-301', coverFileId: '900', ...p301 },
  ])
  server.use(
    http.get('/api/registry/field-defs', ({ request }) =>
      HttpResponse.json(new URL(request.url).searchParams.get('typeId') === '102' ? [areaDef] : []),
    ),
  )
  return backend
}

async function main() {
  return within(await screen.findByRole('main'))
}

async function dialog(name: string) {
  return within(await screen.findByRole('dialog', { name }))
}

function tree() {
  return within(screen.getByRole('navigation', { name: 'Cây địa điểm' }))
}

/** Mở trang của P301 rồi mở form sửa. */
async function editP301() {
  const app = renderApp({ route: '/w/7/places/301', userId: '42' })
  await app.user.click(await (await main()).findByRole('button', { name: 'Sửa' }))
  const form = await dialog('Sửa địa điểm')
  // Mục tuỳ biến của loại "Phòng" tải xong.
  await form.findByRole('textbox', { name: 'Diện tích (m²)' })
  return { ...app, form }
}

test('createsATopLevelPlace', async () => {
  const backend = setUp()
  const { user, router } = renderApp({ route: '/w/7', userId: '42' })

  await user.click(await (await main()).findByRole('button', { name: 'Thêm địa điểm' }))
  const form = await dialog('Thêm địa điểm')
  expect(form.getByRole('combobox', { name: 'Loại' })).toHaveValue('100')
  await user.type(form.getByRole('textbox', { name: 'Tên' }), 'Toà B')
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  await waitFor(() => expect(router.state.location.pathname).toBe('/w/7/places/9000'))
  expect(backend.writes).toHaveLength(1)
  expect(backend.writes[0].body).toEqual({ placeTypeId: '100', name: 'Toà B', status: 'ACTIVE' })
  expect(await (await main()).findByRole('heading', { name: 'Toà B' })).toBeInTheDocument()
  expect(tree().getByRole('link', { name: 'Toà B' })).toBeInTheDocument()
})

test('createsAChildFromTheChildrenTab', async () => {
  const backend = setUp()
  const { user, router } = renderApp({ route: '/w/7/places/13?tab=children', userId: '42' })

  await user.click(await (await main()).findByRole('button', { name: 'Thêm chỗ con' }))
  const form = await dialog('Thêm địa điểm')
  // Loại gợi ý là loại đứng sau loại của cha: trong tầng thường là phòng.
  expect(form.getByRole('combobox', { name: 'Loại' })).toHaveValue('102')
  await user.type(form.getByRole('textbox', { name: 'Tên' }), 'P302')
  await user.type(await form.findByRole('textbox', { name: 'Diện tích (m²)' }), '30')
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  await waitFor(() => expect(router.state.location.pathname).toBe('/w/7/places/9000'))
  expect(backend.writes[0].body).toEqual({
    parentId: '13',
    placeTypeId: '102',
    name: 'P302',
    status: 'ACTIVE',
    attributes: { area_m2: 30 },
  })
  expect(await tree().findByRole('link', { name: 'P302' })).toHaveAttribute('aria-current', 'page')
})

test('archivedTypesAreNotOffered', async () => {
  setUp()
  const { user } = renderApp({ route: '/w/7', userId: '42' })

  await user.click(await (await main()).findByRole('button', { name: 'Thêm địa điểm' }))
  const form = await dialog('Thêm địa điểm')

  const options = within(form.getByRole('combobox', { name: 'Loại' })).getAllByRole('option')
  expect(options.map((option) => option.textContent)).toEqual(['Toà nhà', 'Tầng', 'Phòng'])
})

test('blankNameIsStoppedBeforeSending', async () => {
  const backend = setUp()
  const { user } = renderApp({ route: '/w/7', userId: '42' })

  await user.click(await (await main()).findByRole('button', { name: 'Thêm địa điểm' }))
  const form = await dialog('Thêm địa điểm')
  await user.type(form.getByRole('textbox', { name: 'Tên' }), '   ')
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  expect(await form.findByText('Nhập tên')).toBeInTheDocument()
  expect(backend.writes).toEqual([])
})

test('editSendsUntouchedFieldsBack', async () => {
  // PUT ghi đè cả bản ghi: trường nào form không gửi lại là bị xoá.
  const backend = setUp()
  const { user, form } = await editP301()

  await user.click(form.getByRole('button', { name: 'Lưu' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toEqual({
    version: 4,
    placeTypeId: '102',
    name: 'P301',
    status: 'ACTIVE',
    coverFileId: '900',
  })
})

test('formHasNoCodeBox', async () => {
  setUp()
  const { user } = renderApp({ route: '/w/7', userId: '42' })

  await user.click(await (await main()).findByRole('button', { name: 'Thêm địa điểm' }))
  const form = await dialog('Thêm địa điểm')

  expect(form.queryByRole('textbox', { name: 'Mã' })).not.toBeInTheDocument()
  expect(form.getByText('Mã được cấp khi lưu.')).toBeInTheDocument()
})

test('createdToastShowsTheIssuedCode', async () => {
  setUp()
  const { user } = renderApp({ route: '/w/7', userId: '42' })

  await user.click(await (await main()).findByRole('button', { name: 'Thêm địa điểm' }))
  const form = await dialog('Thêm địa điểm')
  await user.type(form.getByRole('textbox', { name: 'Tên' }), 'Toà B')
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  expect(await screen.findByText('Đã thêm Toà B (DD-000001)')).toBeInTheDocument()
})

test('editShowsTheLockedCode', async () => {
  setUp()
  const { form } = await editP301()

  expect(form.queryByRole('textbox', { name: 'Mã' })).not.toBeInTheDocument()
  expect(form.getByText('Mã R-301 · do hệ thống cấp, không sửa được')).toBeInTheDocument()
})

test('editOfARecordWithoutCodeShowsNoCodeLine', async () => {
  setUp()
  const { user } = renderApp({ route: '/w/7/places/13', userId: '42' })
  await user.click(await (await main()).findByRole('button', { name: 'Sửa' }))
  const form = await dialog('Sửa địa điểm')
  await waitFor(() => expect(form.getByRole('button', { name: 'Lưu' })).toBeEnabled())

  expect(form.queryByText(/do hệ thống cấp/)).not.toBeInTheDocument()
})

test('capacityIsSentAsANumber', async () => {
  const backend = setUp()
  const { user, form } = await editP301()

  expect(form.getByText('Bỏ trống nếu không giới hạn')).toBeInTheDocument()
  await user.type(form.getByRole('spinbutton', { name: 'Sức chứa (người)' }), '4')
  await user.click(form.getByRole('button', { name: 'Lưu' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toMatchObject({ capacity: 4 })
})

test('emptyCapacityMeansNone', async () => {
  // PUT ghi đè cả bản ghi: không gửi `capacity` là bỏ sức chứa.
  const backend = setUp({ capacity: 3 })
  const { user, form } = await editP301()

  const capacity = form.getByRole('spinbutton', { name: 'Sức chứa (người)' })
  expect(capacity).toHaveValue(3)
  await user.clear(capacity)
  await user.click(form.getByRole('button', { name: 'Lưu' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).not.toHaveProperty('capacity')
})

test('capacityMustBeAWholeNumberInRange', async () => {
  const backend = setUp()
  const { user, form } = await editP301()
  const capacity = form.getByRole('spinbutton', { name: 'Sức chứa (người)' })

  for (const typed of ['-1', '1.5', '10000']) {
    await user.clear(capacity)
    await user.type(capacity, typed)
    await user.click(form.getByRole('button', { name: 'Lưu' }))
    expect(await form.findByText('Nhập số nguyên từ 0 đến 9999')).toBeInTheDocument()
  }
  expect(backend.writes).toHaveLength(0)
})

test('numberAttributeIsSentAsNumber', async () => {
  const backend = setUp()
  const { user, form } = await editP301()

  await user.type(form.getByRole('textbox', { name: 'Diện tích (m²)' }), '42')
  await user.click(form.getByRole('button', { name: 'Lưu' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect((backend.writes[0].body as { attributes: unknown }).attributes).toEqual({ area_m2: 42 })
})

test('showsFieldErrorsFromServer', async () => {
  setUp()
  server.use(
    http.put('/api/registry/places/301', () =>
      HttpResponse.json(
        problem(400, 'Invalid attributes', [{ field: 'attributes.area_m2', message: 'must be a number' }]),
        { status: 400 },
      ),
    ),
  )
  const { user, form } = await editP301()

  await user.type(form.getByRole('textbox', { name: 'Diện tích (m²)' }), '5')
  await user.click(form.getByRole('button', { name: 'Lưu' }))

  const input = form.getByRole('textbox', { name: 'Diện tích (m²)' })
  await waitFor(() => expect(input).toHaveAttribute('aria-invalid', 'true'))
  expect(within(input.closest('[role="group"]') as HTMLElement).getByText('must be a number')).toBeInTheDocument()
})

test('versionConflictKeepsTypedValues', async () => {
  const backend = setUp()
  const { user, form } = await editP301()
  backend.editElsewhere('301', { description: 'Người khác vừa sửa' })

  const name = form.getByRole('textbox', { name: 'Tên' })
  await user.clear(name)
  await user.type(name, 'P301 mới')
  await user.click(form.getByRole('button', { name: 'Lưu' }))

  expect(await form.findByText('Đã có người sửa trước bạn.')).toBeInTheDocument()
  expect(form.getByRole('textbox', { name: 'Tên' })).toHaveValue('P301 mới')
})

test('reloadKeepsMyEditsAndTakesNewVersion', async () => {
  const backend = setUp()
  const { user, form } = await editP301()
  backend.editElsewhere('301', { description: 'Mô tả mới' })
  const name = form.getByRole('textbox', { name: 'Tên' })
  await user.clear(name)
  await user.type(name, 'P301 mới')
  await user.click(form.getByRole('button', { name: 'Lưu' }))

  await user.click(await form.findByRole('button', { name: 'Tải bản mới' }))

  // Ô đã sửa giữ chữ của mình; ô chưa đụng tới nhận giá trị mới.
  await waitFor(() => expect(form.getByRole('textbox', { name: 'Mô tả' })).toHaveValue('Mô tả mới'))
  expect(form.getByRole('textbox', { name: 'Tên' })).toHaveValue('P301 mới')
  expect(form.queryByText('Đã có người sửa trước bạn.')).not.toBeInTheDocument()

  await user.click(form.getByRole('button', { name: 'Lưu' }))

  await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Sửa địa điểm' })).not.toBeInTheDocument())
  expect(backend.writes.at(-1)?.body).toMatchObject({ version: 5, name: 'P301 mới', description: 'Mô tả mới' })
})

test('renameUpdatesTreeAndHeader', async () => {
  setUp()
  const { user } = renderApp({ route: '/w/7/places/301', userId: '42' })
  // Xem cây trước khi mở hộp thoại: hộp thoại ẩn phần còn lại của trang với công cụ đọc màn hình.
  await within(await screen.findByRole('navigation', { name: 'Cây địa điểm' })).findByRole('link', { name: 'P301' })
  await user.click(await (await main()).findByRole('button', { name: 'Sửa' }))
  const form = await dialog('Sửa địa điểm')

  const name = form.getByRole('textbox', { name: 'Tên' })
  await user.clear(name)
  await user.type(name, 'Phòng 301')
  await user.click(form.getByRole('button', { name: 'Lưu' }))

  expect(await (await main()).findByRole('heading', { name: 'Phòng 301' })).toBeInTheDocument()
  await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Sửa địa điểm' })).not.toBeInTheDocument())
  expect(await tree().findByRole('link', { name: 'Phòng 301' })).toBeInTheDocument()
  expect(tree().queryByRole('link', { name: 'P301' })).not.toBeInTheDocument()
})

test('renameReachesDescendantBreadcrumbs', async () => {
  setUp()
  const { user } = renderApp({ route: '/w/7/places/301', userId: '42' })
  const trail = within(await (await main()).findByRole('navigation', { name: 'Đường dẫn' }))

  await user.click(trail.getByRole('link', { name: 'Toà A' }))
  await user.click(await (await main()).findByRole('button', { name: 'Sửa' }))
  const form = await dialog('Sửa địa điểm')
  const name = form.getByRole('textbox', { name: 'Tên' })
  await user.clear(name)
  await user.type(name, 'Toà Alpha')
  await user.click(form.getByRole('button', { name: 'Lưu' }))
  await (await main()).findByRole('heading', { name: 'Toà Alpha' })

  await user.click(tree().getByRole('link', { name: 'P301' }))

  const newTrail = within(await (await main()).findByRole('navigation', { name: 'Đường dẫn' }))
  expect(await newTrail.findByRole('link', { name: 'Toà Alpha' })).toBeInTheDocument()
})

test('suggestedTypeArrivesWhenTypesLoadLate', async () => {
  const backend = setUp()
  server.use(
    http.get('/api/registry/place-types', async () => {
      await delay(150)
      return HttpResponse.json(types)
    }),
    http.get('/api/registry/people', () => HttpResponse.json({ items: [], nextCursor: null })),
  )
  // Trang Người không tải loại địa điểm trước.
  const { user } = renderApp({ route: '/w/7/people', userId: '42' })
  const tree = within(await screen.findByRole('navigation', { name: 'Cây địa điểm' }))

  await user.click(await tree.findByRole('button', { name: 'Thêm địa điểm' }))

  const form = await dialog('Thêm địa điểm')
  await waitFor(() => expect(form.getByRole('combobox', { name: 'Loại' })).toHaveValue('100'))
  // Ô chọn hiện "Toà nhà" thì giá trị gửi đi cũng phải là nó.
  await user.type(form.getByRole('textbox', { name: 'Tên' }), 'Toà C')
  await user.click(form.getByRole('button', { name: 'Thêm' }))
  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toMatchObject({ placeTypeId: '100' })
})
