import { MapPinIcon, PlusIcon, TagsIcon } from 'lucide-react'
import { useState } from 'react'

import { ErrorState } from '@/components/common/ErrorState'
import { FormError } from '@/components/common/FormError'
import { Button } from '@/components/ui/button'
import { Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from '@/components/ui/empty'
import { usePlaceTypes, useSetupWorkspace } from '@/features/catalog'
import { PlaceFormDialog } from '@/features/places'

import { useWorkspaceId } from '../session/useSession'

/** Trang `/w/:workspaceId`: chưa chọn địa điểm nào. */
export function WorkspaceHomePage() {
  const workspaceId = useWorkspaceId()
  const placeTypes = usePlaceTypes(workspaceId)
  const [adding, setAdding] = useState(false)

  if (placeTypes.isError) {
    return <ErrorState error={placeTypes.error} onRetry={() => void placeTypes.refetch()} />
  }
  // Không gian được tạo mà bước chuẩn bị danh mục chưa chạy xong: chưa có loại địa điểm nào,
  // nên chưa thêm được địa điểm.
  if (placeTypes.data?.length === 0) {
    return <SetupNotice workspaceId={workspaceId} />
  }
  return (
    <Empty>
      <EmptyHeader>
        <EmptyMedia variant="icon">
          <MapPinIcon aria-hidden="true" />
        </EmptyMedia>
        <EmptyTitle>Chọn một địa điểm trên cây</EmptyTitle>
        <EmptyDescription>Thông tin, người ở và đồ đạc của địa điểm sẽ hiện ở đây.</EmptyDescription>
      </EmptyHeader>
      <EmptyContent>
        <Button onClick={() => setAdding(true)}>
          <PlusIcon aria-hidden="true" />
          Thêm địa điểm
        </Button>
      </EmptyContent>
      <PlaceFormDialog ws={workspaceId} mode="create" parent={null} open={adding} onOpenChange={setAdding} />
    </Empty>
  )
}

function SetupNotice({ workspaceId }: { workspaceId: string }) {
  const setup = useSetupWorkspace(workspaceId)
  return (
    <Empty>
      <EmptyHeader>
        <EmptyMedia variant="icon">
          <TagsIcon aria-hidden="true" />
        </EmptyMedia>
        <EmptyTitle>Không gian chưa có danh mục</EmptyTitle>
        <EmptyDescription>
          Cần có loại địa điểm (toà nhà, tầng, phòng…) trước khi thêm địa điểm đầu tiên.
        </EmptyDescription>
      </EmptyHeader>
      <EmptyContent>
        <FormError messages={setup.error ? [setup.error.detail] : []} />
        <Button disabled={setup.isPending} onClick={() => setup.mutate()}>
          Tạo danh mục mặc định
        </Button>
      </EmptyContent>
    </Empty>
  )
}
