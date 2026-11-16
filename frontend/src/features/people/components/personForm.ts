import { z } from 'zod'

import { attributeDefaults, attributeErrors, attributePayload, type AttributeFormValues, type FieldDef } from '@/features/catalog'
import { todayInBusinessZone } from '@/lib/dates'

import type { Gender, PersonBody, PersonDetail } from '../types'

export type PersonFormValues = {
  fullName: string
  phone: string
  email: string
  gender: Gender | ''
  dateOfBirth: string
  idType: string
  idNumber: string
  note: string
  attributes: AttributeFormValues
}

/** Kiểm tra trước khi gửi; backend kiểm tra lại đầy đủ. */
export function personFormSchema(defs: FieldDef[]) {
  return z
    .object({
      fullName: z.string().trim().min(1, 'Nhập họ tên').max(150, 'Tối đa 150 ký tự'),
      phone: z.string().trim().max(20, 'Tối đa 20 ký tự'),
      email: z
        .string()
        .trim()
        .max(255, 'Tối đa 255 ký tự')
        .refine((value) => value === '' || /^\S+@\S+\.\S+$/.test(value), 'Email không hợp lệ'),
      gender: z.enum(['MALE', 'FEMALE', 'OTHER', '']),
      dateOfBirth: z
        .string()
        .refine((value) => value === '' || value <= todayInBusinessZone(), 'Ngày sinh không được ở tương lai'),
      idType: z.string().trim().max(20, 'Tối đa 20 ký tự'),
      idNumber: z.string().trim().max(30, 'Tối đa 30 ký tự'),
      note: z.string().trim().max(10000, 'Tối đa 10000 ký tự'),
      attributes: z.record(z.string(), z.union([z.string(), z.boolean()])),
    })
    .superRefine((values, ctx) => {
      for (const [key, message] of Object.entries(attributeErrors(defs, values.attributes))) {
        ctx.addIssue({ code: 'custom', path: ['attributes', key], message })
      }
    })
}

export function emptyPersonForm(name = ''): PersonFormValues {
  return {
    fullName: name,
    phone: '',
    email: '',
    gender: '',
    dateOfBirth: '',
    idType: '',
    idNumber: '',
    note: '',
    attributes: {},
  }
}

export function personFormValues(person: PersonDetail, defs: FieldDef[]): PersonFormValues {
  return {
    fullName: person.fullName,
    phone: person.phone ?? '',
    email: person.email ?? '',
    gender: person.gender ?? '',
    dateOfBirth: person.dateOfBirth ?? '',
    idType: person.idType ?? '',
    idNumber: person.idNumber ?? '',
    note: person.note ?? '',
    attributes: attributeDefaults(defs, person.attributes),
  }
}

/** Thân gửi lên; trường trống không gửi. Khi sửa, nơi gọi thêm `version` và `avatarFileId`. */
export function personPayload(values: PersonFormValues, defs: FieldDef[], stored: PersonDetail | null): PersonBody {
  const or = (value: string) => value || undefined
  return {
    fullName: values.fullName,
    phone: or(values.phone),
    email: or(values.email),
    gender: or(values.gender) as Gender | undefined,
    dateOfBirth: or(values.dateOfBirth),
    idType: or(values.idType),
    idNumber: or(values.idNumber),
    note: or(values.note),
    attributes: attributePayload(defs, values.attributes, stored?.attributes ?? null) ?? undefined,
  }
}

/** `id` của người đã mang giấy tờ này, đọc từ thông báo 409 của backend ("Person 77 already has this document"). */
export function duplicateDocumentOwner(detail: string): string | null {
  return /^Person (\d+) already has this document$/.exec(detail)?.[1] ?? null
}
