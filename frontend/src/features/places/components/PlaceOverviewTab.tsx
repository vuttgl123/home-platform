import { Link } from 'react-router'

import { InfoCard, InfoList } from '@/components/common/InfoCard'
import { AttributeView, useFieldDefs } from '@/features/catalog'
import { formatDateTime } from '@/lib/format'

import { placeUrl } from '../routes'
import type { PlaceDetail } from '../types'
import { CapacityLine } from './CapacityLine'

/** Tab "Tổng quan": nằm trong đâu, mô tả, địa chỉ, mục tuỳ biến theo loại. */
export function PlaceOverviewTab({
  ws,
  place,
  addressActions,
}: {
  ws: string
  place: PlaceDetail
  addressActions?: React.ReactNode
}) {
  const defs = useFieldDefs(ws, 'PLACE', place.placeTypeId).data ?? []
  const parent = place.ancestors.at(-1)

  return (
    <div className="grid gap-4 lg:grid-cols-2">
      {/* Loại, mã, trạng thái đã ở đầu trang; khối này nói thêm nằm ở đâu và mô tả. */}
      <InfoCard title="Thông tin">
        <InfoList
          rows={[
            {
              label: 'Nằm trong',
              value: parent ? (
                <Link to={placeUrl(ws, parent.id)} className="font-medium hover:underline">
                  {parent.name}
                </Link>
              ) : (
                'Cấp cao nhất'
              ),
            },
            { label: 'Sức chứa', value: <CapacityLine capacity={place.capacity} occupants={place.occupantCount} /> },
            { label: 'Mô tả', value: place.description ? <span className="whitespace-pre-line">{place.description}</span> : null },
          ]}
        />
      </InfoCard>
      <InfoCard title="Địa chỉ" actions={addressActions}>
        <AddressView ws={ws} place={place} />
      </InfoCard>
      {defs.length > 0 && (
        <InfoCard title="Mục tuỳ biến" className="lg:col-span-2">
          <AttributeView defs={defs} values={place.attributes} />
        </InfoCard>
      )}
      <p className="text-xs text-muted-foreground lg:col-span-2">Sửa lần cuối {formatDateTime(place.updatedAt)}</p>
    </div>
  )
}

/** Địa chỉ của chính nó; không có thì địa chỉ của tổ tiên gần nhất, kèm tên tổ tiên đó. */
function AddressView({ ws, place }: { ws: string; place: PlaceDetail }) {
  const effective = place.effectiveAddress
  if (!effective) {
    return <p className="text-muted-foreground">Chưa có địa chỉ</p>
  }
  const inherited = effective.fromPlaceId !== place.id
  const source = place.ancestors.find((ancestor) => ancestor.id === effective.fromPlaceId)
  return (
    <div className="flex flex-col gap-0.5">
      <p>{effective.address.fullAddress}</p>
      {inherited && source && (
        <p className="text-sm text-muted-foreground">
          lấy từ{' '}
          <Link to={placeUrl(ws, source.id)} className="underline underline-offset-4 hover:text-foreground">
            {source.name}
          </Link>
        </p>
      )}
    </div>
  )
}
