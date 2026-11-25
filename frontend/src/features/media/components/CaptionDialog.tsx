import { useState } from 'react'

import { FormError } from '@/components/common/FormError'
import { TextField } from '@/components/common/TextField'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'

import { useSetCaption } from '../queries'
import type { Photo, PhotoOwner } from '../types'

/** Sửa chú thích của một ảnh; để trống là bỏ chú thích. */
export function CaptionDialog({
  ws,
  owner,
  photo,
  open,
  onOpenChange,
}: {
  ws: string
  owner: PhotoOwner
  photo: Photo
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const [caption, setCaption] = useState(photo.caption ?? '')
  const save = useSetCaption(ws, owner)
  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (!next) save.reset()
        onOpenChange(next)
      }}
    >
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Sửa chú thích</DialogTitle>
        </DialogHeader>
        <form
          className="flex flex-col gap-4"
          onSubmit={(event) => {
            event.preventDefault()
            save.mutate({ id: photo.id, caption }, { onSuccess: () => onOpenChange(false) })
          }}
        >
          <TextField
            label="Chú thích"
            value={caption}
            maxLength={255}
            autoComplete="off"
            description="Để trống nếu không cần chú thích"
            onChange={(event) => setCaption(event.target.value)}
          />
          <FormError messages={save.error ? [save.error.detail] : []} />
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Huỷ
            </Button>
            <Button type="submit" disabled={save.isPending}>
              Lưu
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
