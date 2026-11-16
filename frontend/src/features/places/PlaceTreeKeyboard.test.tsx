import { act, screen, waitFor, within } from '@testing-library/react'
import { delay, http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { page, place } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

const buildingA = place({ id: '1', name: 'Toà A', hasChildren: true })
const buildingB = place({ id: '2', name: 'Toà B', hasChildren: true })
const floor3 = place({ id: '13', parentId: '1', name: 'Tầng 3', depth: 1, hasChildren: true })
const floor4 = place({ id: '14', parentId: '1', name: 'Tầng 4', depth: 1 })
const room = place({ id: '301', parentId: '13', name: 'P301', depth: 2 })

function setUp(options: { slowFloor3?: boolean } = {}) {
  mockShell({ places: [buildingA, buildingB] })
  server.use(
    http.get('/api/registry/places/1/children', () => HttpResponse.json(page([floor3, floor4]))),
    http.get('/api/registry/places/2/children', () => HttpResponse.json(page([]))),
    http.get('/api/registry/places/13/children', async () => {
      if (options.slowFloor3) await delay(200)
      return HttpResponse.json(page([room]))
    }),
  )
  return renderApp({ route: '/w/7', userId: '42' })
}

async function tree() {
  return within(await screen.findByRole('tree', { name: 'Địa điểm' }))
}

async function focusRow(name: string) {
  const row = (await tree()).getByRole('treeitem', { name })
  act(() => row.focus())
  return row
}

function focusedName() {
  return document.activeElement?.getAttribute('aria-label')
}

test('downAndUpMoveBetweenVisibleRows', async () => {
  const { user } = setUp()
  await (await tree()).findByRole('treeitem', { name: 'Toà B' })
  await focusRow('Toà A')

  await user.keyboard('{ArrowDown}')
  expect(focusedName()).toBe('Toà B')
  await user.keyboard('{ArrowDown}')
  expect(focusedName()).toBe('Toà B')
  await user.keyboard('{ArrowUp}')
  expect(focusedName()).toBe('Toà A')
})

test('rightOpensThenEntersFirstChild', async () => {
  const { user } = setUp()
  const row = await (await tree()).findByRole('treeitem', { name: 'Toà A' })
  await focusRow('Toà A')

  await user.keyboard('{ArrowRight}')
  await waitFor(() => expect(row).toHaveAttribute('aria-expanded', 'true'))
  await (await tree()).findByRole('treeitem', { name: 'Tầng 3' })
  expect(focusedName()).toBe('Toà A')

  await user.keyboard('{ArrowRight}')
  expect(focusedName()).toBe('Tầng 3')
  expect((await tree()).getByRole('treeitem', { name: 'Tầng 3' })).toHaveAttribute('aria-level', '2')
})

test('leftClosesThenGoesToParent', async () => {
  const { user } = setUp()
  await (await tree()).findByRole('treeitem', { name: 'Toà A' })
  await focusRow('Toà A')
  await user.keyboard('{ArrowRight}')
  await (await tree()).findByRole('treeitem', { name: 'Tầng 4' })
  await focusRow('Tầng 4')

  await user.keyboard('{ArrowLeft}')
  expect(focusedName()).toBe('Toà A')
  await user.keyboard('{ArrowLeft}')

  await waitFor(async () => expect((await tree()).queryByRole('treeitem', { name: 'Tầng 4' })).not.toBeInTheDocument())
  expect(focusedName()).toBe('Toà A')
})

test('enterOpensThePage', async () => {
  const { user, router } = setUp()
  server.use(http.get('/api/registry/places/2', () => HttpResponse.json({ ...buildingB, ancestors: [] })))
  await (await tree()).findByRole('treeitem', { name: 'Toà B' })
  await focusRow('Toà B')

  await user.keyboard('{Enter}')

  await waitFor(() => expect(router.state.location.pathname).toBe('/w/7/places/2'))
})

test('homeAndEndJump', async () => {
  const { user } = setUp()
  await (await tree()).findByRole('treeitem', { name: 'Toà B' })
  await focusRow('Toà A')
  await user.keyboard('{ArrowRight}')
  await (await tree()).findByRole('treeitem', { name: 'Tầng 4' })

  await user.keyboard('{End}')
  expect(focusedName()).toBe('Toà B')
  await user.keyboard('{Home}')
  expect(focusedName()).toBe('Toà A')
})

test('typeaheadJumpsByFirstLetter', async () => {
  const { user } = setUp()
  await (await tree()).findByRole('treeitem', { name: 'Toà B' })
  await focusRow('Toà A')
  await user.keyboard('{ArrowRight}')
  await (await tree()).findByRole('treeitem', { name: 'Tầng 3' })

  await user.keyboard('t')
  expect(focusedName()).toBe('Tầng 3')
  await user.keyboard('t')
  expect(focusedName()).toBe('Tầng 4')
})

test('onlyOneRowIsTabbable', async () => {
  const { user } = setUp()
  const view = await tree()
  await view.findByRole('treeitem', { name: 'Toà B' })
  await focusRow('Toà A')
  await user.keyboard('{ArrowRight}')
  await view.findByRole('treeitem', { name: 'Tầng 4' })
  await user.keyboard('{ArrowDown}')

  const tabbable = screen.getByRole('tree').querySelectorAll('[tabindex="0"]')
  expect([...tabbable].map((element) => element.getAttribute('aria-label'))).toEqual(['Tầng 3'])
  // Liên kết và nút mũi tên trong dòng không nằm trong thứ tự Tab.
  expect(view.getByRole('link', { name: 'Tầng 3' })).toHaveAttribute('tabindex', '-1')
})

test('arrowKeysSkipLoadingRows', async () => {
  const { user } = setUp({ slowFloor3: true })
  await (await tree()).findByRole('treeitem', { name: 'Toà A' })
  await focusRow('Toà A')
  await user.keyboard('{ArrowRight}')
  await (await tree()).findByRole('treeitem', { name: 'Tầng 3' })
  await focusRow('Tầng 3')

  // Tầng 3 đang tải con: dòng "Đang tải" không phải một treeitem, phím xuống bỏ qua nó.
  await user.keyboard('{ArrowRight}')
  await user.keyboard('{ArrowDown}')

  expect(focusedName()).toBe('Tầng 4')
})
