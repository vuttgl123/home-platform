import { screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { setCurrentUser } from '@/api'
import { Toaster } from '@/components/ui/toast'
import { mockDownloads } from '@/test/downloads'
import { mockShell } from '@/test/handlers'
import { renderWithProviders } from '@/test/render'
import { server } from '@/test/server'

import { ExportButton } from './components/ExportButton'

test('disabledWhileDownloading', async () => {
  const saved = mockDownloads()
  mockShell()
  let release!: () => void
  const gate = new Promise<void>((resolve) => {
    release = resolve
  })
  server.use(
    http.get('/api/registry/exports/people.xlsx', async () => {
      await gate
      return new HttpResponse('file')
    }),
  )
  setCurrentUser('42')
  const { user } = renderWithProviders(<ExportButton ws="7" kind="people" label="Xuất Excel" />)
  const button = screen.getByRole('button', { name: 'Xuất Excel' })

  await user.click(button)

  await waitFor(() => expect(button).toBeDisabled())
  expect(button).toHaveAttribute('aria-busy', 'true')
  expect(button.querySelector('[data-slot="spinner"]')).not.toBeNull()
  // Vòng xoay chỉ để nhìn: tên của nút không đổi trong lúc tải.
  expect(button).toHaveAccessibleName('Xuất Excel')

  release()

  await waitFor(() => expect(button).toBeEnabled())
  expect(button).toHaveAttribute('aria-busy', 'false')
  expect(button.querySelector('[data-slot="spinner"]')).toBeNull()
  expect(saved).toHaveLength(1)
  expect(saved[0].name).toMatch(/-nguoi-\d{4}-\d{2}-\d{2}\.xlsx$/)
})

test('showsTheErrorDetail', async () => {
  const saved = mockDownloads()
  mockShell()
  server.use(
    http.get('/api/registry/exports/items.xlsx', () =>
      HttpResponse.json({ status: 404, title: 'Not Found', detail: 'Item category 9 not found' }, { status: 404 }),
    ),
  )
  setCurrentUser('42')
  const { user } = renderWithProviders(
    <>
      <Toaster />
      <ExportButton ws="7" kind="items" query={{ categoryId: '9' }} label="Xuất Excel" />
    </>,
  )

  await user.click(screen.getByRole('button', { name: 'Xuất Excel' }))

  expect(await screen.findByText('Item category 9 not found')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Xuất Excel' })).toBeEnabled()
  expect(saved).toHaveLength(0)
})

test('sendsTheQueryToThePathOfItsKind', async () => {
  mockDownloads()
  mockShell()
  const sent: string[] = []
  server.use(
    http.get('/api/registry/exports/:file', ({ request }) => {
      const url = new URL(request.url)
      sent.push(url.pathname + url.search)
      return new HttpResponse('file')
    }),
  )
  setCurrentUser('42')
  const { user } = renderWithProviders(
    <>
      <ExportButton ws="7" kind="workspace" label="Tải Excel" />
      <ExportButton ws="7" kind="items" query={{ status: 'BROKEN', categoryId: undefined }} label="Xuất đồ" />
    </>,
  )

  await user.click(screen.getByRole('button', { name: 'Tải Excel' }))
  await user.click(screen.getByRole('button', { name: 'Xuất đồ' }))

  await waitFor(() =>
    expect(sent).toEqual(['/api/registry/exports/workspace.xlsx', '/api/registry/exports/items.xlsx?status=BROKEN']),
  )
})
