import { screen, waitFor, within } from '@testing-library/react'
import { expect, test } from 'vitest'

import { placeType } from '@/test/fixtures'
import { mockShell, recordRequests } from '@/test/handlers'
import { mockPlaceBackend } from '@/test/placeBackend'
import { renderApp } from '@/test/render'

const types = [placeType({ id: '100', code: 'BUILDING', name: 'Toà nhà' }), placeType({ id: '101', code: 'ROOM', name: 'Phòng' })]

function setUp() {
  mockShell({ placeTypes: types })
  return mockPlaceBackend([
    { id: '1', name: 'Toà A', placeTypeId: '100' },
    { id: '13', parentId: '1', name: 'Tầng 1', placeTypeId: '100', code: 'DD-000013' },
    { id: '101', parentId: '13', name: 'P101', placeTypeId: '101', code: 'P-0101' },
    { id: '102', parentId: '13', name: 'P102', placeTypeId: '101', status: 'INACTIVE' },
    { id: '9', name: 'Kho' },
  ])
}

async function main() {
  return within(await screen.findByRole('main'))
}

async function openChildren(id: string) {
  const app = renderApp({ route: `/w/7/places/${id}?tab=children`, userId: '42' })
  return { ...app, view: await main() }
}

function table(view: Awaited<ReturnType<typeof main>>) {
  return within(view.getByRole('table', { name: 'Cả bên trong' }))
}

test('switchIsHiddenForAPlaceWithoutChildren', async () => {
  setUp()
  const { view } = await openChildren('9')

  await view.findByText('Chưa có chỗ nào bên trong')
  expect(view.queryByRole('switch', { name: 'Cả bên trong' })).not.toBeInTheDocument()
})

test('listsEveryDescendantParentsBeforeChildren', async () => {
  setUp()
  const { user, view } = await openChildren('1')

  await user.click(await view.findByRole('switch', { name: 'Cả bên trong' }))

  await waitFor(() => expect(table(view).getAllByRole('link').map((link) => link.textContent)).toEqual(['Tầng 1', 'P101', 'P102']))
})

test('rowsAreIndentedByDepth', async () => {
  setUp()
  const { user, view } = await openChildren('1')
  await user.click(await view.findByRole('switch', { name: 'Cả bên trong' }))

  const rows = await table(view).findAllByRole('row')
  const cellOf = (name: string) => rows.find((row) => row.textContent?.includes(name))!.querySelector('td')!
  expect(cellOf('Tầng 1').style.paddingLeft).toBe('0.75rem')
  expect(cellOf('P101').style.paddingLeft).toBe('1.75rem')
})

test('rowsShowTypeCodeAndStatus', async () => {
  setUp()
  const { user, view } = await openChildren('1')
  await user.click(await view.findByRole('switch', { name: 'Cả bên trong' }))

  const row = within((await table(view).findAllByRole('row')).find((candidate) => candidate.textContent?.includes('P102'))!)
  expect(row.getByText('Phòng')).toBeInTheDocument()
  expect(row.getByText('Tạm ngưng')).toBeInTheDocument()
  expect(table(view).getByText('P-0101')).toBeInTheDocument()
})

test('subtreeLoadsOnlyWhenSwitchedOn', async () => {
  setUp()
  const requests = recordRequests()
  const { user, view } = await openChildren('1')
  await view.findByRole('link', { name: /Tầng 1/ })

  expect(requests.some((request) => request.endsWith('/subtree'))).toBe(false)
  await user.click(view.getByRole('switch', { name: 'Cả bên trong' }))
  await waitFor(() => expect(requests).toContain('GET /api/registry/places/1/subtree'))
})
