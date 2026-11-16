import type { Present, RegistrySchemas } from '@/api'

type S = RegistrySchemas

/** Một dòng sở hữu: người X sở hữu (một phần) địa điểm Y từ ngày A tới ngày B. */
export type Ownership = Present<
  S['OwnershipResponse'],
  'id' | 'placeId' | 'placeName' | 'personId' | 'personFullName' | 'startDate' | 'status' | 'version'
>

export type OwnershipStatus = Ownership['status']
export type OwnershipBody = S['OwnershipCreateRequest']
export type OwnershipUpdateBody = S['OwnershipUpdateRequest']
export type OwnershipTransferBody = S['OwnershipTransferRequest']
