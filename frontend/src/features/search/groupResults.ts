import type { ItemSummary } from '@/features/items'
import type { PersonSummary } from '@/features/people'
import type { PlaceSearchResult } from '@/features/places'

/** Một dòng của ô tìm chung. */
export type SearchRow = {
  /** `PLACE:301`: duy nhất trên mọi nhóm. */
  key: string
  kind: 'PLACE' | 'PERSON' | 'ITEM'
  title: string
  /** Địa điểm: đường dẫn tổ tiên; người: số điện thoại; đồ: chỗ đặt. */
  detail: string | null
  code: string | null
  removed: boolean
  href: string
}

export type SearchGroups = { codeMatches: SearchRow[]; places: SearchRow[]; people: SearchRow[]; items: SearchRow[] }

/**
 * Gộp kết quả của các API tìm thành các nhóm của ô tìm chung. Khớp mã (địa điểm rồi đồ) đứng riêng;
 * một bản ghi đã khớp mã thì không lặp lại trong nhóm theo tên của loại nó.
 */
export function groupResults(
  ws: string,
  found: {
    places?: { byCode: PlaceSearchResult[]; byName: PlaceSearchResult[] }
    people?: PersonSummary[]
    items?: { byCode: ItemSummary[]; byName: ItemSummary[] }
  },
): SearchGroups {
  const place = (row: PlaceSearchResult): SearchRow => ({
    key: `PLACE:${row.id}`,
    kind: 'PLACE',
    title: row.name,
    detail: row.ancestors.length > 0 ? row.ancestors.map((ancestor) => ancestor.name).join(' › ') : null,
    code: row.code ?? null,
    removed: false,
    href: `/w/${ws}/places/${row.id}`,
  })
  const item = (row: ItemSummary): SearchRow => ({
    key: `ITEM:${row.id}`,
    kind: 'ITEM',
    title: row.name,
    detail: `ở ${row.placeName}`,
    code: row.code ?? null,
    removed: row.removedAt != null,
    href: `/w/${ws}/items/${row.id}`,
  })
  const person = (row: PersonSummary): SearchRow => ({
    key: `PERSON:${row.id}`,
    kind: 'PERSON',
    title: row.fullName,
    detail: row.phone ?? null,
    code: null,
    removed: false,
    href: `/w/${ws}/people/${row.id}`,
  })
  const codeMatches = [...(found.places?.byCode ?? []).map(place), ...(found.items?.byCode ?? []).map(item)]
  const taken = new Set(codeMatches.map((row) => row.key))
  const notTaken = (row: SearchRow) => !taken.has(row.key)
  return {
    codeMatches,
    places: (found.places?.byName ?? []).map(place).filter(notTaken),
    people: (found.people ?? []).map(person),
    items: (found.items?.byName ?? []).map(item).filter(notTaken),
  }
}
