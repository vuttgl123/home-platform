import { expect, test } from 'vitest'

import { compareRows } from './compare'
import type { SnapshotRow } from './types'

function row(key: string, raw: unknown, label = key): SnapshotRow {
  return { key, label, value: String(raw), raw }
}

test('sameValuesAreNotChanged', () => {
  const lines = compareRows([row('name', 'P301')], [row('name', 'P301')])

  expect(lines).toEqual([{ key: 'name', label: 'name', older: 'P301', newer: 'P301', changed: false }])
})

test('differentValuesAreChanged', () => {
  const lines = compareRows([row('name', 'P-301'), row('code', 'R1')], [row('name', 'P301'), row('code', 'R1')])

  expect(lines.map((line) => [line.key, line.changed])).toEqual([
    ['name', true],
    ['code', false],
  ])
})

test('linesFollowTheNewerOrderThenKeysOnlyInOlder', () => {
  const lines = compareRows([row('b', 1), row('gone', 'x', 'Mục cũ'), row('a', 1)], [row('a', 1), row('b', 2), row('new', 3)])

  expect(lines.map((line) => line.key)).toEqual(['a', 'b', 'new', 'gone'])
})

test('keyOnlyInOlderIsKept', () => {
  // Mục đã lưu trữ: bản cũ còn giá trị, bản mới không còn dòng đó.
  const [line] = compareRows([row('attr:old', 'giá trị cũ', 'Mục cũ')], [])

  expect(line).toEqual({ key: 'attr:old', label: 'Mục cũ', older: 'giá trị cũ', newer: null, changed: true })
})

test('keyOnlyInNewerIsChanged', () => {
  const [line] = compareRows([], [row('attr:area', 20, 'Diện tích')])

  expect(line).toMatchObject({ older: null, newer: '20', changed: true })
})

test('objectValuesCompareByContent', () => {
  const address = { fullAddress: '1 Tràng Tiền', wardCode: null, latitude: 21.02 }
  // Cùng nội dung, khác đối tượng và khác thứ tự khoá.
  const sameAddress = { latitude: 21.02, fullAddress: '1 Tràng Tiền', wardCode: null }

  expect(compareRows([row('address', address)], [row('address', sameAddress)])[0].changed).toBe(false)
  expect(compareRows([row('address', address)], [row('address', { ...address, fullAddress: '2 Tràng Tiền' })])[0].changed).toBe(true)
})

test('nullAndMissingAreTheSame', () => {
  expect(compareRows([row('attr:pet', null)], [row('attr:pet', undefined)])[0].changed).toBe(false)
  expect(compareRows([row('attr:pet', null)], [row('attr:pet', false)])[0].changed).toBe(true)
})
