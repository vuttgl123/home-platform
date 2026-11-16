import { expect, test } from 'vitest'

import { item, person, searchResult } from '@/test/fixtures'

import { groupResults } from './groupResults'

test('codeMatchesComeFirstAndAreNotRepeated', () => {
  const p301 = searchResult({ id: '301', name: 'P-0042 kho', code: 'P-0042' })
  const groups = groupResults('7', { places: { byCode: [p301], byName: [p301, searchResult({ id: '302', name: 'P-00 khác' })] } })

  expect(groups.codeMatches.map((row) => row.key)).toEqual(['PLACE:301'])
  expect(groups.places.map((row) => row.key)).toEqual(['PLACE:302'])
})

test('placesCarryTheirAncestorTrail', () => {
  const groups = groupResults('7', {
    places: {
      byCode: [],
      byName: [searchResult({ id: '301', name: 'Phòng 301', ancestors: [{ id: '1', name: 'Toà A' }, { id: '3', name: 'Tầng 3' }] })],
    },
  })

  expect(groups.places[0]).toMatchObject({ title: 'Phòng 301', detail: 'Toà A › Tầng 3', href: '/w/7/places/301' })
})

test('removedItemsAreFlagged', () => {
  const groups = groupResults('7', {
    items: { byCode: [], byName: [item({ id: '901', name: 'Quạt', placeName: 'Kho', removedAt: '2026-01-01', status: 'REMOVED' })] },
  })

  expect(groups.items[0]).toMatchObject({ detail: 'ở Kho', removed: true, href: '/w/7/items/901' })
})

test('missingKindsGiveEmptyGroups', () => {
  const groups = groupResults('7', { people: [person({ id: '55', fullName: 'An', phone: '0912' })] })

  expect(groups.places).toEqual([])
  expect(groups.items).toEqual([])
  expect(groups.people[0]).toMatchObject({ title: 'An', detail: '0912', href: '/w/7/people/55' })
})
