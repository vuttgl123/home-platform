import { ApiError } from '@/api'
import { toast } from '@/components/ui/toast'

/** Thời gian nút "Hoàn tác" còn bấm được. */
const UNDO_MS = 8000

/**
 * Thông báo "Đã xoá <name>" có nút "Hoàn tác" trong 8 giây. Bấm thì thông báo đóng ngay (không bấm được
 * lần hai) và `undo` chạy; bị từ chối vì bản ghi đã đổi sau lần xoá thì báo rõ, không thử lại.
 * @param undo khôi phục bản ghi; ném `ApiError` khi bị từ chối
 */
export function deletedToast({ name, undo }: { name: string; undo: () => Promise<void> }): void {
  let used = false
  const id: string = toast.add({
    title: `Đã xoá ${name}`,
    type: 'success',
    timeout: UNDO_MS,
    actionProps: {
      children: 'Hoàn tác',
      onClick: () => {
        if (used) return
        used = true
        toast.close(id)
        undo().then(
          () => toast.add({ title: `Đã khôi phục ${name}`, type: 'success' }),
          (error: unknown) => {
            const detail = error instanceof ApiError ? error.detail : String(error)
            if (error instanceof ApiError && error.status === 409) {
              toast.add({ title: 'Không hoàn tác được: bản ghi đã thay đổi', description: detail, type: 'error' })
            } else {
              toast.add({ title: detail, type: 'error' })
            }
          },
        )
      },
    },
  })
}
