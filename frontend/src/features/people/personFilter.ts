import { filterableDefs, parseConditions, toUrlValues, type FieldDef } from '@/features/catalog'
import { isId } from '@/lib/ids'

import type { PersonFilter } from './types'

/** Bộ lọc từ URL của trang Người (`?q=`, `?living=yes|no`, `?root=`, `?f=`); giá trị lạ bị bỏ qua. */
export function readPersonFilter(params: URLSearchParams, defs: FieldDef[]): PersonFilter {
  const living = params.get('living')
  const root = params.get('root')
  const parsedLiving = living === 'yes' || living === 'no' ? living : null
  return {
    q: params.get('q') ?? '',
    living: parsedLiving,
    rootId: parsedLiving !== 'no' && isId(root) ? root : null,
    conditions: parseConditions(params.getAll('f'), filterableDefs(defs, null)),
  }
}

export function writePersonFilter(filter: PersonFilter): URLSearchParams {
  const params = new URLSearchParams()
  if (filter.q !== '') params.set('q', filter.q)
  if (filter.living) params.set('living', filter.living)
  if (filter.rootId && filter.living !== 'no') params.set('root', filter.rootId)
  for (const value of toUrlValues(filter.conditions)) params.append('f', value)
  return params
}

/** Có lọc gì không: để biết danh sách trống là "không tìm thấy" hay "chưa có ai". */
export function isFiltered(filter: PersonFilter): boolean {
  return filter.q.trim() !== '' || filter.living !== null || filter.rootId !== null || filter.conditions.length > 0
}
