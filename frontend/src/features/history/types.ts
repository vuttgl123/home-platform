import type { Present, RegistrySchemas } from '@/api'

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
