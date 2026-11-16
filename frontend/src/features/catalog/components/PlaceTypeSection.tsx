import { PlusIcon } from 'lucide-react'
import { useState } from 'react'

import { ErrorState } from '@/components/common/ErrorState'
import { TextField } from '@/components/common/TextField'
import { Button } from '@/components/ui/button'
import { toast } from '@/components/ui/toast'

import { placeTypeApi } from '../api'
import { suggestTypeCode } from '../codeRules'
import { useCatalogWrite, usePlaceTypes } from '../queries'
import type { PlaceType } from '../types'
import { CatalogActions } from './CatalogActions'
import { CatalogEntryDialog } from './CatalogEntryDialog'
import { CatalogSection, CatalogTable, StatusBadge } from './CatalogSection'
import { DeleteCatalogDialog } from './DeleteCatalogDialog'

const CODE = /^[A-Z0-9_]+$/

/** Phần "Loại địa điểm": toà nhà, tầng, phòng… do không gian tự định nghĩa. */
export function PlaceTypeSection({ ws }: { ws: string }) {
  const types = usePlaceTypes(ws)
  const [editing, setEditing] = useState<PlaceType | 'new' | null>(null)
  const [deleting, setDeleting] = useState<PlaceType | null>(null)
  const toggle = useCatalogWrite(ws, (type: PlaceType) =>
    type.status === 'ARCHIVED' ? placeTypeApi.unarchive(ws, type.id) : placeTypeApi.archive(ws, type.id),
  )
  const rows = [...(types.data ?? [])].sort((a, b) => Number(a.status === 'ARCHIVED') - Number(b.status === 'ARCHIVED'))

  return (
    <CatalogSection
      title="Loại địa điểm"
      description="Mỗi địa điểm thuộc một loại. Loại đã lưu trữ không chọn được cho địa điểm mới."
      action={
        <Button variant="outline" onClick={() => setEditing('new')}>
          <PlusIcon aria-hidden="true" />
          Thêm loại
        </Button>
      }
    >
      {types.error ? (
        <ErrorState error={types.error} onRetry={() => void types.refetch()} />
      ) : (
        <CatalogTable headers={['Tên', 'Mã', 'Thứ tự', 'Trạng thái']}>
          {rows.map((type) => (
            <tr key={type.id} className={type.status === 'ARCHIVED' ? 'text-muted-foreground' : undefined}>
              <td className="px-3 py-2 font-medium">{type.name}</td>
              <td className="px-3 py-2 font-mono text-xs">{type.code}</td>
              <td className="px-3 py-2">{type.sortOrder}</td>
              <td className="px-3 py-2">
                <StatusBadge archived={type.status === 'ARCHIVED'} />
              </td>
              <td className="px-3 py-2 text-right">
                <CatalogActions
                  label={type.name}
                  archived={type.status === 'ARCHIVED'}
                  onEdit={() => setEditing(type)}
                  onToggleArchive={() =>
                    toggle.mutate(type, { onError: (error) => toast.add({ title: error.detail, type: 'error' }) })
                  }
                  onDelete={() => setDeleting(type)}
                />
              </td>
            </tr>
          ))}
        </CatalogTable>
      )}
      {editing && (
        <PlaceTypeDialog
          ws={ws}
          type={editing === 'new' ? null : editing}
          takenCodes={(types.data ?? []).map((type) => type.code)}
          onClose={() => setEditing(null)}
        />
      )}
      {deleting && (
        <DeleteCatalogDialog
          ws={ws}
          name={deleting.name}
          remove={() => placeTypeApi.remove(ws, deleting.id)}
          archive={() => placeTypeApi.archive(ws, deleting.id)}
          onClose={() => setDeleting(null)}
        />
      )}
    </CatalogSection>
  )
}

/**
 * Thêm hoặc sửa một loại địa điểm. Khi thêm, mã được suy từ tên (`suggestTypeCode`); người dùng vẫn
 * có thể "Tự đặt mã". Mã không đổi được sau khi tạo.
 */
function PlaceTypeDialog({
  ws,
  type,
  takenCodes,
  onClose,
}: {
  ws: string
  type: PlaceType | null
  takenCodes: string[]
  onClose: () => void
}) {
  const [ownCode, setOwnCode] = useState<string | null>(null)
  const [name, setName] = useState(type?.name ?? '')
  const [description, setDescription] = useState(type?.description ?? '')
  const [sortOrder, setSortOrder] = useState(type ? String(type.sortOrder) : '')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const code = type?.code ?? ownCode ?? suggestTypeCode(name, takenCodes)

  function validate() {
    const next: Record<string, string> = {}
    if (!type && ownCode == null && code === '') next.code = 'Tên cần có chữ hoặc số để làm mã; hoặc tự đặt mã'
    else if (!type && !CODE.test(code)) next.code = 'Chỉ chữ in hoa không dấu, số và _'
    if (code.length > 50) next.code = 'Tối đa 50 ký tự'
    if (name.trim() === '') next.name = 'Nhập tên'
    if (name.length > 100) next.name = 'Tối đa 100 ký tự'
    if (description.length > 500) next.description = 'Tối đa 500 ký tự'
    if (sortOrder !== '' && !/^-?\d+$/.test(sortOrder)) next.sortOrder = 'Một số nguyên'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  const common = {
    name: name.trim(),
    description: description.trim() || undefined,
    sortOrder: sortOrder === '' ? undefined : Number(sortOrder),
  }
  return (
    <CatalogEntryDialog
      ws={ws}
      title={type ? 'Sửa loại địa điểm' : 'Thêm loại địa điểm'}
      submitLabel={type ? 'Lưu' : 'Thêm'}
      done={type ? 'Đã lưu thay đổi' : `Đã thêm ${name.trim()}`}
      validate={validate}
      save={() => (type ? placeTypeApi.update(ws, type.id, common) : placeTypeApi.create(ws, { ...common, code }))}
      onClose={onClose}
    >
      {type ? (
        <p className="text-sm text-muted-foreground">
          Mã <span className="font-mono">{type.code}</span> không đổi được sau khi tạo.
        </p>
      ) : null}
      <TextField label="Tên" required value={name} onChange={(event) => setName(event.target.value)} error={errors.name} />
      {!type &&
        (ownCode == null ? (
          <div className="flex flex-col gap-1">
            <div className="flex items-center justify-between gap-2 text-sm">
              <span className="text-muted-foreground">{`Mã: ${code || '—'}`}</span>
              <Button type="button" variant="ghost" size="sm" onClick={() => setOwnCode(code)}>
                Tự đặt mã
              </Button>
            </div>
            {errors.code && <p className="text-sm text-destructive">{errors.code}</p>}
          </div>
        ) : (
          <TextField
            label="Mã"
            required
            autoComplete="off"
            description="Ví dụ KHO_LANH; dùng để tích hợp, không đổi được sau khi tạo."
            value={ownCode}
            onChange={(event) => setOwnCode(event.target.value.toUpperCase())}
            error={errors.code}
          />
        ))}
      <TextField label="Mô tả" value={description} onChange={(event) => setDescription(event.target.value)} error={errors.description} />
      <TextField
        label="Thứ tự"
        inputMode="numeric"
        description="Số nhỏ đứng trước trong các danh sách chọn."
        value={sortOrder}
        onChange={(event) => setSortOrder(event.target.value)}
        error={errors.sortOrder}
      />
    </CatalogEntryDialog>
  )
}
