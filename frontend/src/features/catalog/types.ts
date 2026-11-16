import type { Present, RegistrySchemas } from '@/api'

export type FieldDef = Present<
  RegistrySchemas['FieldDefResponse'],
  'id' | 'target' | 'typeId' | 'fieldKey' | 'label' | 'dataType' | 'required' | 'sortOrder' | 'status'
>

export type FieldTarget = FieldDef['target']

export type PlaceType = Present<RegistrySchemas['PlaceTypeResponse'], 'id' | 'code' | 'name' | 'sortOrder' | 'status'>
