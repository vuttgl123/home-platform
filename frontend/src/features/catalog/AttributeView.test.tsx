import { screen } from '@testing-library/react'
import { expect, test } from 'vitest'

import { fieldDef } from '@/test/fixtures'
import { renderWithProviders } from '@/test/render'

import { AttributeView } from './components/AttributeView'

function valueOf(label: string) {
  return screen.getByText(label).nextElementSibling?.textContent
}

test('rendersAttributesByType', () => {
  const defs = [
    fieldDef({ id: '1', fieldKey: 'area_m2', label: 'Diện tích', dataType: 'NUMBER', sortOrder: 30 }),
    fieldDef({ id: '2', fieldKey: 'has_balcony', label: 'Ban công', dataType: 'BOOLEAN', sortOrder: 10 }),
    fieldDef({ id: '3', fieldKey: 'handover', label: 'Ngày bàn giao', dataType: 'DATE', sortOrder: 20 }),
    fieldDef({ id: '4', fieldKey: 'view', label: 'Hướng nhìn', dataType: 'TEXT', sortOrder: 40 }),
    fieldDef({ id: '5', fieldKey: 'old', label: 'Mục cũ', dataType: 'TEXT', sortOrder: 50, status: 'ARCHIVED' }),
  ]

  renderWithProviders(
    <AttributeView defs={defs} values={{ area_m2: 42, has_balcony: true, handover: '2026-01-31', stray: 'x' }} />,
  )

  expect(valueOf('Diện tích')).toBe('42')
  expect(valueOf('Ban công')).toBe('Có')
  expect(valueOf('Ngày bàn giao')).toBe('31/01/2026')
  // Mục đang dùng mà trống vẫn hiện, để người xem biết còn thiếu gì.
  expect(valueOf('Hướng nhìn')).toBe('—')
  // Mục đã lưu trữ chỉ hiện khi còn giá trị; khoá không có mục thì bỏ qua.
  expect(screen.queryByText('Mục cũ')).not.toBeInTheDocument()
  expect(screen.queryByText('x')).not.toBeInTheDocument()
  expect(screen.getAllByRole('term').map((term) => term.textContent)).toEqual([
    'Ban công',
    'Ngày bàn giao',
    'Diện tích',
    'Hướng nhìn',
  ])
})

test('archivedValueIsShownDimmed', () => {
  const defs = [fieldDef({ fieldKey: 'old', label: 'Mục cũ', dataType: 'TEXT', status: 'ARCHIVED' })]

  renderWithProviders(<AttributeView defs={defs} values={{ old: 'giá trị cũ' }} />)

  expect(screen.getByText('giá trị cũ')).toBeInTheDocument()
  expect(screen.getByText('Mục cũ').closest('div')).toHaveAttribute('data-archived', 'true')
})

test('booleanFalseIsNo', () => {
  const defs = [fieldDef({ fieldKey: 'pet', label: 'Nuôi thú', dataType: 'BOOLEAN' })]

  renderWithProviders(<AttributeView defs={defs} values={{ pet: false }} />)

  expect(valueOf('Nuôi thú')).toBe('Không')
})
