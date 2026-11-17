import {
  ActivityIcon,
  LayoutDashboardIcon,
  PackageIcon,
  SearchIcon,
  TagsIcon,
  Trash2Icon,
  UsersIcon,
  type LucideIcon,
} from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation } from 'react-router'

import { ErrorState } from '@/components/common/ErrorState'
import { Button } from '@/components/ui/button'
import { Kbd } from '@/components/ui/kbd'
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
import { SearchPalette, useSearchShortcut } from '@/features/search'
import { useCurrentUser } from '@/features/users'
import { useMyWorkspaces } from '@/features/workspaces'

import { sessionStore } from '../session/sessionStore'
import { useSession, useWorkspaceId } from '../session/useSession'
import { UserMenu } from './UserMenu'
import { WorkspaceSwitcher } from './WorkspaceSwitcher'

/** `path` rỗng là trang chủ của không gian: chỉ sáng khi đang ở đúng trang đó. */
const SECTIONS: { path: string; label: string; icon: LucideIcon }[] = [
  { path: '', label: 'Tổng quan', icon: LayoutDashboardIcon },
  { path: 'people', label: 'Người', icon: UsersIcon },
  { path: 'items', label: 'Đồ đạc', icon: PackageIcon },
  { path: 'catalog', label: 'Danh mục', icon: TagsIcon },
  { path: 'activity', label: 'Hoạt động', icon: ActivityIcon },
  { path: 'trash', label: 'Thùng rác', icon: Trash2Icon },
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
  const { pathname } = useLocation()
  const [searching, setSearching] = useState(false)
  const openSearch = useCallback(() => setSearching(true), [])
  useSearchShortcut(openSearch)

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
                {/* NavLink đặt aria-current="page" cho công cụ đọc màn hình; isActive tô dòng (data-active). */}
                <SidebarMenuButton
                  isActive={path === '' ? pathname === base || pathname === `${base}/` : pathname.startsWith(`${base}/${path}`)}
                  render={<NavLink to={path === '' ? base : `${base}/${path}`} end={path === ''} />}
                >
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
          <Button variant="outline" size="sm" className="text-muted-foreground" onClick={openSearch}>
            <SearchIcon aria-hidden="true" />
            Tìm…
            <Kbd className="ml-2">Ctrl K</Kbd>
          </Button>
        </header>
        {/* `key`: đổi không gian thì ô tìm bắt đầu lại. */}
        <SearchPalette key={current.id} ws={current.id} open={searching} onOpenChange={setSearching} />
        <div className="flex-1 p-6">
          <Outlet />
        </div>
      </SidebarInset>
    </SidebarProvider>
  )
}
