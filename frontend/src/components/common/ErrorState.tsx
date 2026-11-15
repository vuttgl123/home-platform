import { TriangleAlertIcon } from 'lucide-react'

import { ApiError } from '@/api'
import { Button } from '@/components/ui/button'
import { Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from '@/components/ui/empty'

/**
 * Một phần của trang không tải được.
 * @param onRetry có thì hiện nút "Thử lại"
 */
export function ErrorState({ error, onRetry }: { error: Error; onRetry?: () => void }) {
  const apiError = error instanceof ApiError ? error : null
  return (
    <Empty>
      <EmptyHeader>
        <EmptyMedia variant="icon">
          <TriangleAlertIcon aria-hidden="true" />
        </EmptyMedia>
        <EmptyTitle>Không tải được dữ liệu</EmptyTitle>
        <EmptyDescription>
          {apiError ? apiError.detail : 'Có lỗi ngoài dự tính.'}
          {apiError?.requestId && <span className="block">Mã để báo lại: {apiError.requestId}</span>}
        </EmptyDescription>
      </EmptyHeader>
      {onRetry && (
        <EmptyContent>
          <Button variant="outline" onClick={onRetry}>
            Thử lại
          </Button>
        </EmptyContent>
      )}
    </Empty>
  )
}
