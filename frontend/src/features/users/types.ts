import type { IdentitySchemas, Present } from '@/api'

export type User = Present<IdentitySchemas['UserResponse'], 'id' | 'email' | 'displayName' | 'status'>
