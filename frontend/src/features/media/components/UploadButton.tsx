import { ImagePlusIcon } from 'lucide-react'
import { useRef } from 'react'

import { Button } from '@/components/ui/button'

import { PHOTO_TYPES } from '../urls'

/** Nút "Tải ảnh lên": mở hộp chọn nhiều file ảnh; ô chọn file thật ẩn với mắt nhưng vẫn có nhãn. */
export function UploadButton({ onFiles, disabled }: { onFiles: (files: File[]) => void; disabled?: boolean }) {
  const input = useRef<HTMLInputElement>(null)
  return (
    <>
      <input
        ref={input}
        type="file"
        multiple
        accept={PHOTO_TYPES.join(',')}
        aria-label="Chọn ảnh để tải lên"
        className="sr-only"
        tabIndex={-1}
        onChange={(event) => {
          const files = Array.from(event.target.files ?? [])
          // Xoá lựa chọn để chọn lại đúng các file đó vẫn gọi onChange.
          event.target.value = ''
          if (files.length > 0) onFiles(files)
        }}
      />
      <Button variant="outline" disabled={disabled} onClick={() => input.current?.click()}>
        <ImagePlusIcon aria-hidden="true" />
        Tải ảnh lên
      </Button>
    </>
  )
}
