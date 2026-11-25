import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, expect, test } from 'vitest'

import { setCurrentUser } from '@/api'
import { problem } from '@/test/fixtures'
import { renderWithProviders } from '@/test/render'
import { server } from '@/test/server'

import { PhotosTab } from './components/PhotosTab'
import type { Photo } from './types'

const OWNER = { type: 'PLACE', id: '301' } as const

function photo(index: number, caption: string | null = null): Photo {
  return {
    id: `a${index}`,
    ownerType: 'PLACE',
    ownerId: '301',
    fileId: `f${index}`,
    caption,
    sortOrder: index,
    addedBy: '42',
    addedAt: '2026-11-25T03:00:00Z',
    file: { originalName: `${index}.jpg`, contentType: 'image/jpeg', sizeBytes: '100', width: 800, height: 600 },
  }
}

/** Server giả: danh sách ảnh trong bộ nhớ; `failures` là câu trả lời lỗi cho các lần POST đầu, theo thứ tự. */
function mockPhotos(initial: Photo[], failures: { status: number }[] = []) {
  const photos = [...initial]
  let posts = 0
  server.use(
    http.get('/api/media/attachments', () => HttpResponse.json(photos)),
    http.post('/api/media/attachments', () => {
      const failure = failures[posts]
      posts += 1
      if (failure) return HttpResponse.json(problem(failure.status, 'nope'), { status: failure.status })
      const created = photo(photos.length + 1)
      photos.push(created)
      return HttpResponse.json(created, { status: 201 })
    }),
  )
  return { posts: () => posts }
}

function jpeg(name: string) {
  return new File([new Uint8Array([1, 2, 3])], name, { type: 'image/jpeg' })
}

beforeEach(() => setCurrentUser('42'))

test('showsThumbnailsInOrder', async () => {
  mockPhotos([photo(1, 'Cửa sổ'), photo(2)])
  renderWithProviders(<PhotosTab ws="7" owner={OWNER} />)

  const grid = within(await screen.findByRole('list', { name: 'Ảnh' }))
  const images = grid.getAllByRole('img')
  expect(images.map((image) => image.getAttribute('src'))).toEqual([
    '/api/media/workspaces/7/files/f1/thumbnail',
    '/api/media/workspaces/7/files/f2/thumbnail',
  ])
  expect(images[0]).toHaveAttribute('alt', 'Cửa sổ')
  expect(images[1]).toHaveAttribute('alt', 'Ảnh 2')
  expect(grid.getByText('Cửa sổ')).toBeInTheDocument()
})

test('emptyStateOffersUpload', async () => {
  mockPhotos([])
  renderWithProviders(<PhotosTab ws="7" owner={OWNER} />)

  expect(await screen.findByText('Chưa có ảnh nào.')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Tải ảnh lên' })).toBeInTheDocument()
})

test('uploadsSeveralFilesOneByOne', async () => {
  const backend = mockPhotos([])
  renderWithProviders(<PhotosTab ws="7" owner={OWNER} />)
  const user = userEvent.setup()
  await screen.findByText('Chưa có ảnh nào.')

  await user.upload(screen.getByLabelText('Chọn ảnh để tải lên'), [jpeg('a.jpg'), jpeg('b.jpg'), jpeg('c.jpg')])

  const queue = within(await screen.findByRole('list', { name: 'Ảnh đang tải lên' }))
  await waitFor(() => expect(queue.getAllByText('Xong')).toHaveLength(3))
  expect(backend.posts()).toBe(3)
  expect(within(await screen.findByRole('list', { name: 'Ảnh' })).getAllByRole('img')).toHaveLength(3)
})

test('badFileIsReportedAndOthersGoOn', async () => {
  const backend = mockPhotos([], [{ status: 400 }])
  renderWithProviders(<PhotosTab ws="7" owner={OWNER} />)
  const user = userEvent.setup({ applyAccept: false })
  await screen.findByText('Chưa có ảnh nào.')

  const gif = new File([new Uint8Array([1])], 'dong.gif', { type: 'image/gif' })
  await user.upload(screen.getByLabelText('Chọn ảnh để tải lên'), [gif, jpeg('hong.jpg'), jpeg('tot.jpg')])

  const queue = within(await screen.findByRole('list', { name: 'Ảnh đang tải lên' }))
  await waitFor(() => expect(queue.getByText('Xong')).toBeInTheDocument())
  expect(queue.getByText('dong.gif').closest('li')).toHaveTextContent('Chỉ nhận ảnh JPEG, PNG, WebP')
  expect(queue.getByText('hong.jpg').closest('li')).toHaveTextContent('Không đọc được ảnh này')
  expect(queue.getByText('tot.jpg').closest('li')).toHaveTextContent('Xong')
  expect(backend.posts()).toBe(2)
})

test('fullOwnerShowsTheLimit', async () => {
  mockPhotos([photo(1)], [{ status: 409 }])
  renderWithProviders(<PhotosTab ws="7" owner={OWNER} />)
  const user = userEvent.setup()
  await screen.findByRole('list', { name: 'Ảnh' })

  await user.upload(screen.getByLabelText('Chọn ảnh để tải lên'), jpeg('thua.jpg'))

  const queue = within(await screen.findByRole('list', { name: 'Ảnh đang tải lên' }))
  await waitFor(() => expect(queue.getByText('thua.jpg').closest('li')).toHaveTextContent('Đã đủ 100 ảnh'))
})
