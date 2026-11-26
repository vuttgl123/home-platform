import type { Present, RegistrySchemas } from '@/api'

type S = RegistrySchemas

/** Một lời nhắn về một dòng; `column` là tiêu đề cột như trong mẫu, `null` là của cả dòng. */
export type RowMessage = Present<S['RowMessage'], 'level' | 'text'>

/** Kết quả một dòng của file: `label` là hồ sơ ("mã · tên", họ tên, "người · địa điểm"). */
export type RowReport = Omit<Present<S['RowReport'], 'row' | 'status'>, 'messages'> & { messages: RowMessage[] }

export type SheetReport = Omit<Present<S['SheetReport'], 'sheet' | 'name' | 'rows' | 'errors' | 'warnings'>, 'items'> & {
  items: RowReport[]
}

export type ImportTotals = Present<S['Totals'], 'rows' | 'errors' | 'warnings'>

/**
 * Báo cáo của một lần xem trước hay nhập (spec Đợt 9, mục 4.2). `errors`, `warnings` đếm dòng, không
 * đếm lời nhắn. `committed` chỉ `true` khi dữ liệu đã được lưu.
 */
export type ImportReport = Omit<
  Present<S['ImportReport'], 'fileName' | 'committed' | 'fileErrors' | 'notices'>,
  'totals' | 'sheets'
> & { totals: ImportTotals; sheets: SheetReport[] }
