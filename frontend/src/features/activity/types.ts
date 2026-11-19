import type { Present, RegistrySchemas } from '@/api'
import type { ChangeAction } from '@/features/history'

/** Loại bản ghi trong hoạt động gần đây. */
export type EntityType = 'PLACE' | 'PERSON' | 'ITEM'

/**
 * Bộ lọc của trang Hoạt động, đúng như trên URL. `null`/`false` là không lọc.
 * `from`, `to` là ngày dạng `yyyy-MM-dd`, cả hai tính hết ngày.
 */
export type ActivityFilter = {
  type: EntityType | null
  action: ChangeAction | null
  /** Chỉ việc người dùng đang chọn làm. */
  mine: boolean
  from: string | null
  to: string | null
}

/** Một dòng hoạt động: một version của một địa điểm, người hay món đồ, kèm tên hiện tại của nó. */
export type ActivityEntry = Omit<
  // `changedFields` là null ở dòng tạo.
  Present<RegistrySchemas['ActivityResponse'], 'entityType' | 'entityId' | 'versionNo' | 'action' | 'changedBy' | 'changedAt'>,
  'entityType'
> & { entityType: EntityType }

/** Một bản ghi đang bị xoá, trong thùng rác. */
export type TrashEntry = Omit<
  Present<RegistrySchemas['TrashResponse'], 'entityType' | 'id' | 'name' | 'deletedAt' | 'deletedBy' | 'version'>,
  'entityType'
> & { entityType: EntityType }
