import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { fieldDef, page, place, placeType } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

const types = [
  placeType({ id: '100', code: 'BUILDING', name: 'Toà nhà', sortOrder: 10 }),
  placeType({ id: '102', code: 'ROOM', name: 'Phòng', sortOrder: 30 }),
]

const defs = [
  fieldDef({ id: '1', target: 'PLACE', typeId: '0', fieldKey: 'dien_tich', label: 'Diện tích', dataType: 'NUMBER' }),
  fieldDef({ id: '2', target: 'PLACE', typeId: '0', fieldKey: 'cu', label: 'Cũ', dataType: 'TEXT', status: 'ARCHIVED' }),
]

function row(overrides: Record<string, unknown> = {}) {
  return {
    placeId: '301',
    code: 'P-0301',
    name: 'P101',
    placeTypeId: '102',
    placeTypeName: 'Phòng',
    status: 'ACTIVE',
    ancestors: [
      { id: '1', name: 'Toà A' },
      { id: '13', name: 'Tầng 1' },
    ],
    capacity: 4,
    occupantCount: 1,
    free: 3,
    ...overrides,
  }
}

/** Trang Địa điểm; `pages` là các trang API trả lần lượt theo `cursor`. */
function setUp(pages: { items: object[]; nextCursor?: string }[] = [{ items: [row()] }]) {
  mockShell({ placeTypes: types, places: [place({ id: '1', name: 'Toà A' }), place({ id: '2', name: 'Toà B' })] })
  const requests: URL[] = []
  server.use(
    http.get('/api/registry/field-defs', ({ request }) =>
      HttpResponse.json(new URL(request.url).searchParams.get('target') === 'PLACE' ? defs : []),
    ),
    http.get('/api/registry/places/list', ({ request }) => {
      const url = new URL(request.url)
      requests.push(url)
      const index = url.searchParams.get('cursor') ? Number(url.searchParams.get('cursor')) : 0
      return HttpResponse.json(page(pages[index].items, pages[index].nextCursor ?? null))
    }),
  )
  return { requests }
}

async function main() {
  return within(await screen.findByRole('main'))
}

test('showsTheTableColumns', async () => {
  setUp([{ items: [row(), row({ placeId: '302', code: null, name: 'Kho', status: 'INACTIVE', capacity: null, ancestors: [] })] }])
  renderApp({ route: '/w/7/places', userId: '42' })

  const content = await main()
  expect(content.getByRole('heading', { name: 'Địa điểm' })).toBeInTheDocument()
  expect(await content.findByRole('link', { name: 'P101' })).toHaveAttribute('href', '/w/7/places/301')
  expect(content.getAllByRole('columnheader').map((cell) => cell.textContent)).toEqual([
    'Mã',
    'Tên',
    'Loại',
    'Nằm trong',
    'Sức chứa',
    'Đang ở',
    'Trạng thái',
  ])
  const cells = within(content.getAllByRole('row')[1])
    .getAllByRole('cell')
    .map((cell) => cell.textContent)
  expect(cells).toEqual(['P-0301', 'P101', 'Phòng', 'Toà A › Tầng 1', '4', '1', 'Đang dùng'])
  const second = within(content.getAllByRole('row')[2])
    .getAllByRole('cell')
    .map((cell) => cell.textContent)
  expect(second).toEqual(['—', 'Kho', 'Phòng', '—', '—', '1', 'Tạm ngưng'])
})

test('eachFilterGoesToTheUrlAndTheRequest', async () => {
  const { requests } = setUp()
  const { user, router } = renderApp({ route: '/w/7/places', userId: '42' })
  const content = await main()
  await content.findByRole('link', { name: 'P101' })
  const last = () => requests.at(-1)?.searchParams

  await user.type(content.getByRole('searchbox', { name: 'Tìm địa điểm' }), 'hue')
  await waitFor(() => expect(last()?.get('q')).toBe('hue'))
  expect(new URLSearchParams(router.state.location.search).get('q')).toBe('hue')

  await user.selectOptions(content.getByRole('combobox', { name: 'Loại' }), 'Phòng')
  await waitFor(() => expect(last()?.get('typeId')).toBe('102'))

  await user.selectOptions(content.getByRole('combobox', { name: 'Trạng thái' }), 'Tạm ngưng')
  await waitFor(() => expect(last()?.get('status')).toBe('INACTIVE'))

  await user.selectOptions(await content.findByRole('combobox', { name: 'Toà' }), 'Toà B')
  await waitFor(() => expect(last()?.get('rootId')).toBe('2'))

  await user.click(content.getByRole('checkbox', { name: 'Còn chỗ' }))
  await waitFor(() => expect(last()?.get('hasRoom')).toBe('true'))

  await user.click(await content.findByRole('button', { name: 'Thêm điều kiện' }))
  await user.selectOptions(content.getByRole('combobox', { name: 'Phép so của điều kiện 1' }), 'từ')
  await user.type(content.getByLabelText('Giá trị của điều kiện 1'), '20')
  await waitFor(() => expect(last()?.getAll('attr')).toEqual(['dien_tich:gte:20']))

  const url = new URLSearchParams(router.state.location.search)
  expect(Object.fromEntries(url)).toEqual({ q: 'hue', type: '102', status: 'INACTIVE', root: '2', room: '1', f: 'dien_tich~gte~20' })
})

test('urlFiltersAreReadOnLoad', async () => {
  const { requests } = setUp()
  renderApp({ route: '/w/7/places?q=ab&type=102&status=INACTIVE&root=2&room=1&f=dien_tich~gte~20', userId: '42' })

  const content = await main()
  await content.findByRole('link', { name: 'P101' })
  const first = requests[0].searchParams
  expect(Object.fromEntries(first)).toMatchObject({ q: 'ab', typeId: '102', status: 'INACTIVE', rootId: '2', hasRoom: 'true' })
  expect(first.getAll('attr')).toEqual(['dien_tich:gte:20'])
  expect(content.getByRole('searchbox', { name: 'Tìm địa điểm' })).toHaveValue('ab')
  expect(content.getByRole('checkbox', { name: 'Còn chỗ' })).toBeChecked()
  expect(content.getByLabelText('Giá trị của điều kiện 1')).toHaveValue(20)
})

test('archivedFieldInTheUrlIsIgnored', async () => {
  const { requests } = setUp()
  renderApp({ route: '/w/7/places?f=cu~set~', userId: '42' })

  const content = await main()
  await content.findByRole('link', { name: 'P101' })
  expect(requests.length).toBeGreaterThan(0)
  expect(requests.every((url) => !url.searchParams.has('attr'))).toBe(true)
})

test('strangeValuesAreIgnored', async () => {
  const { requests } = setUp()
  renderApp({ route: '/w/7/places?type=abc&status=X&root=zz&room=yes&q=a', userId: '42' })

  const content = await main()
  await content.findByRole('link', { name: 'P101' })
  expect([...requests[0].searchParams.keys()]).toEqual([])
})

test('emptyResultSaysWhy', async () => {
  setUp([{ items: [] }])
  renderApp({ route: '/w/7/places?q=zz', userId: '42' })

  expect(await (await main()).findByText('Không có địa điểm nào khớp bộ lọc')).toBeInTheDocument()
})

test('loadsMore', async () => {
  const { requests } = setUp([{ items: [row()], nextCursor: '1' }, { items: [row({ placeId: '302', name: 'P102' })] }])
  const { user } = renderApp({ route: '/w/7/places', userId: '42' })

  const content = await main()
  await content.findByRole('link', { name: 'P101' })
  await user.click(content.getByRole('button', { name: 'Tải thêm' }))

  expect(await content.findByRole('link', { name: 'P102' })).toBeInTheDocument()
  expect(requests.at(-1)?.searchParams.get('cursor')).toBe('1')
})

test('sidebarListsPlacesRightAfterTodos', async () => {
  setUp()
  renderApp({ route: '/w/7/places', userId: '42' })

  const link = await screen.findByRole('link', { name: 'Địa điểm' })
  expect(link).toHaveAttribute('href', '/w/7/places')
  await screen.findByRole('link', { name: /Việc cần làm/ })
  const names = screen.getAllByRole('link').map((each) => each.textContent ?? '')
  expect(names.findIndex((name) => name.startsWith('Việc cần làm')) + 1).toBe(names.indexOf('Địa điểm'))
})
