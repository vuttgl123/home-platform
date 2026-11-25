import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { itemCategory, placeDetail, placeType } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { mockItemBackend } from '@/test/itemBackend'
import { mockPeopleBackend } from '@/test/peopleBackend'
import { mockPlaceBackend } from '@/test/placeBackend'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

/** Một ảnh của bản ghi `ownerId`, file `fileId`. */
function photo(ownerType: string, ownerId: string, fileId: string) {
  return {
    id: `a-${fileId}`,
    ownerType,
    ownerId,
    fileId,
    caption: null,
    sortOrder: 0,
    addedBy: '42',
    addedAt: '2026-11-25T03:00:00Z',
    file: { originalName: 'a.jpg', contentType: 'image/jpeg', sizeBytes: '100', width: 800, height: 600 },
  }
}

function mockMedia(photos: ReturnType<typeof photo>[]) {
  server.use(
    http.get('/api/media/attachments', ({ request }) => {
      const query = new URL(request.url).searchParams
      return HttpResponse.json(photos.filter((p) => p.ownerType === query.get('ownerType') && p.ownerId === query.get('ownerId')))
    }),
  )
}

async function main() {
  return within(await screen.findByRole('main'))
}

test('placeHasAPhotosTabThatSetsTheCover', async () => {
  mockShell({ placeTypes: [placeType({ id: '102', name: 'Phòng' })] })
  const backend = mockPlaceBackend([
    { id: '301', name: 'P301', placeTypeId: '102', version: 4, capacity: 2, description: 'Góc', attributes: { area: 20 } },
  ])
  mockMedia([photo('PLACE', '301', 'f1')])
  const { user } = renderApp({ route: '/w/7/places/301?tab=photos', userId: '42' })

  const content = await main()
  expect(await content.findByRole('tab', { name: 'Ảnh' })).toHaveAttribute('aria-selected', 'true')
  await user.click(await content.findByRole('button', { name: 'Thao tác với Ảnh 1' }))
  await user.click(await screen.findByRole('menuitem', { name: 'Đặt làm ảnh bìa' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0].body).toEqual({
    version: 4,
    placeTypeId: '102',
    name: 'P301',
    status: 'ACTIVE',
    capacity: 2,
    description: 'Góc',
    attributes: { area: 20 },
    coverFileId: 'f1',
  })
  // Đầu trang hiện ảnh bìa vừa đặt.
  expect(await content.findByRole('img', { name: 'Ảnh bìa của P301' })).toHaveAttribute(
    'src',
    '/api/media/workspaces/7/files/f1/thumbnail',
  )
})

test('personAvatarShowsThePhoto', async () => {
  mockShell({ placeTypes: [placeType()] })
  mockPlaceBackend([])
  mockPeopleBackend({ people: [{ id: '55', fullName: 'Nguyễn An', avatarFileId: 'f9' }, { id: '56', fullName: 'Trần Bình' }] })
  mockMedia([])

  renderApp({ route: '/w/7/people/55', userId: '42' })

  const content = await main()
  await content.findByRole('heading', { name: 'Nguyễn An' })
  // Avatar ẩn với công cụ đọc màn hình (tên đã nằm ngay cạnh), nên tìm thẳng thẻ img.
  expect(document.querySelector('main img[src="/api/media/workspaces/7/files/f9/thumbnail"]')).not.toBeNull()
  expect(content.getByRole('tab', { name: 'Ảnh' })).toBeInTheDocument()
})

test('peopleListShowsAvatars', async () => {
  mockShell({ placeTypes: [placeType()] })
  mockPeopleBackend({ people: [{ id: '55', fullName: 'Nguyễn An', avatarFileId: 'f9' }, { id: '56', fullName: 'Trần Bình' }] })

  renderApp({ route: '/w/7/people', userId: '42' })

  await (await main()).findByRole('link', { name: /Trần Bình/ })
  const images = document.querySelectorAll('main img')
  expect(Array.from(images).map((image) => image.getAttribute('src'))).toEqual(['/api/media/workspaces/7/files/f9/thumbnail'])
})

test('itemPageShowsItsCover', async () => {
  mockShell({ placeTypes: [placeType()] })
  server.use(
    http.get('/api/registry/places/:id', ({ params }) => HttpResponse.json(placeDetail({ id: params.id as string, name: `P${params.id as string}` }))),
    http.get('/api/registry/item-categories', () => HttpResponse.json([itemCategory()])),
  )
  mockItemBackend({ items: [{ id: '900', name: 'Tủ lạnh', coverFileId: 'f5' }] })
  mockMedia([photo('ITEM', '900', 'f5')])
  const { user } = renderApp({ route: '/w/7/items/900', userId: '42' })

  const content = await main()
  expect(await content.findByRole('img', { name: 'Ảnh bìa của Tủ lạnh' })).toHaveAttribute(
    'src',
    '/api/media/workspaces/7/files/f5/thumbnail',
  )
  await user.click(content.getByRole('tab', { name: 'Ảnh' }))
  const tile = (await content.findByRole('button', { name: 'Xem Ảnh 1' })).closest('li') as HTMLElement
  expect(within(tile).getByText('Ảnh bìa')).toBeInTheDocument()
})
