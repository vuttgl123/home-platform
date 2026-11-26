import { useId } from 'react'

import { Checkbox } from '@/components/ui/checkbox'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'
import { AttributeFilters, usePlaceTypes, type FieldDef } from '@/features/catalog'

import { PLACE_STATUS_LABEL } from '../labels'
import { useTopLevelPlaces } from '../queries'
import type { PlaceFilter, PlaceStatus } from '../types'

const STATUSES: PlaceStatus[] = ['ACTIVE', 'INACTIVE']

/** Bộ lọc của bảng địa điểm: chữ, loại, trạng thái, toà, còn chỗ, rồi các điều kiện theo mục tuỳ biến. */
export function PlaceFilters({
  ws,
  filter,
  defs,
  onChange,
}: {
  ws: string
  filter: PlaceFilter
  /** Mục chọn được (`filterableDefs` theo loại đã chọn). */
  defs: FieldDef[]
  onChange: (next: PlaceFilter) => void
}) {
  const types = (usePlaceTypes(ws).data ?? [])
    .filter((type) => type.status === 'ACTIVE')
    .sort((a, b) => a.sortOrder - b.sortOrder)
  const buildings = useTopLevelPlaces(ws).data ?? []
  const typeId = useId()
  const statusId = useId()
  const buildingId = useId()
  return (
    <div className="flex flex-col gap-4">
      <Input
        type="search"
        aria-label="Tìm địa điểm"
        placeholder="Tên, mã, mô tả…"
        value={filter.q}
        onChange={(event) => onChange({ ...filter, q: event.target.value })}
      />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-[repeat(3,minmax(0,14rem))_auto] lg:items-end">
        <Field>
          <FieldLabel htmlFor={typeId}>Loại</FieldLabel>
          <NativeSelect
            id={typeId}
            className="w-full"
            value={filter.placeTypeId ?? ''}
            // Đổi loại thì điều kiện của mục riêng loại cũ không còn áp dụng; trang đọc lại và bỏ chúng.
            onChange={(event) => onChange({ ...filter, placeTypeId: event.target.value || null })}
          >
            <NativeSelectOption value="">Mọi loại</NativeSelectOption>
            {types.map((type) => (
              <NativeSelectOption key={type.id} value={type.id}>
                {type.name}
              </NativeSelectOption>
            ))}
          </NativeSelect>
        </Field>
        <Field>
          <FieldLabel htmlFor={statusId}>Trạng thái</FieldLabel>
          <NativeSelect
            id={statusId}
            className="w-full"
            value={filter.status ?? ''}
            onChange={(event) => onChange({ ...filter, status: (event.target.value || null) as PlaceStatus | null })}
          >
            <NativeSelectOption value="">Mọi trạng thái</NativeSelectOption>
            {STATUSES.map((status) => (
              <NativeSelectOption key={status} value={status}>
                {PLACE_STATUS_LABEL[status]}
              </NativeSelectOption>
            ))}
          </NativeSelect>
        </Field>
        <Field>
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
        <label className="flex h-9 items-center gap-2 text-sm font-medium">
          <Checkbox
            aria-label="Còn chỗ"
            checked={filter.hasRoom}
            onCheckedChange={(checked) => onChange({ ...filter, hasRoom: checked === true })}
          />
          Còn chỗ
        </label>
      </div>
      <AttributeFilters defs={defs} value={filter.conditions} onChange={(conditions) => onChange({ ...filter, conditions })} />
    </div>
  )
}
