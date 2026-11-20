import { UserIcon } from 'lucide-react'

import { currentUserId } from '@/api'
import { PersonAvatar } from '@/components/common/PersonAvatar'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'

import { useCurrentUser } from '../queries'

/**
 * Ai đã làm một thay đổi: "Bạn" kèm chữ cái đầu của mình, hay "Người dùng khác" kèm biểu tượng
 * người (registry chưa biết tên của các thành viên khác).
 */
export function UserChip({ userId }: { userId: string }) {
  const me = useCurrentUser()
  const mine = userId === currentUserId()
  return (
    <span className="inline-flex items-center gap-1.5">
      {mine ? (
        <PersonAvatar name={me.data?.displayName} size="sm" />
      ) : (
        <Avatar size="sm" aria-hidden="true">
          <AvatarFallback className="bg-muted text-muted-foreground">
            <UserIcon className="size-3" />
          </AvatarFallback>
        </Avatar>
      )}
      {mine ? 'Bạn' : 'Người dùng khác'}
    </span>
  )
}
