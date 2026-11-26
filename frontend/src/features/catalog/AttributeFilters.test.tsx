import { screen, within } from '@testing-library/react'
import { useState } from 'react'
import { expect, test } from 'vitest'

import { fieldDef } from '@/test/fixtures'
import { renderWithProviders } from '@/test/render'

import type { AttributeCondition } from './attributeFilters'
import { AttributeFilters } from './components/AttributeFilters'

const area = fieldDef({ id: '1', fieldKey: 'dien_tich', label: 'Diện tích', dataType: 'NUMBER', typeId: '0', sortOrder: 1 })
const handover = fieldDef({ id: '2', fieldKey: 'ban_giao', label: 'Bàn giao', dataType: 'DATE', typeId: '0', sortOrder: 2 })
const view = fieldDef({
  id: '3',
  fieldKey: 'huong',
  label: 'Hướng',
  dataType: 'SELECT',
  typeId: '0',
  sortOrder: 3,
  options: ['Đông', 'Tây'],
})
const defs = [area, handover, view]

function Harness({ initial = [] }: { initial?: AttributeCondition[] }) {
  const [value, setValue] = useState(initial)
  return (
    <>
      <AttributeFilters defs={defs} value={value} onChange={setValue} />
      <output data-testid="value">{JSON.stringify(value)}</output>
    </>
  )
}

function current(): AttributeCondition[] {
  return JSON.parse(screen.getByTestId('value').textContent ?? '[]')
}

function optionsOf(select: HTMLElement): string[] {
  return within(select)
    .getAllByRole('option')
    .map((option) => option.textContent ?? '')
}

test('addingAConditionPicksTheFirstField', async () => {
  const { user } = renderWithProviders(<Harness />)

  await user.click(screen.getByRole('button', { name: 'Thêm điều kiện' }))

  expect(current()).toEqual([{ key: 'dien_tich', op: 'eq', value: '' }])
  expect(screen.getByRole('combobox', { name: 'Mục của điều kiện 1' })).toHaveValue('dien_tich')
})

test('operatorsFollowTheFieldType', async () => {
  const { user } = renderWithProviders(<Harness initial={[{ key: 'dien_tich', op: 'eq', value: '' }]} />)

  expect(optionsOf(screen.getByRole('combobox', { name: 'Phép so của điều kiện 1' }))).toEqual([
    'bằng',
    'từ',
    'đến',
    'có giá trị',
    'trống',
  ])

  await user.selectOptions(screen.getByRole('combobox', { name: 'Mục của điều kiện 1' }), 'Bàn giao')

  expect(current()).toEqual([{ key: 'ban_giao', op: 'gte', value: '' }])
  expect(optionsOf(screen.getByRole('combobox', { name: 'Phép so của điều kiện 1' }))).toEqual([
    'từ',
    'đến',
    'có giá trị',
    'trống',
  ])
})

test('valueInputFollowsTheType', async () => {
  const { user } = renderWithProviders(
    <Harness
      initial={[
        { key: 'ban_giao', op: 'gte', value: '' },
        { key: 'huong', op: 'eq', value: '' },
      ]}
    />,
  )

  expect(screen.getByLabelText('Giá trị của điều kiện 1')).toHaveAttribute('type', 'date')
  const choice = screen.getByRole('combobox', { name: 'Giá trị của điều kiện 2' })
  expect(optionsOf(choice)).toEqual(['—', 'Đông', 'Tây'])
  await user.selectOptions(choice, 'Tây')
  expect(current()[1]).toEqual({ key: 'huong', op: 'eq', value: 'Tây' })

  await user.selectOptions(screen.getByRole('combobox', { name: 'Phép so của điều kiện 1' }), 'có giá trị')
  expect(screen.queryByLabelText('Giá trị của điều kiện 1')).not.toBeInTheDocument()
})

test('typingANumberUpdatesTheValue', async () => {
  const { user } = renderWithProviders(<Harness initial={[{ key: 'dien_tich', op: 'gte', value: '' }]} />)

  await user.type(screen.getByLabelText('Giá trị của điều kiện 1'), '20')

  expect(current()).toEqual([{ key: 'dien_tich', op: 'gte', value: '20' }])
})

test('addIsDisabledAtThree', () => {
  const full: AttributeCondition[] = [
    { key: 'dien_tich', op: 'set', value: '' },
    { key: 'ban_giao', op: 'set', value: '' },
    { key: 'huong', op: 'set', value: '' },
  ]
  renderWithProviders(<Harness initial={full} />)

  expect(screen.getByRole('button', { name: 'Thêm điều kiện' })).toBeDisabled()
})

test('removeDropsTheCondition', async () => {
  const { user } = renderWithProviders(
    <Harness
      initial={[
        { key: 'dien_tich', op: 'set', value: '' },
        { key: 'huong', op: 'set', value: '' },
      ]}
    />,
  )

  await user.click(screen.getByRole('button', { name: 'Bỏ điều kiện Diện tích' }))

  expect(current()).toEqual([{ key: 'huong', op: 'set', value: '' }])
})

test('noFieldsMeansNoAddButton', () => {
  renderWithProviders(<AttributeFilters defs={[]} value={[]} onChange={() => {}} />)

  expect(screen.queryByRole('button', { name: 'Thêm điều kiện' })).not.toBeInTheDocument()
})
