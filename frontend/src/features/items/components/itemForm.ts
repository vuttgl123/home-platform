import { z } from 'zod'

import { formatDate } from '@/lib/format'
import { attributeDefaults, attributeErrors, attributePayload, type AttributeFormValues, type FieldDef } from '@/features/catalog'

import type { ItemBody, ItemDetail, ItemStatus } from '../types'

export type ItemFormValues = {
  categoryId: string
  name: string
  code: string
  quantity: string
  unit: string
  status: Exclude<ItemStatus, 'REMOVED'>
  placedAt: string
  description: string
  attributes: AttributeFormValues
}

const WHOLE_NUMBER = /^\d+$/

/** Kiểm tra trước khi gửi: các trường cố định, và mục tuỳ biến theo `defs` của nhóm đang chọn. */
export function itemFormSchema(defs: FieldDef[]) {
  return z
    .object({
      categoryId: z.string(),
      name: z.string().trim().min(1, 'Nhập tên').max(255, 'Tối đa 255 ký tự'),
      code: z.string().trim().max(50, 'Tối đa 50 ký tự'),
      quantity: z
        .string()
        .trim()
        .refine((value) => WHOLE_NUMBER.test(value) && Number(value) >= 1, 'Số nguyên từ 1 trở lên'),
      unit: z.string().trim().max(20, 'Tối đa 20 ký tự'),
      status: z.enum(['IN_USE', 'BROKEN', 'STORED']),
      placedAt: z.string(),
      description: z.string().trim().max(10000, 'Tối đa 10000 ký tự'),
      attributes: z.record(z.string(), z.union([z.string(), z.boolean()])),
    })
    .superRefine((values, ctx) => {
      for (const [key, message] of Object.entries(attributeErrors(defs, values.attributes))) {
        ctx.addIssue({ code: 'custom', path: ['attributes', key], message })
      }
    })
}

export function emptyItemForm(): ItemFormValues {
  return {
    categoryId: '',
    name: '',
    code: '',
    quantity: '1',
    unit: '',
    status: 'IN_USE',
    placedAt: '',
    description: '',
    attributes: {},
  }
}

export function itemFormValues(item: ItemDetail, defs: FieldDef[]): ItemFormValues {
  return {
    categoryId: item.categoryId ?? '',
    name: item.name,
    code: item.code ?? '',
    quantity: String(item.quantity),
    unit: item.unit ?? '',
    status: item.status === 'REMOVED' ? 'IN_USE' : item.status,
    placedAt: item.placedAt ?? '',
    description: item.description ?? '',
    attributes: attributeDefaults(defs, item.attributes),
  }
}

/** Thân gửi lên (trừ `placeId`, `version`, `coverFileId` do nơi gọi thêm); trường trống không gửi. */
export function itemPayload(
  values: ItemFormValues,
  defs: FieldDef[],
  stored: ItemDetail | null,
): Omit<ItemBody, 'placeId'> & { status: ItemFormValues['status'] } {
  const or = (value: string) => value || undefined
  return {
    categoryId: or(values.categoryId),
    code: or(values.code),
    name: values.name,
    quantity: Number(values.quantity),
    unit: or(values.unit),
    status: values.status,
    placedAt: or(values.placedAt),
    description: or(values.description),
    attributes: attributePayload(defs, values.attributes, stored?.attributes ?? null) ?? undefined,
  }
}

/** Lỗi của một ngày gỡ: trống, hoặc trước ngày đặt. `null` nếu hợp lệ. */
export function removalDateError(date: string, item: ItemDetail): string | null {
  if (date === '') return 'Chọn ngày gỡ'
  if (item.placedAt && date < item.placedAt) return `Không được trước ngày đặt (${formatDate(item.placedAt)})`
  return null
}
