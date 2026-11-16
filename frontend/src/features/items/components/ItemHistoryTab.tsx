import { useQueryClient } from '@tanstack/react-query'

import type { RegistrySchemas } from '@/api'
import { AttributeView, fieldLabelsWith, useFieldDefs, useItemCategories } from '@/features/catalog'
import { HistoryTab, SnapshotFields } from '@/features/history'
import { formatDate } from '@/lib/format'

import { ITEM_STATUS_LABEL } from '../labels'
import { refreshItems, useFetchFreshItem } from '../queries'
import type { ItemDetail } from '../types'

type ItemSnapshot = RegistrySchemas['ItemSnapshot']

const FIELD_LABELS: Record<string, string> = {
  name: 'Tên',
  code: 'Mã',
  categoryId: 'Nhóm',
  quantity: 'Số lượng',
  unit: 'Đơn vị',
  status: 'Trạng thái',
  placedAt: 'Ngày đặt',
  removedAt: 'Ngày gỡ',
  placeId: 'Chỗ đặt',
  replacedByItemId: 'Món thay',
  ownerPersonId: 'Chủ',
  description: 'Mô tả',
  attributes: 'Mục tuỳ biến',
  coverFileId: 'Ảnh bìa',
  deletedAt: 'Xoá',
}

/** Tab "Lịch sử" của một món. Khôi phục chỉ đổi nội dung: chỗ đặt và trạng thái gỡ, thay giữ nguyên. */
export function ItemHistoryTab({ ws, item }: { ws: string; item: ItemDetail }) {
  const queryClient = useQueryClient()
  const fetchFresh = useFetchFreshItem(ws)
  const defs = useFieldDefs(ws, 'ITEM', item.categoryId ?? '0').data ?? []
  return (
    <HistoryTab<ItemSnapshot>
      ws={ws}
      kind="items"
      id={item.id}
      currentVersion={item.version}
      fieldLabels={fieldLabelsWith(FIELD_LABELS, defs)}
      renderSnapshot={(snapshot) => <ItemSnapshotView ws={ws} snapshot={snapshot} />}
      onRestored={(restored) => refreshItems(queryClient, ws, restored as ItemDetail)}
      onReload={() => fetchFresh(item.id)}
    />
  )
}

function ItemSnapshotView({ ws, snapshot }: { ws: string; snapshot: ItemSnapshot }) {
  const categoryName = useItemCategories(ws).data?.find((category) => category.id === snapshot.categoryId)?.name
  const defs = useFieldDefs(ws, 'ITEM', snapshot.categoryId ?? '0').data ?? []
  return (
    <div className="flex flex-col gap-4">
      <SnapshotFields
        rows={[
          ['Tên', snapshot.name],
          ['Mã', snapshot.code],
          ['Nhóm', categoryName],
          ['Số lượng', [snapshot.quantity, snapshot.unit].filter((part) => part != null).join(' ')],
          ['Trạng thái', snapshot.status ? ITEM_STATUS_LABEL[snapshot.status] : null],
          ['Ngày đặt', snapshot.placedAt ? formatDate(snapshot.placedAt) : null],
          ['Mô tả', snapshot.description],
        ]}
      />
      {defs.length > 0 && <AttributeView defs={defs} values={(snapshot.attributes as Record<string, unknown>) ?? null} />}
    </div>
  )
}
