import { Link } from 'react-router'

import { AttributeView, useFieldDefs } from '@/features/catalog'
import { formatDateTime } from '@/lib/format'

import { RELATION_LABEL } from '../labels'
import type { PersonDetail } from '../types'

/** Tab "Thông tin" của một người: liên lạc, giấy tờ, nơi đang ở, ghi chú, mục tuỳ biến. */
export function PersonInfoTab({ ws, person }: { ws: string; person: PersonDetail }) {
  const defs = useFieldDefs(ws, 'PERSON', '0').data ?? []
  const document = [person.idType, person.idNumber].filter(Boolean).join(' ')
  const rows: [string, React.ReactNode][] = [
    ['Số điện thoại', person.phone],
    ['Email', person.email],
    ['Giấy tờ', document || null],
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
        <div className="contents">
          <dt className="text-muted-foreground">Đang ở</dt>
          <dd className="mb-2 sm:mb-0">
            {person.currentOccupancies.length === 0
              ? '—'
              : person.currentOccupancies.map((stay, index) => (
                  <span key={stay.id}>
                    {index > 0 && ', '}
                    <Link to={`/w/${ws}/places/${stay.placeId}`} className="font-medium hover:underline">
                      {stay.placeName}
                    </Link>{' '}
                    <span className="text-muted-foreground">({RELATION_LABEL[stay.relation]})</span>
                  </span>
                ))}
          </dd>
        </div>
      </dl>
      {person.note && (
        <section className="flex flex-col gap-2">
          <h2 className="text-sm font-medium">Ghi chú</h2>
          <p className="whitespace-pre-line">{person.note}</p>
        </section>
      )}
      {defs.length > 0 && (
        <section className="flex flex-col gap-2">
          <h2 className="text-sm font-medium">Thông tin thêm</h2>
          <AttributeView defs={defs} values={person.attributes} />
        </section>
      )}
      <p className="text-xs text-muted-foreground">Sửa lần cuối {formatDateTime(person.updatedAt)}</p>
    </div>
  )
}
