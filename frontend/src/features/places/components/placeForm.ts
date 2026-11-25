import { z } from 'zod'

import { attributeDefaults, attributeErrors, type AttributeFormValues, type FieldDef, type PlaceType } from '@/features/catalog'

import type { PlaceDetail, PlaceStatus } from '../types'

export type PlaceFormValues = {
  placeTypeId: string
  name: string
  status: PlaceStatus
  /** Sức chứa như người dùng gõ; chuỗi rỗng là chưa đặt. */
  capacity: string
  description: string
  attributes: AttributeFormValues
}

/** Sức chứa hợp lệ: bỏ trống, hoặc số nguyên từ 0 đến 9999 (spec Đợt 6, L1). */
export const CAPACITY_ERROR = 'Nhập số nguyên từ 0 đến 9999'

/** Sức chứa sẽ gửi: `undefined` khi bỏ trống (PUT không có trường này là bỏ sức chứa). */
export function capacityValue(typed: string): number | undefined {
  return typed.trim() === '' ? undefined : Number(typed)
}

/** Kiểm tra form trước khi gửi: các trường cố định, và mục tuỳ biến theo `defs` của loại đang chọn. */
export function placeFormSchema(defs: FieldDef[]) {
  return z
    .object({
      placeTypeId: z.string().min(1, 'Chọn loại'),
      name: z.string().trim().min(1, 'Nhập tên').max(255, 'Tối đa 255 ký tự'),
      status: z.enum(['ACTIVE', 'INACTIVE']),
      capacity: z
        .string()
        .trim()
        .refine((typed) => typed === '' || (/^\d+$/.test(typed) && Number(typed) <= 9999), CAPACITY_ERROR),
      description: z.string().trim().max(10000, 'Tối đa 10000 ký tự'),
      attributes: z.record(z.string(), z.union([z.string(), z.boolean()])),
    })
    .superRefine((values, ctx) => {
      for (const [key, message] of Object.entries(attributeErrors(defs, values.attributes))) {
        ctx.addIssue({ code: 'custom', path: ['attributes', key], message })
      }
    })
}

/** Giá trị của form ứng với một địa điểm đã có. */
export function placeFormValues(place: PlaceDetail, defs: FieldDef[]): PlaceFormValues {
  return {
    placeTypeId: place.placeTypeId,
    name: place.name,
    status: place.status,
    capacity: place.capacity != null ? String(place.capacity) : '',
    description: place.description ?? '',
    attributes: attributeDefaults(defs, place.attributes),
  }
}

/**
 * Loại gợi ý cho một địa điểm mới: loại đang dùng đứng ngay sau loại của cha (toà nhà → tầng →
 * phòng), hoặc loại đầu tiên khi thêm ở cấp cao nhất hay khi loại của cha đã ở cuối.
 */
export function suggestedTypeId(types: PlaceType[], parentTypeId: string | null): string {
  const ordered = [...types].sort((a, b) => a.sortOrder - b.sortOrder)
  const activeTypes = ordered.filter((type) => type.status === 'ACTIVE')
  const parentType = ordered.find((type) => type.id === parentTypeId)
  const next = parentType && activeTypes.find((type) => type.sortOrder > parentType.sortOrder)
  return (next ?? activeTypes[0])?.id ?? ''
}
