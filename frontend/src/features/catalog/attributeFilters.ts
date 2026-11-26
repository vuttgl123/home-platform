import type { FieldDef } from './types'

/** Phép so của một điều kiện lọc theo mục tuỳ biến (spec Đợt 11, S2). */
export type AttributeOp = 'has' | 'eq' | 'gte' | 'lte' | 'set' | 'unset'

/** Một điều kiện; `value` rỗng khi phép không cần giá trị, hay khi người dùng chưa nhập. */
export type AttributeCondition = { key: string; op: AttributeOp; value: string }

export const MAX_CONDITIONS = 3

/** Phép hợp với từng kiểu, theo thứ tự hiện trong ô chọn; phép đầu là phép mặc định. */
export const OPS_BY_TYPE: Record<FieldDef['dataType'], AttributeOp[]> = {
  TEXT: ['has', 'eq', 'set', 'unset'],
  NUMBER: ['eq', 'gte', 'lte', 'set', 'unset'],
  DATE: ['gte', 'lte', 'set', 'unset'],
  BOOLEAN: ['eq', 'set', 'unset'],
  SELECT: ['eq', 'set', 'unset'],
}

const OPS: AttributeOp[] = ['has', 'eq', 'gte', 'lte', 'set', 'unset']

const LABEL: Record<AttributeOp, string> = {
  has: 'chứa',
  eq: 'bằng',
  gte: 'từ',
  lte: 'đến',
  set: 'có giá trị',
  unset: 'trống',
}

/** Chữ của phép trên giao diện; "bằng" đọc là "là" với Có/Không và Lựa chọn. */
export function opLabel(op: AttributeOp, dataType: FieldDef['dataType']): string {
  return op === 'eq' && (dataType === 'BOOLEAN' || dataType === 'SELECT') ? 'là' : LABEL[op]
}

export function needsValue(op: AttributeOp): boolean {
  return op !== 'set' && op !== 'unset'
}

/** Tách ở `count` lần gặp `separator` đầu tiên; phần cuối giữ nguyên, kể cả các dấu ngăn còn lại. */
function splitFirst(text: string, separator: string, count: number): string[] {
  const parts: string[] = []
  let rest = text
  for (let i = 0; i < count; i++) {
    const at = rest.indexOf(separator)
    if (at < 0) break
    parts.push(rest.slice(0, at))
    rest = rest.slice(at + separator.length)
  }
  return [...parts, rest]
}

/**
 * Điều kiện từ các giá trị `?f=khoá~phép~giá trị` của URL (S6). Khoá không thuộc mục đang dùng
 * trong `defs`, phép không hợp kiểu, hay dạng sai thì bị bỏ (đường dẫn cũ vẫn mở được); lấy tối đa 3.
 */
export function parseConditions(raw: string[], defs: FieldDef[]): AttributeCondition[] {
  const active = defs.filter((def) => def.status === 'ACTIVE')
  const conditions: AttributeCondition[] = []
  for (const text of raw) {
    const [key, op, value = ''] = splitFirst(text, '~', 2)
    if (op === undefined || !OPS.includes(op as AttributeOp)) continue
    const fits = active.some((def) => def.fieldKey === key && OPS_BY_TYPE[def.dataType].includes(op as AttributeOp))
    if (fits) conditions.push({ key, op: op as AttributeOp, value })
  }
  return conditions.slice(0, MAX_CONDITIONS)
}

export function toUrlValues(conditions: AttributeCondition[]): string[] {
  return conditions.map((condition) => `${condition.key}~${condition.op}~${condition.value}`)
}

/** Giá trị `attr` gửi API (`khoá:phép:giá trị`); điều kiện cần giá trị mà còn trống thì chưa gửi. */
export function toApiValues(conditions: AttributeCondition[]): string[] {
  return conditions
    .filter((condition) => !needsValue(condition.op) || condition.value.trim() !== '')
    .map((condition) => `${condition.key}:${condition.op}:${condition.value}`)
}

/**
 * Mục chọn được trong bộ lọc: mục đang dùng, chung (`typeId` "0") hoặc của đúng loại đã chọn; chưa
 * chọn loại thì mọi mục. Mỗi khoá một mục (mục có `sortOrder` nhỏ nhất), theo thứ tự hiển thị.
 */
export function filterableDefs(defs: FieldDef[], typeId: string | null): FieldDef[] {
  const shown = defs
    .filter((def) => def.status === 'ACTIVE' && (typeId === null || def.typeId === '0' || def.typeId === typeId))
    .sort((a, b) => a.sortOrder - b.sortOrder)
  const seen = new Set<string>()
  return shown.filter((def) => !seen.has(def.fieldKey) && seen.add(def.fieldKey))
}
