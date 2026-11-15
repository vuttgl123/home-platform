import { screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test, vi } from 'vitest'

import { page, problem, workspace } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

/** Backend giả cho việc tạo không gian: ghi lại thứ tự các lệnh ghi và header của `setup`. */
function mockCreateWorkspace(existing: ReturnType<typeof workspace>[], setupStatus = 200) {
  const calls: string[] = []
  const all = [...existing]
  server.use(
    http.get('/api/identity/workspaces', () => HttpResponse.json(page(all))),
    http.post('/api/identity/workspaces', async ({ request }) => {
      const body = (await request.json()) as { name: string }
      calls.push(`POST /workspaces ${body.name}`)
      const created = workspace({ id: '9', name: body.name })
      all.push(created)
      return HttpResponse.json(created, { status: 201 })
    }),
    http.post('/api/registry/setup', ({ request }) => {
      calls.push(`POST /setup in ${request.headers.get('X-Workspace-Id')}`)
      return setupStatus === 200
        ? HttpResponse.json({ created: true })
        : HttpResponse.json(problem(setupStatus, 'Unexpected error'), { status: setupStatus })
    }),
  )
  return calls
}

test('firstWorkspaceIsCreatedFromTheStartPage', async () => {
  mockShell()
  const calls = mockCreateWorkspace([])
  const { user: actor, router } = renderApp({ route: '/', userId: '42' })

  await actor.type(await screen.findByLabelText('Tên không gian'), 'Nhà An')
  await actor.click(screen.getByRole('button', { name: 'Tạo không gian' }))

  await waitFor(() => expect(router.state.location.pathname).toBe('/w/9'))
  expect(calls).toEqual(['POST /workspaces Nhà An', 'POST /setup in 9'])
})

test('createWorkspaceCallsIdentityThenSetup', async () => {
  mockShell()
  const calls = mockCreateWorkspace([workspace()])
  const { user: actor, router } = renderApp({ route: '/w/7', userId: '42' })

  await actor.click(await screen.findByRole('button', { name: /Nhà An/ }))
  await actor.click(await screen.findByRole('menuitem', { name: 'Tạo không gian' }))
  await actor.type(await screen.findByLabelText('Tên không gian'), 'Văn phòng')
  await actor.click(screen.getByRole('button', { name: 'Tạo không gian' }))

  await waitFor(() => expect(router.state.location.pathname).toBe('/w/9'))
  expect(calls).toEqual(['POST /workspaces Văn phòng', 'POST /setup in 9'])
  // Không gian mới dùng được ngay, không bị coi là "không tìm thấy".
  expect(await screen.findByText('Chọn một địa điểm trên cây')).toBeInTheDocument()
})

test('setupFailureStillEntersTheWorkspace', async () => {
  mockShell({ placeTypes: [] })
  mockCreateWorkspace([], 500)
  const logged = vi.spyOn(console, 'error').mockImplementation(() => {})
  const { user: actor, router } = renderApp({ route: '/', userId: '42' })

  await actor.type(await screen.findByLabelText('Tên không gian'), 'Nhà An')
  await actor.click(screen.getByRole('button', { name: 'Tạo không gian' }))

  // Không gian đã có bên identity; trang chủ của nó mời chạy lại bước chuẩn bị.
  await waitFor(() => expect(router.state.location.pathname).toBe('/w/9'))
  expect(await screen.findByRole('button', { name: 'Tạo danh mục mặc định' })).toBeInTheDocument()
  expect(logged).toHaveBeenCalled()
  logged.mockRestore()
})

test('blankWorkspaceNameIsNotSent', async () => {
  mockShell()
  const calls = mockCreateWorkspace([])
  const { user: actor } = renderApp({ route: '/', userId: '42' })

  await actor.click(await screen.findByRole('button', { name: 'Tạo không gian' }))

  expect(await screen.findByText('Nhập tên không gian')).toBeInTheDocument()
  expect(calls).toEqual([])
})

test('switcherNavigates', async () => {
  mockShell({ workspaces: [workspace(), workspace({ id: '8', name: 'Văn phòng' })] })
  const { user: actor, router } = renderApp({ route: '/w/7', userId: '42' })

  await actor.click(await screen.findByRole('button', { name: /Nhà An/ }))
  await actor.click(await screen.findByRole('menuitem', { name: 'Văn phòng' }))

  await waitFor(() => expect(router.state.location.pathname).toBe('/w/8'))
})
