import {
  ActivityIcon,
  BedDoubleIcon,
  LayoutDashboardIcon,
  PackageIcon,
  SearchIcon,
  TagsIcon,
  Trash2Icon,
  UsersIcon,
  type LucideIcon,
} from 'lucide-react'
import { useCallback, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router'

import { WithTooltip } from '@/components/common/WithTooltip'
import { Button } from '@/components/ui/button'
import { Kbd } from '@/components/ui/kbd'
import { ScrollArea } from '@/components/ui/scroll-area'
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarInset,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
  SidebarSeparator,
  SidebarTrigger,
} from '@/components/ui/sidebar'
import { PlaceTree } from '@/features/places'
import { SearchPalette, useSearchShortcut } from '@/features/search'
import { useMyWorkspaces } from '@/features/workspaces'

import { useWorkspaceId } from '../session/useSession'
import { UserMenu } from './UserMenu'
import { WorkspaceSwitcher } from './WorkspaceSwitcher'

/** `path` rỗng là trang chủ của không gian: chỉ sáng khi đang ở đúng trang đó. */
const SECTIONS: { path: string; label: string; icon: LucideIcon }[] = [
  { path: '', label: 'Tổng quan', icon: LayoutDashboardIcon },
  { path: 'people', label: 'Người', icon: UsersIcon },
  { path: 'vacancies', label: 'Chỗ trống', icon: BedDoubleIcon },
  { path: 'items', label: 'Đồ đạc', icon: PackageIcon },
  { path: 'catalog', label: 'Danh mục', icon: TagsIcon },
  { path: 'activity', label: 'Hoạt động', icon: ActivityIcon },
  { path: 'trash', label: 'Thùng rác', icon: Trash2Icon },
]

/**
 * Khung của mọi trang trong một không gian: thanh bên (không gian, cây địa điểm, các mục khác,
 * người dùng) và phần chính.
 *
 * Nằm dưới `RequireWorkspace`: không gian trên URL đã được kiểm tra là của người dùng.
 */
export function AppLayout() {
  const workspaceId = useWorkspaceId()
  const workspaces = useMyWorkspaces()
  const { pathname } = useLocation()
  const [searching, setSearching] = useState(false)
  const openSearch = useCallback(() => setSearching(true), [])
  useSearchShortcut(openSearch)

  // RequireWorkspace đã chặn không gian không phải của người dùng, nên ở đây luôn tìm thấy.
  const current = workspaces.data?.find((workspace) => workspace.id === workspaceId)
  if (!current || !workspaces.data) {
    return null
  }

  const base = `/w/${current.id}`
  return (
    <SidebarProvider>
      {/* Thanh bên luôn tối: lớp `dark` đổi mọi token bên trong nó sang bộ tối. */}
      <Sidebar className="dark">
        <SidebarHeader>
          <WorkspaceSwitcher workspaces={workspaces.data} current={current} />
        </SidebarHeader>
        <SidebarContent className="gap-0 overflow-hidden">
          <SidebarGroup>
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
          </SidebarGroup>
          <SidebarSeparator />
          {/* Cây dài thì cuộn trong vùng của nó; điều hướng chính và người dùng luôn thấy. */}
          <SidebarGroup className="min-h-0 flex-1">
            <SidebarGroupLabel>Địa điểm</SidebarGroupLabel>
            <ScrollArea className="min-h-0 flex-1">
              {/* `key`: đổi không gian thì cây bắt đầu lại, không giữ các nhánh đã mở của không gian cũ. */}
              <PlaceTree key={current.id} workspaceId={current.id} />
            </ScrollArea>
          </SidebarGroup>
        </SidebarContent>
        <SidebarSeparator />
        <SidebarFooter>
          <UserMenu />
        </SidebarFooter>
      </Sidebar>
      <SidebarInset>
        <header className="sticky top-0 z-10 flex h-14 shrink-0 items-center gap-3 border-b bg-background/85 px-4 backdrop-blur">
          <WithTooltip label="Ẩn hoặc hiện thanh bên">
            <SidebarTrigger aria-label="Ẩn hoặc hiện thanh bên" />
          </WithTooltip>
          <Button
            variant="outline"
            size="sm"
            className="min-w-0 max-w-sm flex-1 justify-start bg-card text-muted-foreground shadow-none"
            onClick={openSearch}
          >
            <SearchIcon aria-hidden="true" />
            Tìm…
            <Kbd className="ml-auto hidden sm:inline-flex">Ctrl K</Kbd>
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
