import { Navigate, useLocation, useNavigate } from 'react-router'

import { ErrorState } from '@/components/common/ErrorState'
import { Button } from '@/components/ui/button'
import { Spinner } from '@/components/ui/spinner'
import { useMyWorkspaces, WorkspaceForm } from '@/features/workspaces'

import { LoginPage } from '../session/LoginPage'
import { sessionStore } from '../session/sessionStore'
import { useSession } from '../session/useSession'

/**
 * Trang `/`: chưa chọn người dùng thì chọn; đã chọn thì quay lại trang đã định mở (nếu bị chặn
 * ở `RequireUser`), không thì vào một không gian.
 */
export function StartPage() {
  const { userId } = useSession()
  const from = (useLocation().state as { from?: string } | null)?.from
  if (userId == null) {
    return <LoginPage />
  }
  // Quyết định ở đây chứ không trong LoginPage: hai lệnh điều hướng đua nhau (về `from` và vào
  // không gian đầu tiên) thì lệnh chạy sau thắng.
  return from ? <Navigate to={from} replace /> : <WorkspaceChooser />
}

/** Vào không gian dùng lần trước, hoặc không gian đầu tiên; chưa có cái nào thì mời tạo. */
function WorkspaceChooser() {
  const workspaces = useMyWorkspaces()
  const navigate = useNavigate()
  const { signOut } = useSession()

  if (workspaces.isPending) {
    return (
      <main className="flex min-h-svh items-center justify-center">
        <Spinner aria-label="Đang tải" />
      </main>
    )
  }
  if (workspaces.isError) {
    return (
      <main className="flex min-h-svh items-center justify-center p-6">
        <ErrorState error={workspaces.error} onRetry={() => void workspaces.refetch()} />
      </main>
    )
  }

  const lastId = sessionStore.getLastWorkspaceId()
  const target = workspaces.data.find((workspace) => workspace.id === lastId) ?? workspaces.data[0]
  if (target) {
    return <Navigate to={`/w/${target.id}`} replace />
  }

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-md flex-col justify-center gap-6 p-6">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">Tạo không gian đầu tiên</h1>
        <p className="text-muted-foreground">
          Không gian là nơi chứa các toà nhà, phòng, người và đồ đạc của bạn. Có thể tạo thêm sau.
        </p>
      </header>
      <WorkspaceForm onCreated={(workspace) => void navigate(`/w/${workspace.id}`, { replace: true })} />
      <Button variant="link" className="self-start px-0" onClick={signOut}>
        Đổi người dùng
      </Button>
    </main>
  )
}
