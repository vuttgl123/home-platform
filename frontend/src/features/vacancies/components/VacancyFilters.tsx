import { useId } from 'react'

import { Field, FieldLabel } from '@/components/ui/field'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { usePlaceTypes } from '@/features/catalog'
import { useChildren } from '@/features/places'

import type { VacancyFilter } from '../types'

const ALL = 'all'

/** Lọc nhanh theo loại (một nút mỗi loại đang dùng) và theo toà (địa điểm cấp cao nhất). */
export function VacancyFilters({
  ws,
  filter,
  onChange,
}: {
  ws: string
  filter: VacancyFilter
  onChange: (next: VacancyFilter) => void
}) {
  const types = (usePlaceTypes(ws).data ?? [])
    .filter((type) => type.status === 'ACTIVE')
    .sort((a, b) => a.sortOrder - b.sortOrder)
  const buildings = useChildren(ws, null).items
  const buildingId = useId()
  return (
    <div className="flex flex-wrap items-end gap-4">
      <ToggleGroup
        aria-label="Loại địa điểm"
        variant="outline"
        size="sm"
        spacing={0}
        className="flex-wrap"
        value={[filter.placeTypeId ?? ALL]}
        onValueChange={(values: string[]) => {
          const picked = values.at(-1)
          onChange({ ...filter, placeTypeId: picked && picked !== ALL ? picked : null })
        }}
      >
        <ToggleGroupItem value={ALL}>Mọi loại</ToggleGroupItem>
        {types.map((type) => (
          <ToggleGroupItem key={type.id} value={type.id}>
            {type.name}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
      <Field className="w-full sm:w-64">
        <FieldLabel htmlFor={buildingId}>Toà</FieldLabel>
        <NativeSelect
          id={buildingId}
          className="w-full"
          value={filter.rootId ?? ''}
          onChange={(event) => onChange({ ...filter, rootId: event.target.value || null })}
        >
          <NativeSelectOption value="">Mọi toà</NativeSelectOption>
          {buildings.map((building) => (
            <NativeSelectOption key={building.id} value={building.id}>
              {building.name}
            </NativeSelectOption>
          ))}
        </NativeSelect>
      </Field>
    </div>
  )
}
