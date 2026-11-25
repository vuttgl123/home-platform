import type { Present, RegistrySchemas } from '@/api'

type S = RegistrySchemas

/** Một người trong danh sách. */
export type PersonSummary = Present<S['PersonResponse'], 'id' | 'fullName' | 'version'>

/** Một dòng cư trú: người X có quan hệ R với địa điểm Y từ ngày A tới ngày B. */
export type Occupancy = Present<
  S['OccupancyResponse'],
  'id' | 'placeId' | 'placeName' | 'personId' | 'personFullName' | 'relation' | 'startDate' | 'status' | 'version'
>

/** Một người đầy đủ, kèm những nơi đang ở. */
export type PersonDetail = Omit<
  Present<S['PersonDetailResponse'], 'id' | 'fullName' | 'version' | 'createdAt' | 'updatedAt'>,
  'currentOccupancies'
> & { currentOccupancies: Occupancy[] }

export type Relation = Occupancy['relation']
export type OccupancyStatus = Occupancy['status']
export type Gender = NonNullable<PersonDetail['gender']>
export type PersonSearchField = 'name' | 'phone' | 'idNumber'

/** Sức chứa và đỉnh của một địa điểm trong một khoảng ngày (spec Đợt 6, L3). */
export type CapacityCheck = Present<S['CapacityCheckResponse'], 'peak'>
/** Khoảng ngày của một dòng cư trú đang nhập; `excludeOccupancyId` là dòng đang sửa. */
export type CapacityRange = { startDate: string; endDate?: string; excludeOccupancyId?: string }

export type PersonBody = S['PersonRequest']
export type PersonUpdateBody = S['PersonUpdateRequest']
/** Mọi lần ở của một địa điểm hay một người, cho dòng thời gian. */
export type OccupancyTimelineData = { rows: Occupancy[]; truncated: boolean }

export type OccupancyBody = S['OccupancyCreateRequest']
export type OccupancyUpdateBody = S['OccupancyUpdateRequest']
