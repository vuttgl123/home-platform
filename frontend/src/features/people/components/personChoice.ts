import { useState } from 'react'

import { useCreatePerson } from '../queries'
import type { PersonDetail, PersonSummary } from '../types'

/** Một người đã có, hoặc tên của người sẽ được tạo khi gửi form. */
export type PersonChoice = { kind: 'existing'; person: PersonSummary } | { kind: 'new'; fullName: string }

export type PersonChoiceState = {
  choice: PersonChoice | null
  setChoice: (choice: PersonChoice | null) => void
  /** `id` và tên của người đã chọn. Với người mới: tạo hồ sơ, đúng một lần kể cả khi gửi lại sau lỗi. `null` khi chưa chọn. */
  resolve: () => Promise<{ id: string; fullName: string } | null>
  isCreating: boolean
  /** Hồ sơ của người mới đã được tạo; không đổi người được nữa. */
  isCreated: boolean
}

export function usePersonChoice(ws: string): PersonChoiceState {
  const [choice, setChoice] = useState<PersonChoice | null>(null)
  // Hồ sơ đã tạo cho người mới: bấm lại sau một lỗi thì dùng lại, không tạo lần hai.
  const [created, setCreated] = useState<PersonDetail | null>(null)
  const createPerson = useCreatePerson(ws)

  async function resolve() {
    if (choice == null) return null
    if (choice.kind === 'existing') return { id: choice.person.id, fullName: choice.person.fullName }
    if (created) return { id: created.id, fullName: created.fullName }
    const made = await createPerson.mutateAsync({ fullName: choice.fullName })
    setCreated(made)
    return { id: made.id, fullName: made.fullName }
  }

  return { choice, setChoice, resolve, isCreating: createPerson.isPending, isCreated: created != null }
}
