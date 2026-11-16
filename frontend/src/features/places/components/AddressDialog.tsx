import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import { FormError } from '@/components/common/FormError'
import { TextField } from '@/components/common/TextField'
import { VersionConflictAlert } from '@/components/common/VersionConflictAlert'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { toast } from '@/components/ui/toast'

import type { AddressBody } from '../api'
import { useFetchFreshPlace, useRemoveAddress, useSetAddress } from '../queries'
import type { Address, PlaceDetail } from '../types'

const COORDINATE = /^-?\d+(\.\d+)?$/

function coordinate(max: number) {
  return z
    .string()
    .trim()
    .refine((value) => value === '' || (COORDINATE.test(value) && Math.abs(Number(value)) <= max), {
      message: `Một số từ -${max} đến ${max}, dùng dấu chấm cho phần thập phân`,
    })
}

const schema = z
  .object({
    fullAddress: z.string().trim().min(1, 'Nhập địa chỉ đầy đủ').max(500, 'Tối đa 500 ký tự'),
    street: z.string().trim().max(255, 'Tối đa 255 ký tự'),
    wardName: z.string().trim().max(100, 'Tối đa 100 ký tự'),
    provinceName: z.string().trim().max(100, 'Tối đa 100 ký tự'),
    latitude: coordinate(90),
    longitude: coordinate(180),
  })
  .superRefine((values, ctx) => {
    if ((values.latitude === '') !== (values.longitude === '')) {
      ctx.addIssue({
        code: 'custom',
        path: [values.latitude === '' ? 'latitude' : 'longitude'],
        message: 'Nhập cả vĩ độ và kinh độ, hoặc để trống cả hai',
      })
    }
  })

type AddressFormValues = z.input<typeof schema>

/**
 * Thân địa chỉ gửi lên: những trường form có ô lấy từ form, những trường form không có ô (mã
 * tỉnh, quận…) lấy lại từ địa chỉ cũ để không bị xoá. Trường trống không gửi.
 */
function addressBody(values: z.output<typeof schema>, old: Address | null): AddressBody {
  const merged: Record<string, unknown> = {
    ...old,
    geohash: undefined,
    street: values.street,
    wardName: values.wardName,
    provinceName: values.provinceName,
    fullAddress: values.fullAddress,
    latitude: values.latitude === '' ? undefined : Number(values.latitude),
    longitude: values.longitude === '' ? undefined : Number(values.longitude),
  }
  return Object.fromEntries(
    Object.entries(merged).filter(([, value]) => value != null && value !== ''),
  ) as AddressBody
}

/** Đặt, sửa hoặc gỡ địa chỉ riêng của một địa điểm. Con cháu không có địa chỉ riêng dùng địa chỉ này. */
export function AddressDialog({
  ws,
  place,
  open,
  onOpenChange,
}: {
  ws: string
  place: PlaceDetail
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Địa chỉ của {place.name}</DialogTitle>
          <DialogDescription>Những chỗ bên trong chưa có địa chỉ riêng sẽ dùng địa chỉ này.</DialogDescription>
        </DialogHeader>
        <AddressForm ws={ws} place={place} onDone={() => onOpenChange(false)} />
      </DialogContent>
    </Dialog>
  )
}

function AddressForm({ ws, place, onDone }: { ws: string; place: PlaceDetail; onDone: () => void }) {
  const old = place.address
  const form = useForm<AddressFormValues, unknown, z.output<typeof schema>>({
    resolver: zodResolver(schema),
    defaultValues: {
      fullAddress: old?.fullAddress ?? '',
      street: old?.street ?? '',
      wardName: old?.wardName ?? '',
      provinceName: old?.provinceName ?? '',
      latitude: old?.latitude != null ? String(old.latitude) : '',
      longitude: old?.longitude != null ? String(old.longitude) : '',
    },
  })
  const save = useSetAddress(ws)
  const remove = useRemoveAddress(ws)
  const fetchFresh = useFetchFreshPlace(ws)
  const [reloading, setReloading] = useState(false)
  const error = save.error ?? remove.error
  const conflict = error?.isVersionConflict === true
  const pending = save.isPending || remove.isPending
  const { errors } = form.formState

  async function reload() {
    setReloading(true)
    try {
      // Trang nhận bản mới qua cache, nên `place` (và `version`) của hộp thoại đổi theo; chữ đã
      // nhập trong form giữ nguyên.
      await fetchFresh(place.id)
      save.reset()
      remove.reset()
    } finally {
      setReloading(false)
    }
  }

  const submit = form.handleSubmit((values) =>
    save.mutate(
      { place, address: addressBody(values, old) },
      {
        onSuccess: () => {
          toast.add({ title: 'Đã lưu địa chỉ', type: 'success' })
          onDone()
        },
      },
    ),
  )

  return (
    <form noValidate onSubmit={submit} className="flex flex-col gap-4">
      <TextField
        label="Địa chỉ đầy đủ"
        required
        placeholder="Ví dụ: 1 Tràng Tiền, Hoàn Kiếm, Hà Nội"
        error={errors.fullAddress?.message}
        {...form.register('fullAddress')}
      />
      <TextField label="Số nhà, đường" error={errors.street?.message} {...form.register('street')} />
      <div className="grid gap-4 sm:grid-cols-2">
        <TextField label="Phường/xã" error={errors.wardName?.message} {...form.register('wardName')} />
        <TextField label="Tỉnh/thành phố" error={errors.provinceName?.message} {...form.register('provinceName')} />
        <TextField label="Vĩ độ" inputMode="decimal" error={errors.latitude?.message} {...form.register('latitude')} />
        <TextField label="Kinh độ" inputMode="decimal" error={errors.longitude?.message} {...form.register('longitude')} />
      </div>
      {conflict && <VersionConflictAlert onReload={() => void reload()} isReloading={reloading} />}
      <FormError messages={error && !conflict ? [error.detail] : []} />
      <DialogFooter className="sm:justify-between">
        {old ? (
          <Button
            type="button"
            variant="destructive"
            disabled={pending || conflict}
            onClick={() =>
              remove.mutate(place, {
                onSuccess: () => {
                  toast.add({ title: 'Đã gỡ địa chỉ', type: 'success' })
                  onDone()
                },
              })
            }
          >
            Gỡ địa chỉ
          </Button>
        ) : (
          <span />
        )}
        <div className="flex flex-col-reverse gap-2 sm:flex-row">
          <Button type="button" variant="outline" onClick={onDone}>
            Huỷ
          </Button>
          <Button type="submit" disabled={pending || conflict}>
            Lưu
          </Button>
        </div>
      </DialogFooter>
    </form>
  )
}
