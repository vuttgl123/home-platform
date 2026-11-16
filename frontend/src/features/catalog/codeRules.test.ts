import { expect, test } from 'vitest'

import { codeExample, PREFIX_PATTERN, suggestTypeCode } from './codeRules'

test('exampleFollowsPrefixDigitsAndYear', () => {
  expect(codeExample({ prefix: 'DD', digits: 6, yearly: false }, 2026)).toBe('DD-000001')
  expect(codeExample({ prefix: 'TS', digits: 4, yearly: true }, 2026)).toBe('TS-2026-0001')
})

test('prefixMustStartWithALetter', () => {
  expect(PREFIX_PATTERN.test('P')).toBe(true)
  expect(PREFIX_PATTERN.test('P2')).toBe(true)
  expect(PREFIX_PATTERN.test('2P')).toBe(false)
  expect(PREFIX_PATTERN.test('A-B')).toBe(false)
  expect(PREFIX_PATTERN.test('ABCDEFGHIJK')).toBe(false)
})

test('typeCodeDropsAccentsAndUppercases', () => {
  expect(suggestTypeCode('Toà nhà', [])).toBe('TOA_NHA')
  expect(suggestTypeCode('Phòng đôi (VIP)', [])).toBe('PHONG_DOI_VIP')
})

test('typeCodeAvoidsTakenCodes', () => {
  expect(suggestTypeCode('Toà nhà', ['TOA_NHA', 'toa_nha_2'])).toBe('TOA_NHA_3')
})

test('nameWithoutLettersGivesNoCode', () => {
  expect(suggestTypeCode('  ?! ', [])).toBe('')
})
