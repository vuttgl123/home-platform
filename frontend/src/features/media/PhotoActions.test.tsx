import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { useState } from 'react'
import { beforeEach, expect, test, vi } from 'vitest'

import { setCurrentUser } from '@/api'
import { TooltipProvider } from '@/components/ui/tooltip'
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
    sortOrder: index - 1,
    addedBy: '42',
    addedAt: '2026-11-25T03:00:00Z',
    file: { originalName: `${index}.jpg`, contentType: 'image/jpeg', sizeBytes: '100', width: 800, height: 600 },
  }
}

/** Ba ảnh; ghi lại mọi lệnh ghi. `orderStatus` là câu trả lời của lệnh sắp xếp. */
function mockPhotos(orderStatus = 200) {
  const writes: { method: string; path: string; body: unknown }[] = []
  let lists = 0
  const photos = [photo(1, 'Cửa sổ'), photo(2), photo(3)]
  server.use(
    http.get('/api/media/attachments', () => {
      lists += 1
      return HttpResponse.json(photos)
    }),
    http.put('/api/media/attachments/order', async ({ request }) => {
      writes.push({ method: 'PUT', path: '/attachments/order', body: await request.json() })
      return orderStatus === 200 ? HttpResponse.json(photos) : HttpResponse.json(problem(orderStatus, 'stale'), { status: orderStatus })
    }),
    http.put('/api/media/attachments/:id', async ({ request, params }) => {
      writes.push({ method: 'PUT', path: `/attachments/${params.id}`, body: await request.json() })
      return HttpResponse.json(photos[0])
    }),
    http.delete('/api/media/attachments/:id', ({ params }) => {
      writes.push({ method: 'DELETE', path: `/attachments/${params.id}`, body: null })
      return new HttpResponse(null, { status: 204 })
    }),
  )
  return { writes, lists: () => lists }
}

/** Trang giả giữ ảnh bìa trong state, như trang địa điểm giữ `coverFileId`. */
function WithCover({ initial, onSet }: { initial: string | null; onSet: (fileId: string | null) => void }) {
  const [cover, setCover] = useState(initial)
  return (
    <PhotosTab
      ws="7"
      owner={OWNER}
      highlight={{
        fileId: cover,
        label: 'Ảnh bìa',
        setLabel: 'Đặt làm ảnh bìa',
        onSet: async (fileId) => {
          onSet(fileId)
          setCover(fileId)
        },
      }}
    />
  )
}

function draw(ui = <PhotosTab ws="7" owner={OWNER} />) {
  return renderWithProviders(<TooltipProvider>{ui}</TooltipProvider>)
}

async function openMenu(user: ReturnType<typeof draw>['user'], alt: string) {
  await user.click(await screen.findByRole('button', { name: `Thao tác với ${alt}` }))
}

beforeEach(() => setCurrentUser('42'))

test('clickingOpensTheViewerAtThatPhoto', async () => {
  mockPhotos()
  const { user } = draw()

  await user.click(await screen.findByRole('button', { name: 'Xem Ảnh 2' }))
  const viewer = within(await screen.findByRole('dialog', { name: 'Ảnh 2 / 3' }))
  expect(viewer.getByRole('img')).toHaveAttribute('src', '/api/media/workspaces/7/files/f2')

  await user.keyboard('{ArrowRight}')
  expect(await screen.findByRole('dialog', { name: 'Ảnh 3 / 3' })).toBeInTheDocument()
  await user.keyboard('{ArrowLeft}{ArrowLeft}')
  const first = within(await screen.findByRole('dialog', { name: 'Ảnh 1 / 3' }))
  expect(first.getByRole('img')).toHaveAttribute('src', '/api/media/workspaces/7/files/f1')
  expect(first.getByText('Cửa sổ')).toBeInTheDocument()
  expect(first.getByRole('button', { name: 'Ảnh trước' })).toBeDisabled()
})

test('editsTheCaption', async () => {
  const backend = mockPhotos()
  const { user } = draw()

  await openMenu(user, 'Ảnh 2')
  await user.click(await screen.findByRole('menuitem', { name: 'Sửa chú thích' }))
  const form = within(await screen.findByRole('dialog', { name: 'Sửa chú thích' }))
  await user.type(form.getByRole('textbox', { name: 'Chú thích' }), 'Góc bếp')
  await user.click(form.getByRole('button', { name: 'Lưu' }))

  await waitFor(() => expect(backend.writes).toEqual([{ method: 'PUT', path: '/attachments/a2', body: { caption: 'Góc bếp' } }]))
})

test('movesAPhotoEarlierAndLater', async () => {
  const backend = mockPhotos()
  const { user } = draw()

  await openMenu(user, 'Cửa sổ')
  expect(screen.queryByRole('menuitem', { name: 'Đưa lên trước' })).toBeNull()
  await user.keyboard('{Escape}')
  await openMenu(user, 'Ảnh 3')
  expect(screen.queryByRole('menuitem', { name: 'Đưa ra sau' })).toBeNull()
  await user.keyboard('{Escape}')

  await openMenu(user, 'Ảnh 2')
  await user.click(await screen.findByRole('menuitem', { name: 'Đưa lên trước' }))
  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toEqual({ ownerType: 'PLACE', ownerId: '301', attachmentIds: ['a2', 'a1', 'a3'] })

  await openMenu(user, 'Ảnh 2')
  await user.click(await screen.findByRole('menuitem', { name: 'Đưa ra sau' }))
  await waitFor(() => expect(backend.writes).toHaveLength(2))
  expect(backend.writes[1].body).toEqual({ ownerType: 'PLACE', ownerId: '301', attachmentIds: ['a1', 'a3', 'a2'] })
})

test('removesAfterConfirm', async () => {
  const backend = mockPhotos()
  const { user } = draw()

  await openMenu(user, 'Ảnh 3')
  await user.click(await screen.findByRole('menuitem', { name: 'Gỡ ảnh' }))
  const confirm = within(await screen.findByRole('alertdialog'))
  expect(backend.writes).toHaveLength(0)
  await user.click(confirm.getByRole('button', { name: 'Gỡ ảnh' }))

  await waitFor(() => expect(backend.writes).toEqual([{ method: 'DELETE', path: '/attachments/a3', body: null }]))
})

test('setsTheCover', async () => {
  mockPhotos()
  const onSet = vi.fn()
  const { user } = draw(<WithCover initial={null} onSet={onSet} />)

  await openMenu(user, 'Ảnh 2')
  await user.click(await screen.findByRole('menuitem', { name: 'Đặt làm ảnh bìa' }))

  await waitFor(() => expect(onSet).toHaveBeenCalledWith('f2'))
  const tile = (await screen.findByRole('button', { name: 'Xem Ảnh 2' })).closest('li') as HTMLElement
  expect(within(tile).getByText('Ảnh bìa')).toBeInTheDocument()
  await openMenu(user, 'Ảnh 2')
  expect(screen.queryByRole('menuitem', { name: 'Đặt làm ảnh bìa' })).toBeNull()
})

test('removingTheCoverClearsIt', async () => {
  const backend = mockPhotos()
  const onSet = vi.fn()
  const { user } = draw(<WithCover initial="f1" onSet={onSet} />)

  await openMenu(user, 'Cửa sổ')
  await user.click(await screen.findByRole('menuitem', { name: 'Gỡ ảnh' }))
  await user.click(within(await screen.findByRole('alertdialog')).getByRole('button', { name: 'Gỡ ảnh' }))

  await waitFor(() => expect(onSet).toHaveBeenCalledWith(null))
  expect(backend.writes).toEqual([{ method: 'DELETE', path: '/attachments/a1', body: null }])
})

test('staleOrderReloads', async () => {
  const backend = mockPhotos(409)
  const { user } = draw()
  await screen.findByRole('button', { name: 'Xem Ảnh 2' })
  const before = backend.lists()

  await openMenu(user, 'Ảnh 2')
  await user.click(await screen.findByRole('menuitem', { name: 'Đưa lên trước' }))

  expect(await screen.findByText('Danh sách ảnh vừa đổi, đã tải lại')).toBeInTheDocument()
  await waitFor(() => expect(backend.lists()).toBeGreaterThan(before))
})
