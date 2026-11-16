import { ArrowRightLeftIcon, FolderInputIcon, HashIcon, MoreHorizontalIcon, UserRoundIcon, PackageMinusIcon, PencilIcon, Trash2Icon, Undo2Icon } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'

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
import { formatDate, formatDateTime } from '@/lib/format'
import { isId } from '@/lib/ids'

import { ChangeOwnerDialog } from '../components/ChangeOwnerDialog'
import { DeleteItemDialog } from '../components/DeleteItemDialog'
import { ItemFormDialog } from '../components/ItemFormDialog'
import { ItemHistoryTab } from '../components/ItemHistoryTab'
import { MoveItemDialog } from '../components/MoveItemDialog'
import { PutBackItemDialog } from '../components/PutBackItemDialog'
import { ReissueItemCodeDialog } from '../components/ReissueItemCodeDialog'
import { RemoveItemDialog } from '../components/RemoveItemDialog'
import { ReplaceItemDialog } from '../components/ReplaceItemDialog'
import { ITEM_STATUS_LABEL } from '../labels'
import { useItem } from '../queries'
import type { ItemDetail } from '../types'

type Dialog = 'edit' | 'move' | 'remove' | 'replace' | 'owner' | 'reissueCode' | 'putBack' | 'delete'

/** Trang `/w/:workspaceId/items/:itemId`: một món đồ và vòng đời của nó. Món đã gỡ chỉ đọc, trừ đưa lại và xoá. */
export function ItemPage() {
  const { workspaceId = '', itemId } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const tab = searchParams.get('tab') === 'history' ? 'history' : 'info'
  const item = useItem(workspaceId, itemId)
  const [dialog, setDialog] = useState<Dialog | null>(null)
  // Món đã bị thay: đọc món thay nó để biết có đưa lại được không.
  const replacement = useItem(workspaceId, item.data?.replacedByItemId)

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
        <p className="text-sm text-muted-foreground">
          Ở{' '}
          <Link to={`/w/${workspaceId}/places/${detail.placeId}?tab=items`} className="font-medium text-foreground hover:underline">
            {detail.placeName}
          </Link>
          {detail.ownerPersonId && (
            <>
              {' · của '}
              <Link to={`/w/${workspaceId}/people/${detail.ownerPersonId}`} className="font-medium text-foreground hover:underline">
                {detail.ownerName}
              </Link>
            </>
          )}
        </p>
        <div className="flex flex-wrap items-start gap-x-4 gap-y-2">
          <h1 className="min-w-0 flex-1 text-2xl font-semibold tracking-tight break-words">{detail.name}</h1>
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
              <DropdownMenuTrigger render={<Button variant="outline" size="icon" aria-label="Thao tác khác" />}>
                <MoreHorizontalIcon aria-hidden="true" />
              </DropdownMenuTrigger>
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
            <Badge variant={detail.status === 'BROKEN' || removed ? 'destructive' : 'outline'}>
              {ITEM_STATUS_LABEL[detail.status]}
            </Badge>
          </dd>
          <dt className="sr-only">Số lượng</dt>
          <dd className="text-muted-foreground">{[detail.quantity, detail.unit].filter((part) => part != null).join(' ')}</dd>
          <dt className="sr-only">Version</dt>
          <dd className="text-muted-foreground" title="Số lần đã sửa, tính cả lần tạo">
            v{detail.version}
          </dd>
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
      <Tabs value={tab} onValueChange={(value) => setSearchParams(value === 'info' ? {} : { tab: 'history' }, { replace: true })}>
        <TabsList variant="line" className="w-full justify-start overflow-x-auto">
          <TabsTrigger value="info" className="flex-none">
            Thông tin
          </TabsTrigger>
          <TabsTrigger value="history" className="flex-none">
            Lịch sử
          </TabsTrigger>
        </TabsList>
        <TabsContent value="info" className="pt-4">
          <ItemInfo ws={workspaceId} item={detail} />
        </TabsContent>
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
  const rows: [string, string | null][] = [
    ['Mã', item.code],
    ['Ngày đặt', item.placedAt ? formatDate(item.placedAt) : null],
    ['Ngày gỡ', item.removedAt ? formatDate(item.removedAt) : null],
  ]
  return (
    <div className="flex max-w-3xl flex-col gap-6">
      <dl className="grid gap-x-6 gap-y-2 sm:grid-cols-[minmax(8rem,max-content)_1fr]">
        {rows.map(([label, value]) => (
          <div key={label} className="contents">
            <dt className="text-muted-foreground">{label}</dt>
            <dd className="mb-2 sm:mb-0">{value ?? '—'}</dd>
          </div>
        ))}
      </dl>
      {item.description && (
        <section className="flex flex-col gap-2">
          <h2 className="text-sm font-medium">Mô tả</h2>
          <p className="whitespace-pre-line">{item.description}</p>
        </section>
      )}
      {defs.length > 0 && (
        <section className="flex flex-col gap-2">
          <h2 className="text-sm font-medium">Thông tin thêm</h2>
          <AttributeView defs={defs} values={item.attributes} />
        </section>
      )}
      <p className="text-xs text-muted-foreground">Sửa lần cuối {formatDateTime(item.updatedAt)}</p>
    </div>
  )
}
