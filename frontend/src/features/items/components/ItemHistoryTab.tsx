import { useQueryClient } from '@tanstack/react-query'

import type { RegistrySchemas } from '@/api'
import { attributeRows, defsOfType, fieldLabelsWith, useAllFieldDefs, useFieldDefs, useItemCategories } from '@/features/catalog'
import { HistoryTab, type SnapshotRow } from '@/features/history'
import { usePerson } from '@/features/people'
import { PlaceName } from '@/features/places'
import { formatDate } from '@/lib/format'

import { ITEM_STATUS_LABEL } from '../labels'
import { refreshItems, useFetchFreshItem, useItem } from '../queries'
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
  const snapshotRows = useItemSnapshotRows(ws)
  return (
    <HistoryTab<ItemSnapshot>
      ws={ws}
      kind="items"
      id={item.id}
      currentVersion={item.version}
      fieldLabels={fieldLabelsWith(FIELD_LABELS, defs)}
      snapshotRows={snapshotRows}
      onRestored={(restored) => refreshItems(queryClient, ws, restored as ItemDetail)}
      onReload={() => fetchFresh(item.id)}
    />
  )
}

/**
 * Các trường của một bản món đồ: tên nhóm, số lượng kèm đơn vị, rồi mục tuỳ biến của nhóm ở bản đó
 * (nhóm có thể đã đổi từ đó tới nay). `null` khi nhóm và mục tuỳ biến còn đang tải.
 */
function useItemSnapshotRows(ws: string): ((snapshot: ItemSnapshot) => SnapshotRow[]) | null {
  const categories = useItemCategories(ws)
  const defs = useAllFieldDefs(ws, 'ITEM')
  if (categories.isPending || defs.isPending) {
    return null
  }
  return (snapshot) => [
    { key: 'name', label: 'Tên', value: snapshot.name, raw: snapshot.name },
    { key: 'code', label: 'Mã', value: snapshot.code, raw: snapshot.code },
    {
      key: 'categoryId',
      label: 'Nhóm',
      value: categories.data?.find((category) => category.id === snapshot.categoryId)?.name,
      raw: snapshot.categoryId,
    },
    {
      key: 'quantity',
      label: 'Số lượng',
      value: [snapshot.quantity, snapshot.unit].filter((part) => part != null).join(' '),
      raw: [snapshot.quantity ?? null, snapshot.unit ?? null],
    },
    { key: 'status', label: 'Trạng thái', value: snapshot.status ? ITEM_STATUS_LABEL[snapshot.status] : null, raw: snapshot.status },
    { key: 'placedAt', label: 'Ngày đặt', value: snapshot.placedAt ? formatDate(snapshot.placedAt) : null, raw: snapshot.placedAt },
    { key: 'removedAt', label: 'Ngày gỡ', value: snapshot.removedAt ? formatDate(snapshot.removedAt) : null, raw: snapshot.removedAt ?? null },
    { key: 'description', label: 'Mô tả', value: snapshot.description, raw: snapshot.description },
    // Chuyển chỗ, đổi chủ, gỡ, thay cũng là một version: không có các dòng này thì hai bản trông giống hệt.
    { key: 'placeId', label: 'Chỗ đặt', value: snapshot.placeId ? <PlaceName ws={ws} id={snapshot.placeId} /> : null, raw: snapshot.placeId ?? null },
    {
      key: 'ownerPersonId',
      label: 'Chủ',
      value: snapshot.ownerPersonId ? <PersonName ws={ws} id={snapshot.ownerPersonId} /> : null,
      raw: snapshot.ownerPersonId ?? null,
    },
    {
      key: 'replacedByItemId',
      label: 'Món thay',
      value: snapshot.replacedByItemId ? <ItemName ws={ws} id={snapshot.replacedByItemId} /> : null,
      raw: snapshot.replacedByItemId ?? null,
    },
    ...attributeRows(defsOfType(defs.data ?? [], snapshot.categoryId), (snapshot.attributes as Record<string, unknown>) ?? null).map(
      (row) => ({ ...row, value: row.display, muted: row.archived }),
    ),
  ]
}

/** Tên hiện tại của một người theo id; không mở được (đã xoá) thì hiện `#id`. */
function PersonName({ ws, id }: { ws: string; id: string }) {
  const person = usePerson(ws, id)
  return <>{person.data?.fullName ?? (person.error ? `#${id}` : '…')}</>
}

/** Tên hiện tại của một món theo id; không mở được (đã xoá) thì hiện `#id`. */
function ItemName({ ws, id }: { ws: string; id: string }) {
  const item = useItem(ws, id)
  return <>{item.data?.name ?? (item.error ? `#${id}` : '…')}</>
}
