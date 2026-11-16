import { useId } from 'react'
import { useParams, useSearchParams } from 'react-router'

import { Field, FieldLabel } from '@/components/ui/field'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'
import { useItemCategories } from '@/features/catalog'
import { isId } from '@/lib/ids'

import { ItemList } from '../components/ItemList'
import { ITEM_STATUS_LABEL } from '../labels'
import { useAllItems } from '../queries'
import type { ItemStatus } from '../types'

const STATUSES: ItemStatus[] = ['IN_USE', 'BROKEN', 'STORED', 'REMOVED']

function isStatus(value: string | null): value is ItemStatus {
  return STATUSES.includes(value as ItemStatus)
}

/**
 * Trang `/w/:workspaceId/items`: mọi món đồ của không gian, lọc theo trạng thái và nhóm. Bộ lọc nằm
 * trên URL (`?status=`, `?category=`) để chép đường dẫn gửi người khác; giá trị lạ bị bỏ qua.
 */
export function ItemsPage() {
  const { workspaceId = '' } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const requestedStatus = searchParams.get('status')
  const requestedCategory = searchParams.get('category')
  const status = isStatus(requestedStatus) ? requestedStatus : null
  const categoryId = isId(requestedCategory) ? requestedCategory : null
  const items = useAllItems(workspaceId, { status, categoryId })
  const categories = useItemCategories(workspaceId).data ?? []
  const statusId = useId()
  const categoryFieldId = useId()

  function update(key: 'status' | 'category', value: string) {
    const next = new URLSearchParams()
    const values = { status: status ?? '', category: categoryId ?? '', [key]: value }
    if (values.status) next.set('status', values.status)
    if (values.category) next.set('category', values.category)
    setSearchParams(next, { replace: true })
  }

  const filtered = status != null || categoryId != null
  return (
    <div className="flex max-w-5xl flex-col gap-6">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">Đồ đạc</h1>
        <p className="text-muted-foreground">Mọi món đồ của không gian này. Thêm đồ từ tab Đồ đạc của một địa điểm.</p>
      </header>
      <div className="grid gap-4 sm:max-w-xl sm:grid-cols-2">
        <Field>
          <FieldLabel htmlFor={statusId}>Trạng thái</FieldLabel>
          <NativeSelect id={statusId} className="w-full" value={status ?? ''} onChange={(event) => update('status', event.target.value)}>
            <NativeSelectOption value="">Chưa gỡ</NativeSelectOption>
            {STATUSES.map((value) => (
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
            onChange={(event) => update('category', event.target.value)}
          >
            <NativeSelectOption value="">Mọi nhóm</NativeSelectOption>
            {categories.map((category) => (
              <NativeSelectOption key={category.id} value={category.id}>
                {category.status === 'ARCHIVED' ? `${category.name} (đã lưu trữ)` : category.name}
              </NativeSelectOption>
            ))}
          </NativeSelect>
        </Field>
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
