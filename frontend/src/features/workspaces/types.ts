import type { IdentitySchemas, Present } from '@/api'

export type Workspace = Present<
  IdentitySchemas['WorkspaceResponse'],
  'id' | 'name' | 'status' | 'ownerUserId' | 'role'
>
