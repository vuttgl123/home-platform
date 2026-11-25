import { z } from 'zod'

import { todayInBusinessZone } from '@/lib/dates'

import type { MaintenanceKind } from '../types'

export type MaintenanceFormValues = {
  kind: MaintenanceKind
  doneOn: string
  work: string
  doneBy: string
  note: string
  markFixed: boolean
}

export const STALE_ENTRY = 'Dòng này vừa được người khác sửa. Đóng hộp thoại và mở lại để làm trên bản mới.'

/** Kiểm tra trước khi gửi, cùng giới hạn với backend (spec Đợt 8, L1). Ngày làm không sau hôm nay. */
export const maintenanceFormSchema = z.object({
  kind: z.enum(['REPAIR', 'SERVICE']),
  doneOn: z
    .string()
    .min(1, 'Chọn ngày làm')
    .refine((value) => value <= todayInBusinessZone(), 'Không được sau hôm nay'),
  work: z.string().trim().min(1, 'Nhập việc đã làm').max(500, 'Tối đa 500 ký tự'),
  doneBy: z.string().trim().max(150, 'Tối đa 150 ký tự'),
  note: z.string().trim().max(1000, 'Tối đa 1000 ký tự'),
  markFixed: z.boolean(),
})
