import { describe, expect, test } from 'vitest'

import { fieldDef } from '@/test/fixtures'

import { attributeDefaults, attributeErrors, attributePayload } from './attributes'

const area = fieldDef({ id: '1', fieldKey: 'area_m2', label: 'Diện tích', dataType: 'NUMBER' })
const note = fieldDef({ id: '2', fieldKey: 'note', label: 'Ghi chú', dataType: 'TEXT' })
const balcony = fieldDef({ id: '3', fieldKey: 'balcony', label: 'Ban công', dataType: 'BOOLEAN' })
const handover = fieldDef({ id: '4', fieldKey: 'handover', label: 'Bàn giao', dataType: 'DATE' })
const old = fieldDef({ id: '5', fieldKey: 'old', label: 'Mục cũ', dataType: 'TEXT', status: 'ARCHIVED' })

describe('attributeDefaults', () => {
  test('turnsStoredValuesIntoFormValues', () => {
    expect(attributeDefaults([area, note, balcony, handover, old], { area_m2: 42, balcony: true, old: 'x' })).toEqual({
      area_m2: '42',
      note: '',
      balcony: true,
      handover: '',
    })
  })
})

describe('attributePayload', () => {
  test('numberTextBecomesANumber', () => {
    expect(attributePayload([area], { area_m2: ' 42.5 ' }, null)).toEqual({ area_m2: 42.5 })
  })

  test('blankValuesAreDropped', () => {
    expect(attributePayload([area, note, balcony], { area_m2: '', note: '  ', balcony: false }, null)).toEqual({
      balcony: false,
    })
  })

  test('datesAndTextAreSentAsStrings', () => {
    expect(attributePayload([note, handover], { note: 'Hướng Đông', handover: '2026-01-31' }, null)).toEqual({
      note: 'Hướng Đông',
      handover: '2026-01-31',
    })
  })

  test('archivedValuesAreKept', () => {
    expect(attributePayload([area, old], { area_m2: '5' }, { area_m2: 4, old: 'giữ nguyên' })).toEqual({
      area_m2: 5,
      old: 'giữ nguyên',
    })
  })

  test('valuesOfOtherTypesAreDropped', () => {
    // Đổi loại địa điểm: mục của loại cũ không còn áp dụng, backend sẽ từ chối nếu gửi lên.
    expect(attributePayload([area], { area_m2: '5' }, { area_m2: 4, star_rating: 3 })).toEqual({ area_m2: 5 })
  })

  test('nothingLeftIsNull', () => {
    expect(attributePayload([area], { area_m2: '' }, { star_rating: 3 })).toBeNull()
  })
})

describe('attributeErrors', () => {
  test('numberTextMustBeANumber', () => {
    expect(attributeErrors([area], { area_m2: '4,5' })).toEqual({ area_m2: 'Phải là số' })
    expect(attributeErrors([area], { area_m2: '12abc' })).toEqual({ area_m2: 'Phải là số' })
    expect(attributeErrors([area], { area_m2: '-3.25' })).toEqual({})
  })

  test('requiredBlankIsAnError', () => {
    const required = { ...note, required: true }
    expect(attributeErrors([required], { note: ' ' })).toEqual({ note: 'Bắt buộc' })
    expect(attributeErrors([required], { note: 'có' })).toEqual({})
  })

  test('archivedFieldsAreNotChecked', () => {
    expect(attributeErrors([{ ...old, required: true }], {})).toEqual({})
  })

  test('longTextIsAnError', () => {
    expect(attributeErrors([note], { note: 'a'.repeat(1001) })).toEqual({ note: 'Tối đa 1000 ký tự' })
  })
})
