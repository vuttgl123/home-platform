import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { page, person } from '@/test/fixtures'
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
  expect(screen.getByRole('link', { name: 'Người' })).toHaveAttribute('aria-current', 'page')
})

test('searchesByPhone', async () => {
  mockShell()
  mockPeopleBackend({ people: [{ id: '1', fullName: 'An', phone: '0909' }, { id: '2', fullName: 'Bình', phone: '0808' }] })
  const { user } = renderApp({ route: '/w/7/people', userId: '42' })
  const view = await main()
  await view.findByRole('link', { name: /Bình/ })

  await user.selectOptions(view.getByRole('combobox', { name: 'Tìm theo' }), 'Số điện thoại')
  await user.type(view.getByRole('searchbox', { name: 'Tìm người' }), '0909')

  // Kết quả tìm thay danh sách đầy đủ sau 300 ms: chờ tới khi chỉ còn An.
  await waitFor(() => {
    expect(view.queryByRole('link', { name: /Bình/ })).not.toBeInTheDocument()
    expect(view.getByRole('link', { name: /An/ })).toBeInTheDocument()
  })
})

test('emptyListInvitesToAdd', async () => {
  mockShell()
  mockPeopleBackend({})

  renderApp({ route: '/w/7/people', userId: '42' })

  const view = await main()
  expect(await view.findByText('Chưa có ai')).toBeInTheDocument()
  expect(view.getAllByRole('button', { name: 'Thêm người' }).length).toBeGreaterThan(0)
})
