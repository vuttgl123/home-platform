import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useSyncExternalStore } from 'react'
import { useParams } from 'react-router'

import { sessionStore } from './sessionStore'

/** Người dùng đang chọn và hai thao tác vào, ra. */
export function useSession(): { userId: string | null; signIn: (userId: string) => void; signOut: () => void } {
  const userId = useSyncExternalStore(sessionStore.subscribe, sessionStore.getUserId)
  const queryClient = useQueryClient()
  const signOut = useCallback(() => {
    sessionStore.signOut()
    // Dữ liệu đã tải thuộc về người vừa ra; người vào sau không được thấy nó.
    queryClient.clear()
  }, [queryClient])
  return { userId, signIn: sessionStore.signIn, signOut }
}

/**
 * Không gian đang làm việc, lấy từ URL `/w/:workspaceId`.
 * @throws Error nếu dùng ở một trang nằm ngoài `/w/:workspaceId`
 */
export function useWorkspaceId(): string {
  const { workspaceId } = useParams()
  if (!workspaceId) {
    throw new Error('useWorkspaceId must be used under /w/:workspaceId')
  }
  return workspaceId
}
