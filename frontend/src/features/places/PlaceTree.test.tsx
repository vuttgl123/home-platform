import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { page, place, placeDetail } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

const BIG_ID = '114540094935228416'

const buildingA = place({ id: '1', name: 'Toà A' })
const floor3 = place({ id: '13', parentId: '1', name: 'Tầng 3', depth: 1 })
const room = (id: string, name: string) => place({ id, parentId: '13', name, depth: 2 })
const p301Detail = placeDetail({
  id: '301',
  parentId: '13',
  name: 'P301',
  depth: 2,
  ancestors: [
    { id: '1', name: 'Toà A' },
    { id: '13', name: 'Tầng 3' },
  ],
})

function tree() {
  return within(screen.getByRole('navigation', { name: 'Cây địa điểm' }))
}

/** Như `tree()`, nhưng chờ cây xuất hiện trước. */
async function findTree() {
  return within(await screen.findByRole('navigation', { name: 'Cây địa điểm' }))
}

function linkNames() {
  return tree()
    .getAllByRole('link')
    .map((link) => link.textContent)
}

test('showsTopLevelPlacesInServerOrder', async () => {
  // Thứ tự là của server; cây không tự sắp lại.
  mockShell({ places: [place({ id: '2', name: 'Toà B' }), buildingA] })

  renderApp({ route: '/w/7', userId: '42' })

  await (await findTree()).findByRole('link', { name: 'Toà B' })
  expect(linkNames()).toEqual(['Toà B', 'Toà A'])
})

test('expandingLoadsChildrenOnce', async () => {
  mockShell({ places: [buildingA] })
  let loads = 0
  server.use(
    http.get('/api/registry/places/1/children', () => {
      loads += 1
      return HttpResponse.json(page([floor3]))
    }),
  )
  const { user } = renderApp({ route: '/w/7', userId: '42' })

  await user.click(await (await findTree()).findByRole('button', { name: 'Mở Toà A' }))
  await (await findTree()).findByRole('link', { name: 'Tầng 3' })
  await user.click(tree().getByRole('button', { name: 'Đóng Toà A' }))
  expect(tree().queryByRole('link', { name: 'Tầng 3' })).not.toBeInTheDocument()
  await user.click(tree().getByRole('button', { name: 'Mở Toà A' }))

  expect(await (await findTree()).findByRole('link', { name: 'Tầng 3' })).toBeInTheDocument()
  expect(loads).toBe(1)
})

test('emptyNodeLosesItsArrow', async () => {
  mockShell({ places: [buildingA] })
  server.use(http.get('/api/registry/places/1/children', () => HttpResponse.json(page([]))))
  const { user } = renderApp({ route: '/w/7', userId: '42' })

  await user.click(await (await findTree()).findByRole('button', { name: 'Mở Toà A' }))

  await waitFor(() => expect(tree().queryByRole('button', { name: /Toà A/ })).not.toBeInTheDocument())
  expect(tree().getByRole('link', { name: 'Toà A' })).toBeInTheDocument()
})

test('loadMoreAppendsWithoutDuplicates', async () => {
  mockShell({ places: [floor3] })
  server.use(
    http.get('/api/registry/places/13/children', ({ request }) => {
      const cursor = new URL(request.url).searchParams.get('cursor')
      // Trang hai trả lại P102: có người vừa thêm một phòng đứng trước nó.
      return cursor === 'c1'
        ? HttpResponse.json(page([room('102', 'P102'), room('103', 'P103')]))
        : HttpResponse.json(page([room('101', 'P101'), room('102', 'P102')], 'c1'))
    }),
  )
  const { user } = renderApp({ route: '/w/7', userId: '42' })
  await user.click(await (await findTree()).findByRole('button', { name: 'Mở Tầng 3' }))

  await user.click(await (await findTree()).findByRole('button', { name: 'Tải thêm' }))

  await (await findTree()).findByRole('link', { name: 'P103' })
  expect(linkNames()).toEqual(['Tầng 3', 'P101', 'P102', 'P103'])
  expect(tree().queryByRole('button', { name: 'Tải thêm' })).not.toBeInTheDocument()
})

test('navigatesWithTheExactId', async () => {
  mockShell({ places: [place({ id: BIG_ID, name: 'Toà A' })] })
  server.use(
    http.get(`/api/registry/places/${BIG_ID}`, () => HttpResponse.json(placeDetail({ id: BIG_ID }))),
  )
  const { user, router } = renderApp({ route: '/w/7', userId: '42' })

  await user.click(await (await findTree()).findByRole('link', { name: 'Toà A' }))

  // 114540094935228416 không biểu diễn được bằng kiểu số: sai một chữ số là sang địa điểm khác.
  await waitFor(() => expect(router.state.location.pathname).toBe(`/w/7/places/${BIG_ID}`))
})

test('opensAncestorsOfTheSelectedPlace', async () => {
  mockShell({ places: [buildingA] })
  server.use(
    http.get('/api/registry/places/301', () => HttpResponse.json(p301Detail)),
    http.get('/api/registry/places/1/children', () => HttpResponse.json(page([floor3]))),
    http.get('/api/registry/places/13/children', () =>
      HttpResponse.json(page([room('301', 'P301'), room('302', 'P302')])),
    ),
  )

  renderApp({ route: '/w/7/places/301', userId: '42' })

  const selected = await (await findTree()).findByRole('link', { name: 'P301' })
  expect(selected).toHaveAttribute('aria-current', 'page')
  expect(linkNames()).toEqual(['Toà A', 'Tầng 3', 'P301', 'P302'])
  expect(tree().getByRole('link', { name: 'Tầng 3' })).not.toHaveAttribute('aria-current')
})

test('showsTheSelectedPlaceBeyondTheLoadedPage', async () => {
  mockShell({ places: [buildingA] })
  server.use(
    http.get('/api/registry/places/301', () => HttpResponse.json(p301Detail)),
    http.get('/api/registry/places/1/children', () => HttpResponse.json(page([floor3]))),
    // Trang đầu của Tầng 3 chưa tới P301.
    http.get('/api/registry/places/13/children', () =>
      HttpResponse.json(page([room('101', 'P101'), room('102', 'P102')], 'c1')),
    ),
  )

  renderApp({ route: '/w/7/places/301', userId: '42' })

  expect(await (await findTree()).findByRole('link', { name: 'P301' })).toHaveAttribute('aria-current', 'page')
  expect(linkNames()).toEqual(['Toà A', 'Tầng 3', 'P101', 'P102', 'P301'])
})

test('emptyWorkspaceShowsAddButton', async () => {
  mockShell({ places: [] })

  renderApp({ route: '/w/7', userId: '42' })

  expect(await (await findTree()).findByText('Chưa có địa điểm')).toBeInTheDocument()
  expect(tree().getByRole('button', { name: 'Thêm địa điểm' })).toBeInTheDocument()
})

test('searchReplacesTheTreeWithResults', async () => {
  mockShell({ places: [buildingA] })
  let searchedFor: string | null = null
  server.use(
    http.get('/api/registry/places/search', ({ request }) => {
      searchedFor = new URL(request.url).searchParams.get('name')
      return HttpResponse.json(page([room('101', 'P101'), place({ id: '102', name: 'P102', code: 'R-102' })]))
    }),
  )
  const { user } = renderApp({ route: '/w/7', userId: '42' })
  await (await findTree()).findByRole('link', { name: 'Toà A' })

  await user.type(tree().getByRole('searchbox', { name: 'Tìm địa điểm' }), 'P10')

  expect(await (await findTree()).findByRole('link', { name: /P101/ })).toBeInTheDocument()
  expect(tree().getByRole('link', { name: /P102/ })).toHaveTextContent('R-102')
  expect(tree().queryByRole('link', { name: 'Toà A' })).not.toBeInTheDocument()
  expect(searchedFor).toBe('P10')

  await user.clear(tree().getByRole('searchbox', { name: 'Tìm địa điểm' }))

  expect(await (await findTree()).findByRole('link', { name: 'Toà A' })).toBeInTheDocument()
  expect(tree().queryByRole('link', { name: /P101/ })).not.toBeInTheDocument()
})

test('treeLoadFailureCanBeRetried', async () => {
  mockShell()
  let attempts = 0
  server.use(
    http.get('/api/registry/places', () => {
      attempts += 1
      return attempts === 1
        ? HttpResponse.json({ status: 400, detail: 'Invalid data' }, { status: 400 })
        : HttpResponse.json(page([buildingA]))
    }),
  )
  const { user } = renderApp({ route: '/w/7', userId: '42' })

  await user.click(await (await findTree()).findByRole('button', { name: 'Thử lại' }))

  expect(await (await findTree()).findByRole('link', { name: 'Toà A' })).toBeInTheDocument()
})

test('toggleReportsWhetherItIsOpen', async () => {
  mockShell({ places: [buildingA] })
  server.use(http.get('/api/registry/places/1/children', () => HttpResponse.json(page([floor3]))))
  const { user } = renderApp({ route: '/w/7', userId: '42' })

  const toggle = await (await findTree()).findByRole('button', { name: 'Mở Toà A' })
  expect(toggle).toHaveAttribute('aria-expanded', 'false')

  await user.click(toggle)

  expect(await (await findTree()).findByRole('button', { name: 'Đóng Toà A' })).toHaveAttribute('aria-expanded', 'true')
  // Cây không cài phím mũi tên của mẫu ARIA "tree", nên không tự nhận là một "tree".
  expect(screen.queryByRole('tree')).not.toBeInTheDocument()
})
