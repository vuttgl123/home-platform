import type { Present, RegistrySchemas } from '@/api'
import type { ItemStatus } from '@/features/items'

/** Một loại địa điểm có ít nhất một chỗ: tổng số chỗ, và số chỗ có người đang ở hôm nay. */
export type PlaceTypeCount = Present<RegistrySchemas['PlaceTypeCount'], 'typeId' | 'name' | 'total' | 'occupied'>

/** Số liệu của trang tổng quan; mọi con số chỉ tính bản ghi chưa xoá. */
export type Overview = {
  places: number
  placeTypes: PlaceTypeCount[]
  /** Tổng số hồ sơ người. */
  people: number
  /** Số người khác nhau đang ở một địa điểm hôm nay. */
  peopleLiving: number
  /** Đủ bốn trạng thái; trạng thái không có món nào là 0. */
  itemsByStatus: Record<ItemStatus, number>
  /** Số bản ghi chưa có mã, như `GET /codes/status`. */
  missingCodes: { places: number; items: number }
}
