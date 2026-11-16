import { Link } from 'react-router'

import { AttributeView, useFieldDefs } from '@/features/catalog'
import { formatDateTime } from '@/lib/format'

import { placeUrl } from '../routes'
import type { PlaceDetail } from '../types'

/** Tab "Tổng quan": địa chỉ, mô tả, mục tuỳ biến theo loại. */
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

  return (
    <div className="flex max-w-3xl flex-col gap-6">
      <Section title="Địa chỉ" actions={addressActions}>
        <AddressView ws={ws} place={place} />
      </Section>
      {place.description && (
        <Section title="Mô tả">
          <p className="whitespace-pre-line">{place.description}</p>
        </Section>
      )}
      {defs.length > 0 && (
        <Section title="Thông tin thêm">
          <AttributeView defs={defs} values={place.attributes} />
        </Section>
      )}
      <p className="text-xs text-muted-foreground">Sửa lần cuối {formatDateTime(place.updatedAt)}</p>
    </div>
  )
}

function Section({ title, actions, children }: { title: string; actions?: React.ReactNode; children: React.ReactNode }) {
  return (
    <section className="flex flex-col gap-2">
      <div className="flex items-center justify-between gap-2">
        <h2 className="text-sm font-medium">{title}</h2>
        {actions}
      </div>
      {children}
    </section>
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
