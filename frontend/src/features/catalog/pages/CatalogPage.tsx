import { useParams } from 'react-router'

import { CodeRuleSection } from '../components/CodeRuleSection'
import { FieldDefSection } from '../components/FieldDefSection'
import { ItemCategorySection } from '../components/ItemCategorySection'
import { PlaceTypeSection } from '../components/PlaceTypeSection'

/** Trang `/w/:workspaceId/catalog`: loại địa điểm, nhóm đồ, mục tuỳ biến và dạng mã tự cấp của không gian. */
export function CatalogPage() {
  const { workspaceId = '' } = useParams()
  return (
    <div className="flex max-w-4xl flex-col gap-10">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">Danh mục</h1>
        <p className="text-muted-foreground">Những gì không gian này dùng để phân loại và mô tả địa điểm, người, đồ đạc.</p>
      </header>
      <PlaceTypeSection ws={workspaceId} />
      <ItemCategorySection ws={workspaceId} />
      <FieldDefSection ws={workspaceId} />
      <CodeRuleSection ws={workspaceId} />
    </div>
  )
}
