import { useQueryClient } from '@tanstack/react-query'

import type { RegistrySchemas } from '@/api'
import { AttributeView, fieldLabelsWith, useFieldDefs, usePlaceTypes } from '@/features/catalog'
import { HistoryTab, SnapshotFields } from '@/features/history'

import { refreshAfterWrite, useFetchFreshPlace } from '../queries'
import type { PlaceDetail } from '../types'

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

const STATUS_LABEL = { ACTIVE: 'Đang dùng', INACTIVE: 'Tạm ngưng' } as const

/** Tab "Lịch sử" của một địa điểm. Khôi phục đổi cả địa chỉ của version đó, nhưng không di chuyển địa điểm. */
export function PlaceHistoryTab({ ws, place }: { ws: string; place: PlaceDetail }) {
  const queryClient = useQueryClient()
  const fetchFresh = useFetchFreshPlace(ws)
  const defs = useFieldDefs(ws, 'PLACE', place.placeTypeId).data ?? []
  return (
    <HistoryTab<PlaceSnapshot>
      ws={ws}
      kind="places"
      id={place.id}
      currentVersion={place.version}
      fieldLabels={fieldLabelsWith(FIELD_LABELS, defs)}
      renderSnapshot={(snapshot) => <PlaceSnapshotView ws={ws} snapshot={snapshot} />}
      // Tên và địa chỉ có thể đã đổi: đường dẫn tổ tiên và địa chỉ thừa hưởng của con cháu cũng cũ.
      onRestored={(restored) => refreshAfterWrite(queryClient, ws, restored as PlaceDetail, 'descendants')}
      onReload={() => fetchFresh(place.id)}
    />
  )
}

function PlaceSnapshotView({ ws, snapshot }: { ws: string; snapshot: PlaceSnapshot }) {
  const typeName = usePlaceTypes(ws).data?.find((type) => type.id === snapshot.placeTypeId)?.name
  const defs = useFieldDefs(ws, 'PLACE', snapshot.placeTypeId).data ?? []
  return (
    <div className="flex flex-col gap-4">
      <SnapshotFields
        rows={[
          ['Tên', snapshot.name],
          ['Mã', snapshot.code],
          ['Loại', typeName],
          ['Trạng thái', snapshot.status ? STATUS_LABEL[snapshot.status] : null],
          ['Mô tả', snapshot.description],
          ['Địa chỉ', snapshot.address?.fullAddress ?? 'Không có địa chỉ riêng'],
        ]}
      />
      {defs.length > 0 && <AttributeView defs={defs} values={(snapshot.attributes as Record<string, unknown>) ?? null} />}
    </div>
  )
}
