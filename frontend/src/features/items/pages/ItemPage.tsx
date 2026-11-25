import { ArrowRightLeftIcon, FolderInputIcon, HashIcon, MoreHorizontalIcon, UserRoundIcon, PackageMinusIcon, PencilIcon, Trash2Icon, Undo2Icon } from 'lucide-react'
import { Fragment, useState, type ReactNode } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'

import { Breadcrumb, BreadcrumbItem, BreadcrumbLink, BreadcrumbList, BreadcrumbPage, BreadcrumbSeparator } from '@/components/ui/breadcrumb'
import { CoverImage } from '@/components/common/CoverImage'
import { PersonAvatar } from '@/components/common/PersonAvatar'
import { InfoCard, InfoList } from '@/components/common/InfoCard'
import { WithTooltip } from '@/components/common/WithTooltip'
import { ToneBadge } from '@/components/common/ToneBadge'
import { ErrorState } from '@/components/common/ErrorState'
import { NotFoundState } from '@/components/common/NotFoundState'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { AttributeView, useFieldDefs } from '@/features/catalog'
import { thumbnailUrl } from '@/features/media'
import { usePlace } from '@/features/places'
import { todayInBusinessZone } from '@/lib/dates'
import { formatDate, formatDateTime } from '@/lib/format'
import { isId } from '@/lib/ids'

import { ChangeOwnerDialog } from '../components/ChangeOwnerDialog'
import { DeleteItemDialog } from '../components/DeleteItemDialog'
import { ItemFormDialog } from '../components/ItemFormDialog'
import { ItemHistoryTab } from '../components/ItemHistoryTab'
import { ItemPhotosTab } from '../components/ItemPhotosTab'
import { MoveItemDialog } from '../components/MoveItemDialog'
import { PutBackItemDialog } from '../components/PutBackItemDialog'
import { ReissueItemCodeDialog } from '../components/ReissueItemCodeDialog'
import { RemoveItemDialog } from '../components/RemoveItemDialog'
import { ReplaceItemDialog } from '../components/ReplaceItemDialog'
import { ITEM_STATUS_LABEL, ITEM_STATUS_TONE, warrantyLine } from '../labels'
import { useItem } from '../queries'
import type { ItemDetail } from '../types'

type Dialog = 'edit' | 'move' | 'remove' | 'replace' | 'owner' | 'reissueCode' | 'putBack' | 'delete'

/** Các tab của trang món đồ thuộc nghiệp vụ khác; trang món đồ không nhập các feature đó. */
export type ItemTabSlots = { maintenance?: (ws: string, item: ItemDetail) => ReactNode }

/** Trang `/w/:workspaceId/items/:itemId`: một món đồ và vòng đời của nó. Món đã gỡ chỉ đọc, trừ đưa lại và xoá. */
export function ItemPage({ tabs = {} }: { tabs?: ItemTabSlots }) {
  const { workspaceId = '', itemId } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const requestedTab = searchParams.get('tab')
  const tab =
    requestedTab === 'history' || requestedTab === 'photos' || (requestedTab === 'maintenance' && tabs.maintenance)
      ? requestedTab
      : 'info'
  const item = useItem(workspaceId, itemId)
  const [dialog, setDialog] = useState<Dialog | null>(null)
  // Món đã bị thay: đọc món thay nó để biết có đưa lại được không.
  const replacement = useItem(workspaceId, item.data?.replacedByItemId)
  // Tổ tiên của chỗ đặt cho thanh đường dẫn; chưa tải xong (hay không mở được) thì chỉ có tên chỗ đặt.
  const place = usePlace(workspaceId, item.data?.placeId)

  if (!isId(itemId) || item.error?.isNotFound) {
    return <NotFoundState description="Món đồ này không có, hoặc đã bị xoá." homeHref={`/w/${workspaceId}`} />
  }
  if (item.error) {
    return <ErrorState error={item.error} onRetry={() => void item.refetch()} />
  }
  if (!item.data) {
    return (
      <div className="flex flex-col gap-3" aria-label="Đang tải">
        <Skeleton className="h-4 w-24" />
        <Skeleton className="h-8 w-64" />
      </div>
    )
  }

  const detail = item.data
  const removed = detail.removedAt != null
  // Đưa lại bị chặn khi món thay nó còn đó; món thay đã bị xoá (404) thì đưa lại được.
  const replacementAlive =
    detail.replacedByItemId != null && (replacement.data != null || (replacement.isLoading && !replacement.error))
  const open = (next: Dialog) => () => setDialog(next)
  const close = (name: Dialog) => (isOpen: boolean) => setDialog(isOpen ? name : null)

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-col gap-3">
        <Breadcrumb aria-label="Đường dẫn">
          <BreadcrumbList>
            <BreadcrumbItem>
              <BreadcrumbLink render={<Link to={`/w/${workspaceId}/items`} />}>Đồ đạc</BreadcrumbLink>
            </BreadcrumbItem>
            <BreadcrumbSeparator />
            {(place.data?.id === detail.placeId ? place.data.ancestors : []).map((ancestor) => (
              <Fragment key={ancestor.id}>
                <BreadcrumbItem>
                  <BreadcrumbLink render={<Link to={`/w/${workspaceId}/places/${ancestor.id}`} />}>{ancestor.name}</BreadcrumbLink>
                </BreadcrumbItem>
                <BreadcrumbSeparator />
              </Fragment>
            ))}
            <BreadcrumbItem>
              <BreadcrumbLink render={<Link to={`/w/${workspaceId}/places/${detail.placeId}?tab=items`} />}>{detail.placeName}</BreadcrumbLink>
            </BreadcrumbItem>
            <BreadcrumbSeparator />
            <BreadcrumbItem>
              <BreadcrumbPage>{detail.name}</BreadcrumbPage>
            </BreadcrumbItem>
          </BreadcrumbList>
        </Breadcrumb>
        <div className="flex flex-wrap items-start gap-x-4 gap-y-2">
          <CoverImage
            src={detail.coverFileId ? thumbnailUrl(workspaceId, detail.coverFileId) : null}
            alt={`Ảnh bìa của ${detail.name}`}
          />
          <h1 className="min-w-0 flex-1 self-center text-2xl font-semibold tracking-tight break-words">{detail.name}</h1>
          <div className="flex shrink-0 gap-2">
            {removed ? (
              <Button variant="outline" disabled={replacementAlive} onClick={open('putBack')}>
                <Undo2Icon aria-hidden="true" />
                Đưa lại
              </Button>
            ) : (
              <Button variant="outline" onClick={open('edit')}>
                <PencilIcon aria-hidden="true" />
                Sửa
              </Button>
            )}
            <DropdownMenu>
              <WithTooltip label="Thao tác khác">
                <DropdownMenuTrigger render={<Button variant="outline" size="icon" aria-label="Thao tác khác" />}>
                  <MoreHorizontalIcon aria-hidden="true" />
                </DropdownMenuTrigger>
              </WithTooltip>
              <DropdownMenuContent align="end" className="min-w-44">
                {!removed && (
                  <>
                    <DropdownMenuItem onClick={open('move')}>
                      <FolderInputIcon aria-hidden="true" />
                      Chuyển chỗ
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={open('remove')}>
                      <PackageMinusIcon aria-hidden="true" />
                      Gỡ
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={open('replace')}>
                      <ArrowRightLeftIcon aria-hidden="true" />
                      Thay
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={open('owner')}>
                      <UserRoundIcon aria-hidden="true" />
                      Đổi chủ
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={open('reissueCode')}>
                      <HashIcon aria-hidden="true" />
                      {detail.code ? 'Cấp mã mới' : 'Cấp mã'}
                    </DropdownMenuItem>
                    <DropdownMenuSeparator />
                  </>
                )}
                <DropdownMenuItem variant="destructive" onClick={open('delete')}>
                  <Trash2Icon aria-hidden="true" />
                  Xoá
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
        <dl className="flex flex-wrap items-center gap-2 text-sm">
          {detail.categoryName && (
            <>
              <dt className="sr-only">Nhóm</dt>
              <dd>
                <Badge variant="secondary">{detail.categoryName}</Badge>
              </dd>
            </>
          )}
          <dt className="sr-only">Trạng thái</dt>
          <dd>
            <ToneBadge tone={ITEM_STATUS_TONE[detail.status]}>{ITEM_STATUS_LABEL[detail.status]}</ToneBadge>
          </dd>
          <dt className="sr-only">Số lượng</dt>
          <dd className="text-muted-foreground">{[detail.quantity, detail.unit].filter((part) => part != null).join(' ')}</dd>
          <dt className="sr-only">Version</dt>
          <dd className="text-muted-foreground" title="Số lần đã sửa, tính cả lần tạo">
            v{detail.version}
          </dd>
          {detail.ownerPersonId && (
            <>
              <dt className="sr-only">Chủ</dt>
              <dd className="inline-flex items-center gap-1.5 text-muted-foreground">
                <PersonAvatar name={detail.ownerName} size="sm" />
                của{' '}
                <Link to={`/w/${workspaceId}/people/${detail.ownerPersonId}`} className="font-medium text-foreground hover:underline">
                  {detail.ownerName}
                </Link>
              </dd>
            </>
          )}
        </dl>
        {removed && (
          <Alert>
            <AlertDescription className="flex flex-col gap-1">
              <span>Đã gỡ ngày {formatDate(detail.removedAt!)}.</span>
              {replacement.data && (
                <span>
                  Đã thay bằng{' '}
                  <Link to={`/w/${workspaceId}/items/${replacement.data.id}`} className="font-medium underline underline-offset-4">
                    {replacement.data.name}
                  </Link>
                  .
                </span>
              )}
              {replacementAlive && <span>Món thay nó vẫn còn, nên chưa đưa lại được.</span>}
            </AlertDescription>
          </Alert>
        )}
      </header>
      <Tabs value={tab} onValueChange={(value) => setSearchParams(value === 'info' ? {} : { tab: String(value) }, { replace: true })}>
        <TabsList variant="line" className="w-full justify-start overflow-x-auto">
          <TabsTrigger value="info" className="flex-none">
            Thông tin
          </TabsTrigger>
          <TabsTrigger value="photos" className="flex-none">
            Ảnh
          </TabsTrigger>
          {tabs.maintenance && (
            <TabsTrigger value="maintenance" className="flex-none">
              Bảo trì
            </TabsTrigger>
          )}
          <TabsTrigger value="history" className="flex-none">
            Lịch sử
          </TabsTrigger>
        </TabsList>
        <TabsContent value="info" className="pt-4">
          <ItemInfo ws={workspaceId} item={detail} />
        </TabsContent>
        <TabsContent value="photos" className="pt-4">
          <ItemPhotosTab ws={workspaceId} item={detail} />
        </TabsContent>
        {tabs.maintenance && (
          <TabsContent value="maintenance" className="pt-4">
            {tabs.maintenance(workspaceId, detail)}
          </TabsContent>
        )}
        <TabsContent value="history" className="pt-4">
          <ItemHistoryTab ws={workspaceId} item={detail} />
        </TabsContent>
      </Tabs>
      {!removed && (
        <>
          <ItemFormDialog ws={workspaceId} mode="edit" item={detail} open={dialog === 'edit'} onOpenChange={close('edit')} />
          <MoveItemDialog ws={workspaceId} item={detail} open={dialog === 'move'} onOpenChange={close('move')} />
          <RemoveItemDialog ws={workspaceId} item={detail} open={dialog === 'remove'} onOpenChange={close('remove')} />
          <ReplaceItemDialog ws={workspaceId} item={detail} open={dialog === 'replace'} onOpenChange={close('replace')} />
          <ChangeOwnerDialog ws={workspaceId} item={detail} open={dialog === 'owner'} onOpenChange={close('owner')} />
          <ReissueItemCodeDialog ws={workspaceId} item={detail} open={dialog === 'reissueCode'} onOpenChange={close('reissueCode')} />
        </>
      )}
      {removed && (
        <PutBackItemDialog ws={workspaceId} item={detail} open={dialog === 'putBack'} onOpenChange={close('putBack')} />
      )}
      <DeleteItemDialog ws={workspaceId} item={detail} open={dialog === 'delete'} onOpenChange={close('delete')} />
    </div>
  )
}

function ItemInfo({ ws, item }: { ws: string; item: ItemDetail }) {
  const defs = useFieldDefs(ws, 'ITEM', item.categoryId ?? '0').data ?? []
  const rows: [string, React.ReactNode][] = [
    ['Mã', item.code],
    ['Ngày đặt', item.placedAt ? formatDate(item.placedAt) : null],
    ['Ngày gỡ', item.removedAt ? formatDate(item.removedAt) : null],
  ]
  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <InfoCard title="Thông tin">
        <InfoList
          rows={[
            ...rows.map(([label, value]) => ({ label, value })),
            { label: 'Bảo hành', value: item.warrantyUntil ? <WarrantyLine warrantyUntil={item.warrantyUntil} /> : null },
            {
              label: 'Bảo trì',
              value:
                item.serviceEveryMonths != null ? (
                  <ServiceLine everyMonths={item.serviceEveryMonths} nextOn={item.nextServiceOn ?? null} />
                ) : null,
            },
            { label: 'Mô tả', value: item.description ? <span className="whitespace-pre-line">{item.description}</span> : null },
          ]}
        />
      </InfoCard>
      {defs.length > 0 && (
        <InfoCard title="Mục tuỳ biến">
          <AttributeView defs={defs} values={item.attributes} />
        </InfoCard>
      )}
      <p className="text-xs text-muted-foreground lg:col-span-2">Sửa lần cuối {formatDateTime(item.updatedAt)}</p>
    </div>
  )
}

function WarrantyLine({ warrantyUntil }: { warrantyUntil: string }) {
  const { text, tone } = warrantyLine(warrantyUntil, todayInBusinessZone())
  return <ToneBadge tone={tone}>{text}</ToneBadge>
}

/** "Bảo trì mỗi N tháng · lần tới …"; hạn đã tới hoặc đã qua thì phần "lần tới" mang màu cảnh báo. */
function ServiceLine({ everyMonths, nextOn }: { everyMonths: number; nextOn: string | null }) {
  return (
    <span>
      {`Bảo trì mỗi ${everyMonths} tháng · `}
      {nextOn && <ToneBadge tone={nextOn <= todayInBusinessZone() ? 'warning' : 'neutral'}>{`lần tới ${formatDate(nextOn)}`}</ToneBadge>}
    </span>
  )
}
