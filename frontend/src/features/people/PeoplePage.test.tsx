import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { mockDownloads } from '@/test/downloads'
import { fieldDef, page, person, place } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { mockPeopleBackend } from '@/test/peopleBackend'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

async function main() {
  return within(await screen.findByRole('main'))
}

test('listsPeopleWithLoadMore', async () => {
  mockShell()
  server.use(
    http.get('/api/registry/people', ({ request }) =>
      new URL(request.url).searchParams.get('cursor') === 'c1'
        ? HttpResponse.json(page([person({ id: '3', fullName: 'Cao Châu' })]))
        : HttpResponse.json(
            page([person({ id: '1', fullName: 'An', phone: '0909', idType: 'CCCD', idNumber: '001' }), person({ id: '2', fullName: 'Bình' })], 'c1'),
          ),
    ),
  )
  const { user } = renderApp({ route: '/w/7/people', userId: '42' })
  const view = await main()

  expect(await view.findByRole('link', { name: /An/ })).toHaveAttribute('href', '/w/7/people/1')
  expect(view.getByRole('link', { name: /An/ })).toHaveTextContent('0909')
  expect(view.getByRole('link', { name: /An/ })).toHaveTextContent('CCCD 001')
  await user.click(view.getByRole('button', { name: 'Tải thêm' }))

  expect(await view.findByRole('link', { name: /Cao Châu/ })).toBeInTheDocument()
  const section = screen.getByRole('link', { name: 'Người' })
  expect(section).toHaveAttribute('aria-current', 'page')
  // Kiểu "đang chọn" của thanh bên dựa vào data-active, không vào aria-current.
  expect(section).toHaveAttribute('data-active')
})

/** Trang Người với hai toà, một mục tuỳ biến của người, và mọi request `GET /people` ghi lại. */
function setUpFilters() {
  mockShell({ places: [place({ id: '1', name: 'Toà A' }), place({ id: '2', name: 'Toà B' })] })
  mockPeopleBackend({ people: [{ id: '1', fullName: 'An', phone: '0909' }, { id: '2', fullName: 'Bình', phone: '0808' }] })
  const requests: URL[] = []
  server.use(
    http.get('/api/registry/field-defs', ({ request }) =>
      HttpResponse.json(
        new URL(request.url).searchParams.get('target') === 'PERSON'
          ? [fieldDef({ id: '9', target: 'PERSON', typeId: '0', fieldKey: 'que_quan', label: 'Quê quán', dataType: 'TEXT' })]
          : [],
      ),
    ),
  )
  server.events.on('request:start', ({ request }) => {
    const url = new URL(request.url)
    if (url.pathname === '/api/registry/people') requests.push(url)
  })
  return { requests, last: () => requests.at(-1)?.searchParams }
}

test('searchBoxSendsQ', async () => {
  const { last } = setUpFilters()
  const { user, router } = renderApp({ route: '/w/7/people', userId: '42' })
  const view = await main()
  await view.findByRole('link', { name: /Bình/ })

  await user.type(view.getByRole('searchbox', { name: 'Tìm người' }), '0909')

  // Kết quả lọc thay danh sách sau 300 ms: chờ tới khi chỉ còn An.
  await waitFor(() => {
    expect(view.queryByRole('link', { name: /Bình/ })).not.toBeInTheDocument()
    expect(view.getByRole('link', { name: /An/ })).toBeInTheDocument()
  })
  expect(last()?.get('q')).toBe('0909')
  expect(router.state.location.search).toBe('?q=0909')
  expect(view.queryByRole('combobox', { name: 'Tìm theo' })).not.toBeInTheDocument()
})

test('livingFilterSendsLiving', async () => {
  const { last } = setUpFilters()
  const { user, router } = renderApp({ route: '/w/7/people', userId: '42' })
  const view = await main()
  await view.findByRole('link', { name: /Bình/ })

  await user.selectOptions(view.getByRole('combobox', { name: 'Cư trú' }), 'Đang ở')
  await waitFor(() => expect(last()?.get('living')).toBe('true'))
  expect(router.state.location.search).toBe('?living=yes')

  await user.selectOptions(view.getByRole('combobox', { name: 'Cư trú' }), 'Không ở đâu')
  await waitFor(() => expect(last()?.get('living')).toBe('false'))
})

test('choosingABuildingMeansLivingThere', async () => {
  const { last } = setUpFilters()
  const { user, router } = renderApp({ route: '/w/7/people', userId: '42' })
  const view = await main()
  await view.findByRole('link', { name: /Bình/ })

  await user.selectOptions(await view.findByRole('combobox', { name: 'Toà' }), 'Toà B')

  await waitFor(() => expect(last()?.get('rootId')).toBe('2'))
  expect(last()?.get('living')).toBe('true')
  expect(view.getByRole('combobox', { name: 'Cư trú' })).toHaveValue('yes')
  expect(new URLSearchParams(router.state.location.search).get('root')).toBe('2')
})

test('notLivingAnywhereDisablesTheBuilding', async () => {
  const { last } = setUpFilters()
  const { user } = renderApp({ route: '/w/7/people?living=yes&root=2', userId: '42' })
  const view = await main()
  await view.findByRole('link', { name: /Bình/ })

  await user.selectOptions(view.getByRole('combobox', { name: 'Cư trú' }), 'Không ở đâu')

  await waitFor(() => expect(last()?.get('living')).toBe('false'))
  expect(last()?.has('rootId')).toBe(false)
  expect(view.getByRole('combobox', { name: 'Toà' })).toBeDisabled()
  expect(view.getByRole('combobox', { name: 'Toà' })).toHaveValue('')
})

test('customFieldConditionSendsAttr', async () => {
  const { last } = setUpFilters()
  const { user } = renderApp({ route: '/w/7/people', userId: '42' })
  const view = await main()
  await view.findByRole('link', { name: /Bình/ })

  await user.click(await view.findByRole('button', { name: 'Thêm điều kiện' }))
  await user.type(view.getByLabelText('Giá trị của điều kiện 1'), 'hue')

  await waitFor(() => expect(last()?.getAll('attr')).toEqual(['que_quan:has:hue']))
})

test('urlFiltersAreReadOnLoad', async () => {
  const { requests } = setUpFilters()
  renderApp({ route: '/w/7/people?q=an&living=yes&root=2&f=que_quan~has~hue', userId: '42' })
  const view = await main()
  await view.findByRole('link', { name: /An/ })

  const first = requests[0].searchParams
  expect(Object.fromEntries(first)).toMatchObject({ q: 'an', living: 'true', rootId: '2' })
  expect(first.getAll('attr')).toEqual(['que_quan:has:hue'])
  expect(view.getByRole('searchbox', { name: 'Tìm người' })).toHaveValue('an')
})

test('filteredEmptyResultSaysNobodyFound', async () => {
  setUpFilters()
  renderApp({ route: '/w/7/people?q=zz', userId: '42' })

  expect(await (await main()).findByText('Không tìm thấy ai.')).toBeInTheDocument()
})

test('emptyListInvitesToAdd', async () => {
  mockShell()
  mockPeopleBackend({})

  renderApp({ route: '/w/7/people', userId: '42' })

  const view = await main()
  expect(await view.findByText('Chưa có ai')).toBeInTheDocument()
  expect(view.getAllByRole('button', { name: 'Thêm người' }).length).toBeGreaterThan(0)
})

test('peoplePageHasAnExportButton', async () => {
  mockShell()
  const saved = mockDownloads()
  let asked = false
  server.use(
    http.get('/api/registry/people', () => HttpResponse.json(page([person({ id: '1', fullName: 'An' })]))),
    http.get('/api/registry/exports/people.xlsx', () => {
      asked = true
      return new HttpResponse('file')
    }),
  )
  const { user } = renderApp({ route: '/w/7/people', userId: '42' })
  const view = await main()
  await view.findByRole('link', { name: /An/ })

  await user.click(view.getByRole('button', { name: 'Xuất Excel' }))

  await waitFor(() => expect(saved).toHaveLength(1))
  expect(asked).toBe(true)
  expect(saved[0].name).toMatch(/^nha-an-nguoi-\d{4}-\d{2}-\d{2}\.xlsx$/)
})
