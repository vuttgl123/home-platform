import type { Present, RegistrySchemas } from '@/api'

export type PlaceType = Present<RegistrySchemas['PlaceTypeResponse'], 'id' | 'code' | 'name' | 'sortOrder' | 'status'>
