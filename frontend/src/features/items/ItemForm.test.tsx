import { screen, waitFor, within } from '@testing-library/react'
import { delay, http, HttpResponse } from 'msw'
import { MemoryRouter } from 'react-router'
import { expect, test } from 'vitest'

import { setCurrentUser } from '@/api'
import { Toaster } from '@/components/ui/toast'
import { fieldDef, itemCategory, page, person } from '@/test/fixtures'
import { mockItemBackend } from '@/test/itemBackend'
import { renderWithProviders } from '@/test/render'
import { server } from '@/test/server'

import { ItemFormDialog } from './components/ItemFormDialog'
import type { ItemDetail } from './types'

function mockCatalog() {
  server.use(
    http.get('/api/registry/people/search', () => HttpResponse.json(page([person({ id: '8000', fullName: 'An' })]))),
    http.get('/api/registry/item-categories', () =>
      HttpResponse.json([
        itemCategory({ id: '600', name: 'Nội thất', sortOrder: 10 }),
        itemCategory({ id: '601', name: 'Điện tử', sortOrder: 20 }),
        itemCategory({ id: '602', name: 'Cũ', sortOrder: 30, status: 'ARCHIVED' }),
      ]),
    ),
    http.get('/api/registry/field-defs', ({ request }) => {
      const typeId = new URL(request.url).searchParams.get('typeId')
      const defs = {
        '600': [fieldDef({ target: 'ITEM', typeId: '600', fieldKey: 'material', label: 'Chất liệu', dataType: 'TEXT' })],
        '601': [fieldDef({ target: 'ITEM', typeId: '601', fieldKey: 'power_w', label: 'Công suất (W)', dataType: 'NUMBER' })],
      }[typeId ?? '']
      return HttpResponse.json(defs ?? [])
    }),
  )
}

async function openCreate() {
  setCurrentUser('42')
  mockCatalog()
  const app = renderWithProviders(
    <MemoryRouter>
      <Toaster />
      <ItemFormDialog ws="7" mode="create" place={{ id: '301', name: 'P301' }} open onOpenChange={() => {}} />
    </MemoryRouter>,
  )
  const form = within(await screen.findByRole('dialog', { name: 'Thêm đồ' }))
  await waitFor(() => expect(form.getByRole('button', { name: 'Thêm' })).toBeEnabled())
  return { ...app, form }
}

async function openEdit(item: ItemDetail) {
  setCurrentUser('42')
  mockCatalog()
  const app = renderWithProviders(
    <MemoryRouter>
      <ItemFormDialog ws="7" mode="edit" item={item} open onOpenChange={() => {}} />
    </MemoryRouter>,
  )
  const form = within(await screen.findByRole('dialog', { name: 'Sửa món đồ' }))
  await waitFor(() => expect(form.getByRole('button', { name: 'Lưu' })).toBeEnabled())
  return { ...app, form }
}

test('createsAnItemInThePlace', async () => {
  const backend = mockItemBackend({})
  const { user, form } = await openCreate()

  await user.type(form.getByRole('textbox', { name: 'Tên' }), 'Tủ lạnh')
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toEqual({ placeId: '301', name: 'Tủ lạnh', quantity: 1, status: 'IN_USE' })
})

test('itemFormHasNoCodeBox', async () => {
  mockItemBackend({})
  const { form } = await openCreate()

  expect(form.queryByRole('textbox', { name: 'Mã' })).not.toBeInTheDocument()
  expect(form.getByText('Mã được cấp khi lưu.')).toBeInTheDocument()
})

test('createdItemToastShowsTheIssuedCode', async () => {
  mockItemBackend({})
  const { user, form } = await openCreate()

  await user.type(form.getByRole('textbox', { name: 'Tên' }), 'Tủ lạnh')
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  expect(await screen.findByText('Đã thêm Tủ lạnh (TS-000001)')).toBeInTheDocument()
})

test('submittedItemBodyHasNoCode', async () => {
  const backend = mockItemBackend({ items: [{ id: '900', name: 'Tủ lạnh', code: 'TS-000005' }] })
  const { user, form } = await openEdit(backend.items.get('900')!)

  expect(form.getByText('Mã TS-000005 · do hệ thống cấp, không sửa được')).toBeInTheDocument()
  await user.click(form.getByRole('button', { name: 'Lưu' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).not.toHaveProperty('code')
})

test('quantityMustBeAPositiveWholeNumber', async () => {
  const backend = mockItemBackend({})
  const { user, form } = await openCreate()
  await user.type(form.getByRole('textbox', { name: 'Tên' }), 'Ghế')
  const quantity = form.getByRole('textbox', { name: 'Số lượng' })

  for (const value of ['1.5', '0', '-2']) {
    await user.clear(quantity)
    await user.type(quantity, value)
    await user.click(form.getByRole('button', { name: 'Thêm' }))
    expect(await form.findByText('Số nguyên từ 1 trở lên')).toBeInTheDocument()
  }
  expect(backend.writes).toEqual([])
})

test('archivedCategoriesAreNotOffered', async () => {
  mockItemBackend({})
  const { form } = await openCreate()

  const options = within(form.getByRole('combobox', { name: 'Nhóm' })).getAllByRole('option')
  expect(options.map((option) => option.textContent)).toEqual(['Không nhóm', 'Nội thất', 'Điện tử'])
})

test('changingCategoryDropsOldCategoryFields', async () => {
  const backend = mockItemBackend({})
  const { user, form } = await openCreate()
  await user.type(form.getByRole('textbox', { name: 'Tên' }), 'Máy lạnh')

  await user.selectOptions(form.getByRole('combobox', { name: 'Nhóm' }), 'Nội thất')
  await user.type(await form.findByRole('textbox', { name: 'Chất liệu' }), 'Gỗ')
  await user.selectOptions(form.getByRole('combobox', { name: 'Nhóm' }), 'Điện tử')
  await user.type(await form.findByRole('textbox', { name: 'Công suất (W)' }), '900')
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toMatchObject({ categoryId: '601', attributes: { power_w: 900 } })
  expect((backend.writes[0].body as { attributes: object }).attributes).not.toHaveProperty('material')
})

test('editSendsCoverAndPlacedAtBack', async () => {
  const backend = mockItemBackend({
    items: [{ id: '900', name: 'Tủ lạnh', unit: 'cái', placedAt: '2026-01-01', coverFileId: '77', quantity: 2 }],
  })
  const { user, form } = await openEdit(backend.items.get('900')!)

  await user.click(form.getByRole('button', { name: 'Lưu' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toEqual({
    version: 1,
    name: 'Tủ lạnh',
    quantity: 2,
    unit: 'cái',
    status: 'IN_USE',
    placedAt: '2026-01-01',
    coverFileId: '77',
  })
})

test('versionConflictKeepsTypedValues', async () => {
  const backend = mockItemBackend({ items: [{ id: '900', name: 'Tủ lạnh' }] })
  const { user, form } = await openEdit(backend.items.get('900')!)
  backend.editElsewhere('900', { description: 'Mới bảo trì' })

  const name = form.getByRole('textbox', { name: 'Tên' })
  await user.clear(name)
  await user.type(name, 'Tủ lạnh Toshiba')
  await user.click(form.getByRole('button', { name: 'Lưu' }))
  await user.click(await form.findByRole('button', { name: 'Tải bản mới' }))

  await waitFor(() => expect(form.getByRole('textbox', { name: 'Mô tả' })).toHaveValue('Mới bảo trì'))
  expect(form.getByRole('textbox', { name: 'Tên' })).toHaveValue('Tủ lạnh Toshiba')
})

test('editKeepsTheCategoryWhenCategoriesLoadLate', async () => {
  const backend = mockItemBackend({ items: [{ id: '900', name: 'Tủ lạnh', categoryId: '600' }] })
  setCurrentUser('42')
  mockCatalog()
  server.use(
    http.get('/api/registry/item-categories', async () => {
      await delay(150)
      return HttpResponse.json([itemCategory({ id: '600', name: 'Nội thất' })])
    }),
  )
  const { user } = renderWithProviders(
    <MemoryRouter>
      <ItemFormDialog ws="7" mode="edit" item={backend.items.get('900')!} open onOpenChange={() => {}} />
    </MemoryRouter>,
  )
  const form = within(await screen.findByRole('dialog', { name: 'Sửa món đồ' }))

  await waitFor(() => expect(form.getByRole('combobox', { name: 'Nhóm' })).toHaveValue('600'))
  await user.click(form.getByRole('button', { name: 'Lưu' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toMatchObject({ categoryId: '600' })
})

async function chooseOwner(user: Awaited<ReturnType<typeof openCreate>>['user'], form: Awaited<ReturnType<typeof openCreate>>['form']) {
  await user.click(form.getByRole('radio', { name: 'Của một người' }))
  await user.type(form.getByRole('searchbox', { name: 'Tìm người' }), 'An')
  await user.click(await form.findByRole('radio', { name: 'An' }))
}

test('newItemBelongsToThePlaceByDefault', async () => {
  const backend = mockItemBackend({})
  const { user, form } = await openCreate()

  expect(form.getByRole('radio', { name: 'Của địa điểm này' })).toBeChecked()
  await user.type(form.getByRole('textbox', { name: 'Tên' }), 'Ghế')
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).not.toHaveProperty('ownerPersonId')
})

test('createsAnItemOwnedByAPerson', async () => {
  const backend = mockItemBackend({})
  const { user, form } = await openCreate()

  await user.type(form.getByRole('textbox', { name: 'Tên' }), 'Tủ lạnh')
  await chooseOwner(user, form)
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toMatchObject({ name: 'Tủ lạnh', ownerPersonId: '8000' })
})

test('switchingBackToThePlaceSendsNoOwner', async () => {
  const backend = mockItemBackend({})
  const { user, form } = await openCreate()

  await user.type(form.getByRole('textbox', { name: 'Tên' }), 'Tủ lạnh')
  await chooseOwner(user, form)
  await user.click(form.getByRole('radio', { name: 'Của địa điểm này' }))
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).not.toHaveProperty('ownerPersonId')
})

test('choosingAPersonIsRequiredOnceAsked', async () => {
  const backend = mockItemBackend({})
  const { user, form } = await openCreate()

  await user.type(form.getByRole('textbox', { name: 'Tên' }), 'Tủ lạnh')
  await user.click(form.getByRole('radio', { name: 'Của một người' }))
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  expect(await form.findByText('Chọn một người')).toBeInTheDocument()
  expect(backend.writes).toEqual([])
})

test('editFormHasNoOwnerField', async () => {
  const backend = mockItemBackend({ items: [{ id: '900', name: 'Tủ lạnh' }] })
  const { form } = await openEdit(backend.items.get('900')!)

  expect(form.queryByRole('radio', { name: 'Của địa điểm này' })).not.toBeInTheDocument()
})
