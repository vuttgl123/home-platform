import { expect, test } from 'vitest'

import { fieldDef } from '@/test/fixtures'

import { filterableDefs, MAX_CONDITIONS, parseConditions, toApiValues, toUrlValues } from './attributeFilters'

const area = fieldDef({ id: '1', fieldKey: 'dien_tich', label: 'Diện tích', dataType: 'NUMBER', typeId: '0' })
const note = fieldDef({ id: '2', fieldKey: 'ghi_chu', label: 'Ghi chú', dataType: 'TEXT', typeId: '0' })
const handover = fieldDef({ id: '3', fieldKey: 'ban_giao', label: 'Bàn giao', dataType: 'DATE', typeId: '0' })
const old = fieldDef({ id: '4', fieldKey: 'cu', label: 'Cũ', dataType: 'TEXT', typeId: '0', status: 'ARCHIVED' })
const defs = [area, note, handover, old]

test('parsesAndWritesTheUrlForm', () => {
  const conditions = parseConditions(['dien_tich~gte~20', 'ghi_chu~set~'], defs)

  expect(conditions).toEqual([
    { key: 'dien_tich', op: 'gte', value: '20' },
    { key: 'ghi_chu', op: 'set', value: '' },
  ])
  expect(toUrlValues(conditions)).toEqual(['dien_tich~gte~20', 'ghi_chu~set~'])
})

test('valueKeepsItsTildes', () => {
  expect(parseConditions(['ghi_chu~has~a~b'], defs)).toEqual([{ key: 'ghi_chu', op: 'has', value: 'a~b' }])
})

test('unknownKeysAndWrongOpsAreDropped', () => {
  expect(parseConditions(['x~set~', 'cu~set~', 'dien_tich~has~2', 'ban_giao~eq~2026-01-01', 'ghi_chu~zz~a', 'ghi_chu'], defs)).toEqual([])
})

test('atMostThree', () => {
  const raw = ['ghi_chu~set~', 'dien_tich~set~', 'ban_giao~set~', 'ghi_chu~unset~']

  expect(parseConditions(raw, defs)).toHaveLength(MAX_CONDITIONS)
})

test('apiFormSkipsIncompleteConditions', () => {
  expect(
    toApiValues([
      { key: 'ghi_chu', op: 'has', value: '  ' },
      { key: 'ghi_chu', op: 'set', value: '' },
      { key: 'ghi_chu', op: 'has', value: 'a:b' },
    ]),
  ).toEqual(['ghi_chu:set:', 'ghi_chu:has:a:b'])
})

test('filterableDefsNarrowByType', () => {
  const room = fieldDef({ id: '5', fieldKey: 'so_giuong', label: 'Số giường', typeId: '100', sortOrder: 5 })
  const shelf = fieldDef({ id: '6', fieldKey: 'so_tang', label: 'Số tầng', typeId: '200' })
  const sameKey = fieldDef({ id: '7', fieldKey: 'so_giuong', label: 'Số giường (kho)', typeId: '200', sortOrder: 9 })
  const all = [...defs, room, shelf, sameKey]

  expect(filterableDefs(all, null).map((def) => def.id)).toEqual(['5', '1', '2', '3', '6'])
  expect(filterableDefs(all, '100').map((def) => def.id)).toEqual(['5', '1', '2', '3'])
})
