import { expect, test } from 'vitest'

import { dueLabel, todoHref } from './labels'

const TODAY = '2026-11-25'

test('overdueIsDanger', () => {
  expect(dueLabel({ kind: 'SERVICE', dueOn: '2026-11-22' }, TODAY)).toEqual({ text: 'quá hạn 3 ngày', tone: 'danger' })
})

test('todayAndThisWeekAreWarnings', () => {
  expect(dueLabel({ kind: 'WARRANTY', dueOn: TODAY }, TODAY)).toEqual({ text: 'hôm nay', tone: 'warning' })
  expect(dueLabel({ kind: 'MOVE_OUT', dueOn: '2026-11-30' }, TODAY)).toEqual({ text: 'còn 5 ngày', tone: 'warning' })
  expect(dueLabel({ kind: 'CUSTOM_DATE', dueOn: '2026-12-02' }, TODAY)).toEqual({ text: 'còn 7 ngày', tone: 'warning' })
})

test('laterIsNeutral', () => {
  expect(dueLabel({ kind: 'WARRANTY', dueOn: '2026-12-07' }, TODAY)).toEqual({ text: 'còn 12 ngày', tone: 'neutral' })
})

test('brokenSaysSinceWhen', () => {
  expect(dueLabel({ kind: 'BROKEN', dueOn: '2026-11-20' }, TODAY)).toEqual({ text: 'hỏng từ 20/11', tone: 'danger' })
})

test('hrefOpensTheRightRecord', () => {
  expect(todoHref('7', { kind: 'BROKEN', subjectType: 'ITEM', subjectId: '900' })).toBe('/w/7/items/900')
  expect(todoHref('7', { kind: 'SERVICE', subjectType: 'ITEM', subjectId: '900' })).toBe('/w/7/items/900?tab=maintenance')
  expect(todoHref('7', { kind: 'MOVE_OUT', subjectType: 'PERSON', subjectId: '55' })).toBe('/w/7/people/55')
  expect(todoHref('7', { kind: 'CUSTOM_DATE', subjectType: 'PLACE', subjectId: '1' })).toBe('/w/7/places/1')
})
