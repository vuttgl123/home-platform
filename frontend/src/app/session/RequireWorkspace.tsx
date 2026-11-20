import { useEffect } from 'react'
import { Link, Outlet } from 'react-router'

import { ErrorState } from '@/components/common/ErrorState'
import { Button } from '@/components/ui/button'
import { Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Spinner } from '@/components/ui/spinner'
import { useCurrentUser } from '@/features/users'
import { useMyWorkspaces } from '@/features/workspaces'

import { sessionStore } from './sessionStore'
import { useSession, useWorkspaceId } from './useSession'

/**
 * Chặn mọi trang dưới `/w/:workspaceId`: không gian trên URL phải là một không gian của người
 * dùng. Registry chưa kiểm tra điều đó, nên route này chặn trước khi trang con nào kịp gọi nó; cả
 * những trang nằm ngoài khung có thanh bên (trang in) cũng đi qua đây.
 */
export function RequireWorkspace() {
  const workspaceId = useWorkspaceId()
  const workspaces = useMyWorkspaces()
  const me = useCurrentUser()
  const { signOut } = useSession()

  // Id đã lưu không còn ứng với người dùng nào (database đã đổi): quay về chọn người dùng.
  const userIsGone = me.error?.isNotFound === true
  useEffect(() => {
    if (userIsGone) {
      signOut()
    }
  }, [userIsGone, signOut])

  const currentId = workspaces.data?.find((workspace) => workspace.id === workspaceId)?.id
  useEffect(() => {
    if (currentId) {
      sessionStore.rememberWorkspace(currentId)
    }
  }, [currentId])

  if (workspaces.isPending) {
    return (
      <div className="flex min-h-svh items-center justify-center">
        <Spinner aria-label="Đang tải" />
      </div>
    )
  }
  if (workspaces.isError) {
    return (
      <div className="flex min-h-svh items-center justify-center p-6">
        <ErrorState error={workspaces.error} onRetry={() => void workspaces.refetch()} />
      </div>
    )
  }
  if (!currentId) {
    return (
      <main className="flex min-h-svh items-center justify-center p-6">
        <Empty>
          <EmptyHeader>
            <EmptyTitle>Không tìm thấy không gian</EmptyTitle>
            <EmptyDescription>Không gian này không có, hoặc bạn không phải thành viên của nó.</EmptyDescription>
          </EmptyHeader>
          <EmptyContent>
            <Button nativeButton={false} render={<Link to="/" />}>
              Về không gian của tôi
            </Button>
          </EmptyContent>
        </Empty>
      </main>
    )
  }
  return <Outlet />
}
