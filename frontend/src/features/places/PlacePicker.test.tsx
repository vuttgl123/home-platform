import { screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { useState } from 'react'
import { expect, test } from 'vitest'

import { setCurrentUser } from '@/api'
import { page, searchResult } from '@/test/fixtures'
import { renderWithProviders } from '@/test/render'
import { server } from '@/test/server'

import { PlacePicker, type PlaceChoice } from './components/PlacePicker'

function Harness({ exclude = [], allowTop = false }: { exclude?: string[]; allowTop?: boolean }) {
  const [choice, setChoice] = useState<PlaceChoice | null>(null)
  return (
    <>
      <PlacePicker ws="7" exclude={exclude} allowTop={allowTop} value={choice} onChange={setChoice} />
      <output>{choice == null ? 'none' : choice.kind === 'top' ? 'top' : choice.place.id}</output>
    </>
  )
}

function mockSearch() {
  server.use(
    http.get('/api/registry/places/search', () =>
      HttpResponse.json(
        page([
          searchResult({ id: '301', name: 'P301', ancestors: [{ id: '1', name: 'Toà A' }, { id: '13', name: 'Tầng 3' }] }),
          searchResult({ id: '302', name: 'P302', code: 'R-302', ancestors: [{ id: '1', name: 'Toà A' }] }),
        ]),
      ),
    ),
  )
}

test('movePickerShowsAncestors', async () => {
  setCurrentUser('42')
  mockSearch()
  const { user } = renderWithProviders(<Harness />)

  await user.type(screen.getByRole('searchbox', { name: 'Tìm nơi chuyển tới' }), 'P3')

  const p301 = await screen.findByRole('radio', { name: 'P301' })
  expect(p301).toHaveAccessibleDescription('Toà A › Tầng 3')
  expect(screen.getByRole('radio', { name: 'P302' })).toHaveAccessibleDescription('Toà A · R-302')
  await user.click(p301)
  expect(screen.getByRole('status')).toHaveTextContent('301')
})

test('pickerExcludesAndOffersTheTop', async () => {
  setCurrentUser('42')
  mockSearch()
  const { user } = renderWithProviders(<Harness exclude={['302']} allowTop />)

  await user.click(screen.getByRole('radio', { name: 'Lên cấp cao nhất' }))
  expect(screen.getByRole('status')).toHaveTextContent('top')

  await user.type(screen.getByRole('searchbox', { name: 'Tìm nơi chuyển tới' }), 'P3')
  await screen.findByRole('radio', { name: 'P301' })
  await waitFor(() => expect(screen.queryByRole('radio', { name: 'P302' })).not.toBeInTheDocument())
})

