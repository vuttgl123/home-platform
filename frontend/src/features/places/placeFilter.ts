import { filterableDefs, parseConditions, toUrlValues, type FieldDef } from '@/features/catalog'
import { isId } from '@/lib/ids'

import type { PlaceFilter, PlaceStatus } from './types'

const STATUSES: PlaceStatus[] = ['ACTIVE', 'INACTIVE']

/**
 * Bộ lọc từ URL của trang Địa điểm (`?q=`, `?type=`, `?status=`, `?root=`, `?room=1`, `?f=`); giá trị
 * lạ bị bỏ qua. `defs` là mọi mục tuỳ biến của địa điểm; điều kiện chỉ giữ mục chọn được với loại đã chọn.
 */
export function readPlaceFilter(params: URLSearchParams, defs: FieldDef[]): PlaceFilter {
  const type = params.get('type')
  const status = params.get('status')
  const root = params.get('root')
  const placeTypeId = isId(type) ? type : null
  return {
    q: params.get('q') ?? '',
    placeTypeId,
    status: STATUSES.includes(status as PlaceStatus) ? (status as PlaceStatus) : null,
    rootId: isId(root) ? root : null,
    hasRoom: params.get('room') === '1',
    conditions: parseConditions(params.getAll('f'), filterableDefs(defs, placeTypeId)),
  }
}

export function writePlaceFilter(filter: PlaceFilter): URLSearchParams {
  const params = new URLSearchParams()
  if (filter.q !== '') params.set('q', filter.q)
  if (filter.placeTypeId) params.set('type', filter.placeTypeId)
  if (filter.status) params.set('status', filter.status)
  if (filter.rootId) params.set('root', filter.rootId)
  if (filter.hasRoom) params.set('room', '1')
  for (const value of toUrlValues(filter.conditions)) params.append('f', value)
  return params
}
