import { expect, test } from 'vitest'

import { formatShare, ownershipFormSchema, parseShare } from './components/ownershipForm'

test('parsesSharesWithCommaOrDot', () => {
  expect(parseShare('')).toBeNull()
  expect(parseShare('33,33')).toBe(33.33)
  expect(parseShare('50')).toBe(50)
  expect(parseShare(' 12.5 ')).toBe(12.5)
})

test('rejectsZeroOverOneHundredAndThreeDecimals', () => {
  for (const text of ['0', '101', '33,333', 'abc', '-5']) {
    expect(parseShare(text)).toBe('invalid')
  }
})

test('formatsSharesTheVietnameseWay', () => {
  expect(formatShare(50)).toBe('50%')
  expect(formatShare(33.33)).toBe('33,33%')
  expect(formatShare(null)).toBe('—')
})

test('endDateCannotBeBeforeTheStart', () => {
  const result = ownershipFormSchema.safeParse({
    sharePercent: '',
    startDate: '2026-02-01',
    endDate: '2026-01-31',
    documentNo: '',
    note: '',
  })
  expect(result.success).toBe(false)
})
