import { expect, test } from 'vitest'

import { todayInBusinessZone } from './dates'

test('todayFollowsTheBusinessZone', () => {
  // 23:30 UTC đã là 6:30 sáng hôm sau ở Việt Nam.
  expect(todayInBusinessZone(new Date('2026-11-15T23:30:00Z'))).toBe('2026-11-16')
  expect(todayInBusinessZone(new Date('2026-11-15T16:59:59Z'))).toBe('2026-11-15')
})
