import { useId } from 'react'

import { Field, FieldLabel } from '@/components/ui/field'
import { PersonPicker } from '@/features/people'

import type { OwnerChoice } from './ownerChoice'


/** Ô "Của ai": của địa điểm (mặc định) hoặc của một người, tìm theo tên. */
export function OwnerField({ ws, value, onChange }: { ws: string; value: OwnerChoice; onChange: (choice: OwnerChoice) => void }) {
  const group = useId()
  return (
    <Field>
      <FieldLabel>Của ai</FieldLabel>
      <div role="radiogroup" aria-label="Của ai" className="flex flex-wrap gap-4 text-sm">
        <label className="flex items-center gap-2">
          <input
            type="radio"
            name={group}
            className="accent-primary"
            checked={value.kind === 'place'}
            onChange={() => onChange({ kind: 'place' })}
          />
          Của địa điểm này
        </label>
        <label className="flex items-center gap-2">
          <input
            type="radio"
            name={group}
            className="accent-primary"
            checked={value.kind === 'person'}
            onChange={() => onChange({ kind: 'person', person: null })}
          />
          Của một người
        </label>
      </div>
      {value.kind === 'person' && (
        <PersonPicker ws={ws} value={value.person} onChange={(person) => onChange({ kind: 'person', person })} />
      )}
    </Field>
  )
}
