import type { CompareLine, SnapshotRow } from './types'

/** JSON không phụ thuộc thứ tự khoá; `undefined` coi như `null`. */
function canonical(value: unknown): string {
  return JSON.stringify(value ?? null, (_key, inner: unknown) =>
    inner != null && typeof inner === 'object' && !Array.isArray(inner)
      ? Object.fromEntries(Object.entries(inner).sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0)))
      : inner,
  )
}

/**
 * Hợp các dòng của hai bản theo `key`: thứ tự dòng của bản mới, rồi các khoá chỉ có ở bản cũ.
 * Một dòng `changed` khi giá trị gốc (`raw`) của hai bên khác nội dung; bên thiếu dòng coi như trống.
 */
export function compareRows(older: SnapshotRow[], newer: SnapshotRow[]): CompareLine[] {
  const olderByKey = new Map(older.map((row) => [row.key, row]))
  const newerKeys = new Set(newer.map((row) => row.key))
  const line = (before: SnapshotRow | undefined, after: SnapshotRow | undefined): CompareLine => {
    const label = (after ?? before)?.label ?? ''
    return {
      key: (after ?? before)?.key ?? '',
      label,
      older: before ? before.value : null,
      newer: after ? after.value : null,
      changed: canonical(before?.raw) !== canonical(after?.raw),
    }
  }
  return [
    ...newer.map((row) => line(olderByKey.get(row.key), row)),
    ...older.filter((row) => !newerKeys.has(row.key)).map((row) => line(row, undefined)),
  ]
}
