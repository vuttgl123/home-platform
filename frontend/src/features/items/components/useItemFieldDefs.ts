import { useWatch, type UseFormReturn } from 'react-hook-form'

import { useFieldDefs, type FieldDef } from '@/features/catalog'

import type { ItemFormValues } from './itemForm'

const NO_DEFS: FieldDef[] = []

/**
 * Mục tuỳ biến của món đồ theo nhóm đang chọn trong form: mục chung (`typeId` 0) cộng mục của
 * nhóm. Dùng chung cho form thêm, sửa, thay.
 * @returns `ready` false khi chưa tải xong: gửi lúc đó sẽ làm mất giá trị của các mục
 */
export function useItemFieldDefs(ws: string, form: UseFormReturn<ItemFormValues>) {
  const categoryId = useWatch({ control: form.control, name: 'categoryId' })
  const query = useFieldDefs(ws, 'ITEM', categoryId || '0')
  return { defs: query.data ?? NO_DEFS, ready: query.isSuccess }
}
