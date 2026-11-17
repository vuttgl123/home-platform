// Dữ liệu mẫu cho test, đúng dạng backend trả về: đủ mọi khoá, trường trống là null.
import type { FieldDef, ItemCategory, PlaceType } from '@/features/catalog'
import type { ItemDetail, ItemSummary } from '@/features/items'
import type { Overview } from '@/features/overview'
import type { Ownership } from '@/features/ownership'
import type { Occupancy, PersonDetail, PersonSummary } from '@/features/people'
import type { PlaceDetail, PlaceSearchResult, PlaceSummary } from '@/features/places'
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
    ...overrides,
  }
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
    sortOrder: 10,
    status: 'ACTIVE',
    createdAt: AT,
    updatedAt: AT,
    ...overrides,
  }
}

export function person(overrides: Partial<PersonSummary> = {}): PersonSummary {
  return { id: '55', fullName: 'Nguyễn An', phone: null, idType: null, idNumber: null, gender: null, version: 1, ...overrides }
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
