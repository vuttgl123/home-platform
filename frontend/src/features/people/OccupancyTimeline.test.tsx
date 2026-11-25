import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { expect, test } from 'vitest'

import { TooltipProvider } from '@/components/ui/tooltip'
import { occupancy } from '@/test/fixtures'

import { OccupancyTimeline } from './components/OccupancyTimeline'

function draw(rows: ReturnType<typeof occupancy>[], truncated = false) {
  return render(
    <MemoryRouter>
      <TooltipProvider>
        <OccupancyTimeline ws="7" groupBy="person" rows={rows} truncated={truncated} today="2026-06-15" />
      </TooltipProvider>
    </MemoryRouter>,
  )
}

test('drawsLanesBarsAndToday', () => {
  draw([
    occupancy({ id: '1', personId: '55', personFullName: 'Nguyễn An', startDate: '2026-01-10', endDate: '2026-03-01', status: 'PAST' }),
    occupancy({ id: '2', personId: '55', personFullName: 'Nguyễn An', startDate: '2026-04-01', endDate: null, status: 'CURRENT' }),
    occupancy({ id: '3', personId: '56', personFullName: 'Trần Bình', startDate: '2026-07-01', endDate: null, status: 'UPCOMING' }),
  ])

  const card = within(screen.getByRole('region', { name: 'Dòng thời gian' }))
  expect(card.getByRole('link', { name: 'Nguyễn An' })).toHaveAttribute('href', '/w/7/people/55')
  expect(card.getByRole('link', { name: 'Trần Bình' })).toHaveAttribute('href', '/w/7/people/56')
  const bars = card.getAllByRole('img')
  expect(bars.map((bar) => bar.getAttribute('data-status'))).toEqual(['PAST', 'CURRENT', 'UPCOMING'])
  expect(bars[0]).toHaveAccessibleName('Người ở · 10/01/2026 → 01/03/2026')
  expect(bars[1]).toHaveAccessibleName('Người ở · 01/04/2026 → nay')
  expect(card.getByText('Hôm nay')).toBeInTheDocument()
})

test('hiddenWhenThereAreNoRows', () => {
  draw([])
  expect(screen.queryByRole('region', { name: 'Dòng thời gian' })).toBeNull()
})

test('notesTruncation', () => {
  draw([occupancy()], true)
  expect(screen.getByText('Chỉ hiện 200 lần ở gần nhất')).toBeInTheDocument()
})
