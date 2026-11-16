import { screen, waitFor, within } from '@testing-library/react'
import { expect, test } from 'vitest'

import { placeType } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { renderApp } from '@/test/render'
import { mockTrashBackend } from '@/test/trashBackend'

async function main() {
  return within(await screen.findByRole('main'))
}

test('listsDeletedPlacesWithTheirParent', async () => {
  mockShell({ placeTypes: [placeType()] })
  mockTrashBackend({ entries: [{ entityType: 'PLACE', id: '13', name: 'Tầng 3', code: 'DD-000013', context: 'Toà A' }] })
  renderApp({ route: '/w/7/trash', userId: '42' })

  const view = await main()
  expect(await view.findByRole('heading', { name: 'Thùng rác' })).toBeInTheDocument()
  expect(await view.findByText('Tầng 3')).toBeInTheDocument()
  expect(view.getByText('DD-000013')).toBeInTheDocument()
  expect(view.getByText('trong Toà A')).toBeInTheDocument()
})

test('tabsSwitchTheKindAndTheUrl', async () => {
  mockShell({ placeTypes: [placeType()] })
  mockTrashBackend({
    entries: [
      { entityType: 'PLACE', id: '13', name: 'Tầng 3' },
      { entityType: 'ITEM', id: '901', name: 'Tủ lạnh', context: 'P301' },
    ],
  })
  const { user, router } = renderApp({ route: '/w/7/trash', userId: '42' })
  const view = await main()
  await view.findByText('Tầng 3')

  await user.click(view.getByRole('tab', { name: 'Đồ đạc' }))

  await waitFor(() => expect(router.state.location.search).toBe('?type=ITEM'))
  expect(await view.findByText('Tủ lạnh')).toBeInTheDocument()
  expect(view.getByText('ở P301')).toBeInTheDocument()
  expect(view.queryByText('Tầng 3')).not.toBeInTheDocument()
})

test('restoringRemovesTheRowAndSaysSo', async () => {
  mockShell({ placeTypes: [placeType()] })
  const backend = mockTrashBackend({ entries: [{ entityType: 'PLACE', id: '13', name: 'Tầng 3', version: 4 }] })
  const { user } = renderApp({ route: '/w/7/trash', userId: '42' })
  const view = await main()

  await user.click(await view.findByRole('button', { name: 'Khôi phục Tầng 3' }))

  expect(await screen.findByText('Đã khôi phục Tầng 3')).toBeInTheDocument()
  expect(backend.writes).toEqual([{ method: 'POST', path: '/places/13/restore', body: { version: 4 } }])
  await waitFor(() => expect(view.queryByText('Tầng 3')).not.toBeInTheDocument())
})

test('blockedRestoreShowsTheReasonAndKeepsTheRow', async () => {
  mockShell({ placeTypes: [placeType()] })
  mockTrashBackend({
    entries: [{ entityType: 'PLACE', id: '13', name: 'Tầng 3' }],
    blocked: { '13': 'Restore the parent place 12 first' },
  })
  const { user } = renderApp({ route: '/w/7/trash', userId: '42' })
  const view = await main()

  await user.click(await view.findByRole('button', { name: 'Khôi phục Tầng 3' }))

  expect(await view.findByText('Restore the parent place 12 first')).toBeInTheDocument()
  expect(view.getByText('Tầng 3')).toBeInTheDocument()
})

test('staleRowIsRefreshedAfterAVersionConflict', async () => {
  mockShell({ placeTypes: [placeType()] })
  const backend = mockTrashBackend({ entries: [{ entityType: 'PLACE', id: '13', name: 'Tầng 3', version: 2 }] })
  const { user } = renderApp({ route: '/w/7/trash', userId: '42' })
  const view = await main()
  await view.findByText('Tầng 3')
  backend.entries[0].version = 3

  await user.click(view.getByRole('button', { name: 'Khôi phục Tầng 3' }))

  expect(await view.findByText('Đã được khôi phục hoặc thay đổi sau lần xoá này.')).toBeInTheDocument()
})

test('emptyTrashSaysSo', async () => {
  mockShell({ placeTypes: [placeType()] })
  mockTrashBackend({ entries: [] })
  renderApp({ route: '/w/7/trash', userId: '42' })

  expect(await (await main()).findByText('Không có gì đang bị xoá.')).toBeInTheDocument()
})

test('sidebarLinksToTheTrash', async () => {
  mockShell({ placeTypes: [placeType()] })
  mockTrashBackend({ entries: [] })
  renderApp({ route: '/w/7', userId: '42' })

  expect(await screen.findByRole('link', { name: 'Thùng rác' })).toHaveAttribute('href', '/w/7/trash')
})
