import { expect, test } from 'vitest'

import { initials } from './initials'

test('initialsOfVietnameseNames', () => {
  expect(initials('Nguyễn Thị Lan')).toBe('NL')
  expect(initials('An')).toBe('A')
  expect(initials('  trần   văn minh ')).toBe('TM')
  expect(initials('Đỗ Mai Phương')).toBe('ĐP')
  expect(initials('')).toBe('?')
  expect(initials('   ')).toBe('?')
  expect(initials(null)).toBe('?')
  expect(initials(undefined)).toBe('?')
})
