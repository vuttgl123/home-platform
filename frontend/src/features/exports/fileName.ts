import { todayInBusinessZone } from '@/lib/dates'

/** Một loại file tải về: cả không gian, đồ đạc, người, hay file mẫu để nhập. */
export type ExportKind = 'workspace' | 'items' | 'people' | 'template'

const KIND_SLUG: Record<ExportKind, string> = { workspace: 'toan-bo', items: 'do-dac', people: 'nguoi', template: 'mau-nhap' }

/**
 * Tên file khi lưu về máy: `<tên không gian bỏ dấu>-<loại>-<yyyy-MM-dd>.xlsx`. Máy chủ không biết tên
 * không gian nên giao diện tự đặt. Ngày là ngày ở Việt Nam, như tên dự phòng máy chủ gửi kèm.
 * @param workspaceName `undefined` hoặc tên không còn chữ số nào sau khi bỏ dấu thì dùng `khong-gian`
 */
export function exportFileName(workspaceName: string | undefined, kind: ExportKind, today: Date): string {
  return `${slug(workspaceName ?? '') || 'khong-gian'}-${KIND_SLUG[kind]}-${todayInBusinessZone(today)}.xlsx`
}

/** Bỏ dấu tiếng Việt, viết thường, mọi chuỗi ký tự khác `a-z0-9` thành một dấu `-`. */
function slug(text: string): string {
  return text
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[đĐ]/g, 'd')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
}
