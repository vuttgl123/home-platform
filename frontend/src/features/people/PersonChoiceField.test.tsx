import { screen, waitFor } from '@testing-library/react'
import { useState } from 'react'
import { expect, test } from 'vitest'

import { setCurrentUser } from '@/api'
import { mockPeopleBackend } from '@/test/peopleBackend'
import { renderWithProviders } from '@/test/render'

import { PersonChoiceField } from './components/PersonChoiceField'
import { usePersonChoice } from './components/personChoice'

/** Ô chọn người cùng một nút "Xác nhận" gọi `resolve` và in kết quả. */
function Harness() {
  const state = usePersonChoice('7')
  const [resolved, setResolved] = useState('')
  return (
    <div>
      <PersonChoiceField ws="7" state={state} />
      <button type="button" onClick={() => void state.resolve().then((person) => setResolved(person ? `${person.id}:${person.fullName}` : 'none'))}>
        Xác nhận
      </button>
      <output>{resolved}</output>
    </div>
  )
}

function setUp() {
  setCurrentUser('42')
  const backend = mockPeopleBackend({ people: [{ id: '55', fullName: 'Nguyễn An' }] })
  return { backend, ...renderWithProviders(<Harness />) }
}

test('picksAnExistingPerson', async () => {
  const { user, backend } = setUp()

  await user.type(screen.getByRole('searchbox', { name: 'Tìm người' }), 'Nguyễn')
  await user.click(await screen.findByRole('radio', { name: 'Nguyễn An' }))
  await user.click(screen.getByRole('button', { name: 'Xác nhận' }))

  expect(await screen.findByText('55:Nguyễn An')).toBeInTheDocument()
  expect(backend.writes).toEqual([])
})

test('newPersonIsCreatedOnResolve', async () => {
  const { user, backend } = setUp()

  await user.type(screen.getByRole('searchbox', { name: 'Tìm người' }), 'Phạm Mới')
  await user.click(await screen.findByRole('button', { name: 'Thêm người mới “Phạm Mới”' }))
  expect(screen.getByText(/Người mới:/)).toBeInTheDocument()
  expect(backend.writes).toEqual([])
  await user.click(screen.getByRole('button', { name: 'Xác nhận' }))

  expect(await screen.findByText('8000:Phạm Mới')).toBeInTheDocument()
  expect(backend.writes.map((write) => write.path)).toEqual(['/people'])
})

test('resolveTwiceCreatesOnePerson', async () => {
  const { user, backend } = setUp()

  await user.type(screen.getByRole('searchbox', { name: 'Tìm người' }), 'Phạm Mới')
  await user.click(await screen.findByRole('button', { name: 'Thêm người mới “Phạm Mới”' }))
  await user.click(screen.getByRole('button', { name: 'Xác nhận' }))
  await screen.findByText('8000:Phạm Mới')
  await user.click(screen.getByRole('button', { name: 'Xác nhận' }))

  await waitFor(() => expect(screen.getByText('8000:Phạm Mới')).toBeInTheDocument())
  expect(backend.writes.map((write) => write.path)).toEqual(['/people'])
})

test('choiceCanBeChangedBeforeCreating', async () => {
  const { user } = setUp()

  await user.type(screen.getByRole('searchbox', { name: 'Tìm người' }), 'Phạm Mới')
  await user.click(await screen.findByRole('button', { name: 'Thêm người mới “Phạm Mới”' }))
  await user.click(screen.getByRole('button', { name: 'Đổi' }))

  expect(screen.getByRole('searchbox', { name: 'Tìm người' })).toBeInTheDocument()
  await user.click(screen.getByRole('button', { name: 'Xác nhận' }))
  expect(await screen.findByText('none')).toBeInTheDocument()
})
