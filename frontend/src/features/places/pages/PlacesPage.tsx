import { useParams, useSearchParams } from 'react-router'

import { filterableDefs, useAllFieldDefs } from '@/features/catalog'
import { useDebouncedValue } from '@/hooks/use-debounced-value'

import { PlaceFilters } from '../components/PlaceFilters'
import { PlaceListTable } from '../components/PlaceListTable'
import { readPlaceFilter, writePlaceFilter } from '../placeFilter'
import { usePlaceList } from '../queries'
import type { PlaceFilter } from '../types'

/**
 * Trang `/w/:workspaceId/places`: mọi địa điểm dạng bảng, tìm chữ và lọc (spec Đợt 11, S3). Bộ lọc
 * nằm trên URL để chép đường dẫn gửi người khác; ô gõ đổi URL ngay, danh sách tải lại sau 300 ms.
 */
export function PlacesPage() {
  const { workspaceId = '' } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const fieldDefs = useAllFieldDefs(workspaceId, 'PLACE')
  const defs = fieldDefs.data ?? []
  const filter = readPlaceFilter(searchParams, defs)
  const settled = new URLSearchParams(useDebouncedValue(searchParams.toString(), 300))
  // Điều kiện trên URL chỉ đọc được khi đã có danh sách mục; chờ để không tải một lần không có chúng.
  const waiting = settled.has('f') && fieldDefs.isPending
  const list = usePlaceList(workspaceId, readPlaceFilter(settled, defs), !waiting)

  function update(next: PlaceFilter) {
    setSearchParams(writePlaceFilter(next), { replace: true })
  }

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">Địa điểm</h1>
        <p className="text-muted-foreground">Mọi địa điểm của không gian này, tìm và lọc theo nhiều điều kiện.</p>
      </header>
      <PlaceFilters ws={workspaceId} filter={filter} defs={filterableDefs(defs, filter.placeTypeId)} onChange={update} />
      <PlaceListTable ws={workspaceId} list={list} />
    </div>
  )
}
