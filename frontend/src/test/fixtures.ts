// Dữ liệu mẫu cho test, đúng dạng backend trả về: đủ mọi khoá, trường trống là null.
import type { FieldDef, PlaceType } from '@/features/catalog'
import type { PlaceDetail, PlaceSummary } from '@/features/places'
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
    ...overrides,
  }
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
