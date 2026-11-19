import type { ReactNode } from 'react'

import type { Present, RegistrySchemas } from '@/api'

/**
 * Một trường của một bản: nhãn, cách hiện, và giá trị gốc để so hai bản. `key` là duy nhất trong
 * một bản; mục tuỳ biến có khoá `attr:<fieldKey>`.
 */
export type SnapshotRow = {
  key: string
  label: string
  value: ReactNode
  raw: unknown
  /** Hiện chữ mờ (mục tuỳ biến đã lưu trữ). */
  muted?: boolean
}

/** Một dòng của bảng so sánh; bên nào không có trường đó thì là `null`. */
export type CompareLine = { key: string; label: string; older: ReactNode; newer: ReactNode; changed: boolean }

/** Loại bản ghi có lịch sử version, đúng như đoạn đầu đường dẫn API của nó. */
export type EntityKind = 'places' | 'people' | 'items'

/**
 * Một version trong danh sách. Cùng dạng cho địa điểm, người, đồ đạc. `changedFields` là `null` ở
 * version tạo (chưa có gì để so).
 */
export type VersionSummary = Present<
  RegistrySchemas['PlaceVersionSummaryResponse'],
  'versionNo' | 'action' | 'changedBy' | 'changedAt'
>

/** Một version kèm nội dung của bản ghi lúc đó. */
export type Version<Snapshot> = VersionSummary & { snapshot: Snapshot }

export type ChangeAction = VersionSummary['action']
