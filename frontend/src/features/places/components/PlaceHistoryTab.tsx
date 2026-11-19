import { useQueryClient } from '@tanstack/react-query'

import type { RegistrySchemas } from '@/api'
import { attributeRows, defsOfType, fieldLabelsWith, useAllFieldDefs, useFieldDefs, usePlaceTypes } from '@/features/catalog'
import { HistoryTab, type SnapshotRow } from '@/features/history'

import { PLACE_STATUS_LABEL } from '../labels'
import { refreshAfterWrite, useFetchFreshPlace } from '../queries'
import type { PlaceDetail } from '../types'
import { PlaceName } from './PlaceName'

type PlaceSnapshot = RegistrySchemas['PlaceSnapshot']

const FIELD_LABELS: Record<string, string> = {
  name: 'Tên',
  code: 'Mã',
  description: 'Mô tả',
  status: 'Trạng thái',
  placeTypeId: 'Loại',
  attributes: 'Mục tuỳ biến',
  address: 'Địa chỉ',
  coverFileId: 'Ảnh bìa',
  parentId: 'Chỗ chứa',
  deletedAt: 'Xoá',
}

/** Tab "Lịch sử" của một địa điểm. Khôi phục đổi cả địa chỉ của version đó, nhưng không di chuyển địa điểm. */
export function PlaceHistoryTab({ ws, place }: { ws: string; place: PlaceDetail }) {
  const queryClient = useQueryClient()
  const fetchFresh = useFetchFreshPlace(ws)
  const defs = useFieldDefs(ws, 'PLACE', place.placeTypeId).data ?? []
  const snapshotRows = usePlaceSnapshotRows(ws)
  return (
    <HistoryTab<PlaceSnapshot>
      ws={ws}
      kind="places"
      id={place.id}
      currentVersion={place.version}
      fieldLabels={fieldLabelsWith(FIELD_LABELS, defs)}
      snapshotRows={snapshotRows}
      // Tên và địa chỉ có thể đã đổi: đường dẫn tổ tiên và địa chỉ thừa hưởng của con cháu cũng cũ.
      onRestored={(restored) => refreshAfterWrite(queryClient, ws, restored as PlaceDetail, 'descendants')}
      onReload={() => fetchFresh(place.id)}
    />
  )
}

/**
 * Các trường của một bản địa điểm: tên loại, trạng thái, địa chỉ của chính bản đó, rồi mục tuỳ biến
 * của loại ở bản đó (loại có thể đã đổi từ đó tới nay). `null` khi loại và mục tuỳ biến còn đang tải.
 */
function usePlaceSnapshotRows(ws: string): ((snapshot: PlaceSnapshot) => SnapshotRow[]) | null {
  const types = usePlaceTypes(ws)
  const defs = useAllFieldDefs(ws, 'PLACE')
  if (types.isPending || defs.isPending) {
    return null
  }
  return (snapshot) => [
    { key: 'name', label: 'Tên', value: snapshot.name, raw: snapshot.name },
    { key: 'code', label: 'Mã', value: snapshot.code, raw: snapshot.code },
    {
      key: 'placeTypeId',
      label: 'Loại',
      value: types.data?.find((type) => type.id === snapshot.placeTypeId)?.name,
      raw: snapshot.placeTypeId,
    },
    { key: 'status', label: 'Trạng thái', value: snapshot.status ? PLACE_STATUS_LABEL[snapshot.status] : null, raw: snapshot.status },
    { key: 'description', label: 'Mô tả', value: snapshot.description, raw: snapshot.description },
    { key: 'address', label: 'Địa chỉ', value: snapshot.address?.fullAddress ?? 'Không có địa chỉ riêng', raw: snapshot.address ?? null },
    // Chuyển chỗ cũng là một version: không có dòng này thì hai bản trước và sau khi chuyển trông giống hệt.
    { key: 'parentId', label: 'Chỗ chứa', value: snapshot.parentId ? <PlaceName ws={ws} id={snapshot.parentId} /> : null, raw: snapshot.parentId ?? null },
    ...attributeRows(defsOfType(defs.data ?? [], snapshot.placeTypeId), (snapshot.attributes as Record<string, unknown>) ?? null).map(
      (row) => ({ ...row, value: row.display, muted: row.archived }),
    ),
  ]
}
