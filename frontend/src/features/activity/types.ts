import type { Present, RegistrySchemas } from '@/api'

/** Loại bản ghi trong hoạt động gần đây. */
export type EntityType = 'PLACE' | 'PERSON' | 'ITEM'

/** Một dòng hoạt động: một version của một địa điểm, người hay món đồ, kèm tên hiện tại của nó. */
export type ActivityEntry = Omit<
  // `changedFields` là null ở dòng tạo.
  Present<RegistrySchemas['ActivityResponse'], 'entityType' | 'entityId' | 'versionNo' | 'action' | 'changedBy' | 'changedAt'>,
  'entityType'
> & { entityType: EntityType }
