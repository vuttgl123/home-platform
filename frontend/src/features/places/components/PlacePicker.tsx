import { useId, useState } from 'react'

import { Input } from '@/components/ui/input'
import { useDebouncedValue } from '@/hooks/use-debounced-value'

import { usePlaceSearch } from '../queries'
import type { PlaceSearchResult } from '../types'

/** Một lựa chọn trong `PlacePicker`: lên cấp cao nhất, hoặc một địa điểm cụ thể. */
export type PlaceChoice = { kind: 'top' } | { kind: 'place'; place: PlaceSearchResult }

/**
 * Chọn một địa điểm bằng cách tìm theo tên. Mỗi kết quả kèm đường dẫn tổ tiên và mã, để phân biệt
 * hai địa điểm cùng tên ở hai nơi.
 *
 * @param exclude các `id` không được chọn (ví dụ chính nó, chỗ đang chứa nó)
 * @param allowTop có thêm lựa chọn "Lên cấp cao nhất"
 */
export function PlacePicker({
  ws,
  exclude,
  value,
  onChange,
  allowTop = false,
  label = 'Tìm nơi chuyển tới',
}: {
  ws: string
  exclude: (string | null | undefined)[]
  value: PlaceChoice | null
  onChange: (choice: PlaceChoice) => void
  allowTop?: boolean
  label?: string
}) {
  const [term, setTerm] = useState('')
  const settled = useDebouncedValue(term.trim(), 300)
  const searchFor = term.trim() === '' ? '' : settled
  const results = usePlaceSearch(ws, searchFor)
  const name = useId()
  const candidates = results.items.filter((item) => !exclude.includes(item.id))
  const selectedId = value?.kind === 'place' ? value.place.id : value?.kind

  return (
    <div className="flex flex-col gap-3">
      <Input
        type="search"
        aria-label={label}
        placeholder={`${label} theo tên…`}
        value={term}
        onChange={(event) => setTerm(event.target.value)}
        autoFocus
      />
      <div role="radiogroup" aria-label="Kết quả" className="flex max-h-64 flex-col overflow-y-auto">
        {allowTop && (
          <Choice group={name} id="top" checked={selectedId === 'top'} onSelect={() => onChange({ kind: 'top' })}>
            Lên cấp cao nhất
          </Choice>
        )}
        {candidates.map((candidate) => (
          <Choice
            key={candidate.id}
            group={name}
            id={candidate.id}
            checked={selectedId === candidate.id}
            onSelect={() => onChange({ kind: 'place', place: candidate })}
            note={[candidate.ancestors.map((ancestor) => ancestor.name).join(' › '), candidate.code]
              .filter(Boolean)
              .join(' · ')}
          >
            {candidate.name}
          </Choice>
        ))}
        {searchFor !== '' && results.isLoaded && candidates.length === 0 && (
          <p className="px-2 py-1.5 text-sm text-muted-foreground">Không có nơi nào có tên bắt đầu bằng “{searchFor}”.</p>
        )}
      </div>
    </div>
  )
}

/** Một lựa chọn. `note` (đường dẫn tổ tiên, mã) là mô tả của nút chọn, không nằm trong tên của nó. */
function Choice({
  group,
  id,
  checked,
  onSelect,
  note,
  children,
}: {
  group: string
  id: string
  checked: boolean
  onSelect: () => void
  note?: string
  children: React.ReactNode
}) {
  const nameId = `${group}-${id}-name`
  const noteId = `${group}-${id}-note`
  return (
    <label className="flex cursor-pointer items-start gap-2 rounded-md px-2 py-1.5 text-sm hover:bg-muted has-[:checked]:bg-muted">
      <input
        type="radio"
        name={group}
        value={id}
        checked={checked}
        onChange={onSelect}
        aria-labelledby={nameId}
        aria-describedby={note ? noteId : undefined}
        className="mt-0.5 accent-primary"
      />
      <span className="flex min-w-0 flex-col">
        <span id={nameId} className="truncate">
          {children}
        </span>
        {note && (
          <span id={noteId} className="truncate text-xs text-muted-foreground">
            {note}
          </span>
        )}
      </span>
    </label>
  )
}
