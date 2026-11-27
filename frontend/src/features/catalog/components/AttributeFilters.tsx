import { PlusIcon, XIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'

import { MAX_CONDITIONS, needsValue, opLabel, OPS_BY_TYPE, type AttributeCondition, type AttributeOp } from '../attributeFilters'
import type { FieldDef } from '../types'

/**
 * Các điều kiện lọc theo mục tuỳ biến (spec Đợt 11, S2): mỗi hàng là mục, phép so, giá trị và nút
 * bỏ; tối đa 3 hàng, cùng đúng. `defs` là mục chọn được (`filterableDefs`); không có mục nào thì
 * không hiện gì.
 */
export function AttributeFilters({
  defs,
  value,
  onChange,
}: {
  defs: FieldDef[]
  value: AttributeCondition[]
  onChange: (next: AttributeCondition[]) => void
}) {
  if (defs.length === 0) return null

  function replace(index: number, condition: AttributeCondition) {
    onChange(value.map((current, at) => (at === index ? condition : current)))
  }

  function add() {
    const def = defs[0]
    onChange([...value, { key: def.fieldKey, op: OPS_BY_TYPE[def.dataType][0], value: '' }])
  }

  return (
    <div className="flex flex-col gap-2">
      {value.map((condition, index) => {
        const def = defs.find((candidate) => candidate.fieldKey === condition.key)
        if (!def) return null
        const n = index + 1
        return (
          <div key={index} className="flex flex-wrap items-center gap-2">
            <NativeSelect
              aria-label={`Mục của điều kiện ${n}`}
              value={condition.key}
              onChange={(event) => {
                const next = defs.find((candidate) => candidate.fieldKey === event.target.value) ?? def
                replace(index, { key: next.fieldKey, op: OPS_BY_TYPE[next.dataType][0], value: '' })
              }}
            >
              {defs.map((candidate) => (
                <NativeSelectOption key={candidate.fieldKey} value={candidate.fieldKey}>
                  {candidate.label}
                </NativeSelectOption>
              ))}
            </NativeSelect>
            <NativeSelect
              aria-label={`Phép so của điều kiện ${n}`}
              value={condition.op}
              onChange={(event) => {
                const op = event.target.value as AttributeOp
                replace(index, { ...condition, op, value: needsValue(op) ? condition.value : '' })
              }}
            >
              {OPS_BY_TYPE[def.dataType].map((op) => (
                <NativeSelectOption key={op} value={op}>
                  {opLabel(op, def.dataType)}
                </NativeSelectOption>
              ))}
            </NativeSelect>
            {needsValue(condition.op) && (
              <ValueInput
                def={def}
                label={`Giá trị của điều kiện ${n}`}
                value={condition.value}
                onChange={(text) => replace(index, { ...condition, value: text })}
              />
            )}
            <Button
              type="button"
              variant="ghost"
              size="icon"
              aria-label={`Bỏ điều kiện ${def.label}`}
              onClick={() => onChange(value.filter((_, at) => at !== index))}
            >
              <XIcon aria-hidden="true" />
            </Button>
          </div>
        )
      })}
      <div>
        <Button type="button" variant="outline" size="sm" disabled={value.length >= MAX_CONDITIONS} onClick={add}>
          <PlusIcon aria-hidden="true" />
          Thêm điều kiện
        </Button>
      </div>
    </div>
  )
}

/** Ô giá trị theo kiểu của mục: chữ, số, ngày, hoặc chọn (Có/Không, các lựa chọn). */
function ValueInput({
  def,
  label,
  value,
  onChange,
}: {
  def: FieldDef
  label: string
  value: string
  onChange: (value: string) => void
}) {
  if (def.dataType === 'BOOLEAN' || def.dataType === 'SELECT') {
    const choices =
      def.dataType === 'BOOLEAN'
        ? [
            { value: 'true', label: 'Có' },
            { value: 'false', label: 'Không' },
          ]
        : (def.options ?? []).map((option) => ({ value: option, label: option }))
    return (
      <NativeSelect aria-label={label} value={value} onChange={(event) => onChange(event.target.value)}>
        <NativeSelectOption value="">—</NativeSelectOption>
        {choices.map((choice) => (
          <NativeSelectOption key={choice.value} value={choice.value}>
            {choice.label}
          </NativeSelectOption>
        ))}
      </NativeSelect>
    )
  }
  const type = def.dataType === 'NUMBER' ? 'number' : def.dataType === 'DATE' ? 'date' : 'text'
  return (
    <Input
      aria-label={label}
      type={type}
      step={def.dataType === 'NUMBER' ? 'any' : undefined}
      inputMode={def.dataType === 'NUMBER' ? 'decimal' : undefined}
      value={value}
      onChange={(event) => onChange(event.target.value)}
      className="w-auto min-w-0 flex-[1_1_10rem] sm:max-w-56"
    />
  )
}
