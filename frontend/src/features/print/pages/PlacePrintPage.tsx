import { PrinterIcon } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { useParams, useSearchParams } from 'react-router'

import type { CursorList } from '@/api'
import { ErrorState } from '@/components/common/ErrorState'
import { NotFoundState } from '@/components/common/NotFoundState'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Switch } from '@/components/ui/switch'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { AttributeView, useFieldDefs, usePlaceTypes } from '@/features/catalog'
import { ITEM_STATUS_LABEL, useItemsAtPlace, useItemsInSubtree } from '@/features/items'
import { useOwnershipsAtPlace } from '@/features/ownership'
import { RELATION_LABEL, useOccupanciesAtPlace } from '@/features/people'
import { capacityText, PLACE_STATUS_LABEL, usePlace } from '@/features/places'
import { formatDate, formatDateTime, formatNumber } from '@/lib/format'
import { isId } from '@/lib/ids'

import { useAllPages } from '../useAllPages'

/**
 * Trang `/w/:workspaceId/places/:placeId/print`: hồ sơ một địa điểm để in (hay "Lưu thành PDF"),
 * không có thanh bên và cây. Mọi danh sách được tải hết trước khi cho in. `?subtree=1` lấy đồ đạc
 * của cả cây con và thêm cột "Ở đâu".
 */
export function PlacePrintPage() {
  const { workspaceId = '', placeId = '' } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const subtree = searchParams.get('subtree') === '1'
  // Giờ mở trang, không đổi khi bật tắt công tắc.
  const [openedAt] = useState(() => new Date())

  const place = usePlace(workspaceId, placeId)
  const typeName = usePlaceTypes(workspaceId).data?.find((type) => type.id === place.data?.placeTypeId)?.name
  const defs = useFieldDefs(workspaceId, 'PLACE', place.data?.placeTypeId).data ?? []
  // Các danh sách chỉ tải khi đã biết có địa điểm này.
  const found = place.data != null
  const occupantList = useOccupanciesAtPlace(workspaceId, placeId, 'CURRENT', { enabled: found })
  const ownerList = useOwnershipsAtPlace(workspaceId, placeId, 'CURRENT', { enabled: found })
  const itemsHere = useItemsAtPlace(workspaceId, placeId, false, { enabled: found && !subtree })
  const itemsBelow = useItemsInSubtree(workspaceId, placeId, found && subtree)
  const itemList = subtree ? itemsBelow : itemsHere
  const occupants = useAllPages(occupantList)
  const owners = useAllPages(ownerList)
  const items = useAllPages(itemList)

  if (!isId(placeId) || place.error?.isNotFound) {
    return (
      <main className="p-6">
        <NotFoundState description="Địa điểm này không có, hoặc đã bị xoá." homeHref={`/w/${workspaceId}`} />
      </main>
    )
  }
  if (place.error) {
    return (
      <main className="p-6">
        <ErrorState error={place.error} onRetry={() => void place.refetch()} />
      </main>
    )
  }
  if (!place.data) {
    return (
      <main className="mx-auto flex max-w-4xl flex-col gap-3 p-6" aria-label="Đang tải">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-4 w-96" />
      </main>
    )
  }

  const detail = place.data
  const ready = occupants.done && owners.done && items.done
  const path = [...detail.ancestors.map((ancestor) => ancestor.name), detail.name].join(' / ')

  return (
    <main className="mx-auto flex max-w-4xl flex-col gap-8 p-6 text-sm print:max-w-none print:p-0">
      <div className="flex flex-wrap items-center gap-4 print:hidden">
        <Button type="button" disabled={!ready} onClick={() => window.print()}>
          <PrinterIcon aria-hidden="true" />
          In
        </Button>
        <label className="flex items-center gap-2">
          <Switch
            checked={subtree}
            onCheckedChange={(on) => setSearchParams(on ? { subtree: '1' } : {}, { replace: true })}
            aria-label="Kể cả bên trong"
          />
          Kể cả bên trong
        </label>
        {!ready && <span className="text-muted-foreground">Đang tải đủ các danh sách…</span>}
      </div>

      <section className="flex flex-col gap-4">
        <h1 className="text-2xl font-semibold tracking-tight">{detail.name}</h1>
        <dl className="grid gap-x-6 gap-y-2 sm:grid-cols-[minmax(8rem,max-content)_1fr]">
          <Field label="Mã" value={detail.code} />
          <Field label="Loại" value={typeName} />
          <Field label="Đường dẫn" value={path} />
          <Field label="Trạng thái" value={PLACE_STATUS_LABEL[detail.status]} />
          <Field label="Sức chứa" value={capacityText(detail.capacity, detail.occupantCount)} />
          <Field label="Mô tả" value={detail.description} />
        </dl>
        <AttributeView defs={defs} values={detail.attributes} />
      </section>

      <PrintTable
        title="Người đang ở"
        list={occupantList}
        state={occupants}
        columns={['Tên', 'Quan hệ', 'Từ ngày']}
        row={(occupancy) => [occupancy.personFullName, RELATION_LABEL[occupancy.relation], formatDate(occupancy.startDate)]}
      />
      <PrintTable
        title="Chủ sở hữu"
        list={ownerList}
        state={owners}
        columns={['Tên', 'Tỷ lệ', 'Từ ngày', 'Số giấy tờ']}
        row={(ownership) => [
          ownership.personFullName,
          ownership.sharePercent != null ? `${formatNumber(ownership.sharePercent)}%` : null,
          formatDate(ownership.startDate),
          ownership.documentNo,
        ]}
      />
      <PrintTable
        title="Đồ đạc"
        list={itemList}
        state={items}
        columns={['Mã', 'Tên', 'Nhóm', 'Số lượng', 'Trạng thái', 'Chủ', ...(subtree ? ['Ở đâu'] : [])]}
        row={(item) => [
          item.code,
          item.name,
          item.categoryName,
          [item.quantity, item.unit].filter((part) => part != null).join(' '),
          ITEM_STATUS_LABEL[item.status],
          item.ownerName,
          ...(subtree ? [item.placeName] : []),
        ]}
      />

      <footer className="text-xs text-muted-foreground">In lúc {formatDateTime(openedAt.toISOString())}</footer>
    </main>
  )
}

function Field({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="contents">
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="mb-2 sm:mb-0">{value == null || value === '' ? '—' : value}</dd>
    </div>
  )
}

/** Một bảng của hồ sơ; trống thì ghi "Không có". Ô trống hiện "—". */
function PrintTable<T extends { id: string }>({
  title,
  list,
  state,
  columns,
  row,
}: {
  title: string
  list: CursorList<T>
  state: { items: T[]; done: boolean; error: unknown }
  columns: string[]
  row: (entry: T) => ReactNode[]
}) {
  return (
    <section className="flex flex-col gap-2">
      <h2 className="text-base font-semibold">{title}</h2>
      {list.error ? (
        <ErrorState error={list.error} onRetry={list.refetch} />
      ) : state.items.length === 0 ? (
        state.done ? (
          <p className="text-muted-foreground">Không có</p>
        ) : (
          <Skeleton className="h-10" />
        )
      ) : (
        <Table aria-label={title}>
          <TableHeader>
            <TableRow>
              {columns.map((column) => (
                <TableHead key={column}>{column}</TableHead>
              ))}
            </TableRow>
          </TableHeader>
          <TableBody>
            {state.items.map((entry) => (
              <TableRow key={entry.id} className="break-inside-avoid">
                {row(entry).map((cell, index) => (
                  <TableCell key={columns[index]} className="whitespace-normal">
                    {cell == null || cell === '' ? '—' : cell}
                  </TableCell>
                ))}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </section>
  )
}
