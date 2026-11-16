import type { FieldDef } from './types'

/** Giá trị mục tuỳ biến trong một form: chữ trong ô nhập, hoặc `boolean` với mục BOOLEAN. */
export type AttributeFormValues = Record<string, string | boolean>

const NUMBER_TEXT = /^-?\d+(\.\d+)?$/
const MAX_TEXT_LENGTH = 1000

function active(defs: FieldDef[]): FieldDef[] {
  return defs.filter((def) => def.status === 'ACTIVE')
}

function isBlank(value: unknown): boolean {
  return value == null || (typeof value === 'string' && value.trim() === '')
}

/** Giá trị ban đầu của các ô mục tuỳ biến, từ giá trị đã lưu. Chỉ mục đang dùng có ô. */
export function attributeDefaults(defs: FieldDef[], stored: Record<string, unknown> | null): AttributeFormValues {
  const values: AttributeFormValues = {}
  for (const def of active(defs)) {
    const value = stored?.[def.fieldKey]
    values[def.fieldKey] = def.dataType === 'BOOLEAN' ? value === true : value == null ? '' : String(value)
  }
  return values
}

/**
 * Lỗi của các ô mục tuỳ biến, theo khoá, kiểm tra được trước khi gửi. Backend kiểm tra lại đầy đủ.
 * Mục đã lưu trữ không có ô nên không kiểm tra.
 */
export function attributeErrors(defs: FieldDef[], values: AttributeFormValues): Record<string, string> {
  const errors: Record<string, string> = {}
  for (const def of active(defs)) {
    const value = values[def.fieldKey]
    if (isBlank(value)) {
      if (def.required) errors[def.fieldKey] = 'Bắt buộc'
      continue
    }
    if (typeof value !== 'string') continue
    if (def.dataType === 'NUMBER' && !NUMBER_TEXT.test(value.trim())) {
      errors[def.fieldKey] = 'Phải là số'
    } else if (def.dataType === 'TEXT' && value.length > MAX_TEXT_LENGTH) {
      errors[def.fieldKey] = 'Tối đa 1000 ký tự'
    }
  }
  return errors
}

/**
 * `attributes` gửi lên backend.
 *
 * - Mục đang dùng: lấy từ form; ô trống thì bỏ khoá, NUMBER thành số, BOOLEAN giữ `boolean`.
 * - Mục đã lưu trữ: giữ giá trị đã lưu (form không có ô cho nó).
 * - Khoá không thuộc mục nào trong `defs` (ví dụ của loại cũ sau khi đổi loại): bỏ, vì backend
 *   từ chối khoá không có mục.
 *
 * @returns `null` nếu không còn khoá nào
 */
export function attributePayload(
  defs: FieldDef[],
  values: AttributeFormValues,
  stored: Record<string, unknown> | null,
): Record<string, unknown> | null {
  const payload: Record<string, unknown> = {}
  for (const def of defs) {
    const key = def.fieldKey
    if (def.status === 'ARCHIVED') {
      if (!isBlank(stored?.[key])) payload[key] = stored![key]
      continue
    }
    const value = values[key]
    if (typeof value === 'boolean') {
      payload[key] = value
    } else if (!isBlank(value)) {
      payload[key] = def.dataType === 'NUMBER' ? Number(value!.trim()) : value!.trim()
    }
  }
  return Object.keys(payload).length > 0 ? payload : null
}

/** Nhãn của các trường một bản ghi, thêm nhãn của từng mục tuỳ biến dưới khoá `attributes.<khoá>`. */
export function fieldLabelsWith(base: Record<string, string>, defs: FieldDef[]): Record<string, string> {
  return { ...base, ...Object.fromEntries(defs.map((def) => [`attributes.${def.fieldKey}`, def.label])) }
}
