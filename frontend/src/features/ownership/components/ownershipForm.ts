import { z } from 'zod'

import { formatDate } from '@/lib/format'

export type OwnershipFormValues = { sharePercent: string; startDate: string; endDate: string; documentNo: string; note: string }

const SHARE = /^\d{1,3}([.,]\d{1,2})?$/

export const SHARE_ERROR = 'Từ 0,01 đến 100, tối đa hai chữ số thập phân'

/** Tỷ lệ gõ tay: trống là không ghi; nhận cả dấu phẩy và dấu chấm thập phân. */
export function parseShare(text: string): number | null | 'invalid' {
  const trimmed = text.trim()
  if (trimmed === '') return null
  if (!SHARE.test(trimmed)) return 'invalid'
  const value = Number(trimmed.replace(',', '.'))
  return value > 0 && value <= 100 ? value : 'invalid'
}

/** `50` thành `50%`, `33.33` thành `33,33%`; không ghi tỷ lệ thì `—`. */
export function formatShare(share: number | null | undefined): string {
  if (share == null) return '—'
  return `${String(Number(share.toFixed(2))).replace('.', ',')}%`
}

export const ownershipFormSchema = z
  .object({
    sharePercent: z.string().refine((value) => parseShare(value) !== 'invalid', SHARE_ERROR),
    startDate: z.string().min(1, 'Chọn ngày bắt đầu'),
    endDate: z.string(),
    documentNo: z.string().trim().max(100, 'Tối đa 100 ký tự'),
    note: z.string().trim().max(500, 'Tối đa 500 ký tự'),
  })
  .superRefine((values, ctx) => {
    if (values.endDate !== '' && values.startDate !== '' && values.endDate < values.startDate) {
      ctx.addIssue({ code: 'custom', path: ['endDate'], message: `Không được trước ngày bắt đầu (${formatDate(values.startDate)})` })
    }
  })

/** Dòng sở hữu vừa được người khác sửa: chưa có trang chi tiết để tải lại, nên mời mở lại. */
export const STALE_ROW = 'Dòng này vừa được người khác sửa. Đóng hộp thoại và mở lại để làm trên bản mới.'
