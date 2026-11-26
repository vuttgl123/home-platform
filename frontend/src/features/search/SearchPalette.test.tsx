import { screen, waitFor, within } from '@testing-library/react'
import { delay, http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { item, page, person, placeType, problem, searchResult } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

type Seen = { path: string; query: string }

/** Ba API tìm; ghi lại mọi lần gọi. `fail` làm một loại trả 500. */
function mockSearch(options: { fail?: 'items'; slowFor?: string } = {}) {
  const seen: Seen[] = []
  const note = (request: Request) => {
    const url = new URL(request.url)
    seen.push({ path: url.pathname.replace('/api/registry', ''), query: url.searchParams.toString() })
    return url.searchParams
  }
  server.use(
    http.get('/api/registry/places/search', async ({ request }) => {
      const query = note(request)
      if (options.slowFor && query.get('name') === options.slowFor) await delay(600)
      const term = query.get('name') ?? query.get('code') ?? ''
      if (query.get('code') === 'p-0042') return HttpResponse.json(page([searchResult({ id: '301', name: 'Phòng 301', code: 'P-0042' })]))
      if (query.get('name') === 'p-0042') return HttpResponse.json(page([searchResult({ id: '301', name: 'Phòng 301', code: 'P-0042' })]))
      if (query.get('name')?.startsWith('ph')) {
        return HttpResponse.json(
          page([
            searchResult({ id: '301', name: `Phòng 301 (${term})`, ancestors: [{ id: '1', name: 'Toà A' }] }),
            searchResult({ id: '401', name: `Phòng 301 (${term})`, ancestors: [{ id: '2', name: 'Toà B' }] }),
          ]),
        )
      }
      return HttpResponse.json(page([]))
    }),
    http.get('/api/registry/people/search', ({ request }) => {
      const query = note(request)
      if (query.get('phone') === '0912') return HttpResponse.json(page([person({ id: '55', fullName: 'Nguyễn An', phone: '0912345' })]))
      return HttpResponse.json(page([]))
    }),
    http.get('/api/registry/items/search', ({ request }) => {
      const query = note(request)
      if (options.fail === 'items') return HttpResponse.json(problem(500, 'boom'), { status: 500 })
      if (query.get('name')?.startsWith('ph')) return HttpResponse.json(page([item({ id: '901', name: 'Phích', placeName: 'Kho' })]))
      return HttpResponse.json(page([]))
    }),
  )
  return seen
}

async function openPalette(route = '/w/7') {
  mockShell({ placeTypes: [placeType()] })
  const app = renderApp({ route, userId: '42' })
  await screen.findByRole('main')
  await app.user.keyboard('{Control>}k{/Control}')
  return { ...app, palette: within(await screen.findByRole('dialog', { name: 'Tìm nhanh' })) }
}

test('ctrlKOpensThePaletteFromAnyPage', async () => {
  mockSearch()
  server.use(http.get('/api/registry/trash', () => HttpResponse.json(page([]))))
  const { palette } = await openPalette('/w/7/trash')
  expect(palette.getByRole('combobox', { name: 'Tìm địa điểm, người, đồ đạc' })).toBeInTheDocument()
})

test('searchButtonOpensIt', async () => {
  mockSearch()
  mockShell({ placeTypes: [placeType()] })
  const { user } = renderApp({ route: '/w/7', userId: '42' })

  await user.click(await screen.findByRole('button', { name: /Tìm…/ }))

  expect(await screen.findByRole('dialog', { name: 'Tìm nhanh' })).toBeInTheDocument()
})

test('emptyBoxOffersShortcuts', async () => {
  mockSearch()
  const { palette } = await openPalette()

  const options = palette.getAllByRole('option').map((option) => option.textContent)
  expect(options).toEqual(['Người', 'Đồ đạc', 'Danh mục', 'Hoạt động', 'Thùng rác'])
})

test('typingFastSendsOneRoundOfRequests', async () => {
  const seen = mockSearch()
  const { user, palette } = await openPalette()

  await user.type(palette.getByRole('combobox'), 'phong')

  await palette.findAllByRole('option', { name: /Phòng 301/ })
  const placeCalls = seen.filter((call) => call.path === '/places/search')
  expect(placeCalls.map((call) => new URLSearchParams(call.query).get('name') ?? `code:${new URLSearchParams(call.query).get('code')}`).sort())
    .toEqual(['code:phong', 'phong'])
  expect(seen.filter((call) => call.path === '/items/search')).toHaveLength(2)
  expect(seen.filter((call) => call.path === '/people/search')).toHaveLength(1)
})

test('slowAnswerForAnOldTermIsNotShown', async () => {
  mockSearch({ slowFor: 'ph' })
  const { user, palette } = await openPalette()
  const input = palette.getByRole('combobox')

  await user.type(input, 'ph')
  await new Promise((resolve) => setTimeout(resolve, 350))
  await user.type(input, 'ong')

  expect((await palette.findAllByRole('option', { name: /\(phong\)/ })).length).toBeGreaterThan(0)
  await new Promise((resolve) => setTimeout(resolve, 700))
  expect(palette.queryAllByRole('option', { name: /\(ph\)/ })).toHaveLength(0)
})

test('resultsAreGroupedByKind', async () => {
  mockSearch()
  const { user, palette } = await openPalette()

  await user.type(palette.getByRole('combobox'), 'phong')

  expect(await palette.findByRole('group', { name: 'Địa điểm' })).toBeInTheDocument()
  expect(palette.getByRole('group', { name: 'Đồ đạc' })).toBeInTheDocument()
  expect(palette.queryByRole('group', { name: 'Người' })).not.toBeInTheDocument()
})

test('aCodeMatchIsShownOnceUnderCodeMatches', async () => {
  mockSearch()
  const { user, palette } = await openPalette()

  await user.type(palette.getByRole('combobox'), 'p-0042')

  const codes = within(await palette.findByRole('group', { name: 'Khớp mã' }))
  expect(codes.getByRole('option', { name: /Phòng 301/ })).toBeInTheDocument()
  expect(palette.getAllByRole('option', { name: /Phòng 301/ })).toHaveLength(1)
})

test('twoPlacesWithTheSameNameShowDifferentTrails', async () => {
  mockSearch()
  const { user, palette } = await openPalette()

  await user.type(palette.getByRole('combobox'), 'phong')

  expect(await palette.findByText('Toà A')).toBeInTheDocument()
  expect(palette.getByText('Toà B')).toBeInTheDocument()
})

test('digitsAlsoSearchPhones', async () => {
  const seen = mockSearch()
  const { user, palette } = await openPalette()

  await user.type(palette.getByRole('combobox'), '0912')

  expect(await palette.findByRole('option', { name: /Nguyễn An/ })).toBeInTheDocument()
  expect(seen.some((call) => call.path === '/people/search' && call.query.includes('phone=0912'))).toBe(true)
})

test('lettersDoNotSearchPhones', async () => {
  const seen = mockSearch()
  const { user, palette } = await openPalette()

  await user.type(palette.getByRole('combobox'), 'phong')
  await palette.findAllByRole('option', { name: /Phòng 301/ })

  expect(seen.some((call) => call.query.includes('phone='))).toBe(false)
})

test('oneFailingSearchKeepsTheOthers', async () => {
  mockSearch({ fail: 'items' })
  const { user, palette } = await openPalette()

  await user.type(palette.getByRole('combobox'), 'phong')

  expect(await palette.findByRole('group', { name: 'Địa điểm' })).toBeInTheDocument()
  expect(await palette.findByText('Không tìm được Đồ đạc')).toBeInTheDocument()
})

test('enterOpensTheRecordAndClosesThePalette', async () => {
  mockSearch()
  // Trang của địa điểm được mở; test chỉ cần đường đi đúng, nên trang đó báo không tìm thấy là đủ.
  server.use(http.get('/api/registry/places/301', () => HttpResponse.json(problem(404, 'Not found'), { status: 404 })))
  const { user, palette, router } = await openPalette()

  await user.type(palette.getByRole('combobox'), 'p-0042')
  await palette.findByRole('option', { name: /Phòng 301/ })
  await user.keyboard('{Enter}')

  await waitFor(() => expect(router.state.location.pathname).toBe('/w/7/places/301'))
  expect(screen.queryByRole('dialog', { name: 'Tìm nhanh' })).not.toBeInTheDocument()
})

test('nothingFoundSaysSo', async () => {
  mockSearch()
  const { user, palette } = await openPalette()

  await user.type(palette.getByRole('combobox'), 'zzz')

  expect(await palette.findByText('Không tìm thấy gì.')).toBeInTheDocument()
})
