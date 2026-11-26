import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { afterEach, expect, test, vi } from 'vitest'

import { fieldDef, item, occupancy, ownership, page, placeType } from '@/test/fixtures'
import { mockShell, recordRequests } from '@/test/handlers'
import { mockPlaceBackend } from '@/test/placeBackend'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

afterEach(() => {
  vi.restoreAllMocks()
})

type Lists = {
  occupancies?: ReturnType<typeof occupancy>[]
  ownerships?: ReturnType<typeof ownership>[]
  items?: ReturnType<typeof item>[]
  subtreeItems?: ReturnType<typeof item>[]
}

/** P301 (Phòng) trong Toà A › Tầng 3; ghi lại tham số của các request danh sách. */
function setUp(lists: Lists = {}) {
  mockShell({ placeTypes: [placeType({ id: '102', name: 'Phòng' })] })
  mockPlaceBackend([
    { id: '1', name: 'Toà A' },
    { id: '13', parentId: '1', name: 'Tầng 3' },
    {
      id: '301',
      parentId: '13',
      name: 'P301',
      placeTypeId: '102',
      code: 'DD-000041',
      status: 'INACTIVE',
      description: 'Phòng góc',
      attributes: { area_m2: 42 },
    },
  ])
  const asked: Record<string, Record<string, string>[]> = { occupancies: [], ownerships: [], items: [] }
  const query = (request: Request) => Object.fromEntries(new URL(request.url).searchParams)
  server.use(
    http.get('/api/registry/field-defs', () => HttpResponse.json([fieldDef({ typeId: '102', fieldKey: 'area_m2', label: 'Diện tích' })])),
    http.get('/api/registry/occupancies', ({ request }) => {
      asked.occupancies.push(query(request))
      return HttpResponse.json(page(lists.occupancies ?? []))
    }),
    http.get('/api/registry/ownerships', ({ request }) => {
      asked.ownerships.push(query(request))
      return HttpResponse.json(page(lists.ownerships ?? []))
    }),
    http.get('/api/registry/items', ({ request }) => {
      const params = query(request)
      asked.items.push(params)
      return HttpResponse.json(page(params.subtreeOf ? (lists.subtreeItems ?? []) : (lists.items ?? [])))
    }),
  )
  return asked
}

/** Chữ của từng ô trong các dòng dữ liệu của bảng tên `name`. */
function cells(name: string) {
  return within(screen.getByRole('table', { name }))
    .getAllByRole('row')
    .slice(1)
    .map((row: HTMLElement) => within(row).getAllByRole('cell').map((cell) => cell.textContent))
}

function headers(name: string) {
  return within(screen.getByRole('table', { name }))
    .getAllByRole('columnheader')
    .map((header) => header.textContent)
}

test('showsDetailsPeopleOwnersAndItems', async () => {
  const asked = setUp({
    occupancies: [occupancy({ personFullName: 'Nguyễn An', relation: 'RESIDENT', startDate: '2026-01-01' })],
    ownerships: [ownership({ personFullName: 'Trần Bình', sharePercent: 33.333, startDate: '2025-06-01', documentNo: 'SĐ-123' })],
    items: [item({ code: 'TL-1', name: 'Tủ lạnh', categoryName: 'Điện máy', quantity: 2, unit: 'cái', status: 'BROKEN', ownerName: 'Nguyễn An' })],
  })

  renderApp({ route: '/w/7/places/301/print', userId: '42' })

  expect(await screen.findByRole('heading', { level: 1, name: 'P301' })).toBeInTheDocument()
  await screen.findByRole('cell', { name: 'Tủ lạnh' })
  expect(screen.getByText('DD-000041')).toBeInTheDocument()
  expect(screen.getByText('Phòng')).toBeInTheDocument()
  expect(screen.getByText('Toà A / Tầng 3 / P301')).toBeInTheDocument()
  expect(screen.getByText('Tạm ngưng')).toBeInTheDocument()
  expect(screen.getByText('Phòng góc')).toBeInTheDocument()
  expect(await screen.findByText('Diện tích')).toBeInTheDocument()
  expect(screen.getByText('42')).toBeInTheDocument()
  expect(cells('Người đang ở')).toEqual([['Nguyễn An', 'Người ở', '01/01/2026']])
  // Cùng cách ghi với tab Sở hữu (formatShare).
  expect(cells('Chủ sở hữu')).toEqual([['Trần Bình', '33,33%', '01/06/2025', 'SĐ-123']])
  expect(headers('Đồ đạc')).toEqual(['Mã', 'Tên', 'Nhóm', 'Số lượng', 'Trạng thái', 'Chủ'])
  expect(cells('Đồ đạc')).toEqual([['TL-1', 'Tủ lạnh', 'Điện máy', '2 cái', 'Hỏng', 'Nguyễn An']])
  expect(screen.getByText(/^In lúc \d{2}\/\d{2}\/\d{4} \d{2}:\d{2}$/)).toBeInTheDocument()
  // Chỉ những gì còn hiệu lực hôm nay, và đồ chưa gỡ ngay ở chỗ này.
  expect(asked.occupancies).toEqual([{ placeId: '301', status: 'CURRENT' }])
  expect(asked.ownerships).toEqual([{ placeId: '301', status: 'CURRENT' }])
  expect(asked.items).toEqual([{ placeId: '301', removed: 'false' }])
  // Trang in không có thanh bên, cây địa điểm.
  expect(screen.queryByRole('navigation', { name: 'Cây địa điểm' })).not.toBeInTheDocument()
})

test('loadsEveryPageBeforePrinting', async () => {
  setUp()
  let releaseSecondPage: () => void = () => {}
  const secondPage = new Promise<void>((resolve) => {
    releaseSecondPage = resolve
  })
  server.use(
    http.get('/api/registry/items', async ({ request }) => {
      if (new URL(request.url).searchParams.get('cursor') === 'c2') {
        await secondPage
        return HttpResponse.json(page([item({ id: '902', name: 'Ghế' })]))
      }
      return HttpResponse.json(page([item({ id: '901', name: 'Bàn' })], 'c2'))
    }),
  )
  renderApp({ route: '/w/7/places/301/print', userId: '42' })

  await screen.findByRole('cell', { name: 'Bàn' })
  expect(screen.getByRole('button', { name: 'In' })).toBeDisabled()
  releaseSecondPage()

  expect(await screen.findByRole('cell', { name: 'Ghế' })).toBeInTheDocument()
  await waitFor(() => expect(screen.getByRole('button', { name: 'In' })).toBeEnabled())
  // Không có nút "Tải thêm" trên trang in.
  expect(screen.queryByRole('button', { name: 'Tải thêm' })).not.toBeInTheDocument()
})

test('subtreeSwitchUsesTheWholeSubtreeAndAddsWhere', async () => {
  const asked = setUp({
    items: [item({ id: '901', name: 'Bàn', placeName: 'P301' })],
    subtreeItems: [item({ id: '901', name: 'Bàn', placeName: 'P301' }), item({ id: '903', name: 'Giường', placeName: 'Gác xép' })],
  })
  const { user, router } = renderApp({ route: '/w/7/places/301/print', userId: '42' })
  await screen.findByRole('cell', { name: 'Bàn' })
  expect(headers('Đồ đạc')).not.toContain('Ở đâu')

  await user.click(screen.getByRole('switch', { name: 'Kể cả bên trong' }))

  await waitFor(() => expect(router.state.location.search).toBe('?subtree=1'))
  expect(await screen.findByRole('cell', { name: 'Giường' })).toBeInTheDocument()
  expect(headers('Đồ đạc')).toEqual(['Mã', 'Tên', 'Nhóm', 'Số lượng', 'Trạng thái', 'Chủ', 'Ở đâu'])
  expect(cells('Đồ đạc').map((row) => [row[1], row[6]])).toEqual([
    ['Bàn', 'P301'],
    ['Giường', 'Gác xép'],
  ])
  expect(asked.items).toContainEqual({ subtreeOf: '301' })
})

test('printButtonCallsWindowPrint', async () => {
  setUp()
  const print = vi.spyOn(window, 'print').mockImplementation(() => {})
  const { user } = renderApp({ route: '/w/7/places/301/print', userId: '42' })
  const button = await screen.findByRole('button', { name: 'In' })
  await waitFor(() => expect(button).toBeEnabled())

  await user.click(button)

  expect(print).toHaveBeenCalledTimes(1)
})

test('emptyTablesSayNone', async () => {
  setUp()

  renderApp({ route: '/w/7/places/301/print', userId: '42' })

  await waitFor(() => expect(screen.getAllByText('Không có')).toHaveLength(3))
  expect(screen.queryByRole('table', { name: 'Người đang ở' })).not.toBeInTheDocument()
})

test('unknownPlaceShowsNotFound', async () => {
  setUp()

  renderApp({ route: '/w/7/places/999/print', userId: '42' })

  expect(await screen.findByText('Địa điểm này không có, hoặc đã bị xoá.')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'In' })).not.toBeInTheDocument()
})

test('workspaceIAmNotInShowsNotFound', async () => {
  setUp()
  const requests = recordRequests()

  // Trang in nằm ngoài khung có thanh bên, nhưng vẫn phải chặn không gian mà người dùng không ở trong đó.
  renderApp({ route: '/w/999/places/301/print', userId: '42' })

  expect(await screen.findByText('Không tìm thấy không gian')).toBeInTheDocument()
  expect(requests.filter((request) => request.includes('/api/registry/'))).toEqual([])
  expect(screen.queryByRole('heading', { name: 'P301' })).not.toBeInTheDocument()
})
