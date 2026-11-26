import { expect, test } from 'vitest'

import { ApiError } from './errors'

test('readsFieldErrors', () => {
  const error = ApiError.from(400, {
    status: 400,
    detail: 'Invalid request content.',
    errors: [
      { field: 'name', message: 'must not be blank' },
      { field: 'name', message: 'size must be between 0 and 255' },
      { field: null, message: 'bad object' },
    ],
  })

  expect(error.status).toBe(400)
  expect(error.detail).toBe('Invalid request content.')
  expect(error.fieldErrors).toEqual({ name: 'must not be blank; size must be between 0 and 255' })
  expect(error.formErrors).toEqual(['bad object'])
})

test('recognisesVersionConflict', () => {
  const stale = ApiError.from(409, { status: 409, detail: 'Place 1 was changed by someone else' })
  const rule = ApiError.from(409, {
    status: 409,
    detail: 'Place 1 still has 2 child places, 0 occupancies and 0 items',
  })

  expect(stale.isVersionConflict).toBe(true)
  expect(rule.isVersionConflict).toBe(false)
  // Cùng câu chữ nhưng không phải 409 thì không phải xung đột version.
  expect(ApiError.from(400, { detail: 'Place 1 was changed by someone else' }).isVersionConflict).toBe(false)
})

test('nonProblemBodyIsUnexpected', () => {
  expect(ApiError.from(502, '').detail).toBe('Unexpected error')
  expect(ApiError.from(502, '<html>').detail).toBe('Unexpected error')
  expect(ApiError.from(500, undefined).fieldErrors).toEqual({})
})

test('keepsTheRequestIdOfA500', () => {
  const error = ApiError.from(500, { status: 500, detail: 'Unexpected error', requestId: 'abc-123' })

  expect(error.requestId).toBe('abc-123')
  expect(ApiError.from(404, { detail: 'User 1 not found' }).requestId).toBeNull()
})

test('networkFailureHasStatusZero', () => {
  const error = ApiError.network()

  expect(error.status).toBe(0)
  expect(error.detail).toBe('Không kết nối được máy chủ')
  expect(error.isNotFound).toBe(false)
})

test('versionConflictIsReadFromTheCode', () => {
  const error = ApiError.from(409, { status: 409, detail: 'Anything at all', code: 'VERSION_CONFLICT' })

  expect(error.code).toBe('VERSION_CONFLICT')
  expect(error.isVersionConflict).toBe(true)
})

test('messageIsStillAFallback', () => {
  // Backend cũ chưa gửi `code`.
  expect(ApiError.from(409, { detail: 'Item 7 was changed by someone else' }).isVersionConflict).toBe(true)
  expect(ApiError.from(409, { detail: 'Item 7 was changed by someone else', code: 'OTHER' }).isVersionConflict).toBe(false)
})

test('busyConflictReadsInVietnamese', () => {
  const error = ApiError.from(409, {
    status: 409,
    detail: 'The record is being changed by someone else; try again',
    code: 'BUSY',
  })

  expect(error.detail).toBe('Đang có thao tác khác trên dữ liệu này (ví dụ đang nhập Excel); thử lại sau ít phút')
  expect(error.isBusy).toBe(true)
  expect(error.isVersionConflict).toBe(false)
})
