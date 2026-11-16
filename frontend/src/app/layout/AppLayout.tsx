import { ActivityIcon, TagsIcon, UsersIcon, type LucideIcon } from 'lucide-react'
import { useEffect } from 'react'
import { Link, NavLink, Outlet } from 'react-router'

import { ErrorState } from '@/components/common/ErrorState'
import { Button } from '@/components/ui/button'
import { Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarInset,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
  SidebarSeparator,
  SidebarTrigger,
} from '@/components/ui/sidebar'
import { Spinner } from '@/components/ui/spinner'
import { PlaceTree } from '@/features/places'
import { useCurrentUser } from '@/features/users'
import { useMyWorkspaces } from '@/features/workspaces'

import { sessionStore } from '../session/sessionStore'
import { useSession, useWorkspaceId } from '../session/useSession'
import { UserMenu } from './UserMenu'
import { WorkspaceSwitcher } from './WorkspaceSwitcher'

const SECTIONS: { path: string; label: string; icon: LucideIcon }[] = [
  { path: 'people', label: 'Người', icon: UsersIcon },
  { path: 'catalog', label: 'Danh mục', icon: TagsIcon },
  { path: 'activity', label: 'Hoạt động', icon: ActivityIcon },
]

/**
 * Khung của mọi trang trong một không gian: thanh bên (không gian, cây địa điểm, các mục khác,
 * người dùng) và phần chính.
 *
 * Không gian trên URL phải là một không gian của người dùng. Registry chưa kiểm tra điều đó, nên
 * khung này chặn trước khi trang con nào kịp gọi nó.
 */
export function AppLayout() {
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

  const current = workspaces.data?.find((workspace) => workspace.id === workspaceId)
  const currentId = current?.id
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
  if (!current) {
    return (
      <main className="flex min-h-svh items-center justify-center p-6">
        <Empty>
          <EmptyHeader>
            <EmptyTitle>Không tìm thấy không gian</EmptyTitle>
            <EmptyDescription>
              Không gian này không có, hoặc bạn không phải thành viên của nó.
            </EmptyDescription>
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

  const base = `/w/${current.id}`
  return (
    <SidebarProvider>
      <Sidebar>
        <SidebarHeader>
          <WorkspaceSwitcher workspaces={workspaces.data} current={current} />
        </SidebarHeader>
        <SidebarContent>
          {/* `key`: đổi không gian thì cây bắt đầu lại, không giữ các nhánh đã mở của không gian cũ. */}
          <PlaceTree key={current.id} workspaceId={current.id} />
        </SidebarContent>
        <SidebarSeparator />
        <SidebarFooter>
          <SidebarMenu>
            {SECTIONS.map(({ path, label, icon: Icon }) => (
              <SidebarMenuItem key={path}>
                {/* NavLink đặt aria-current="page" khi đang ở trang đó (hoặc trang con của nó). */}
                <SidebarMenuButton render={<NavLink to={`${base}/${path}`} />}>
                  <Icon aria-hidden="true" />
                  <span>{label}</span>
                </SidebarMenuButton>
              </SidebarMenuItem>
            ))}
          </SidebarMenu>
          <UserMenu />
        </SidebarFooter>
      </Sidebar>
      <SidebarInset>
        <header className="flex h-12 shrink-0 items-center gap-2 border-b px-4">
          <SidebarTrigger aria-label="Ẩn hoặc hiện thanh bên" />
        </header>
        <div className="flex-1 p-6">
          <Outlet />
        </div>
      </SidebarInset>
    </SidebarProvider>
  )
}
