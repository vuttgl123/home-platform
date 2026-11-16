import { PlusIcon } from 'lucide-react'
import { useId, useState } from 'react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useDebouncedValue } from '@/hooks/use-debounced-value'

import { usePeopleSearch } from '../queries'
import type { PersonSummary } from '../types'

/**
 * Chọn một người bằng cách tìm theo tên; không có thì tạo người mới với đúng tên đang gõ.
 * @param onCreateNew người dùng chọn tạo người mới với tên này; không truyền thì không có lựa chọn đó
 */
export function PersonPicker({
  ws,
  value,
  onChange,
  onCreateNew,
}: {
  ws: string
  value: PersonSummary | null
  onChange: (person: PersonSummary) => void
  onCreateNew?: (name: string) => void
}) {
  const [term, setTerm] = useState('')
  const typed = term.trim()
  const settled = useDebouncedValue(typed, 300)
  const searchFor = typed === '' ? '' : settled
  const results = usePeopleSearch(ws, 'name', searchFor)
  const group = useId()

  return (
    <div className="flex flex-col gap-2">
      <Input
        type="search"
        aria-label="Tìm người"
        placeholder="Tìm người theo tên…"
        value={term}
        onChange={(event) => setTerm(event.target.value)}
        autoFocus
      />
      <div role="radiogroup" aria-label="Người" className="flex max-h-56 flex-col overflow-y-auto">
        {results.items.map((person) => {
          const nameId = `${group}-${person.id}`
          const note = [person.phone, [person.idType, person.idNumber].filter(Boolean).join(' ')].filter(Boolean).join(' · ')
          return (
            <label
              key={person.id}
              className="flex cursor-pointer items-start gap-2 rounded-md px-2 py-1.5 text-sm hover:bg-muted has-[:checked]:bg-muted"
            >
              <input
                type="radio"
                name={group}
                checked={value?.id === person.id}
                onChange={() => onChange(person)}
                aria-labelledby={nameId}
                className="mt-0.5 accent-primary"
              />
              <span className="flex min-w-0 flex-col">
                <span id={nameId} className="truncate">
                  {person.fullName}
                </span>
                {note && <span className="truncate text-xs text-muted-foreground">{note}</span>}
              </span>
            </label>
          )
        })}
      </div>
      {onCreateNew && searchFor !== '' && results.isLoaded && (
        <Button type="button" variant="ghost" size="sm" className="justify-start" onClick={() => onCreateNew(typed)}>
          <PlusIcon aria-hidden="true" />
          Thêm người mới “{typed}”
        </Button>
      )}
    </div>
  )
}
