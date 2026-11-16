import { PlusIcon } from 'lucide-react'
import { useState } from 'react'

import { ErrorState } from '@/components/common/ErrorState'
import { TextField } from '@/components/common/TextField'
import { Button } from '@/components/ui/button'
import { toast } from '@/components/ui/toast'

import { itemCategoryApi } from '../api'
import { useCatalogWrite, useItemCategories } from '../queries'
import type { ItemCategory } from '../types'
import { CatalogActions } from './CatalogActions'
import { CatalogEntryDialog } from './CatalogEntryDialog'
import { CatalogSection, CatalogTable, StatusBadge } from './CatalogSection'
import { DeleteCatalogDialog } from './DeleteCatalogDialog'

/** Phần "Nhóm đồ": nội thất, điện tử… để xếp đồ đạc và gắn mục tuỳ biến theo nhóm. */
export function ItemCategorySection({ ws }: { ws: string }) {
  const categories = useItemCategories(ws)
  const [editing, setEditing] = useState<ItemCategory | 'new' | null>(null)
  const [deleting, setDeleting] = useState<ItemCategory | null>(null)
  const toggle = useCatalogWrite(ws, (category: ItemCategory) =>
    category.status === 'ARCHIVED' ? itemCategoryApi.unarchive(ws, category.id) : itemCategoryApi.archive(ws, category.id),
  )
  const rows = [...(categories.data ?? [])].sort((a, b) => Number(a.status === 'ARCHIVED') - Number(b.status === 'ARCHIVED'))

  return (
    <CatalogSection
      title="Nhóm đồ"
      description="Món đồ có thể thuộc một nhóm. Nhóm đã lưu trữ không chọn được cho món mới."
      action={
        <Button variant="outline" onClick={() => setEditing('new')}>
          <PlusIcon aria-hidden="true" />
          Thêm nhóm
        </Button>
      }
    >
      {categories.error ? (
        <ErrorState error={categories.error} onRetry={() => void categories.refetch()} />
      ) : (
        <CatalogTable headers={['Tên', 'Thứ tự', 'Trạng thái']}>
          {rows.map((category) => (
            <tr key={category.id} className={category.status === 'ARCHIVED' ? 'text-muted-foreground' : undefined}>
              <td className="px-3 py-2 font-medium">{category.name}</td>
              <td className="px-3 py-2">{category.sortOrder}</td>
              <td className="px-3 py-2">
                <StatusBadge archived={category.status === 'ARCHIVED'} />
              </td>
              <td className="px-3 py-2 text-right">
                <CatalogActions
                  label={category.name}
                  archived={category.status === 'ARCHIVED'}
                  onEdit={() => setEditing(category)}
                  onToggleArchive={() =>
                    toggle.mutate(category, { onError: (error) => toast.add({ title: error.detail, type: 'error' }) })
                  }
                  onDelete={() => setDeleting(category)}
                />
              </td>
            </tr>
          ))}
        </CatalogTable>
      )}
      {editing && <ItemCategoryDialog ws={ws} category={editing === 'new' ? null : editing} onClose={() => setEditing(null)} />}
      {deleting && (
        <DeleteCatalogDialog
          ws={ws}
          name={deleting.name}
          remove={() => itemCategoryApi.remove(ws, deleting.id)}
          archive={() => itemCategoryApi.archive(ws, deleting.id)}
          onClose={() => setDeleting(null)}
        />
      )}
    </CatalogSection>
  )
}

function ItemCategoryDialog({ ws, category, onClose }: { ws: string; category: ItemCategory | null; onClose: () => void }) {
  const [name, setName] = useState(category?.name ?? '')
  const [sortOrder, setSortOrder] = useState(category ? String(category.sortOrder) : '')
  const [errors, setErrors] = useState<Record<string, string>>({})

  function validate() {
    const next: Record<string, string> = {}
    if (name.trim() === '') next.name = 'Nhập tên'
    if (name.length > 100) next.name = 'Tối đa 100 ký tự'
    if (sortOrder !== '' && !/^-?\d+$/.test(sortOrder)) next.sortOrder = 'Một số nguyên'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  const body = { name: name.trim(), sortOrder: sortOrder === '' ? undefined : Number(sortOrder) }
  return (
    <CatalogEntryDialog
      ws={ws}
      title={category ? 'Sửa nhóm đồ' : 'Thêm nhóm đồ'}
      submitLabel={category ? 'Lưu' : 'Thêm'}
      done={category ? 'Đã lưu thay đổi' : `Đã thêm ${name.trim()}`}
      validate={validate}
      save={() => (category ? itemCategoryApi.update(ws, category.id, body) : itemCategoryApi.create(ws, body))}
      onClose={onClose}
    >
      <TextField label="Tên" required value={name} onChange={(event) => setName(event.target.value)} error={errors.name} />
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
