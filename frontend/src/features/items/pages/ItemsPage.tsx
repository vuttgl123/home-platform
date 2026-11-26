import { useId } from 'react'
import { useParams, useSearchParams } from 'react-router'

import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'
import { AttributeFilters, filterableDefs, useAllFieldDefs, useItemCategories } from '@/features/catalog'
import { ExportButton } from '@/features/exports'
import { useTopLevelPlaces } from '@/features/places'
import { useDebouncedValue } from '@/hooks/use-debounced-value'

import type { ItemFilter } from '../api'
import { ItemList } from '../components/ItemList'
import { ITEM_STATUSES, readItemFilter, writeItemFilter } from '../itemFilter'
import { ITEM_STATUS_LABEL } from '../labels'
import { useAllItems } from '../queries'
import type { ItemStatus } from '../types'

/**
 * Trang `/w/:workspaceId/items`: mọi món đồ của không gian (spec Đợt 11, S5): tìm trong tên, mã, mô
 * tả; lọc theo trạng thái, nhóm, toà và mục tuỳ biến. Bộ lọc nằm trên URL để chép đường dẫn gửi người
 * khác; giá trị lạ bị bỏ qua. Ô gõ đổi URL ngay, danh sách tải lại sau 300 ms.
 */
export function ItemsPage() {
  const { workspaceId = '' } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const fieldDefs = useAllFieldDefs(workspaceId, 'ITEM')
  const defs = fieldDefs.data ?? []
  const filter = readItemFilter(searchParams, defs)
  const { status, categoryId } = filter
  const settled = new URLSearchParams(useDebouncedValue(searchParams.toString(), 300))
  const settledFilter = readItemFilter(settled, defs)
  // Điều kiện trên URL chỉ đọc được khi đã có danh sách mục; chờ để không tải một lần không có chúng.
  const items = useAllItems(workspaceId, settledFilter, !(settled.has('f') && fieldDefs.isPending))
  const categories = useItemCategories(workspaceId).data ?? []
  const buildings = useTopLevelPlaces(workspaceId).data ?? []
  const statusId = useId()
  const categoryFieldId = useId()
  const buildingId = useId()

  /** Đọc lại sau khi ghi: đổi nhóm thì điều kiện của mục riêng nhóm cũ rời cả URL, không chỉ request. */
  function update(next: ItemFilter) {
    setSearchParams(writeItemFilter(readItemFilter(writeItemFilter(next), defs)), { replace: true })
  }

  const filtered =
    settledFilter.status != null ||
    settledFilter.categoryId != null ||
    settledFilter.q.trim() !== '' ||
    settledFilter.rootId != null ||
    settledFilter.conditions.length > 0
  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-wrap items-start gap-3">
        <div className="flex flex-1 flex-col gap-1">
          <h1 className="text-2xl font-semibold tracking-tight">Đồ đạc</h1>
          <p className="text-muted-foreground">Mọi món đồ của không gian này. Thêm đồ từ tab Đồ đạc của một địa điểm.</p>
        </div>
        <ExportButton
          ws={workspaceId}
          kind="items"
          query={{ status: status ?? undefined, categoryId: categoryId ?? undefined }}
          label="Xuất Excel"
        />
      </header>
      <div className="flex flex-col gap-4">
        <Input
          type="search"
          aria-label="Tìm đồ"
          placeholder="Tên, mã, mô tả…"
          value={filter.q}
          onChange={(event) => update({ ...filter, q: event.target.value })}
        />
        <div className="grid gap-4 sm:max-w-3xl sm:grid-cols-3">
          <Field>
            <FieldLabel htmlFor={statusId}>Trạng thái</FieldLabel>
            <NativeSelect
              id={statusId}
              className="w-full"
              value={status ?? ''}
              onChange={(event) => update({ ...filter, status: (event.target.value || null) as ItemStatus | null })}
            >
              <NativeSelectOption value="">Chưa gỡ</NativeSelectOption>
              {ITEM_STATUSES.map((value) => (
                <NativeSelectOption key={value} value={value}>
                  {ITEM_STATUS_LABEL[value]}
                </NativeSelectOption>
              ))}
            </NativeSelect>
          </Field>
          <Field>
            <FieldLabel htmlFor={categoryFieldId}>Nhóm</FieldLabel>
            <NativeSelect
              id={categoryFieldId}
              className="w-full"
              value={categoryId ?? ''}
                onChange={(event) => update({ ...filter, categoryId: event.target.value || null })}
            >
              <NativeSelectOption value="">Mọi nhóm</NativeSelectOption>
              {categories.map((category) => (
                <NativeSelectOption key={category.id} value={category.id}>
                  {category.status === 'ARCHIVED' ? `${category.name} (đã lưu trữ)` : category.name}
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
              onChange={(event) => update({ ...filter, rootId: event.target.value || null })}
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
        <AttributeFilters
          defs={filterableDefs(defs, categoryId)}
          value={filter.conditions}
          onChange={(conditions) => update({ ...filter, conditions })}
        />
      </div>
      <ItemList
        ws={workspaceId}
        list={items}
        showPlace
        removed={status === 'REMOVED'}
        empty={filtered ? 'Không có món nào khớp bộ lọc.' : 'Chưa có món đồ nào.'}
      />
    </div>
  )
}
