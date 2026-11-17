import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import type { ApiError } from '@/api'
import { setupWorkspace } from '@/features/catalog'

import { createWorkspace, listMyWorkspaces } from './api'
import type { Workspace } from './types'

export const workspaceKeys = {
  mine: ['workspaces', 'mine'] as const,
}

export function useMyWorkspaces() {
  return useQuery<Workspace[], ApiError>({ queryKey: workspaceKeys.mine, queryFn: listMyWorkspaces })
}

/** Tên của không gian `ws`, hoặc `undefined` khi danh sách chưa tải xong hay không có không gian đó. */
export function useWorkspaceName(ws: string): string | undefined {
  return useMyWorkspaces().data?.find((workspace) => workspace.id === ws)?.name
}

/**
 * Tạo không gian bên identity, rồi tạo danh mục mặc định của nó bên registry.
 *
 * Bước thứ hai hỏng thì không gian vẫn được trả về: nó đã tồn tại, và trang chủ của nó mời
 * chạy lại bước này.
 */
export function useCreateWorkspace() {
  const queryClient = useQueryClient()
  return useMutation<Workspace, ApiError, { name: string }>({
    mutationFn: async ({ name }) => {
      const created = await createWorkspace(name)
      await setupWorkspace(created.id).catch((error: unknown) => console.error(error))
      return created
    },
    onSuccess: (created) => {
      // Thêm ngay vào cache để trang của không gian mới không bị coi là "không tìm thấy".
      queryClient.setQueryData<Workspace[]>(workspaceKeys.mine, (old) => [...(old ?? []), created])
      void queryClient.invalidateQueries({ queryKey: workspaceKeys.mine })
    },
  })
}
