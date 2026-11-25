import type { Present, RegistrySchemas } from '@/api'

type S = RegistrySchemas

/** Một dòng nhật ký bảo trì của món đồ: một lần sửa chữa hay bảo trì định kỳ. */
export type MaintenanceEntry = Present<
  S['MaintenanceResponse'],
  'id' | 'itemId' | 'kind' | 'doneOn' | 'work' | 'version' | 'createdAt' | 'updatedAt'
>

export type MaintenanceKind = MaintenanceEntry['kind']
export type MaintenanceCreateBody = S['MaintenanceCreateRequest']
export type MaintenanceUpdateBody = S['MaintenanceUpdateRequest']
