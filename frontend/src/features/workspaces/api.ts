import { fetchAllPages, identity, unwrap, userHeaders, type CursorPage } from '@/api'

import type { Workspace } from './types'

/** Mọi không gian mà người dùng hiện tại là thành viên, theo tên. */
export function listMyWorkspaces(): Promise<Workspace[]> {
  return fetchAllPages(
    async (cursor) =>
      (await unwrap(
        identity.GET('/workspaces', { params: { header: userHeaders(), query: { cursor, limit: 100 } } }),
      )) as CursorPage<Workspace>,
  )
}

/** Tạo một không gian; người gọi thành chủ của nó. */
export async function createWorkspace(name: string): Promise<Workspace> {
  return (await unwrap(identity.POST('/workspaces', { params: { header: userHeaders() }, body: { name } }))) as Workspace
}
