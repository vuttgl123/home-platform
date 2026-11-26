// Dữ liệu mẫu cho test, đúng dạng backend trả về: đủ mọi khoá, trường trống là null.
import type { FieldDef, ItemCategory, PlaceType } from '@/features/catalog'
import type { ImportReport, RowReport, SheetReport } from '@/features/imports'
import type { ItemDetail, ItemSummary } from '@/features/items'
import type { Overview } from '@/features/overview'
import type { Ownership } from '@/features/ownership'
import type { Occupancy, PersonDetail, PersonSummary } from '@/features/people'
import type { PlaceDetail, PlaceSearchResult, PlaceSummary } from '@/features/places'
import type { Todo, TodoSummary } from '@/features/todos'
import type { User } from '@/features/users'
import type { Workspace } from '@/features/workspaces'

const AT = '2026-11-01T02:00:00Z'

export function page<T>(items: T[], nextCursor: string | null = null) {
  return { items, nextCursor }
}

/** Thân của một response lỗi dạng Problem Details. */
export function problem(status: number, detail: string, errors?: { field: string | null; message: string }[]) {
  return { status, title: 'Error', detail, instance: '/', ...(errors ? { errors } : {}) }
}

/** Số liệu trang tổng quan của một không gian đang có dữ liệu. */
export function overview(overrides: Partial<Overview> = {}): Overview {
  return {
    places: 42,
    placeTypes: [
      { typeId: '500', name: 'Phòng', total: 20, occupied: 12 },
      { typeId: '501', name: 'Căn hộ', total: 6, occupied: 5 },
    ],
    people: 25,
    peopleLiving: 18,
    itemsByStatus: { IN_USE: 230, BROKEN: 3, STORED: 12, REMOVED: 40 },
    missingCodes: { places: 4, items: 2 },
    activityByDay: activityDays(),
    endingSoon: { total: 0, rows: [] },
    ...overrides,
  }
}

/**
 * 14 ngày hoạt động liên tiếp, kết thúc ngày 2026-11-20; `counts` đặt số cho vài ngày theo chỉ số
 * (0 là ngày cũ nhất), các ngày khác là 0.
 */
export function activityDays(counts: Record<number, Partial<Record<'created' | 'updated' | 'deleted' | 'restored', number>>> = {}) {
  return Array.from({ length: 14 }, (_, index) => ({
    date: `2026-11-${String(7 + index).padStart(2, '0')}`,
    created: 0,
    updated: 0,
    deleted: 0,
    restored: 0,
    ...counts[index],
  }))
}

/** Số liệu của một không gian chưa có gì: trang chủ giữ lời mời "chọn một địa điểm". */
export function emptyOverview(): Overview {
  return overview({
    places: 0,
    placeTypes: [],
    people: 0,
    peopleLiving: 0,
    itemsByStatus: { IN_USE: 0, BROKEN: 0, STORED: 0, REMOVED: 0 },
    missingCodes: { places: 0, items: 0 },
  })
}

export function user(overrides: Partial<User> = {}): User {
  return {
    id: '42',
    email: 'an@example.com',
    displayName: 'An',
    status: 'ACTIVE',
    createdAt: AT,
    updatedAt: AT,
    ...overrides,
  }
}

export function workspace(overrides: Partial<Workspace> = {}): Workspace {
  return {
    id: '7',
    name: 'Nhà An',
    status: 'ACTIVE',
    ownerUserId: '42',
    role: 'OWNER',
    createdAt: AT,
    updatedAt: AT,
    ...overrides,
  }
}

export function placeType(overrides: Partial<PlaceType> = {}): PlaceType {
  return {
    id: '100',
    code: 'BUILDING',
    name: 'Toà nhà',
    description: null,
    sortOrder: 10,
    status: 'ACTIVE',
    createdAt: AT,
    updatedAt: AT,
    ...overrides,
  }
}

export function place(overrides: Partial<PlaceSummary> = {}): PlaceSummary {
  return {
    id: '1',
    parentId: null,
    placeTypeId: '100',
    code: null,
    name: 'Toà A',
    status: 'ACTIVE',
    depth: 0,
    coverFileId: null,
    version: 1,
    hasChildren: false,
    ...overrides,
  }
}

export function searchResult(overrides: Partial<PlaceSearchResult> = {}): PlaceSearchResult {
  return { ...place(), ancestors: [], ...overrides }
}

export function placeDetail(overrides: Partial<PlaceDetail> = {}): PlaceDetail {
  return {
    id: '1',
    parentId: null,
    placeTypeId: '100',
    path: '/1/',
    depth: 0,
    code: null,
    name: 'Toà A',
    description: null,
    status: 'ACTIVE',
    capacity: null,
    occupantCount: 0,
    attributes: null,
    coverFileId: null,
    createdAt: AT,
    createdBy: '42',
    updatedAt: AT,
    updatedBy: '42',
    version: 1,
    deletedAt: null,
    ancestors: [],
    address: null,
    effectiveAddress: null,
    ...overrides,
  }
}

export function fieldDef(overrides: Partial<FieldDef> = {}): FieldDef {
  return {
    id: '500',
    target: 'PLACE',
    typeId: '100',
    fieldKey: 'area_m2',
    label: 'Diện tích (m²)',
    dataType: 'NUMBER',
    options: null,
    required: false,
    remindDays: null,
    sortOrder: 10,
    status: 'ACTIVE',
    createdAt: AT,
    updatedAt: AT,
    ...overrides,
  }
}

export function person(overrides: Partial<PersonSummary> = {}): PersonSummary {
  return { id: '55', fullName: 'Nguyễn An', phone: null, idType: null, idNumber: null, gender: null, avatarFileId: null, version: 1, ...overrides }
}

export function personDetail(overrides: Partial<PersonDetail> = {}): PersonDetail {
  return {
    id: '55',
    fullName: 'Nguyễn An',
    phone: null,
    email: null,
    idType: null,
    idNumber: null,
    dateOfBirth: null,
    gender: null,
    note: null,
    attributes: null,
    avatarFileId: null,
    createdAt: AT,
    createdBy: '42',
    updatedAt: AT,
    updatedBy: '42',
    version: 1,
    deletedAt: null,
    currentOccupancies: [],
    ...overrides,
  }
}

export function occupancy(overrides: Partial<Occupancy> = {}): Occupancy {
  return {
    id: '700',
    placeId: '301',
    placeName: 'P301',
    personId: '55',
    personFullName: 'Nguyễn An',
    relation: 'RESIDENT',
    startDate: '2026-01-01',
    endDate: null,
    status: 'CURRENT',
    note: null,
    createdAt: AT,
    createdBy: '42',
    updatedAt: AT,
    updatedBy: '42',
    version: 1,
    ...overrides,
  }
}

export function itemCategory(overrides: Partial<ItemCategory> = {}): ItemCategory {
  return { id: '600', name: 'Nội thất', sortOrder: 10, status: 'ACTIVE', createdAt: AT, updatedAt: AT, ...overrides }
}

export function itemDetail(overrides: Partial<ItemDetail> = {}): ItemDetail {
  return {
    id: '900',
    placeId: '301',
    placeName: 'P301',
    categoryId: null,
    categoryName: null,
    ownerPersonId: null,
    ownerName: null,
    code: null,
    name: 'Tủ lạnh',
    quantity: 1,
    unit: null,
    status: 'IN_USE',
    placedAt: null,
    warrantyUntil: null,
    serviceEveryMonths: null,
    nextServiceOn: null,
    removedAt: null,
    replacedByItemId: null,
    description: null,
    attributes: null,
    coverFileId: null,
    createdAt: AT,
    createdBy: '42',
    updatedAt: AT,
    updatedBy: '42',
    version: 1,
    deletedAt: null,
    ...overrides,
  }
}

export function item(overrides: Partial<ItemSummary> = {}): ItemSummary {
  const { id, placeId, placeName, categoryId, categoryName, ownerPersonId, ownerName, code, name, quantity, unit, status, removedAt, version } =
    itemDetail(overrides as Partial<ItemDetail>)
  return { id, placeId, placeName, categoryId, categoryName, ownerPersonId, ownerName, code, name, quantity, unit, status, removedAt, version }
}

export function ownership(overrides: Partial<Ownership> = {}): Ownership {
  return {
    id: '1',
    placeId: '301',
    placeName: 'P301',
    personId: '55',
    personFullName: 'Nguyễn An',
    sharePercent: null,
    startDate: '2026-01-01',
    endDate: null,
    status: 'CURRENT',
    documentNo: null,
    note: null,
    createdAt: AT,
    createdBy: '42',
    updatedAt: AT,
    updatedBy: '42',
    version: 1,
    ...overrides,
  }
}

/** Một việc cần làm: mặc định món "Tủ lạnh" hỏng từ hôm 20/11. */
export function todo(overrides: Partial<Todo> = {}): Todo {
  const key = overrides.key ?? `${overrides.kind ?? 'BROKEN'}:${overrides.subjectId ?? '900'}`
  return {
    id: key,
    key,
    kind: 'BROKEN',
    dueOn: '2026-11-20',
    since: '2026-11-20T03:00:00Z',
    isNew: false,
    subjectType: 'ITEM',
    subjectId: '900',
    subjectName: 'Tủ lạnh',
    subjectCode: null,
    context: 'P301',
    ...overrides,
  }
}

/** Tóm tắt việc cần làm của một không gian chưa có việc gì. */
export function emptyTodoSummary(): TodoSummary {
  return { total: 0, byKind: { BROKEN: 0, WARRANTY: 0, SERVICE: 0, MOVE_OUT: 0, CUSTOM_DATE: 0 }, newCount: 0, latest: [] }
}

/** Một dòng của báo cáo nhập; mặc định là dòng 2 của sheet Người, không có lời nhắn. */
export function rowReport(overrides: Partial<RowReport> = {}): RowReport {
  return { row: 2, label: 'Nguyễn An', code: null, status: 'OK', messages: [], ...overrides }
}

/** Một sheet của báo cáo nhập; số dòng, lỗi, cảnh báo đếm từ `items`. */
export function sheetReport(overrides: Partial<SheetReport> = {}): SheetReport {
  const items = overrides.items ?? [rowReport()]
  return {
    sheet: 'PERSON',
    name: 'Người',
    rows: items.length,
    errors: items.filter((item) => item.status === 'ERROR').length,
    warnings: items.filter((item) => item.status === 'WARNING').length,
    items,
    ...overrides,
  }
}

/** Báo cáo của một lần xem trước; tổng đếm từ `sheets`. */
export function importReport(overrides: Partial<ImportReport> = {}): ImportReport {
  const sheets = overrides.sheets ?? [sheetReport()]
  const sum = (key: 'rows' | 'errors' | 'warnings') => sheets.reduce((total, sheet) => total + sheet[key], 0)
  return {
    fileName: 'nha.xlsx',
    committed: false,
    fileErrors: [],
    notices: [],
    totals: { rows: sum('rows'), errors: sum('errors'), warnings: sum('warnings') },
    sheets,
    ...overrides,
  }
}
