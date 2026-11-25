import type { ActivityType, EntityType } from './types'

export const ENTITY_LABEL: Record<ActivityType, string> = { PLACE: 'Địa điểm', PERSON: 'Người', ITEM: 'Đồ đạc', MAINTENANCE: 'Bảo trì' }

/** Đoạn đường dẫn trang của từng loại bản ghi. */
export const ENTITY_PATH: Record<EntityType, string> = { PLACE: 'places', PERSON: 'people', ITEM: 'items' }

/** Nhãn của các trường thường gặp trong `changedFields`, chung cho ba loại bản ghi. */
export const FIELD_LABEL: Record<string, string> = {
  name: 'Tên',
  fullName: 'Họ tên',
  code: 'Mã',
  description: 'Mô tả',
  capacity: 'Sức chứa',
  status: 'Trạng thái',
  placeTypeId: 'Loại',
  address: 'Địa chỉ',
  attributes: 'Mục tuỳ biến',
  phone: 'Số điện thoại',
  email: 'Email',
  idType: 'Loại giấy tờ',
  idNumber: 'Số giấy tờ',
  dateOfBirth: 'Ngày sinh',
  gender: 'Giới tính',
  note: 'Ghi chú',
  categoryId: 'Nhóm',
  quantity: 'Số lượng',
  unit: 'Đơn vị',
  placedAt: 'Ngày đặt',
  removedAt: 'Ngày gỡ',
  warrantyUntil: 'Hết bảo hành',
  serviceEveryMonths: 'Bảo trì (tháng)',
  placeId: 'Chỗ đặt',
  replacedByItemId: 'Món thay',
  ownerPersonId: 'Chủ',
  parentId: 'Chỗ chứa',
  coverFileId: 'Ảnh bìa',
  avatarFileId: 'Ảnh đại diện',
  deletedAt: 'Xoá',
}
