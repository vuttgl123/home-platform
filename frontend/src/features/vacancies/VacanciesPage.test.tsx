import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { todayInBusinessZone } from '@/lib/dates'
import { page, place, placeType } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { mockPeopleBackend } from '@/test/peopleBackend'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

const types = [
  placeType({ id: '100', code: 'BUILDING', name: 'Toà nhà', sortOrder: 10 }),
  placeType({ id: '102', code: 'ROOM', name: 'Phòng', sortOrder: 30 }),
]

function vacancy(overrides: Record<string, unknown> = {}) {
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

/** Trang Chỗ trống; `pages` là các trang API trả lần lượt theo `cursor`. */
function setUp(pages: { items: object[]; nextCursor?: string }[] = [{ items: [vacancy()] }]) {
  mockShell({ placeTypes: types, places: [place({ id: '1', name: 'Toà A' }), place({ id: '2', name: 'Toà B' })] })
  const backend = mockPeopleBackend({
    today: todayInBusinessZone(),
    people: [{ id: '55', fullName: 'Nguyễn An' }],
    placeNames: { '301': 'P101' },
  })
  const requests: URL[] = []
  server.use(
    http.get('/api/registry/places/:id/capacity-check', () => HttpResponse.json({ capacity: 4, peak: 1 })),
    http.get('/api/registry/places/vacancies', ({ request }) => {
      const url = new URL(request.url)
      requests.push(url)
      const index = url.searchParams.get('cursor') ? Number(url.searchParams.get('cursor')) : 0
      return HttpResponse.json(page(pages[index].items, pages[index].nextCursor ?? null))
    }),
  )
  return { backend, requests }
}

async function main() {
  return within(await screen.findByRole('main'))
}

test('listsPlacesWithRoom', async () => {
  setUp([{ items: [vacancy(), vacancy({ placeId: '302', name: 'P102', capacity: null, free: null, occupantCount: 0 })] }])

  renderApp({ route: '/w/7/vacancies', userId: '42' })

  const content = await main()
  expect(content.getByRole('heading', { name: 'Chỗ trống' })).toBeInTheDocument()
  expect(await content.findByRole('link', { name: 'P101' })).toHaveAttribute('href', '/w/7/places/301')
  expect(content.getByText('1/4 người · còn 3 chỗ')).toBeInTheDocument()
  expect(content.getAllByText('Toà A › Tầng 1')).toHaveLength(2)
  expect(content.getByText('Trống, chưa đặt sức chứa')).toBeInTheDocument()
  expect(content.getAllByRole('progressbar')).toHaveLength(1)
})

test('sidebarLinksToVacancies', async () => {
  setUp()

  renderApp({ route: '/w/7/vacancies', userId: '42' })

  const nav = await screen.findByRole('link', { name: 'Chỗ trống' })
  expect(nav).toHaveAttribute('href', '/w/7/vacancies')
})

test('typeToggleFiltersAndLandsOnTheUrl', async () => {
  const { requests } = setUp()
  const { user, router } = renderApp({ route: '/w/7/vacancies?type=102', userId: '42' })

  const content = await main()
  await content.findByRole('link', { name: 'P101' })
  expect(requests.at(-1)?.searchParams.get('placeTypeId')).toBe('102')
  expect(content.getByRole('button', { name: 'Phòng' })).toHaveAttribute('aria-pressed', 'true')

  await user.click(content.getByRole('button', { name: 'Mọi loại' }))
  await waitFor(() => expect(requests.at(-1)?.searchParams.has('placeTypeId')).toBe(false))
  expect(router.state.location.search).toBe('')

  await user.click(content.getByRole('button', { name: 'Toà nhà' }))
  await waitFor(() => expect(requests.at(-1)?.searchParams.get('placeTypeId')).toBe('100'))
  expect(router.state.location.search).toBe('?type=100')
})

test('buildingSelectFilters', async () => {
  const { requests } = setUp()
  const { user } = renderApp({ route: '/w/7/vacancies', userId: '42' })

  const content = await main()
  await content.findByRole('link', { name: 'P101' })
  await user.selectOptions(await content.findByRole('combobox', { name: 'Toà' }), 'Toà B')

  await waitFor(() => expect(requests.at(-1)?.searchParams.get('rootId')).toBe('2'))
})

test('seatSomeoneOpensTheFormForThatPlace', async () => {
  const { backend, requests } = setUp()
  const { user } = renderApp({ route: '/w/7/vacancies', userId: '42' })

  const content = await main()
  await content.findByRole('link', { name: 'P101' })
  await user.click(content.getByRole('button', { name: 'Xếp người vào P101' }))
  const form = within(await screen.findByRole('dialog', { name: 'Thêm người ở' }))
  expect(form.getByText('Tại P101')).toBeInTheDocument()

  const before = requests.length
  await user.type(form.getByRole('searchbox', { name: 'Tìm người' }), 'Nguyễn')
  await user.click(await form.findByRole('radio', { name: 'Nguyễn An' }))
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0]).toMatchObject({ method: 'POST', path: '/occupancies', body: { placeId: '301', personId: '55' } })
  await waitFor(() => expect(requests.length).toBeGreaterThan(before))
})

test('emptyStateWhenNothingMatches', async () => {
  setUp([{ items: [] }])

  renderApp({ route: '/w/7/vacancies', userId: '42' })

  expect(await (await main()).findByText('Không còn chỗ trống nào khớp bộ lọc')).toBeInTheDocument()
})

test('loadsMore', async () => {
  const { requests } = setUp([{ items: [vacancy()], nextCursor: '1' }, { items: [vacancy({ placeId: '303', name: 'P103' })] }])
  const { user } = renderApp({ route: '/w/7/vacancies', userId: '42' })

  const content = await main()
  await content.findByRole('link', { name: 'P101' })
  await user.click(content.getByRole('button', { name: 'Tải thêm' }))

  expect(await content.findByRole('link', { name: 'P103' })).toBeInTheDocument()
  expect(requests.at(-1)?.searchParams.get('cursor')).toBe('1')
})

test('buildingFilterListsEveryTopLevelPlace', async () => {
  setUp()
  const roots = Array.from({ length: 25 }, (_, index) => place({ id: String(1000 + index), name: `Toà ${index + 1}` }))
  server.use(
    http.get('/api/registry/places', ({ request }) => {
      const cursor = new URL(request.url).searchParams.get('cursor')
      return HttpResponse.json(cursor ? { items: roots.slice(20), nextCursor: null } : { items: roots.slice(0, 20), nextCursor: 'p2' })
    }),
  )

  renderApp({ route: '/w/7/vacancies', userId: '42' })

  const select = await screen.findByLabelText('Toà')
  await waitFor(() => expect(within(select).getAllByRole('option')).toHaveLength(26))
  expect(within(select).getByRole('option', { name: 'Toà 25' })).toBeInTheDocument()
})
