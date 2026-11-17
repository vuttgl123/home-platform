import { expect, test } from 'vitest'

import { exportFileName } from './fileName'

// 10:00 ngày 17/11/2026 ở Việt Nam.
const TODAY = new Date('2026-11-17T03:00:00Z')

test('removesVietnameseMarks', () => {
  expect(exportFileName('Chung cư Hòa Bình', 'items', TODAY)).toBe('chung-cu-hoa-binh-do-dac-2026-11-17.xlsx')
})

test('otherCharactersCollapseIntoOneDash', () => {
  expect(exportFileName('Đảo  Ngọc!', 'workspace', TODAY)).toBe('dao-ngoc-toan-bo-2026-11-17.xlsx')
  expect(exportFileName('  Nhà #2 (thuê) ', 'people', TODAY)).toBe('nha-2-thue-nguoi-2026-11-17.xlsx')
})

test('emptyNameFallsBack', () => {
  expect(exportFileName('', 'people', TODAY)).toBe('khong-gian-nguoi-2026-11-17.xlsx')
  expect(exportFileName(undefined, 'items', TODAY)).toBe('khong-gian-do-dac-2026-11-17.xlsx')
  expect(exportFileName('!!!', 'workspace', TODAY)).toBe('khong-gian-toan-bo-2026-11-17.xlsx')
})

/** Ngày trong tên file là ngày ở Việt Nam, như tên dự phòng của máy chủ, không phải ngày UTC. */
test('dateIsTheBusinessDay', () => {
  expect(exportFileName('A', 'items', new Date('2026-11-17T18:30:00Z'))).toBe('a-do-dac-2026-11-18.xlsx')
})
