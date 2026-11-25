import { expect, test } from 'vitest'

import { occupancy } from '@/test/fixtures'

import { timelineLayout } from './timeline'

test('axisSpansFirstStartToLastEndOrToday', () => {
  const rows = [
    occupancy({ id: '1', personId: '55', startDate: '2026-01-10', endDate: '2026-03-01', status: 'PAST' }),
    occupancy({ id: '2', personId: '56', startDate: '2026-02-01', endDate: null }),
  ]
  expect(timelineLayout(rows, 'person', '2026-06-15')).toMatchObject({ start: '2026-01-03', end: '2026-06-22' })

  const later = [...rows, occupancy({ id: '3', personId: '57', startDate: '2026-05-01', endDate: '2026-09-01', status: 'CURRENT' })]
  expect(timelineLayout(later, 'person', '2026-06-15')).toMatchObject({ start: '2026-01-03', end: '2026-09-08' })
})

test('barsArePlacedByDate', () => {
  // Trục 01/01 tới 18/01 (18 ngày): thanh 08/01 tới hết 11/01 bắt đầu ở ngày thứ 7, dài 4 ngày.
  const rows = [occupancy({ id: '1', startDate: '2026-01-08', endDate: '2026-01-11', status: 'PAST' })]
  const layout = timelineLayout(rows, 'person', '2026-01-08')
  expect(layout.start).toBe('2026-01-01')
  expect(layout.end).toBe('2026-01-18')
  const bar = layout.lanes[0].bars[0]
  expect(bar.left).toBeCloseTo((7 / 18) * 100)
  expect(bar.width).toBeCloseTo((4 / 18) * 100)
  expect(bar.open).toBe(false)
  expect(layout.todayLeft).toBeCloseTo((7 / 18) * 100)
})

test('sameRowHoldsEveryStayOfOnePerson', () => {
  const rows = [
    occupancy({ id: '2', personId: '55', personFullName: 'Nguyễn An', startDate: '2026-12-11', endDate: '2026-12-20' }),
    occupancy({ id: '1', personId: '55', personFullName: 'Nguyễn An', startDate: '2026-12-01', endDate: '2026-12-10' }),
    occupancy({ id: '3', personId: '56', personFullName: 'Trần Bình', startDate: '2026-12-05', endDate: null }),
  ]
  const layout = timelineLayout(rows, 'person', '2026-12-15')
  expect(layout.lanes.map((lane) => [lane.key, lane.label, lane.bars.length])).toEqual([
    ['55', 'Nguyễn An', 2],
    ['56', 'Trần Bình', 1],
  ])
  const byPlace = timelineLayout(rows, 'place', '2026-12-15')
  expect(byPlace.lanes.map((lane) => [lane.key, lane.label])).toEqual([['301', 'P301']])
})

test('openFutureStayReachesTheRightEdge', () => {
  const rows = [occupancy({ id: '1', startDate: '2026-08-14', endDate: null, status: 'UPCOMING' })]
  const layout = timelineLayout(rows, 'person', '2026-06-15')
  expect(layout.end).toBe('2026-08-21')
  const bar = layout.lanes[0].bars[0]
  expect(bar.open).toBe(true)
  expect(bar.left + bar.width).toBeCloseTo(100)
  expect(bar.status).toBe('UPCOMING')
})

test('monthTicksAreLabelledInVietnamese', () => {
  const rows = [occupancy({ startDate: '2026-11-20', endDate: '2027-02-10', status: 'PAST' })]
  const labels = timelineLayout(rows, 'person', '2026-12-01').months.map((month) => month.label)
  expect(labels).toEqual(['T12/2026', 'T1/2027', 'T2/2027'])
})

test('longRangesKeepFewTicks', () => {
  // Gõ nhầm năm (2999) không được sinh hàng nghìn vạch tháng.
  const rows = [
    occupancy({ id: '1', startDate: '2020-01-01', endDate: '2020-12-31', status: 'PAST' }),
    occupancy({ id: '2', personId: '56', startDate: '2999-01-01', endDate: null, status: 'UPCOMING' }),
  ]
  const months = timelineLayout(rows, 'person', '2026-06-15').months
  expect(months.length).toBeGreaterThan(0)
  expect(months.length).toBeLessThanOrEqual(12)
  expect(months[0].label).toBe('T1/2020')
})
