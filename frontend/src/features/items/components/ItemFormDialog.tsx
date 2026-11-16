import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm, type Resolver } from 'react-hook-form'

import type { ApiError } from '@/api'
import { FormError } from '@/components/common/FormError'
import { applyServerErrors } from '@/components/common/serverErrors'
import { VersionConflictAlert } from '@/components/common/VersionConflictAlert'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { toast } from '@/components/ui/toast'
import type { FieldDef } from '@/features/catalog'

import { useCreateItem, useFetchFreshItem, useUpdateItem } from '../queries'
import type { ItemDetail } from '../types'
import { emptyItemForm, itemFormSchema, itemFormValues, itemPayload, type ItemFormValues } from './itemForm'
import { ItemFields } from './ItemFields'
import { useItemFieldDefs } from './useItemFieldDefs'

type Mode = { mode: 'create'; place: { id: string; name: string } } | { mode: 'edit'; item: ItemDetail }

const NO_DEFS: FieldDef[] = []
const FIXED_FIELDS = new Set(['categoryId', 'name', 'code', 'quantity', 'unit', 'status', 'placedAt', 'description'])

/** Hộp thoại thêm một món vào một địa điểm, hoặc sửa một món chưa gỡ. */
export function ItemFormDialog({
  ws,
  open,
  onOpenChange,
  ...mode
}: { ws: string; open: boolean; onOpenChange: (open: boolean) => void } & Mode) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{mode.mode === 'create' ? 'Thêm đồ' : 'Sửa món đồ'}</DialogTitle>
        </DialogHeader>
        <ItemForm ws={ws} onDone={() => onOpenChange(false)} {...mode} />
      </DialogContent>
    </Dialog>
  )
}

function ItemForm({ ws, onDone, ...mode }: { ws: string; onDone: () => void } & Mode) {
  const [base, setBase] = useState(mode.mode === 'edit' ? mode.item : null)
  const [defs, setDefs] = useState<FieldDef[]>(NO_DEFS)
  const form = useForm<ItemFormValues>({
    resolver: zodResolver(itemFormSchema(defs)) as Resolver<ItemFormValues>,
    defaultValues: base ? itemFormValues(base, []) : emptyItemForm(),
  })
  const loaded = useItemFieldDefs(ws, form)
  if (loaded.defs !== defs) {
    setDefs(loaded.defs)
  }
  const create = useCreateItem(ws)
  const update = useUpdateItem(ws)
  const fetchFresh = useFetchFreshItem(ws)
  const [formErrors, setFormErrors] = useState<string[]>([])
  const [conflict, setConflict] = useState(false)
  const [reloading, setReloading] = useState(false)
  const activeKeys = new Set(defs.filter((def) => def.status === 'ACTIVE').map((def) => def.fieldKey))
  const isField = (name: string) =>
    FIXED_FIELDS.has(name) || (name.startsWith('attributes.') && activeKeys.has(name.slice('attributes.'.length)))

  function onError(error: ApiError) {
    if (error.isVersionConflict) {
      setConflict(true)
      return
    }
    setFormErrors(applyServerErrors(error, (name, value) => form.setError(name as keyof ItemFormValues, value), isField))
  }

  async function reload() {
    if (!base) return
    setReloading(true)
    try {
      const fresh = await fetchFresh(base.id)
      setBase(fresh)
      form.reset(itemFormValues(fresh, defs), { keepDirtyValues: true })
      setConflict(false)
      update.reset()
    } finally {
      setReloading(false)
    }
  }

  const submit = form.handleSubmit((values) => {
    setFormErrors([])
    const body = itemPayload(values, defs, base)
    if (base) {
      update.mutate(
        // Lệnh sửa ghi đè cả bản ghi: gửi lại cả ảnh bìa, thứ form không có ô.
        { id: base.id, body: { ...body, version: base.version, coverFileId: base.coverFileId ?? undefined } },
        {
          onSuccess: () => {
            toast.add({ title: 'Đã lưu thay đổi', type: 'success' })
            onDone()
          },
          onError,
        },
      )
    } else if (mode.mode === 'create') {
      create.mutate(
        { ...body, placeId: mode.place.id },
        {
          onSuccess: (created) => {
            toast.add({ title: `Đã thêm ${created.name}`, type: 'success' })
            onDone()
          },
          onError,
        },
      )
    }
  })

  return (
    <form noValidate onSubmit={submit} className="flex flex-col gap-4">
      {mode.mode === 'create' && <p className="text-sm text-muted-foreground">Đặt ở {mode.place.name}</p>}
      <ItemFields ws={ws} form={form} defs={defs} keep={base} />
      {conflict && <VersionConflictAlert onReload={() => void reload()} isReloading={reloading} />}
      <FormError messages={formErrors} />
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onDone}>
          Huỷ
        </Button>
        <Button type="submit" disabled={create.isPending || update.isPending || conflict || !loaded.ready}>
          {base ? 'Lưu' : 'Thêm'}
        </Button>
      </DialogFooter>
    </form>
  )
}
