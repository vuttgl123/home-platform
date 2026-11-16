import { Link } from 'react-router'

import { Button } from '@/components/ui/button'
import { Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'

/**
 * Thứ được hỏi tới không có.
 * @param description câu giải thích, mặc định cho một đường dẫn
 * @param homeHref nơi nút quay về trỏ tới
 */
export function NotFoundState({
  description = 'Đường dẫn này không còn, hoặc chưa từng có.',
  homeHref = '/',
}: {
  description?: string
  homeHref?: string
}) {
  return (
    <Empty>
      <EmptyHeader>
        <EmptyTitle>Không tìm thấy</EmptyTitle>
        <EmptyDescription>{description}</EmptyDescription>
      </EmptyHeader>
      <EmptyContent>
        <Button variant="outline" nativeButton={false} render={<Link to={homeHref} />}>
          Về trang đầu
        </Button>
      </EmptyContent>
    </Empty>
  )
}
