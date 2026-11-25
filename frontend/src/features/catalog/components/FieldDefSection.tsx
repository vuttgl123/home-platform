import { PlusIcon } from 'lucide-react'
import { useId, useState } from 'react'

import { ErrorState } from '@/components/common/ErrorState'
import { TextField } from '@/components/common/TextField'
import { Button } from '@/components/ui/button'
import { Field, FieldDescription, FieldError, FieldLabel } from '@/components/ui/field'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'
import { Switch } from '@/components/ui/switch'
import { Textarea } from '@/components/ui/textarea'
import { toast } from '@/components/ui/toast'

import { fieldDefApi } from '../api'
import { suggestFieldKey } from '../fieldKeys'
import { useAllFieldDefs, useCatalogWrite, useItemCategories, usePlaceTypes } from '../queries'
import type { FieldDef, FieldTarget } from '../types'
import { CatalogActions } from './CatalogActions'
import { CatalogEntryDialog } from './CatalogEntryDialog'
import { CatalogSection, CatalogTable, StatusBadge } from './CatalogSection'

const TARGETS: { value: FieldTarget; label: string; everything: string }[] = [
  { value: 'PLACE', label: 'Địa điểm', everything: 'Mọi địa điểm' },
  { value: 'PERSON', label: 'Người', everything: 'Mọi người' },
  { value: 'ITEM', label: 'Đồ đạc', everything: 'Mọi món đồ' },
]

const DATA_TYPES: { value: FieldDef['dataType']; label: string }[] = [
  { value: 'TEXT', label: 'Chữ' },
  { value: 'NUMBER', label: 'Số' },
  { value: 'BOOLEAN', label: 'Có/Không' },
  { value: 'DATE', label: 'Ngày' },
  { value: 'SELECT', label: 'Chọn từ danh sách' },
]

const KEY = /^[a-z][a-z0-9_]*$/

/** Các phạm vi chọn được cho một đích: "mọi …" (`0`), rồi từng loại địa điểm hay nhóm đồ đang dùng. */
function useScopes(ws: string, target: FieldTarget, keepId?: string) {
  const types = usePlaceTypes(ws).data ?? []
  const categories = useItemCategories(ws).data ?? []
  const owners = target === 'PLACE' ? types : target === 'ITEM' ? categories : []
  const everything = TARGETS.find((option) => option.value === target)!.everything
  return [
    { id: '0', name: everything },
    ...owners
      .filter((owner) => owner.status === 'ACTIVE' || owner.id === keepId)
      .map((owner) => ({ id: owner.id, name: owner.name })),
  ]
}

/** Phần "Mục tuỳ biến": thông tin thêm do không gian tự định nghĩa cho địa điểm, người, đồ đạc. */
export function FieldDefSection({ ws }: { ws: string }) {
  const [target, setTarget] = useState<FieldTarget>('PLACE')
  const defs = useAllFieldDefs(ws, target)
  const types = usePlaceTypes(ws).data ?? []
  const categories = useItemCategories(ws).data ?? []
  const [editing, setEditing] = useState<FieldDef | 'new' | null>(null)
  const toggle = useCatalogWrite(ws, (def: FieldDef) =>
    def.status === 'ARCHIVED' ? fieldDefApi.unarchive(ws, def.id) : fieldDefApi.archive(ws, def.id),
  )
  const targetId = useId()
  const everything = TARGETS.find((option) => option.value === target)!.everything
  const scopeName = (def: FieldDef) =>
    def.typeId === '0'
      ? everything
      : ((target === 'PLACE' ? types : categories).find((owner) => owner.id === def.typeId)?.name ?? '…')
  const rows = [...(defs.data ?? [])].sort((a, b) => Number(a.status === 'ARCHIVED') - Number(b.status === 'ARCHIVED'))

  return (
    <CatalogSection
      title="Mục tuỳ biến"
      description="Thông tin thêm mà mỗi bản ghi có thể điền, cho mọi bản ghi hoặc riêng một loại, một nhóm."
      action={
        <div className="flex items-center gap-2">
          <label htmlFor={targetId} className="text-sm text-muted-foreground">
            Cho
          </label>
          <NativeSelect id={targetId} value={target} onChange={(event) => setTarget(event.target.value as FieldTarget)}>
            {TARGETS.map((option) => (
              <NativeSelectOption key={option.value} value={option.value}>
                {option.label}
              </NativeSelectOption>
            ))}
          </NativeSelect>
          <Button variant="outline" onClick={() => setEditing('new')}>
            <PlusIcon aria-hidden="true" />
            Thêm mục
          </Button>
        </div>
      }
    >
      {defs.error ? (
        <ErrorState error={defs.error} onRetry={() => void defs.refetch()} />
      ) : rows.length === 0 && defs.isSuccess ? (
        <p className="text-sm text-muted-foreground">Chưa có mục tuỳ biến nào.</p>
      ) : (
        <CatalogTable headers={['Nhãn', 'Khoá', 'Kiểu', 'Áp dụng cho', 'Bắt buộc', 'Nhắc', 'Trạng thái']}>
          {rows.map((def) => (
            <tr key={def.id} className={def.status === 'ARCHIVED' ? 'text-muted-foreground' : undefined}>
              <td className="px-3 py-2 font-medium">{def.label}</td>
              <td className="px-3 py-2 tabular-nums text-xs">{def.fieldKey}</td>
              <td className="px-3 py-2">{DATA_TYPES.find((type) => type.value === def.dataType)?.label}</td>
              <td className="px-3 py-2">{scopeName(def)}</td>
              <td className="px-3 py-2">{def.required ? 'Có' : 'Không'}</td>
              <td className="px-3 py-2">{def.remindDays != null ? `trước ${def.remindDays} ngày` : '—'}</td>
              <td className="px-3 py-2">
                <StatusBadge archived={def.status === 'ARCHIVED'} />
              </td>
              <td className="px-3 py-2 text-right">
                <CatalogActions
                  label={def.label}
                  archived={def.status === 'ARCHIVED'}
                  onEdit={() => setEditing(def)}
                  onToggleArchive={() =>
                    toggle.mutate(def, { onError: (error) => toast.add({ title: error.detail, type: 'error' }) })
                  }
                />
              </td>
            </tr>
          ))}
        </CatalogTable>
      )}
      {editing && (
        <FieldDefDialog
          ws={ws}
          target={target}
          def={editing === 'new' ? null : editing}
          scopeName={editing === 'new' ? undefined : scopeName(editing)}
          onClose={() => setEditing(null)}
        />
      )}
    </CatalogSection>
  )
}

/** Số ngày nhắc trước khi bật nhắc cho một mục kiểu ngày. */
const DEFAULT_REMIND_DAYS = 30

/** Lỗi của danh sách lựa chọn (mỗi dòng một lựa chọn), hoặc `null` nếu hợp lệ. */
function optionsError(options: string[]): string | null {
  if (options.length === 0) return 'Nhập ít nhất một lựa chọn, mỗi dòng một lựa chọn'
  if (options.length > 100) return 'Tối đa 100 lựa chọn'
  if (options.some((option) => option.length > 100)) return 'Mỗi lựa chọn tối đa 100 ký tự'
  if (new Set(options.map((option) => option.toLocaleLowerCase('vi'))).size !== options.length) {
    return 'Các lựa chọn không được trùng nhau'
  }
  return null
}

function FieldDefDialog({
  ws,
  target,
  def,
  scopeName,
  onClose,
}: {
  ws: string
  target: FieldTarget
  def: FieldDef | null
  scopeName?: string
  onClose: () => void
}) {
  const scopes = useScopes(ws, target)
  const [scope, setScope] = useState('0')
  const [label, setLabel] = useState(def?.label ?? '')
  const [key, setKey] = useState(def?.fieldKey ?? '')
  // Khoá đi theo nhãn cho tới khi người dùng tự sửa khoá.
  const [keyTouched, setKeyTouched] = useState(false)
  const [dataType, setDataType] = useState<FieldDef['dataType']>(def?.dataType ?? 'TEXT')
  const [optionsText, setOptionsText] = useState((def?.options ?? []).join('\n'))
  const [required, setRequired] = useState(def?.required ?? false)
  // Mục kiểu ngày có thể nhắc trong Việc cần làm (spec Đợt 8, L4); bật lên thì mặc định trước 30 ngày.
  const [remind, setRemind] = useState(def?.remindDays != null)
  const [remindDays, setRemindDays] = useState(String(def?.remindDays ?? DEFAULT_REMIND_DAYS))
  const [sortOrder, setSortOrder] = useState(def ? String(def.sortOrder) : '')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const scopeId = useId()
  const typeId = useId()
  const optionsId = useId()
  const reminding = dataType === 'DATE' && remind
  const options = optionsText
    .split('\n')
    .map((option) => option.trim())
    .filter((option) => option !== '')

  function validate() {
    const next: Record<string, string> = {}
    if (label.trim() === '') next.label = 'Nhập nhãn'
    if (label.length > 100) next.label = 'Tối đa 100 ký tự'
    if (!def && !KEY.test(key)) next.key = 'Bắt đầu bằng chữ thường; chỉ chữ thường không dấu, số và _'
    if (!def && key.length > 50) next.key = 'Tối đa 50 ký tự'
    if (dataType === 'SELECT') {
      const problem = optionsError(options)
      if (problem) next.options = problem
    }
    if (sortOrder !== '' && !/^-?\d+$/.test(sortOrder)) next.sortOrder = 'Một số nguyên'
    if (reminding && !(/^\d+$/.test(remindDays.trim()) && Number(remindDays) >= 1 && Number(remindDays) <= 365)) {
      next.remindDays = 'Số nguyên từ 1 tới 365'
    }
    setErrors(next)
    return Object.keys(next).length === 0
  }

  function save() {
    const common = {
      label: label.trim(),
      options: dataType === 'SELECT' ? options : undefined,
      required,
      sortOrder: sortOrder === '' ? undefined : Number(sortOrder),
      remindDays: reminding ? Number(remindDays) : undefined,
    }
    return def
      ? fieldDefApi.update(ws, def.id, common)
      : fieldDefApi.create(ws, { ...common, target, typeId: scope, fieldKey: key, dataType })
  }

  return (
    <CatalogEntryDialog
      ws={ws}
      title={def ? 'Sửa mục tuỳ biến' : 'Thêm mục tuỳ biến'}
      submitLabel={def ? 'Lưu' : 'Thêm'}
      done={def ? 'Đã lưu thay đổi' : `Đã thêm ${label.trim()}`}
      validate={validate}
      save={save}
      onClose={onClose}
    >
      {def ? (
        <div className="flex flex-col gap-1 rounded-lg border bg-muted/40 px-3 py-2 text-sm">
          <span>
            Khoá <span className="tabular-nums">{def.fieldKey}</span> · {DATA_TYPES.find((type) => type.value === def.dataType)?.label} ·{' '}
            {scopeName}
          </span>
          <span className="text-muted-foreground">
            Khoá, kiểu và phạm vi không đổi được sau khi tạo; muốn đổi thì lưu trữ mục này rồi tạo mục mới.
          </span>
        </div>
      ) : (
        <Field>
          <FieldLabel htmlFor={scopeId}>Áp dụng cho</FieldLabel>
          <NativeSelect id={scopeId} className="w-full" value={scope} onChange={(event) => setScope(event.target.value)}>
            {scopes.map((option) => (
              <NativeSelectOption key={option.id} value={option.id}>
                {option.name}
              </NativeSelectOption>
            ))}
          </NativeSelect>
        </Field>
      )}
      <TextField
        label="Nhãn"
        required
        autoComplete="off"
        value={label}
        onChange={(event) => {
          setLabel(event.target.value)
          if (!def && !keyTouched) setKey(suggestFieldKey(event.target.value))
        }}
        error={errors.label}
      />
      {!def && (
        <>
          <TextField
            label="Khoá"
            required
            autoComplete="off"
            description="Tên máy đọc, dùng trong dữ liệu; không đổi được sau khi tạo."
            value={key}
            onChange={(event) => {
              setKeyTouched(true)
              setKey(event.target.value)
            }}
            error={errors.key}
          />
          <Field>
            <FieldLabel htmlFor={typeId}>Kiểu</FieldLabel>
            <NativeSelect
              id={typeId}
              className="w-full"
              value={dataType}
              onChange={(event) => setDataType(event.target.value as FieldDef['dataType'])}
            >
              {DATA_TYPES.map((type) => (
                <NativeSelectOption key={type.value} value={type.value}>
                  {type.label}
                </NativeSelectOption>
              ))}
            </NativeSelect>
          </Field>
        </>
      )}
      {dataType === 'SELECT' && (
        <Field data-invalid={errors.options ? true : undefined}>
          <FieldLabel htmlFor={optionsId}>Lựa chọn</FieldLabel>
          <Textarea
            id={optionsId}
            rows={4}
            value={optionsText}
            onChange={(event) => setOptionsText(event.target.value)}
            aria-invalid={errors.options ? true : undefined}
          />
          <FieldDescription>Mỗi dòng một lựa chọn. Bớt một lựa chọn không đổi dữ liệu đã có.</FieldDescription>
          {errors.options && <FieldError>{errors.options}</FieldError>}
        </Field>
      )}
      <label className="flex items-center gap-2 text-sm">
        <Switch checked={required} onCheckedChange={setRequired} aria-label="Bắt buộc" />
        Bắt buộc
      </label>
      {dataType === 'DATE' && (
        <label className="flex items-center gap-2 text-sm">
          <Switch checked={remind} onCheckedChange={setRemind} aria-label="Nhắc trong Việc cần làm" />
          Nhắc trong Việc cần làm
        </label>
      )}
      {reminding && (
        <TextField
          label="Nhắc trước (ngày)"
          inputMode="numeric"
          description="Giá trị ngày của mục này vào Việc cần làm từ bấy nhiêu ngày trước."
          value={remindDays}
          onChange={(event) => setRemindDays(event.target.value)}
          error={errors.remindDays}
        />
      )}
      <TextField
        label="Thứ tự"
        inputMode="numeric"
        value={sortOrder}
        onChange={(event) => setSortOrder(event.target.value)}
        error={errors.sortOrder}
      />
    </CatalogEntryDialog>
  )
}
