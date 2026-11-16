import { RefreshCwIcon } from 'lucide-react'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'

/** Bản ghi đã được người khác sửa sau khi form mở; lưu tiếp được sau khi tải bản mới. */
export function VersionConflictAlert({ onReload, isReloading }: { onReload: () => void; isReloading: boolean }) {
  return (
    <Alert>
      <AlertTitle>Đã có người sửa trước bạn.</AlertTitle>
      <AlertDescription>
        <p>Tải bản mới rồi lưu lại. Những ô bạn đã sửa được giữ nguyên.</p>
        <Button type="button" variant="outline" size="sm" className="mt-2" disabled={isReloading} onClick={onReload}>
          <RefreshCwIcon aria-hidden="true" />
          Tải bản mới
        </Button>
      </AlertDescription>
    </Alert>
  )
}
