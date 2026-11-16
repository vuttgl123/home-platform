import { screen, waitFor } from '@testing-library/react'
import { expect, test, vi } from 'vitest'

import { ApiError } from '@/api'
import { Toaster } from '@/components/ui/toast'
import { renderWithProviders } from '@/test/render'

import { deletedToast } from './deletedToast'

test('showsUndoNextToTheMessage', async () => {
  renderWithProviders(<Toaster />)
  deletedToast({ name: 'P302', undo: async () => {} })

  expect(await screen.findByText('Đã xoá P302')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Hoàn tác' })).toBeInTheDocument()
})

test('undoRunsOnlyOnce', async () => {
  const { user } = renderWithProviders(<Toaster />)
  const undo = vi.fn(async () => {})
  deletedToast({ name: 'P302', undo })

  const button = await screen.findByRole('button', { name: 'Hoàn tác' })
  await user.click(button)
  await user.click(button).catch(() => {})

  await waitFor(() => expect(undo).toHaveBeenCalledTimes(1))
})

test('successSaysRestored', async () => {
  const { user } = renderWithProviders(<Toaster />)
  deletedToast({ name: 'P302', undo: async () => {} })

  await user.click(await screen.findByRole('button', { name: 'Hoàn tác' }))

  expect(await screen.findByText('Đã khôi phục P302')).toBeInTheDocument()
})

test('conflictSaysTheRecordChanged', async () => {
  const { user } = renderWithProviders(<Toaster />)
  deletedToast({
    name: 'P302',
    undo: async () => {
      throw new ApiError(409, 'Place 302 was changed by someone else', { code: 'VERSION_CONFLICT' })
    },
  })

  await user.click(await screen.findByRole('button', { name: 'Hoàn tác' }))

  expect(await screen.findByText('Không hoàn tác được: bản ghi đã thay đổi')).toBeInTheDocument()
})
