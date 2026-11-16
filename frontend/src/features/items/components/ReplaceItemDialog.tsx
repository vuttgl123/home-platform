import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm, type Resolver } from 'react-hook-form'
import { useNavigate } from 'react-router'

import { FormError } from '@/components/common/FormError'
import { TextField } from '@/components/common/TextField'
import { VersionConflictAlert } from '@/components/common/VersionConflictAlert'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Switch } from '@/components/ui/switch'
import { toast } from '@/components/ui/toast'
import type { FieldDef } from '@/features/catalog'
import { PlacePicker, type PlaceChoice } from '@/features/places'
import { todayInBusinessZone } from '@/lib/dates'

import { useReplaceItem } from '../queries'
import type { ItemDetail } from '../types'
import { emptyItemForm, itemFormSchema, itemPayload, removalDateError, type ItemFormValues } from './itemForm'
import { ItemFields } from './ItemFields'
import { useItemFieldDefs } from './useItemFieldDefs'
import { useItemReload } from './useItemReload'

const NO_DEFS: FieldDef[] = []

/**
 * Thay một món: gỡ nó và tạo món thay nó trong một bước. Món mới mặc định giống món cũ (nhóm, tên,
 * số lượng, đơn vị) và ở cùng chỗ; xong thì tới trang của món mới.
 */
export function ReplaceItemDialog({
  ws,
  item,
  open,
  onOpenChange,
}: {
  ws: string
  item: ItemDetail
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Thay {item.name}</DialogTitle>
          <DialogDescription>Món cũ được gỡ và trỏ tới món mới; cả hai đều còn trong lịch sử.</DialogDescription>
        </DialogHeader>
        <ReplaceForm ws={ws} item={item} onDone={() => onOpenChange(false)} />
      </DialogContent>
    </Dialog>
  )
}

function ReplaceForm({ ws, item, onDone }: { ws: string; item: ItemDetail; onDone: () => void }) {
  const navigate = useNavigate()
  const [defs, setDefs] = useState<FieldDef[]>(NO_DEFS)
  const form = useForm<ItemFormValues>({
    resolver: zodResolver(itemFormSchema(defs)) as Resolver<ItemFormValues>,
    defaultValues: {
      ...emptyItemForm(),
      categoryId: item.categoryId ?? '',
      name: item.name,
      quantity: String(item.quantity),
      unit: item.unit ?? '',
    },
  })
  const loaded = useItemFieldDefs(ws, form)
  if (loaded.defs !== defs) {
    setDefs(loaded.defs)
  }
  const [removedAt, setRemovedAt] = useState(todayInBusinessZone())
  const [dateError, setDateError] = useState<string | null>(null)
  const [elsewhere, setElsewhere] = useState(false)
  const [target, setTarget] = useState<PlaceChoice | null>(null)
  const [placeError, setPlaceError] = useState<string | null>(null)
  const replace = useReplaceItem(ws)
  const conflict = replace.error?.isVersionConflict === true
  const { reload, reloading } = useItemReload(ws, item, replace.reset)

  const submit = form.handleSubmit((values) => {
    const problem = removalDateError(removedAt, item)
    setDateError(problem)
    const missingPlace = elsewhere && target?.kind !== 'place'
    setPlaceError(missingPlace ? 'Chọn chỗ đặt món mới' : null)
    if (problem || missingPlace) return
    const placeId = elsewhere && target?.kind === 'place' ? target.place.id : undefined
    replace.mutate(
      { item, removedAt, replacement: { ...itemPayload(values, defs, null), placeId } },
      {
        onSuccess: ({ replacement }) => {
          toast.add({ title: `Đã thay bằng ${replacement.name}`, type: 'success' })
          onDone()
          void navigate(`/w/${ws}/items/${replacement.id}`)
        },
      },
    )
  })

  return (
    <form noValidate onSubmit={submit} className="flex flex-col gap-4">
      <ItemFields ws={ws} form={form} defs={defs} />
      {item.ownerName && <p className="text-sm text-muted-foreground">Món mới cũng là của {item.ownerName}.</p>}
      <TextField
        label="Ngày gỡ món cũ"
        type="date"
        value={removedAt}
        onChange={(event) => setRemovedAt(event.target.value)}
        error={dateError ?? undefined}
      />
      <label className="flex items-center gap-2 text-sm">
        <Switch checked={elsewhere} onCheckedChange={setElsewhere} aria-label="Đặt món mới ở chỗ khác" />
        Đặt món mới ở chỗ khác (mặc định ở {item.placeName})
      </label>
      {elsewhere && (
        <div className="flex flex-col gap-1">
          <PlacePicker ws={ws} exclude={[item.placeId]} value={target} onChange={setTarget} label="Tìm địa điểm" />
          {placeError && <p className="text-sm text-destructive">{placeError}</p>}
        </div>
      )}
      {conflict && <VersionConflictAlert onReload={reload} isReloading={reloading} />}
      <FormError messages={replace.error && !conflict ? [replace.error.detail] : []} />
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onDone}>
          Huỷ
        </Button>
        <Button type="submit" disabled={replace.isPending || conflict || !loaded.ready}>
          Thay
        </Button>
      </DialogFooter>
    </form>
  )
}
