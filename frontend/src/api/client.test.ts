import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { server } from '@/test/server'

import { identity, unwrap } from './client'
import { ApiError } from './errors'
import { setCurrentUser, userHeaders, workspaceHeaders } from './headers'

function getMe() {
  return unwrap(identity.GET('/users/me', { params: { header: userHeaders() } }))
}

test('callsWithoutUserThrow', () => {
  expect(() => userHeaders()).toThrow('No user is signed in')
  expect(() => workspaceHeaders('1')).toThrow('No user is signed in')

  setCurrentUser('42')

  expect(userHeaders()).toEqual({ 'X-User-Id': '42' })
  expect(workspaceHeaders('7')).toEqual({ 'X-Workspace-Id': '7', 'X-User-Id': '42' })
})

test('keepsLargeIdsAsStrings', async () => {
  let sentUser: string | null = null
  server.use(
    http.get('/api/identity/users/me', ({ request }) => {
      sentUser = request.headers.get('X-User-Id')
      return HttpResponse.json({ id: '114540094935228416', email: 'an@example.com' })
    }),
  )
  setCurrentUser('42')

  const user = await getMe()

  // 114540094935228416 lớn hơn 2^53: đổi sang số là mất chữ số cuối.
  expect(user.id).toBe('114540094935228416')
  expect(sentUser).toBe('42')
})

test('turnsProblemDetailsIntoApiError', async () => {
  server.use(
    http.get('/api/identity/users/me', () =>
      HttpResponse.json(
        { status: 404, title: 'Not Found', detail: 'User 1 not found' },
        { status: 404, headers: { 'Content-Type': 'application/problem+json' } },
      ),
    ),
  )
  setCurrentUser('1')

  const error = await getMe().catch((caught: unknown) => caught)

  expect(error).toBeInstanceOf(ApiError)
  expect((error as ApiError).isNotFound).toBe(true)
  expect((error as ApiError).detail).toBe('User 1 not found')
})

test('emptySuccessIsUndefined', async () => {
  server.use(http.get('/api/identity/users/me', () => new HttpResponse(null, { status: 204 })))
  setCurrentUser('1')

  expect(await getMe()).toBeUndefined()
})

test('networkFailureIsAnApiError', async () => {
  server.use(http.get('/api/identity/users/me', () => HttpResponse.error()))
  setCurrentUser('1')

  const error = await getMe().catch((caught: unknown) => caught)

  expect(error).toBeInstanceOf(ApiError)
  expect((error as ApiError).status).toBe(0)
})
