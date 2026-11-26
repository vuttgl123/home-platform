import type { ImportReport, SheetReport } from './types'

/** Số dòng hiện mỗi lần; bảng dài hơn có nút "Hiện thêm". */
export const PAGE_SIZE = 200

/** "Người 2 (1 lỗi)", "Đồ đạc 3 (2 cảnh báo)", "Cư trú 5". */
export function sheetSummary(sheet: SheetReport): string {
  const base = `${sheet.name} ${sheet.rows}`
  if (sheet.errors > 0) return `${base} (${sheet.errors} lỗi)`
  if (sheet.warnings > 0) return `${base} (${sheet.warnings} cảnh báo)`
  return base
}

/** Số lỗi phải sửa trước khi nhập: dòng lỗi cộng lỗi cả file. */
export function blockingErrors(report: ImportReport): number {
  return report.totals.errors + report.fileErrors.length
}
