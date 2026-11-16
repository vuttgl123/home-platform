import type { ReactNode } from 'react'

import { FormError } from '@/components/common/FormError'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { toast } from '@/components/ui/toast'

import { useCatalogWrite } from '../queries'

/**
 * Khung chung của hộp thoại thêm, sửa một mục danh mục: nội dung là các ô; `validate` trả lỗi
 * theo ô (rỗng là hợp lệ) trước khi gửi.
 */
export function CatalogEntryDialog({
  ws,
  title,
  submitLabel,
  done,
  validate,
  save,
  onClose,
  children,
}: {
  ws: string
  title: string
  submitLabel: string
  done: string
  validate: () => boolean
  save: () => Promise<unknown>
  onClose: () => void
  children: ReactNode
}) {
  const write = useCatalogWrite(ws, save)
  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
        </DialogHeader>
        <form
          noValidate
          className="flex flex-col gap-4"
          onSubmit={(event) => {
            event.preventDefault()
            if (!validate()) return
            write.mutate(undefined, {
              onSuccess: () => {
                toast.add({ title: done, type: 'success' })
                onClose()
              },
            })
          }}
        >
          {children}
          <FormError messages={write.error ? [write.error.detail] : []} />
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>
              Huỷ
            </Button>
            <Button type="submit" disabled={write.isPending}>
              {submitLabel}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
