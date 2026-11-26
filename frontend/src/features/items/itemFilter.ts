import { filterableDefs, parseConditions, toUrlValues, type FieldDef } from '@/features/catalog'
import { isId } from '@/lib/ids'

import type { ItemFilter } from './api'
import type { ItemStatus } from './types'

export const ITEM_STATUSES: ItemStatus[] = ['IN_USE', 'BROKEN', 'STORED', 'REMOVED']

/**
 * Bộ lọc từ URL của trang Đồ đạc (`?status=`, `?category=`, `?q=`, `?root=`, `?f=`); giá trị lạ bị
 * bỏ qua. Điều kiện chỉ giữ mục chung và mục của nhóm đã chọn.
 */
export function readItemFilter(params: URLSearchParams, defs: FieldDef[]): ItemFilter {
  const status = params.get('status')
  const category = params.get('category')
  const root = params.get('root')
  const categoryId = isId(category) ? category : null
  return {
    status: ITEM_STATUSES.includes(status as ItemStatus) ? (status as ItemStatus) : null,
    categoryId,
    q: params.get('q') ?? '',
    rootId: isId(root) ? root : null,
    conditions: parseConditions(params.getAll('f'), filterableDefs(defs, categoryId)),
  }
}

export function writeItemFilter(filter: ItemFilter): URLSearchParams {
  const params = new URLSearchParams()
  if (filter.status) params.set('status', filter.status)
  if (filter.categoryId) params.set('category', filter.categoryId)
  if (filter.q !== '') params.set('q', filter.q)
  if (filter.rootId) params.set('root', filter.rootId)
  for (const value of toUrlValues(filter.conditions)) params.append('f', value)
  return params
}
