import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, test } from 'vitest'

import { todayInBusinessZone } from '@/lib/dates'
import { placeType } from '@/test/fixtures'
import { mockShell } from '@/test/handlers'
import { mockPeopleBackend } from '@/test/peopleBackend'
import { mockPlaceBackend } from '@/test/placeBackend'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

const TODAY = todayInBusinessZone()
const WARNING = 'P301 chứa 2 người; ngày 01/12/2026 đã có 2 người. Vẫn lưu được.'

/** P301 với các dòng cư trú cho sẵn; `check` là câu trả lời của API kiểm tra sức chứa. */
function setUp(check: object, occupancies: Parameters<typeof mockPeopleBackend>[0]['occupancies'] = []) {
  mockShell({ placeTypes: [placeType()] })
  mockPlaceBackend([{ id: '301', name: 'P301' }])
  const backend = mockPeopleBackend({
    today: TODAY,
    people: [{ id: '55', fullName: 'Nguyễn An' }],
    occupancies,
    placeNames: { '301': 'P301' },
  })
  const checks: URL[] = []
  server.use(
    http.get('/api/registry/places/:id/capacity-check', ({ request }) => {
      checks.push(new URL(request.url))
      return HttpResponse.json(check)
    }),
  )
  return { backend, checks }
}

async function openAdd() {
  const app = renderApp({ route: '/w/7/places/301?tab=people', userId: '42' })
  const main = within(await screen.findByRole('main'))
  await app.user.click(await main.findByRole('button', { name: 'Thêm người ở' }))
  return { ...app, form: within(await screen.findByRole('dialog', { name: 'Thêm người ở' })) }
}

test('warnsWhenThePeakIsFull', async () => {
  const { checks } = setUp({ capacity: 2, peak: 2, peakDate: '2026-12-01' })
  const { form } = await openAdd()

  const warning = await form.findByText(WARNING)
  expect(warning.closest('[role="alert"]')).not.toBeNull()
  expect(form.getByRole('button', { name: 'Thêm' })).toBeEnabled()
  expect(checks.at(-1)?.pathname).toBe('/api/registry/places/301/capacity-check')
  expect(checks.at(-1)?.searchParams.get('startDate')).toBe(TODAY)
})

test('staysQuietBelowCapacity', async () => {
  const { checks } = setUp({ capacity: 2, peak: 1, peakDate: TODAY })
  const { form } = await openAdd()

  await waitFor(() => expect(checks.length).toBeGreaterThan(0))
  expect(form.queryByText(/Vẫn lưu được/)).toBeNull()
})

test('staysQuietWithoutCapacity', async () => {
  const { checks } = setUp({ peak: 7, peakDate: TODAY })
  const { form } = await openAdd()

  await waitFor(() => expect(checks.length).toBeGreaterThan(0))
  expect(form.queryByText(/Vẫn lưu được/)).toBeNull()
})

test('editDoesNotWarnAboutItself', async () => {
  const { checks } = setUp({ capacity: 2, peak: 1, peakDate: '2026-01-01' }, [
    { id: '1', personId: '55', placeId: '301', startDate: '2026-01-01' },
  ])
  const { user } = renderApp({ route: '/w/7/places/301?tab=people', userId: '42' })
  const current = within(await within(await screen.findByRole('main')).findByRole('region', { name: 'Đang ở' }))
  await user.click(await current.findByRole('button', { name: 'Thao tác với Nguyễn An' }))
  await user.click(await screen.findByRole('menuitem', { name: 'Sửa' }))
  await screen.findByRole('dialog', { name: 'Sửa cư trú' })

  await waitFor(() => expect(checks.length).toBeGreaterThan(0))
  const sent = checks.at(-1)!
  expect(sent.pathname).toBe('/api/registry/places/301/capacity-check')
  expect(sent.searchParams.get('excludeOccupancyId')).toBe('1')
  expect(sent.searchParams.get('startDate')).toBe('2026-01-01')
})

test('savesDespiteTheWarning', async () => {
  const { backend } = setUp({ capacity: 2, peak: 2, peakDate: '2026-12-01' })
  const { user, form } = await openAdd()

  await user.type(form.getByRole('searchbox', { name: 'Tìm người' }), 'Nguyễn')
  await user.click(await form.findByRole('radio', { name: 'Nguyễn An' }))
  await form.findByText(WARNING)
  await user.click(form.getByRole('button', { name: 'Thêm' }))

  await waitFor(() => expect(backend.writes).toHaveLength(1))
  expect(backend.writes[0]).toMatchObject({ method: 'POST', path: '/occupancies' })
})
