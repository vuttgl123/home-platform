import { PencilIcon, PlusIcon } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { useParams, useSearchParams } from 'react-router'

import { ComingSoonState } from '@/components/common/ComingSoonState'
import { ErrorState } from '@/components/common/ErrorState'
import { NotFoundState } from '@/components/common/NotFoundState'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { isId } from '@/lib/ids'

import { AddressDialog } from '../components/AddressDialog'
import { PlaceActionsMenu } from '../components/PlaceActionsMenu'
import { PlaceChildrenTab } from '../components/PlaceChildrenTab'
import { DeletePlaceDialog } from '../components/DeletePlaceDialog'
import { MovePlaceDialog } from '../components/MovePlaceDialog'
import { PlaceFormDialog } from '../components/PlaceFormDialog'
import { PlaceHistoryTab } from '../components/PlaceHistoryTab'
import { PlaceHeader } from '../components/PlaceHeader'
import { PlaceOverviewTab } from '../components/PlaceOverviewTab'
import { ReissuePlaceCodeDialog } from '../components/ReissuePlaceCodeDialog'
import { usePlace } from '../queries'
import type { PlaceDetail } from '../types'

const TABS = [
  { value: 'overview', label: 'Tổng quan' },
  { value: 'children', label: 'Bên trong' },
  { value: 'owners', label: 'Sở hữu' },
  { value: 'people', label: 'Người ở' },
  { value: 'items', label: 'Đồ đạc' },
  { value: 'photos', label: 'Ảnh' },
  { value: 'history', label: 'Lịch sử' },
] as const

type Tab = (typeof TABS)[number]['value']

/**
 * Nội dung của những tab thuộc nghiệp vụ dựa trên địa điểm (sở hữu, người ở, đồ đạc). Trang địa điểm không
 * nhập các feature đó; `app/` ghép chúng vào. Tab chưa có nội dung hiện "Sắp có".
 */
export type PlaceTabSlots = Partial<Record<'owners' | 'people' | 'items' | 'photos', (ws: string, place: PlaceDetail) => ReactNode>>

function isTab(value: string | null): value is Tab {
  return TABS.some((tab) => tab.value === value)
}

/** Trang `/w/:workspaceId/places/:placeId`, màn hình chính. Tab đang mở nằm trên URL (`?tab=`). */
export function PlacePage({ tabs = {} }: { tabs?: PlaceTabSlots }) {
  const { workspaceId = '', placeId } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const requested = searchParams.get('tab')
  const tab: Tab = isTab(requested) ? requested : 'overview'
  const place = usePlace(workspaceId, placeId)
  const [dialog, setDialog] = useState<'edit' | 'addChild' | 'move' | 'address' | 'reissueCode' | 'delete' | null>(null)

  if (!isId(placeId) || place.error?.isNotFound) {
    return <NotFoundState description="Địa điểm này không có, hoặc đã bị xoá." homeHref={`/w/${workspaceId}`} />
  }
  if (place.error) {
    return <ErrorState error={place.error} onRetry={() => void place.refetch()} />
  }
  if (!place.data) {
    return (
      <div className="flex flex-col gap-3" aria-label="Đang tải">
        <Skeleton className="h-4 w-48" />
        <Skeleton className="h-8 w-72" />
        <Skeleton className="h-5 w-40" />
      </div>
    )
  }

  const detail = place.data
  function selectTab(next: Tab) {
    // Tab mặc định không ghi lên URL, để đường dẫn của một địa điểm luôn có một dạng gọn.
    setSearchParams(next === 'overview' ? {} : { tab: next }, { replace: true })
  }

  return (
    <div className="flex flex-col gap-6">
      <PlaceHeader
        ws={workspaceId}
        place={detail}
        actions={
          <>
            <Button variant="outline" onClick={() => setDialog('edit')}>
              <PencilIcon aria-hidden="true" />
              Sửa
            </Button>
            <PlaceActionsMenu hasCode={detail.code != null} printHref={`/w/${workspaceId}/places/${detail.id}/print`} onSelect={setDialog} />
          </>
        }
      />
      <Tabs value={tab} onValueChange={(value) => selectTab(value as Tab)}>
        <TabsList variant="line" className="w-full justify-start overflow-x-auto overflow-y-hidden">
          {TABS.map(({ value, label }) => (
            <TabsTrigger key={value} value={value} className="flex-none">
              {label}
            </TabsTrigger>
          ))}
        </TabsList>
        <TabsContent value="overview" className="pt-4">
          <PlaceOverviewTab
            ws={workspaceId}
            place={detail}
            addressActions={
              <Button variant="outline" size="sm" onClick={() => setDialog('address')}>
                {detail.address ? 'Sửa địa chỉ' : 'Thêm địa chỉ'}
              </Button>
            }
          />
        </TabsContent>
        <TabsContent value="children" className="pt-4">
          <PlaceChildrenTab
            ws={workspaceId}
            place={detail}
            addAction={
              <Button variant="outline" onClick={() => setDialog('addChild')}>
                <PlusIcon aria-hidden="true" />
                Thêm chỗ con
              </Button>
            }
          />
        </TabsContent>
        {(['owners', 'people', 'items', 'photos'] as const).map((value) => (
          <TabsContent key={value} value={value} className="pt-4">
            {tabs[value]?.(workspaceId, detail) ?? <ComingSoonState />}
          </TabsContent>
        ))}
        <TabsContent value="history" className="pt-4">
          <PlaceHistoryTab ws={workspaceId} place={detail} />
        </TabsContent>
      </Tabs>
      <PlaceFormDialog
        ws={workspaceId}
        mode="edit"
        place={detail}
        open={dialog === 'edit'}
        onOpenChange={(open) => setDialog(open ? 'edit' : null)}
      />
      <PlaceFormDialog
        ws={workspaceId}
        mode="create"
        parent={detail}
        open={dialog === 'addChild'}
        onOpenChange={(open) => setDialog(open ? 'addChild' : null)}
      />
      <MovePlaceDialog
        ws={workspaceId}
        place={detail}
        open={dialog === 'move'}
        onOpenChange={(open) => setDialog(open ? 'move' : null)}
      />
      <AddressDialog
        ws={workspaceId}
        place={detail}
        open={dialog === 'address'}
        onOpenChange={(open) => setDialog(open ? 'address' : null)}
      />
      <ReissuePlaceCodeDialog
        ws={workspaceId}
        place={detail}
        open={dialog === 'reissueCode'}
        onOpenChange={(open) => setDialog(open ? 'reissueCode' : null)}
      />
      <DeletePlaceDialog
        ws={workspaceId}
        place={detail}
        open={dialog === 'delete'}
        onOpenChange={(open) => setDialog(open ? 'delete' : null)}
      />
    </div>
  )
}
