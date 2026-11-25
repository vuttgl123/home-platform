import { expect, test } from 'vitest'

import { addDays, daysBetween, todayInBusinessZone } from './dates'

test('todayFollowsTheBusinessZone', () => {
  // 23:30 UTC đã là 6:30 sáng hôm sau ở Việt Nam.
  expect(todayInBusinessZone(new Date('2026-11-15T23:30:00Z'))).toBe('2026-11-16')
  expect(todayInBusinessZone(new Date('2026-11-15T16:59:59Z'))).toBe('2026-11-15')
})

test('addDaysAndDaysBetweenFollowTheCalendar', () => {
  expect(addDays('2026-02-27', 2)).toBe('2026-03-01')
  expect(addDays('2026-03-01', -1)).toBe('2026-02-28')
  expect(daysBetween('2026-11-25', '2026-12-05')).toBe(10)
  expect(daysBetween('2026-11-25', '2026-11-20')).toBe(-5)
})
