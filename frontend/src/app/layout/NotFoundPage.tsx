import { Link } from 'react-router'

import { Button } from '@/components/ui/button'
import { Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'

/**
 * Đường dẫn không ứng với gì cả.
 * @param homeHref nơi nút quay về trỏ tới; mặc định là trang đầu
 */
export function NotFoundPage({ homeHref = '/' }: { homeHref?: string }) {
  return (
    <Empty>
      <EmptyHeader>
        <EmptyTitle>Không tìm thấy</EmptyTitle>
        <EmptyDescription>Đường dẫn này không còn, hoặc chưa từng có.</EmptyDescription>
      </EmptyHeader>
      <EmptyContent>
        <Button variant="outline" nativeButton={false} render={<Link to={homeHref} />}>
          Về trang đầu
        </Button>
      </EmptyContent>
    </Empty>
  )
}
