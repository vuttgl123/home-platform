import { expect, test } from 'vitest'

import { hasFilters, rangeIsValid, readFilter, toQuery, writeFilter } from './filters'
import type { ActivityFilter } from './types'

const NONE: ActivityFilter = { type: null, action: null, mine: false, from: null, to: null }

test('readsEveryFilterFromTheUrl', () => {
  expect(readFilter(new URLSearchParams('type=ITEM&action=DELETE&mine=1&from=2026-11-01&to=2026-11-17'))).toEqual({
    type: 'ITEM',
    action: 'DELETE',
    mine: true,
    from: '2026-11-01',
    to: '2026-11-17',
  })
})

test('unknownValuesAreIgnored', () => {
  expect(readFilter(new URLSearchParams('action=XYZ&mine=yes&from=17-11-2026'))).toEqual(NONE)
  // Đúng dạng nhưng không có ngày đó trên lịch.
  expect(readFilter(new URLSearchParams('type=ROOM&to=2026-02-30'))).toEqual(NONE)
})

test('writeThenReadRoundTrips', () => {
  const filter: ActivityFilter = { type: 'PERSON', action: 'UPDATE', mine: true, from: '2026-11-01', to: '2026-11-17' }

  expect(writeFilter(filter).toString()).toBe('type=PERSON&action=UPDATE&mine=1&from=2026-11-01&to=2026-11-17')
  expect(readFilter(writeFilter(filter))).toEqual(filter)
})

test('emptyKeysAreLeftOut', () => {
  expect(writeFilter({ ...NONE, action: 'DELETE' }).toString()).toBe('action=DELETE')
  expect(writeFilter(NONE).toString()).toBe('')
})

test('toDateIncludesTheWholeDay', () => {
  const query = toQuery({ ...NONE, from: '2026-11-17', to: '2026-11-17' }, null)

  // Mốc theo giờ máy: 00:00 của ngày `from`, 00:00 của ngày sau `to` (backend không tính mốc `to`).
  expect(query.from).toBe(new Date(2026, 10, 17).toISOString())
  expect(query.to).toBe(new Date(2026, 10, 18).toISOString())
  expect(new Date(2026, 10, 17, 23, 59, 59).getTime()).toBeLessThan(Date.parse(query.to ?? ''))
})

test('toDateOnTheLastDayOfAMonthEndsAtTheNextMonth', () => {
  expect(toQuery({ ...NONE, to: '2026-12-31' }, null).to).toBe(new Date(2027, 0, 1).toISOString())
})

test('mineSendsTheCurrentUser', () => {
  expect(toQuery({ ...NONE, mine: true }, '42')).toEqual({ changedBy: '42' })
  expect(toQuery(NONE, '42')).toEqual({})
})

test('typeAndActionAreSentAsTheyAre', () => {
  expect(toQuery({ ...NONE, type: 'ITEM', action: 'RESTORE' }, '42')).toEqual({ entityType: 'ITEM', action: 'RESTORE' })
})

test('theTypeTabAloneIsNotAFilter', () => {
  expect(hasFilters({ ...NONE, type: 'PLACE' })).toBe(false)
  expect(hasFilters({ ...NONE, mine: true })).toBe(true)
  expect(hasFilters({ ...NONE, to: '2026-11-17' })).toBe(true)
})

test('fromMayEqualToButNotFollowIt', () => {
  expect(rangeIsValid({ ...NONE, from: '2026-11-17', to: '2026-11-17' })).toBe(true)
  expect(rangeIsValid({ ...NONE, from: '2026-11-18', to: '2026-11-17' })).toBe(false)
  expect(rangeIsValid({ ...NONE, from: '2026-11-18' })).toBe(true)
})
