import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { fieldDef, page, place, placeDetail, placeType, problem } from '@/test/fixtures'
import { mockShell, recordRequests } from '@/test/handlers'
import { mockPlaceBackend } from '@/test/placeBackend'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

const p301 = placeDetail({
  id: '301',
  parentId: '13',
  placeTypeId: '102',
  name: 'P301',
  code: 'R-301',
  depth: 2,
  version: 4,
  description: 'Phòng góc, hai cửa sổ.',
  ancestors: [
    { id: '1', name: 'Toà A' },
    { id: '13', name: 'Tầng 3' },
  ],
})

const types = [
  placeType({ id: '100', code: 'BUILDING', name: 'Toà nhà' }),
  placeType({ id: '102', code: 'ROOM', name: 'Phòng', sortOrder: 30 }),
]

function mockP301(detail = p301) {
  mockShell({ places: [place({ id: '1', name: 'Toà A' })], placeTypes: types })
  server.use(
    http.get('/api/registry/places/301', () => HttpResponse.json(detail)),
    http.get('/api/registry/places/1/children', () => HttpResponse.json(page([]))),
    http.get('/api/registry/places/13/children', () => HttpResponse.json(page([]))),
  )
}

/** Vùng nội dung chính, sau khi khung trang đã dựng xong. */
async function main() {
  return within(await screen.findByRole('main'))
}

test('showsBreadcrumbAndHeader', async () => {
  mockP301()

  renderApp({ route: '/w/7/places/301', userId: '42' })

  expect(await (await main()).findByRole('heading', { name: 'P301' })).toBeInTheDocument()
  const trail = within((await main()).getByRole('navigation', { name: 'Đường dẫn' }))
  expect(trail.getByRole('link', { name: 'Toà A' })).toHaveAttribute('href', '/w/7/places/1')
  expect(trail.getByRole('link', { name: 'Tầng 3' })).toHaveAttribute('href', '/w/7/places/13')
  expect(await (await main()).findByText('Phòng')).toBeInTheDocument()
  expect((await main()).getByText('Đang dùng')).toBeInTheDocument()
  expect((await main()).getByText('R-301')).toBeInTheDocument()
  expect((await main()).getByText('v4')).toBeInTheDocument()
})

test('showsInheritedAddress', async () => {
  mockP301({
    ...p301,
    address: null,
    effectiveAddress: {
      fromPlaceId: '1',
      address: { ...emptyAddress(), fullAddress: '1 Tràng Tiền, Hoàn Kiếm, Hà Nội' },
    },
  })

  renderApp({ route: '/w/7/places/301', userId: '42' })

  expect(await (await main()).findByText('1 Tràng Tiền, Hoàn Kiếm, Hà Nội')).toBeInTheDocument()
  const source = (await main()).getByText(/lấy từ/)
  expect(source).toHaveTextContent('lấy từ Toà A')
  expect(within(source).getByRole('link', { name: 'Toà A' })).toHaveAttribute('href', '/w/7/places/1')
})

test('showsOwnAddressAndNoAddress', async () => {
  mockP301()

  renderApp({ route: '/w/7/places/301', userId: '42' })

  expect(await (await main()).findByText('Chưa có địa chỉ')).toBeInTheDocument()
})

test('showsAttributesOfTheType', async () => {
  mockP301({ ...p301, attributes: { area_m2: 25 } })
  let askedType: string | null = null
  server.use(
    http.get('/api/registry/field-defs', ({ request }) => {
      askedType = new URL(request.url).searchParams.get('typeId')
      return HttpResponse.json([fieldDef({ fieldKey: 'area_m2', label: 'Diện tích (m²)' })])
    }),
  )

  renderApp({ route: '/w/7/places/301', userId: '42' })

  expect(await (await main()).findByText('Diện tích (m²)')).toBeInTheDocument()
  expect((await main()).getByText('25')).toBeInTheDocument()
  expect(askedType).toBe('102')
})

test('tabFollowsTheUrl', async () => {
  mockP301()
  server.use(
    http.get('/api/registry/places/301/children', () =>
      HttpResponse.json(page([place({ id: '3011', parentId: '301', name: 'Giường 1' })])),
    ),
  )
  const { user, router } = renderApp({ route: '/w/7/places/301?tab=children', userId: '42' })

  expect(await (await main()).findByRole('link', { name: 'Giường 1' })).toHaveAttribute('href', '/w/7/places/3011')
  expect((await main()).getByRole('tab', { name: 'Bên trong' })).toHaveAttribute('aria-selected', 'true')

  await user.click((await main()).getByRole('tab', { name: 'Tổng quan' }))

  await waitFor(() => expect(router.state.location.search).toBe(''))
  expect(await (await main()).findByText('Chưa có địa chỉ')).toBeInTheDocument()
})

test('unknownTabFallsBackToOverview', async () => {
  mockP301()

  renderApp({ route: '/w/7/places/301?tab=nope', userId: '42' })

  expect(await (await main()).findByRole('tab', { name: 'Tổng quan' })).toHaveAttribute('aria-selected', 'true')
})

test('childrenTabShowsEmptyState', async () => {
  mockP301()
  server.use(http.get('/api/registry/places/301/children', () => HttpResponse.json(page([]))))

  renderApp({ route: '/w/7/places/301?tab=children', userId: '42' })

  expect(await (await main()).findByText('Chưa có chỗ nào bên trong')).toBeInTheDocument()
})

test('unknownPlaceShowsNotFound', async () => {
  mockShell({ places: [place({ id: '1', name: 'Toà A' })] })
  server.use(
    http.get('/api/registry/places/999', () => HttpResponse.json(problem(404, 'Place 999 not found'), { status: 404 })),
  )

  renderApp({ route: '/w/7/places/999', userId: '42' })

  expect(await (await main()).findByText('Không tìm thấy')).toBeInTheDocument()
  // Cây vẫn dùng được.
  expect(screen.getByRole('link', { name: 'Toà A' })).toBeInTheDocument()
})

test('malformedIdShowsNotFoundWithoutCalling', async () => {
  mockShell()
  const requests = recordRequests()

  renderApp({ route: '/w/7/places/abc', userId: '42' })

  expect(await (await main()).findByText('Không tìm thấy')).toBeInTheDocument()
  expect(requests.filter((request) => request.startsWith('GET /api/registry/places/'))).toEqual([])
})

test('loadFailureCanBeRetried', async () => {
  mockShell()
  let attempts = 0
  server.use(
    http.get('/api/registry/places/301', () => {
      attempts += 1
      return attempts === 1
        ? HttpResponse.json(problem(409, 'The record is being changed by someone else; try again'), { status: 409 })
        : HttpResponse.json(p301)
    }),
  )
  const { user } = renderApp({ route: '/w/7/places/301', userId: '42' })

  await user.click(await (await main()).findByRole('button', { name: 'Thử lại' }))

  expect(await (await main()).findByRole('heading', { name: 'P301' })).toBeInTheDocument()
})

function emptyAddress() {
  return {
    countryCode: 'VN',
    provinceCode: null,
    provinceName: null,
    districtCode: null,
    districtName: null,
    wardCode: null,
    wardName: null,
    street: null,
    fullAddress: '',
    latitude: null,
    longitude: null,
    geohash: null,
  }
}

test('ownersTabSitsBeforeOccupants', async () => {
  mockShell()
  mockPlaceBackend([{ id: '301', name: 'P301' }])
  renderApp({ route: '/w/7/places/301', userId: '42' })

  const tabs = within(await screen.findByRole('tablist'))
  const names = tabs.getAllByRole('tab').map((tab) => tab.textContent)
  expect(names.indexOf('Sở hữu')).toBe(names.indexOf('Người ở') - 1)
})

test('placeOverviewShowsTheInformationCard', async () => {
  mockP301()

  renderApp({ route: '/w/7/places/301', userId: '42' })

  // Loại, mã, trạng thái đã ở đầu trang; khối "Thông tin" nói thêm nằm trong đâu và mô tả.
  const info = within(await (await main()).findByRole('region', { name: 'Thông tin' }))
  expect(info.getByRole('link', { name: 'Tầng 3' })).toHaveAttribute('href', '/w/7/places/13')
  expect(info.getByText('Phòng góc, hai cửa sổ.')).toBeInTheDocument()
})
