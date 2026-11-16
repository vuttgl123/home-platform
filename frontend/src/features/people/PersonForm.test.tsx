import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { MemoryRouter } from 'react-router'
import { expect, test } from 'vitest'

import { setCurrentUser } from '@/api'
import { fieldDef } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { mockPeopleBackend } from '@/test/peopleBackend'
import { renderApp, renderWithProviders } from '@/test/render'
import { server } from '@/test/server'

import { PersonFormDialog } from './components/PersonFormDialog'
import type { PersonDetail } from './types'

async function openCreate() {
  const app = renderApp({ route: '/w/7/people', userId: '42' })
  const view = within(await screen.findByRole('main'))
  await app.user.click((await view.findAllByRole('button', { name: 'Thêm người' }))[0])
  return { ...app, form: within(await screen.findByRole('dialog', { name: 'Thêm người' })) }
}

function renderEdit(person: PersonDetail) {
  setCurrentUser('42')
  return renderWithProviders(
    <MemoryRouter>
      <PersonFormDialog ws="7" mode="edit" person={person} open onOpenChange={() => {}} />
    </MemoryRouter>,
  )
}

test('createsAPersonAndOpensTheirPage', async () => {
  mockShell()
  const backend = mockPeopleBackend({})
  const { user, form, router } = await openCreate()

  await user.type(form.getByRole('textbox', { name: 'Họ tên' }), 'Trần Bình')
  await user.type(form.getByRole('textbox', { name: 'Số điện thoại' }), '0909 123 456')
  await user.selectOptions(form.getByRole('combobox', { name: 'Giới tính' }), 'Nữ')
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  await waitFor(() => expect(router.state.location.pathname).toBe('/w/7/people/8000'))
  expect(backend.writes[0].body).toEqual({ fullName: 'Trần Bình', phone: '0909 123 456', gender: 'FEMALE' })
})

test('blankNameIsStoppedBeforeSending', async () => {
  mockShell()
  const backend = mockPeopleBackend({})
  const { user, form } = await openCreate()

  await user.click(form.getByRole('button', { name: 'Thêm' }))

  expect(await form.findByText('Nhập họ tên')).toBeInTheDocument()
  expect(backend.writes).toEqual([])
})

test('birthDateCannotBeInTheFuture', async () => {
  mockShell()
  const backend = mockPeopleBackend({})
  const { user, form } = await openCreate()

  await user.type(form.getByRole('textbox', { name: 'Họ tên' }), 'Trần Bình')
  await user.type(form.getByLabelText('Ngày sinh'), '2999-01-01')
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  expect(await form.findByText('Ngày sinh không được ở tương lai')).toBeInTheDocument()
  expect(backend.writes).toEqual([])
})

test('duplicateDocumentLinksToTheOtherPerson', async () => {
  mockShell()
  mockPeopleBackend({ people: [{ id: '77', fullName: 'Người đã có', idType: 'CCCD', idNumber: '001' }] })
  const { user, form } = await openCreate()

  await user.type(form.getByRole('textbox', { name: 'Họ tên' }), 'Trần Bình')
  await user.type(form.getByRole('combobox', { name: 'Loại giấy tờ' }), 'CCCD')
  await user.type(form.getByRole('textbox', { name: 'Số giấy tờ' }), '001')
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  expect(await form.findByText(/Đã có người mang giấy tờ này/)).toBeInTheDocument()
  expect(form.getByRole('link', { name: 'Mở hồ sơ đã có' })).toHaveAttribute('href', '/w/7/people/77')
})

test('personAttributesAreSent', async () => {
  mockShell()
  const backend = mockPeopleBackend({})
  server.use(
    http.get('/api/registry/field-defs', ({ request }) => {
      const query = new URL(request.url).searchParams
      return HttpResponse.json(
        query.get('target') === 'PERSON' && query.get('typeId') === '0'
          ? [fieldDef({ target: 'PERSON', typeId: '0', fieldKey: 'job', label: 'Nghề nghiệp', dataType: 'TEXT' })]
          : [],
      )
    }),
  )
  const { user, form } = await openCreate()

  await user.type(form.getByRole('textbox', { name: 'Họ tên' }), 'Trần Bình')
  await user.type(await form.findByRole('textbox', { name: 'Nghề nghiệp' }), 'Kỹ sư')
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toMatchObject({ attributes: { job: 'Kỹ sư' } })
})

test('editSendsAvatarBack', async () => {
  const backend = mockPeopleBackend({ people: [{ id: '55', fullName: 'An', avatarFileId: '900', phone: '0909' }] })
  server.use(http.get('/api/registry/field-defs', () => HttpResponse.json([])))
  const { user } = renderEdit(backend.people.get('55')!)
  const form = within(await screen.findByRole('dialog', { name: 'Sửa hồ sơ' }))

  await user.click(form.getByRole('button', { name: 'Lưu' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toEqual({ version: 1, fullName: 'An', phone: '0909', avatarFileId: '900' })
})

test('versionConflictKeepsTypedValues', async () => {
  const backend = mockPeopleBackend({ people: [{ id: '55', fullName: 'An' }] })
  server.use(http.get('/api/registry/field-defs', () => HttpResponse.json([])))
  const { user } = renderEdit(backend.people.get('55')!)
  const form = within(await screen.findByRole('dialog', { name: 'Sửa hồ sơ' }))
  backend.editPersonElsewhere('55', { note: 'Ghi chú mới' })

  const name = form.getByRole('textbox', { name: 'Họ tên' })
  await user.clear(name)
  await user.type(name, 'An Nguyễn')
  await user.click(form.getByRole('button', { name: 'Lưu' }))
  await user.click(await form.findByRole('button', { name: 'Tải bản mới' }))

  await waitFor(() => expect(form.getByRole('textbox', { name: 'Ghi chú' })).toHaveValue('Ghi chú mới'))
  expect(form.getByRole('textbox', { name: 'Họ tên' })).toHaveValue('An Nguyễn')
  await user.click(form.getByRole('button', { name: 'Lưu' }))
  await waitFor(() => expect(backend.writes.at(-1)?.body).toMatchObject({ version: 2, fullName: 'An Nguyễn' }))
})
