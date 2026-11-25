import { render, screen } from '@testing-library/react'
import { expect, test } from 'vitest'

import { CapacityLine } from './components/CapacityLine'

test('capacityLineShowsEachCase', () => {
  const { rerender } = render(<CapacityLine capacity={undefined} occupants={3} />)
  expect(screen.getByText('Chưa đặt · đang ở 3')).toBeInTheDocument()
  expect(document.querySelector('[data-tone]')).toBeNull()

  rerender(<CapacityLine capacity={4} occupants={1} />)
  expect(screen.getByText('1/4 người · còn 3 chỗ')).toHaveAttribute('data-tone', 'success')

  rerender(<CapacityLine capacity={4} occupants={4} />)
  expect(screen.getByText('4/4 · đã đầy')).toHaveAttribute('data-tone', 'warning')

  rerender(<CapacityLine capacity={2} occupants={5} />)
  expect(screen.getByText('5/2 · vượt 3')).toHaveAttribute('data-tone', 'danger')

  rerender(<CapacityLine capacity={0} occupants={0} />)
  expect(screen.getByText('0/0 · đã đầy')).toHaveAttribute('data-tone', 'warning')
})
