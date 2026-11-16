import type { ApiError } from '@/api'
import { FormError } from '@/components/common/FormError'
import { VersionConflictAlert } from '@/components/common/VersionConflictAlert'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'

/**
 * Hỏi lại trước khi cấp mã mới cho một địa điểm hay món đồ. Có mã thì nói rõ mã cũ sẽ không còn
 * dùng được; chưa có mã thì chỉ báo mã sẽ theo dạng mã của Danh mục. Gặp 409 "đã có người sửa" thì
 * cho tải bản mới.
 */
export function ReissueCodeDialog({
  open,
  onOpenChange,
  name,
  code,
  isPending,
  error,
  onConfirm,
  onReload,
  isReloading,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  name: string
  code: string | null
  isPending: boolean
  error: ApiError | null
  onConfirm: () => void
  onReload: () => void
  isReloading: boolean
}) {
  const conflict = error?.isVersionConflict === true
  const action = code ? 'Cấp mã mới' : 'Cấp mã'
  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>
            {action} cho {name}?
          </AlertDialogTitle>
          <AlertDialogDescription>
            {code
              ? `Mã hiện tại ${code} sẽ không còn tìm ra bản ghi này. Nhãn đã in theo mã cũ sẽ sai. Mã cũ không được cấp lại cho bản ghi nào khác.`
              : 'Hệ thống sẽ cấp một mã theo dạng mã đang đặt trong Danh mục.'}
          </AlertDialogDescription>
        </AlertDialogHeader>
        {conflict && <VersionConflictAlert onReload={onReload} isReloading={isReloading} />}
        <FormError messages={error && !conflict ? [error.detail] : []} />
        <AlertDialogFooter>
          <AlertDialogCancel>Huỷ</AlertDialogCancel>
          <AlertDialogAction disabled={isPending || conflict} onClick={onConfirm}>
            {action}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
