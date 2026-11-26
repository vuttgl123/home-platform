import { screen, waitFor, within } from '@testing-library/react'
import { delay, http, HttpResponse } from 'msw'
import { expect, onTestFinished, test, vi } from 'vitest'

import { mockCatalogBackend } from '@/test/catalogBackend'
import { fieldDef } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { mockPlaceBackend } from '@/test/placeBackend'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

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
  await user.click(dialog.getByRole('button', { name: 'Tự đặt mã' }))
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

test('placeTypeCodeComesFromTheName', async () => {
  const backend = setUp()
  const { user } = renderApp({ route: '/w/7/catalog', userId: '42' })

  await user.click(await (await section('Loại địa điểm')).findByRole('button', { name: 'Thêm loại' }))
  const dialog = within(await screen.findByRole('dialog', { name: 'Thêm loại địa điểm' }))
  expect(dialog.queryByRole('textbox', { name: 'Mã' })).not.toBeInTheDocument()
  await user.type(dialog.getByRole('textbox', { name: 'Tên' }), 'Kho lạnh')
  expect(dialog.getByText('Mã: KHO_LANH')).toBeInTheDocument()
  await user.click(dialog.getByRole('button', { name: 'Thêm' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0]).toMatchObject({ path: '/place-types', body: { code: 'KHO_LANH', name: 'Kho lạnh' } })
})

test('listsBuiltInRulesWithExamples', async () => {
  setUp()
  renderApp({ route: '/w/7/catalog', userId: '42' })

  const codes = await section('Mã tự cấp')
  const places = within(await codes.findByRole('row', { name: /Mọi địa điểm/ }))
  expect(places.getByText('DD')).toBeInTheDocument()
  expect(places.getByText('DD-000001')).toBeInTheDocument()
  expect(places.getByText('(có sẵn)')).toBeInTheDocument()
  expect(within(codes.getByRole('row', { name: /Mọi đồ đạc/ })).getByText('TS-000001')).toBeInTheDocument()
})

test('exampleChangesWhileTyping', async () => {
  setUp()
  const { user } = renderApp({ route: '/w/7/catalog', userId: '42' })
  const codes = await section('Mã tự cấp')

  await user.click(await codes.findByRole('button', { name: 'Sửa Mọi địa điểm' }))
  const dialog = within(await screen.findByRole('dialog', { name: 'Dạng mã: Mọi địa điểm' }))
  const prefix = dialog.getByRole('textbox', { name: 'Tiền tố' })
  await user.clear(prefix)
  await user.type(prefix, 'p')
  await user.selectOptions(dialog.getByRole('combobox', { name: 'Số chữ số' }), '4')
  await user.click(dialog.getByRole('switch', { name: 'Bắt đầu lại mỗi năm' }))

  expect(prefix).toHaveValue('P')
  expect(dialog.getByText(new RegExp(`Ví dụ: P-\\d{4}-0001`))).toBeInTheDocument()
})

test('savesARuleForOnePlaceType', async () => {
  const backend = setUp()
  const { user } = renderApp({ route: '/w/7/catalog', userId: '42' })
  const codes = await section('Mã tự cấp')

  await user.click(await codes.findByRole('button', { name: 'Thêm dạng riêng' }))
  const dialog = within(await screen.findByRole('dialog', { name: 'Thêm dạng riêng' }))
  await user.selectOptions(dialog.getByRole('combobox', { name: 'Áp dụng cho' }), 'Loại: Kho')
  await user.type(dialog.getByRole('textbox', { name: 'Tiền tố' }), 'K')
  await user.selectOptions(dialog.getByRole('combobox', { name: 'Số chữ số' }), '4')
  await user.click(dialog.getByRole('button', { name: 'Lưu' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0]).toEqual({ method: 'PUT', path: '/code-rules/PLACE/101', body: { prefix: 'K', digits: 4, yearly: false } })
  expect(await codes.findByRole('row', { name: /Loại: Kho/ })).toBeInTheDocument()
})

test('removingAnOwnRuleFallsBackToTheSharedOne', async () => {
  const backend = setUp({ codeRules: [{ target: 'PLACE', typeId: '101', prefix: 'K', digits: 4, yearly: false }] })
  const { user } = renderApp({ route: '/w/7/catalog', userId: '42' })
  const codes = await section('Mã tự cấp')

  await user.click(await codes.findByRole('button', { name: 'Bỏ Loại: Kho' }))

  await waitFor(() => expect(backend.writes).toEqual([{ method: 'DELETE', path: '/code-rules/PLACE/101', body: null }]))
  await waitFor(() => expect(codes.queryByRole('row', { name: /Loại: Kho/ })).not.toBeInTheDocument())
})

test('warnsWhenThePrefixIsUsedByTheOtherTarget', async () => {
  setUp()
  const { user } = renderApp({ route: '/w/7/catalog', userId: '42' })
  const codes = await section('Mã tự cấp')

  await user.click(await codes.findByRole('button', { name: 'Sửa Mọi địa điểm' }))
  const dialog = within(await screen.findByRole('dialog', { name: 'Dạng mã: Mọi địa điểm' }))
  const prefix = dialog.getByRole('textbox', { name: 'Tiền tố' })
  await user.clear(prefix)
  await user.type(prefix, 'TS')

  expect(dialog.getByText('Địa điểm và đồ đạc sẽ có mã giống nhau')).toBeInTheDocument()
  expect(dialog.getByRole('button', { name: 'Lưu' })).toBeEnabled()
})

test('invalidPrefixIsBlockedBeforeSending', async () => {
  const backend = setUp()
  const { user } = renderApp({ route: '/w/7/catalog', userId: '42' })
  const codes = await section('Mã tự cấp')

  await user.click(await codes.findByRole('button', { name: 'Sửa Mọi địa điểm' }))
  const dialog = within(await screen.findByRole('dialog', { name: 'Dạng mã: Mọi địa điểm' }))
  const prefix = dialog.getByRole('textbox', { name: 'Tiền tố' })
  await user.clear(prefix)
  await user.type(prefix, '1A')
  await user.click(dialog.getByRole('button', { name: 'Lưu' }))

  expect(await dialog.findByText('1 đến 10 chữ cái không dấu hoặc số, bắt đầu bằng chữ')).toBeInTheDocument()
  expect(backend.writes).toEqual([])
})

test('backfillBannerIsHiddenWhenEverythingHasACode', async () => {
  setUp({ withoutCode: { places: 0, items: 0 } })
  renderApp({ route: '/w/7/catalog', userId: '42' })
  const codes = await section('Mã tự cấp')

  await codes.findByRole('row', { name: /Mọi địa điểm/ })
  expect(codes.queryByRole('button', { name: 'Cấp mã' })).not.toBeInTheDocument()
})

test('backfillRunsUntilNothingRemains', async () => {
  const backend = setUp({ withoutCode: { places: 200, items: 40 } })
  const { user } = renderApp({ route: '/w/7/catalog', userId: '42' })
  const codes = await section('Mã tự cấp')

  expect(await codes.findByText('Có 200 địa điểm và 40 món đồ chưa có mã.')).toBeInTheDocument()
  await user.click(codes.getByRole('button', { name: 'Cấp mã' }))

  expect(await screen.findByText('Đã cấp mã cho 240 bản ghi')).toBeInTheDocument()
  expect(backend.writes.filter((write) => write.path === '/codes/backfill')).toHaveLength(3)
  await waitFor(() => expect(codes.queryByRole('button', { name: 'Cấp mã' })).not.toBeInTheDocument())
})

test('backfillStopsWhenARoundAssignsNothing', async () => {
  const backend = setUp({ withoutCode: { places: 3, items: 0 }, backfillBatch: 0 })
  const { user } = renderApp({ route: '/w/7/catalog', userId: '42' })
  const codes = await section('Mã tự cấp')

  await user.click(await codes.findByRole('button', { name: 'Cấp mã' }))

  expect(await screen.findByText('Đã cấp mã cho 0 bản ghi')).toBeInTheDocument()
  expect(backend.writes.filter((write) => write.path === '/codes/backfill')).toHaveLength(1)
})

test('dateFieldCanRemind', async () => {
  const backend = setUp()
  const { user } = renderApp({ route: '/w/7/catalog', userId: '42' })
  await user.click(await (await section('Mục tuỳ biến')).findByRole('button', { name: 'Thêm mục' }))
  const dialog = within(await screen.findByRole('dialog', { name: 'Thêm mục tuỳ biến' }))

  await user.type(dialog.getByRole('textbox', { name: 'Nhãn' }), 'Hạn kiểm định PCCC')
  await user.selectOptions(dialog.getByRole('combobox', { name: 'Kiểu' }), 'Ngày')
  expect(dialog.queryByRole('textbox', { name: 'Nhắc trước (ngày)' })).not.toBeInTheDocument()
  await user.click(dialog.getByRole('switch', { name: 'Nhắc trong Việc cần làm' }))
  expect(dialog.getByRole('textbox', { name: 'Nhắc trước (ngày)' })).toHaveValue('30')
  await user.click(dialog.getByRole('button', { name: 'Thêm' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toMatchObject({ dataType: 'DATE', remindDays: 30 })
})

test('reminderOffSendsNoRemindDays', async () => {
  const backend = setUp({
    fieldDefs: [{ id: '501', target: 'PLACE', typeId: '0', fieldKey: 'fire_check', label: 'Kiểm định', dataType: 'DATE', remindDays: 30 }],
  })
  const { user } = renderApp({ route: '/w/7/catalog', userId: '42' })

  await rowAction(await section('Mục tuỳ biến'), 'Kiểm định', 'Sửa', user)
  const dialog = within(await screen.findByRole('dialog', { name: 'Sửa mục tuỳ biến' }))
  expect(dialog.getByRole('switch', { name: 'Nhắc trong Việc cần làm' })).toBeChecked()
  await user.click(dialog.getByRole('switch', { name: 'Nhắc trong Việc cần làm' }))
  await user.click(dialog.getByRole('button', { name: 'Lưu' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).not.toHaveProperty('remindDays')
})

test('otherTypesHaveNoReminder', async () => {
  setUp()
  const { user } = renderApp({ route: '/w/7/catalog', userId: '42' })
  await user.click(await (await section('Mục tuỳ biến')).findByRole('button', { name: 'Thêm mục' }))
  const dialog = within(await screen.findByRole('dialog', { name: 'Thêm mục tuỳ biến' }))

  expect(dialog.queryByRole('switch', { name: 'Nhắc trong Việc cần làm' })).not.toBeInTheDocument()
})

test('remindRange', async () => {
  const backend = setUp()
  const { user } = renderApp({ route: '/w/7/catalog', userId: '42' })
  await user.click(await (await section('Mục tuỳ biến')).findByRole('button', { name: 'Thêm mục' }))
  const dialog = within(await screen.findByRole('dialog', { name: 'Thêm mục tuỳ biến' }))
  await user.type(dialog.getByRole('textbox', { name: 'Nhãn' }), 'Hạn')
  await user.selectOptions(dialog.getByRole('combobox', { name: 'Kiểu' }), 'Ngày')
  await user.click(dialog.getByRole('switch', { name: 'Nhắc trong Việc cần làm' }))
  const days = dialog.getByRole('textbox', { name: 'Nhắc trước (ngày)' })

  for (const value of ['0', '366', 'x']) {
    await user.clear(days)
    await user.type(days, value)
    await user.click(dialog.getByRole('button', { name: 'Thêm' }))
    expect(await dialog.findByText('Số nguyên từ 1 tới 365')).toBeInTheDocument()
  }
  expect(backend.writes).toEqual([])
})

test('tableShowsTheReminder', async () => {
  setUp({
    fieldDefs: [
      { id: '501', target: 'PLACE', typeId: '0', fieldKey: 'fire_check', label: 'Kiểm định', dataType: 'DATE', remindDays: 45 },
      { id: '502', target: 'PLACE', typeId: '0', fieldKey: 'built', label: 'Năm xây', dataType: 'NUMBER' },
    ],
  })
  renderApp({ route: '/w/7/catalog', userId: '42' })

  const region = await section('Mục tuỳ biến')
  await region.findByText('Kiểm định')
  const rows = region.getAllByRole('row')
  expect(within(rows.find((row) => row.textContent?.includes('Kiểm định'))!).getByText('trước 45 ngày')).toBeInTheDocument()
  expect(within(rows.find((row) => row.textContent?.includes('Năm xây'))!).getByText('—')).toBeInTheDocument()
  expect(within(rows[0]).getByText('Nhắc')).toBeInTheDocument()
})

test('codeAnchorScrollsOnceTheSectionLoads', async () => {
  setUp()
  server.use(
    http.get('/api/registry/code-rules', async () => {
      await delay(300)
      return HttpResponse.json([])
    }),
  )
  const original = Element.prototype.scrollIntoView
  const scrolled = vi.fn()
  Element.prototype.scrollIntoView = scrolled
  onTestFinished(() => {
    Element.prototype.scrollIntoView = original
  })

  renderApp({ route: '/w/7/catalog#ma-tu-cap', userId: '42' })

  await waitFor(() => expect(scrolled).toHaveBeenCalled(), { timeout: 2000 })
  expect(scrolled.mock.contexts[0]).toHaveAttribute('id', 'ma-tu-cap')
})

test('codeAnchorWaitsForTheSectionsAboveIt', async () => {
  setUp()
  // Bảng mã tải xong trước, mục tuỳ biến (ở phía trên) tải sau: cuộn sớm thì phần Mã tự cấp bị đẩy đi.
  server.use(
    http.get('/api/registry/code-rules', async () => {
      await delay(50)
      return HttpResponse.json([])
    }),
    http.get('/api/registry/field-defs', async () => {
      await delay(400)
      return HttpResponse.json([fieldDef({ id: '500', target: 'PLACE', typeId: '0', fieldKey: 'area_m2', label: 'Diện tích (m²)', dataType: 'NUMBER' })])
    }),
  )
  const original = Element.prototype.scrollIntoView
  let fieldsWereThere = false
  const scrolled = vi.fn(() => {
    fieldsWereThere = screen.queryByText('Diện tích (m²)') != null
  })
  Element.prototype.scrollIntoView = scrolled
  onTestFinished(() => {
    Element.prototype.scrollIntoView = original
  })

  renderApp({ route: '/w/7/catalog#ma-tu-cap', userId: '42' })

  await waitFor(() => expect(scrolled).toHaveBeenCalled(), { timeout: 3000 })
  expect(fieldsWereThere).toBe(true)
  expect(scrolled).toHaveBeenCalledTimes(1)
})
