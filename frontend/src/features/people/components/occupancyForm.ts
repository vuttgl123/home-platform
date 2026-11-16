import { z } from 'zod'

import { formatDate } from '@/lib/format'

import type { Relation } from '../types'

export type OccupancyFormValues = { relation: Relation; startDate: string; endDate: string; note: string }

export const occupancyFormSchema = z
  .object({
    relation: z.enum(['OWNER', 'RESIDENT', 'GUEST', 'STAFF', 'OTHER']),
    startDate: z.string().min(1, 'Chọn ngày bắt đầu'),
    endDate: z.string(),
    note: z.string().trim().max(500, 'Tối đa 500 ký tự'),
  })
  .superRefine((values, ctx) => {
    if (values.endDate !== '' && values.startDate !== '' && values.endDate < values.startDate) {
      ctx.addIssue({ code: 'custom', path: ['endDate'], message: notBefore(values.startDate) })
    }
  })

/** Thông báo cho một ngày kết thúc đứng trước ngày bắt đầu. */
export function notBefore(startDate: string): string {
  return `Không được trước ngày bắt đầu (${formatDate(startDate)})`
}

/** Dòng cư trú vừa bị người khác sửa: chưa có trang chi tiết để tải lại, nên mời mở lại. */
export const STALE_ROW = 'Dòng này vừa được người khác sửa. Đóng hộp thoại và mở lại để làm trên bản mới.'
