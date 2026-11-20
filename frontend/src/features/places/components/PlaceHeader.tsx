import { Fragment } from 'react'
import { Link } from 'react-router'

import { ToneBadge } from '@/components/common/ToneBadge'
import { Badge } from '@/components/ui/badge'
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from '@/components/ui/breadcrumb'
import { usePlaceTypes } from '@/features/catalog'

import { PLACE_STATUS_LABEL, PLACE_STATUS_TONE } from '../labels'
import { placeUrl } from '../routes'
import type { PlaceDetail } from '../types'

/**
 * Đầu trang của một địa điểm: chuỗi tổ tiên, tên, loại, trạng thái, mã, version.
 * @param actions các nút thao tác, đặt bên phải tên
 */
export function PlaceHeader({ ws, place, actions }: { ws: string; place: PlaceDetail; actions?: React.ReactNode }) {
  const typeName = usePlaceTypes(ws).data?.find((type) => type.id === place.placeTypeId)?.name

  return (
    <header className="flex flex-col gap-3">
      <Breadcrumb aria-label="Đường dẫn">
        <BreadcrumbList>
          {place.ancestors.map((ancestor) => (
            <Fragment key={ancestor.id}>
              <BreadcrumbItem>
                <BreadcrumbLink render={<Link to={placeUrl(ws, ancestor.id)} />}>{ancestor.name}</BreadcrumbLink>
              </BreadcrumbItem>
              <BreadcrumbSeparator />
            </Fragment>
          ))}
          <BreadcrumbItem>
            <BreadcrumbPage>{place.name}</BreadcrumbPage>
          </BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>
      <div className="flex flex-wrap items-start gap-x-4 gap-y-2">
        <h1 className="min-w-0 flex-1 text-2xl font-semibold tracking-tight break-words">{place.name}</h1>
        {actions && <div className="flex shrink-0 gap-2">{actions}</div>}
      </div>
      <dl className="flex flex-wrap items-center gap-2 text-sm">
        <dt className="sr-only">Loại</dt>
        <dd>
          <Badge variant="secondary">{typeName ?? '…'}</Badge>
        </dd>
        <dt className="sr-only">Trạng thái</dt>
        <dd>
          <ToneBadge tone={PLACE_STATUS_TONE[place.status]}>{PLACE_STATUS_LABEL[place.status]}</ToneBadge>
        </dd>
        {place.code && (
          <>
            <dt className="sr-only">Mã</dt>
            <dd className="text-muted-foreground">{place.code}</dd>
          </>
        )}
        <dt className="sr-only">Version</dt>
        <dd className="text-muted-foreground" title="Số lần đã sửa, tính cả lần tạo">
          v{place.version}
        </dd>
      </dl>
    </header>
  )
}
