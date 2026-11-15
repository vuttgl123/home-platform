import { LogOutIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { useCurrentUser } from '@/features/users'

import { useSession } from '../session/useSession'

/** Chân thanh bên: ai đang dùng, và lối đổi sang người dùng khác. */
export function UserMenu() {
  const me = useCurrentUser()
  const { signOut } = useSession()

  return (
    <div data-testid="user-menu" className="flex items-center gap-2 rounded-lg px-2 py-1.5">
      <div className="flex min-w-0 flex-1 flex-col leading-tight">
        <span className="truncate text-sm font-medium">{me.data?.displayName ?? '…'}</span>
        <span className="truncate text-xs text-muted-foreground">{me.data?.email}</span>
      </div>
      <Button variant="ghost" size="icon-sm" aria-label="Đổi người dùng" title="Đổi người dùng" onClick={signOut}>
        <LogOutIcon aria-hidden="true" />
      </Button>
    </div>
  )
}
