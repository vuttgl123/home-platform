import { screen, waitFor, within } from '@testing-library/react'
import { expect, test } from 'vitest'

import { mockCatalogBackend } from '@/test/catalogBackend'
import { mockShell } from '@/test/handlers'
import { mockPlaceBackend } from '@/test/placeBackend'
import { renderApp } from '@/test/render'

import { suggestFieldKey } from './fieldKeys'

function setUp(options: Parameters<typeof mockCatalogBackend>[0] = {}) {
  mockShell()
  return mockCatalogBackend({
    placeTypes: [
      { id: '100', code: 'BUILDING', name: 'Toà nhà', sortOrder: 10 },
      { id: '101', code: 'STORE', name: 'Kho', sortOrder: 20 },
    ],
    itemCategories: [{ id: '600', name: 'Nội thất', sortOrder: 10 }],
    fieldDefs: [{ id: '500', target: 'PLACE', typeId: '0', fieldKey: 'area_m2', label: 'Diện tích (m²)', dataType: 'NUMBER' }],
    ...options,
  })
}

async function section(name: string) {
  return within(await screen.findByRole('region', { name }))
}

async function rowAction(region: Awaited<ReturnType<typeof section>>, rowName: string, action: string, user: ReturnType<typeof renderApp>['user']) {
  await user.click(await region.findByRole('button', { name: `Thao tác với ${rowName}` }))
  await user.click(await screen.findByRole('menuitem', { name: action }))
}

test('listsTheThreeCatalogs', async () => {
  setUp()

  renderApp({ route: '/w/7/catalog', userId: '42' })

  const types = await section('Loại địa điểm')
  expect(await types.findByText('Toà nhà')).toBeInTheDocument()
  expect(types.getByText('BUILDING')).toBeInTheDocument()
  expect(await (await section('Nhóm đồ')).findByText('Nội thất')).toBeInTheDocument()
  const fields = await section('Mục tuỳ biến')
  expect(await fields.findByText('Diện tích (m²)')).toBeInTheDocument()
  expect(fields.getByText('Mọi địa điểm')).toBeInTheDocument()
  expect(screen.getByRole('link', { name: 'Danh mục' })).toHaveAttribute('aria-current', 'page')
})

test('placeTypeCodeIsUppercased', async () => {
  const backend = setUp()
  const { user } = renderApp({ route: '/w/7/catalog', userId: '42' })

  await user.click(await (await section('Loại địa điểm')).findByRole('button', { name: 'Thêm loại' }))
  const dialog = within(await screen.findByRole('dialog', { name: 'Thêm loại địa điểm' }))
  await user.type(dialog.getByRole('textbox', { name: 'Mã' }), 'kho_lanh')
  expect(dialog.getByRole('textbox', { name: 'Mã' })).toHaveValue('KHO_LANH')
  await user.type(dialog.getByRole('textbox', { name: 'Tên' }), 'Kho lạnh')
  await user.click(dialog.getByRole('button', { name: 'Thêm' }))

  expect(await (await section('Loại địa điểm')).findByText('Kho lạnh')).toBeInTheDocument()
  expect(backend.writes[0]).toMatchObject({ method: 'POST', path: '/place-types', body: { code: 'KHO_LANH', name: 'Kho lạnh' } })
})

test('deletingAUsedTypeSuggestsArchiving', async () => {
  const backend = setUp({ usedPlaceTypeIds: ['101'] })
  const { user } = renderApp({ route: '/w/7/catalog', userId: '42' })
  const types = await section('Loại địa điểm')

  await rowAction(types, 'Kho', 'Xoá', user)
  const dialog = within(await screen.findByRole('alertdialog', { name: 'Xoá Kho?' }))
  await user.click(dialog.getByRole('button', { name: 'Xoá' }))
  expect(await dialog.findByText('PlaceType 101 is used by places; archive it instead')).toBeInTheDocument()
  await user.click(dialog.getByRole('button', { name: 'Lưu trữ thay vì xoá' }))

  await waitFor(() => expect(backend.types.find((type) => type.id === '101')?.status).toBe('ARCHIVED'))
  expect(await types.findByText('Đã lưu trữ')).toBeInTheDocument()
})

test('archivedTypeLeavesNewFormsButStaysOnOldPlaces', async () => {
  setUp()
  mockPlaceBackend([{ id: '1', name: 'Kho A', placeTypeId: '101' }])
  const { user, router } = renderApp({ route: '/w/7/catalog', userId: '42' })
  await rowAction(await section('Loại địa điểm'), 'Kho', 'Lưu trữ', user)
  await (await section('Loại địa điểm')).findByText('Đã lưu trữ')

  await router.navigate('/w/7/places/1')
  const main = within(await screen.findByRole('main'))
  expect(await main.findByText('Kho')).toBeInTheDocument()
  await user.click(within(screen.getByRole('navigation', { name: 'Cây địa điểm' })).getByRole('button', { name: 'Thêm địa điểm' }))
  const form = within(await screen.findByRole('dialog', { name: 'Thêm địa điểm' }))
  const options = within(form.getByRole('combobox', { name: 'Loại' })).getAllByRole('option')
  expect(options.map((option) => option.textContent)).toEqual(['Toà nhà'])
})

test('fieldKeyIsSuggestedFromTheLabel', async () => {
  expect(suggestFieldKey('Diện tích sàn')).toBe('dien_tich_san')
  expect(suggestFieldKey('Đường dây  điện (kW)')).toBe('duong_day_dien_kw')
  expect(suggestFieldKey('2 tầng')).toBe('f_2_tang')

  const backend = setUp()
  const { user } = renderApp({ route: '/w/7/catalog', userId: '42' })
  await user.click(await (await section('Mục tuỳ biến')).findByRole('button', { name: 'Thêm mục' }))
  const dialog = within(await screen.findByRole('dialog', { name: 'Thêm mục tuỳ biến' }))

  await user.type(dialog.getByRole('textbox', { name: 'Nhãn' }), 'Số giường')
  expect(dialog.getByRole('textbox', { name: 'Khoá' })).toHaveValue('so_giuong')
  await user.click(dialog.getByRole('button', { name: 'Thêm' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toMatchObject({
    target: 'PLACE',
    typeId: '0',
    fieldKey: 'so_giuong',
    label: 'Số giường',
    dataType: 'TEXT',
    required: false,
  })
})

test('selectNeedsOptions', async () => {
  const backend = setUp()
  const { user } = renderApp({ route: '/w/7/catalog', userId: '42' })
  await user.click(await (await section('Mục tuỳ biến')).findByRole('button', { name: 'Thêm mục' }))
  const dialog = within(await screen.findByRole('dialog', { name: 'Thêm mục tuỳ biến' }))

  await user.type(dialog.getByRole('textbox', { name: 'Nhãn' }), 'Hướng')
  await user.selectOptions(dialog.getByRole('combobox', { name: 'Kiểu' }), 'Chọn từ danh sách')
  await user.click(dialog.getByRole('button', { name: 'Thêm' }))
  expect(await dialog.findByText('Nhập ít nhất một lựa chọn, mỗi dòng một lựa chọn')).toBeInTheDocument()
  expect(backend.writes).toEqual([])

  await user.type(dialog.getByRole('textbox', { name: 'Lựa chọn' }), 'Đông{enter}Tây{enter}Đông')
  await user.click(dialog.getByRole('button', { name: 'Thêm' }))
  expect(await dialog.findByText('Các lựa chọn không được trùng nhau')).toBeInTheDocument()

  await user.clear(dialog.getByRole('textbox', { name: 'Lựa chọn' }))
  await user.type(dialog.getByRole('textbox', { name: 'Lựa chọn' }), 'Đông{enter}Tây')
  await user.click(dialog.getByRole('button', { name: 'Thêm' }))
  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toMatchObject({ dataType: 'SELECT', options: ['Đông', 'Tây'] })
})

test('fieldKeyAndTypeAreReadOnlyWhenEditing', async () => {
  const backend = setUp()
  const { user } = renderApp({ route: '/w/7/catalog', userId: '42' })

  await rowAction(await section('Mục tuỳ biến'), 'Diện tích (m²)', 'Sửa', user)
  const dialog = within(await screen.findByRole('dialog', { name: 'Sửa mục tuỳ biến' }))
  expect(dialog.queryByRole('textbox', { name: 'Khoá' })).not.toBeInTheDocument()
  expect(dialog.getByText('area_m2')).toBeInTheDocument()
  expect(dialog.getByText(/không đổi được sau khi tạo/)).toBeInTheDocument()
  await user.click(dialog.getByRole('switch', { name: 'Bắt buộc' }))
  await user.click(dialog.getByRole('button', { name: 'Lưu' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0]).toMatchObject({
    method: 'PUT',
    path: '/field-defs/500',
    body: { label: 'Diện tích (m²)', required: true, sortOrder: 10 },
  })
})

test('overlappingKeyConflictIsShown', async () => {
  setUp()
  const { user } = renderApp({ route: '/w/7/catalog', userId: '42' })
  await user.click(await (await section('Mục tuỳ biến')).findByRole('button', { name: 'Thêm mục' }))
  const dialog = within(await screen.findByRole('dialog', { name: 'Thêm mục tuỳ biến' }))

  await user.type(dialog.getByRole('textbox', { name: 'Nhãn' }), 'Diện tích')
  const key = dialog.getByRole('textbox', { name: 'Khoá' })
  await user.clear(key)
  await user.type(key, 'area_m2')
  await user.selectOptions(dialog.getByRole('combobox', { name: 'Áp dụng cho' }), 'Kho')
  await user.click(dialog.getByRole('button', { name: 'Thêm' }))

  expect(await dialog.findByText('Field area_m2 already exists for this target and type')).toBeInTheDocument()
})

test('itemCategoryIsAddedAndArchived', async () => {
  const backend = setUp()
  const { user } = renderApp({ route: '/w/7/catalog', userId: '42' })
  const categories = await section('Nhóm đồ')

  await user.click(await categories.findByRole('button', { name: 'Thêm nhóm' }))
  const dialog = within(await screen.findByRole('dialog', { name: 'Thêm nhóm đồ' }))
  await user.type(dialog.getByRole('textbox', { name: 'Tên' }), 'Điện lạnh')
  await user.click(dialog.getByRole('button', { name: 'Thêm' }))
  await categories.findByText('Điện lạnh')
  await rowAction(categories, 'Nội thất', 'Lưu trữ', user)

  await waitFor(() => expect(backend.categories.find((category) => category.id === '600')?.status).toBe('ARCHIVED'))
})
