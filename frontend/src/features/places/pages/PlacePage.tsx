import { useParams, useSearchParams } from 'react-router'

import { ComingSoonState } from '@/components/common/ComingSoonState'
import { ErrorState } from '@/components/common/ErrorState'
import { NotFoundState } from '@/components/common/NotFoundState'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { isId } from '@/lib/ids'

import { PlaceChildrenTab } from '../components/PlaceChildrenTab'
import { PlaceHeader } from '../components/PlaceHeader'
import { PlaceOverviewTab } from '../components/PlaceOverviewTab'
import { usePlace } from '../queries'

const TABS = [
  { value: 'overview', label: 'Tổng quan' },
  { value: 'children', label: 'Bên trong' },
  { value: 'people', label: 'Người ở' },
  { value: 'items', label: 'Đồ đạc' },
  { value: 'history', label: 'Lịch sử' },
] as const

type Tab = (typeof TABS)[number]['value']

function isTab(value: string | null): value is Tab {
  return TABS.some((tab) => tab.value === value)
}

/** Trang `/w/:workspaceId/places/:placeId`, màn hình chính. Tab đang mở nằm trên URL (`?tab=`). */
export function PlacePage() {
  const { workspaceId = '', placeId } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const requested = searchParams.get('tab')
  const tab: Tab = isTab(requested) ? requested : 'overview'
  const place = usePlace(workspaceId, placeId)

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
      <PlaceHeader ws={workspaceId} place={detail} />
      <Tabs value={tab} onValueChange={(value) => selectTab(value as Tab)}>
        <TabsList variant="line" className="w-full justify-start overflow-x-auto">
          {TABS.map(({ value, label }) => (
            <TabsTrigger key={value} value={value} className="flex-none">
              {label}
            </TabsTrigger>
          ))}
        </TabsList>
        <TabsContent value="overview" className="pt-4">
          <PlaceOverviewTab ws={workspaceId} place={detail} />
        </TabsContent>
        <TabsContent value="children" className="pt-4">
          <PlaceChildrenTab ws={workspaceId} place={detail} />
        </TabsContent>
        {(['people', 'items', 'history'] as const).map((value) => (
          <TabsContent key={value} value={value} className="pt-4">
            <ComingSoonState />
          </TabsContent>
        ))}
      </Tabs>
    </div>
  )
}
