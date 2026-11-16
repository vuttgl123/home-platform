import type { Present, RegistrySchemas } from '@/api'

type S = RegistrySchemas

/** Một món đồ trong danh sách. */
export type ItemSummary = Present<S['ItemResponse'], 'id' | 'placeId' | 'placeName' | 'name' | 'quantity' | 'status' | 'version'>

/** Một món đồ đầy đủ. `removedAt` có giá trị nghĩa là đã gỡ; `replacedByItemId` là món đã thay nó. */
export type ItemDetail = Present<
  S['ItemDetailResponse'],
  'id' | 'placeId' | 'placeName' | 'name' | 'quantity' | 'status' | 'version' | 'createdAt' | 'updatedAt'
>

export type ItemStatus = ItemSummary['status']
export type ItemBody = S['ItemRequest']
export type ItemUpdateBody = S['ItemUpdateRequest']
