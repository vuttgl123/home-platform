import type { Present, RegistrySchemas } from '@/api'

type S = RegistrySchemas

/** Một địa điểm còn chỗ hôm nay (spec Đợt 6, L4); `id` là `placeId`, để dùng chung bộ phân trang. */
export type Vacancy = Omit<
  Present<S['VacancyResponse'], 'placeId' | 'name' | 'placeTypeId' | 'status' | 'occupantCount'>,
  'ancestors'
> & { id: string; ancestors: { id: string; name: string }[] }

/** Bộ lọc của trang Chỗ trống; `null` là không lọc. */
export type VacancyFilter = { placeTypeId: string | null; rootId: string | null }
