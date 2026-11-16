import type { Present, RegistrySchemas } from '@/api'

export type FieldDef = Present<
  RegistrySchemas['FieldDefResponse'],
  'id' | 'target' | 'typeId' | 'fieldKey' | 'label' | 'dataType' | 'required' | 'sortOrder' | 'status'
>

export type FieldTarget = FieldDef['target']

export type ItemCategory = Present<RegistrySchemas['ItemCategoryResponse'], 'id' | 'name' | 'sortOrder' | 'status'>

export type CodeTarget = 'PLACE' | 'ITEM'

/** Một dạng mã tự cấp; `typeId` `'0'` là dạng chung của đích, `builtIn` khi không gian chưa đặt. */
export type CodeRule = Omit<
  Present<RegistrySchemas['CodeRuleResponse'], 'target' | 'typeId' | 'prefix' | 'digits' | 'yearly' | 'builtIn'>,
  'target'
> & { target: CodeTarget }

export type CodeStatus = Present<RegistrySchemas['CodeStatusResponse'], 'placesWithoutCode' | 'itemsWithoutCode'>

export type CodeBackfillResult = Present<
  RegistrySchemas['CodeBackfillResponse'],
  'placesAssigned' | 'itemsAssigned' | 'placesRemaining' | 'itemsRemaining'
>

export type PlaceType = Present<RegistrySchemas['PlaceTypeResponse'], 'id' | 'code' | 'name' | 'sortOrder' | 'status'>
