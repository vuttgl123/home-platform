import type { Present, RegistrySchemas } from '@/api'

type S = RegistrySchemas

/** Tên và `id` của một tổ tiên. */
export type PlaceRef = Present<S['PlaceRef'], 'id' | 'name'>

/** Một địa điểm trong danh sách (cây, kết quả tìm kiếm). */
export type PlaceSummary = Present<
  S['PlaceResponse'],
  'id' | 'placeTypeId' | 'name' | 'status' | 'depth' | 'version' | 'hasChildren'
>

/** Một kết quả tìm địa điểm: như `PlaceSummary`, kèm tổ tiên từ gốc tới cha. */
export type PlaceSearchResult = Omit<
  Present<S['PlaceSearchResponse'], 'id' | 'placeTypeId' | 'name' | 'status' | 'depth' | 'version' | 'hasChildren'>,
  'ancestors'
> & { ancestors: PlaceRef[] }

export type Address = Present<S['AddressResponse'], 'countryCode' | 'fullAddress'>

/** Địa chỉ dùng cho một địa điểm: của chính nó, hoặc của tổ tiên gần nhất có địa chỉ (`fromPlaceId`). */
export type EffectiveAddress = { fromPlaceId: string; address: Address }

/** Một địa điểm đầy đủ, kèm tổ tiên từ gốc tới cha. */
export type PlaceDetail = Omit<
  Present<
    S['PlaceDetailResponse'],
    | 'id'
    | 'placeTypeId'
    | 'path'
    | 'depth'
    | 'name'
    | 'status'
    | 'createdAt'
    | 'createdBy'
    | 'updatedAt'
    | 'updatedBy'
    | 'version'
    | 'occupantCount'
  >,
  'ancestors' | 'address' | 'effectiveAddress'
> & {
  ancestors: PlaceRef[]
  address: Address | null
  effectiveAddress: EffectiveAddress | null
}

export type PlaceStatus = PlaceSummary['status']
