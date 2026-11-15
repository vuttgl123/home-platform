import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { page, placeType, problem, user, workspace } from '@/test/fixtures'
import { mockShell, recordRequests } from '@/test/handlers'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

const BIG_ID = '114540094935228416'

test('redirectsToLoginWithoutUser', async () => {
  const requests = recordRequests()

  const { router } = renderApp({ route: '/w/1' })

  expect(await screen.findByRole('heading', { name: 'Tạo người dùng' })).toBeInTheDocument()
  expect(router.state.location.pathname).toBe('/')
  expect(requests).toEqual([])
})

test('createsAUserAndEntersTheirWorkspace', async () => {
  mockShell({ me: user({ id: BIG_ID }) })
  let created: unknown = null
  server.use(
    http.post('/api/identity/users', async ({ request }) => {
      created = await request.json()
      return HttpResponse.json(user({ id: BIG_ID }), { status: 201 })
    }),
  )
  const { user: actor, router } = renderApp({ route: '/' })

  await actor.type(await screen.findByLabelText('Email'), 'an@example.com')
  await actor.type(screen.getByLabelText('Tên hiển thị'), 'An')
  await actor.click(screen.getByRole('button', { name: 'Tạo và vào' }))

  await waitFor(() => expect(router.state.location.pathname).toBe('/w/7'))
  expect(created).toEqual({ email: 'an@example.com', displayName: 'An' })
  expect(localStorage.getItem('hp.userId')).toBe(BIG_ID)
})

test('duplicateEmailIsExplained', async () => {
  server.use(
    http.post('/api/identity/users', () =>
      HttpResponse.json(problem(409, 'User with email an@example.com already exists'), { status: 409 }),
    ),
  )
  const { user: actor } = renderApp({ route: '/' })

  await actor.type(await screen.findByLabelText('Email'), 'an@example.com')
  await actor.type(screen.getByLabelText('Tên hiển thị'), 'An')
  await actor.click(screen.getByRole('button', { name: 'Tạo và vào' }))

  expect(await screen.findByText('Email này đã có người dùng')).toBeInTheDocument()
  expect(localStorage.getItem('hp.userId')).toBeNull()
})

test('existingIdIsChecked', async () => {
  let askedFor: string | null = null
  server.use(
    http.get('/api/identity/users/me', ({ request }) => {
      askedFor = request.headers.get('X-User-Id')
      return HttpResponse.json(problem(404, 'User 999 not found'), { status: 404 })
    }),
  )
  const { user: actor } = renderApp({ route: '/' })

  await actor.type(await screen.findByLabelText('Id người dùng'), '999')
  await actor.click(screen.getByRole('button', { name: 'Vào' }))

  expect(await screen.findByText('Không tìm thấy người dùng')).toBeInTheDocument()
  expect(askedFor).toBe('999')
  expect(localStorage.getItem('hp.userId')).toBeNull()
})

test('returnsToTheRequestedPageAfterLogin', async () => {
  mockShell()
  const { user: actor, router } = renderApp({ route: '/w/7/people' })

  await actor.type(await screen.findByLabelText('Id người dùng'), '42')
  await actor.click(screen.getByRole('button', { name: 'Vào' }))

  await waitFor(() => expect(router.state.location.pathname).toBe('/w/7/people'))
  expect(await screen.findByText('Sắp có')).toBeInTheDocument()
})

test('signedInUserGoesStraightToTheirWorkspace', async () => {
  mockShell({ workspaces: [workspace({ id: '7' }), workspace({ id: '8', name: 'Văn phòng' })] })
  localStorage.setItem('hp.lastWorkspaceId', '8')

  const { router } = renderApp({ route: '/', userId: '42' })

  await waitFor(() => expect(router.state.location.pathname).toBe('/w/8'))
})

test('unknownWorkspaceShowsNotFound', async () => {
  mockShell()
  const requests = recordRequests()

  renderApp({ route: '/w/999', userId: '42' })

  expect(await screen.findByText('Không tìm thấy không gian')).toBeInTheDocument()
  expect(requests.filter((request) => request.includes('/api/registry/'))).toEqual([])
})

test('unknownPageKeepsTheSidebar', async () => {
  mockShell()

  renderApp({ route: '/w/7/nope', userId: '42' })

  expect(await screen.findByText('Không tìm thấy')).toBeInTheDocument()
  expect(screen.getByRole('link', { name: 'Người' })).toBeInTheDocument()
})

test('offersSetupWhenCatalogIsEmpty', async () => {
  mockShell({ placeTypes: [] })
  let setUpWorkspace: string | null = null
  server.use(
    http.post('/api/registry/setup', ({ request }) => {
      setUpWorkspace = request.headers.get('X-Workspace-Id')
      // Sau khi setup, danh mục có dữ liệu.
      server.use(http.get('/api/registry/place-types', () => HttpResponse.json([placeType()])))
      return HttpResponse.json({ created: true })
    }),
  )
  const { user: actor } = renderApp({ route: '/w/7', userId: '42' })

  await actor.click(await screen.findByRole('button', { name: 'Tạo danh mục mặc định' }))

  expect(await screen.findByText('Chọn một địa điểm trên cây')).toBeInTheDocument()
  expect(screen.queryByText('Không gian chưa có danh mục')).not.toBeInTheDocument()
  expect(setUpWorkspace).toBe('7')
})

test('signOutClearsCachedData', async () => {
  const houseOfA = workspace({ id: '7', name: 'Nhà A', ownerUserId: '1' })
  const houseOfB = workspace({ id: '8', name: 'Nhà B', ownerUserId: '2' })
  mockShell()
  server.use(
    http.get('/api/identity/users/me', ({ request }) =>
      HttpResponse.json(user({ id: request.headers.get('X-User-Id') ?? '' })),
    ),
    http.get('/api/identity/workspaces', ({ request }) =>
      HttpResponse.json(page([request.headers.get('X-User-Id') === '1' ? houseOfA : houseOfB])),
    ),
  )
  const { user: actor, router } = renderApp({ route: '/w/7', userId: '1' })
  await screen.findByText('Nhà A')

  await actor.click(screen.getByRole('button', { name: 'Đổi người dùng' }))
  await actor.type(await screen.findByLabelText('Id người dùng'), '2')
  await actor.click(screen.getByRole('button', { name: 'Vào' }))

  // Nếu danh sách không gian của người A còn trong cache, người B sẽ bị đưa vào /w/7.
  await waitFor(() => expect(router.state.location.pathname).toBe('/w/8'))
  expect(await screen.findByText('Nhà B')).toBeInTheDocument()
  expect(screen.queryByText('Nhà A')).not.toBeInTheDocument()
})

test('storedUserThatNoLongerExistsIsSignedOut', async () => {
  mockShell()
  server.use(
    http.get('/api/identity/users/me', () => HttpResponse.json(problem(404, 'User 42 not found'), { status: 404 })),
  )

  const { router } = renderApp({ route: '/w/7', userId: '42' })

  expect(await screen.findByRole('heading', { name: 'Tạo người dùng' })).toBeInTheDocument()
  expect(router.state.location.pathname).toBe('/')
  expect(localStorage.getItem('hp.userId')).toBeNull()
})

test('sidebarShowsWhoIsSignedIn', async () => {
  mockShell({ me: user({ displayName: 'Trần An' }) })

  renderApp({ route: '/w/7', userId: '42' })

  const footer = within(await screen.findByTestId('user-menu'))
  expect(await footer.findByText('Trần An')).toBeInTheDocument()
})
