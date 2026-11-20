import { Link } from 'react-router'

import { InfoCard, InfoList } from '@/components/common/InfoCard'
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
    <div className="grid max-w-5xl gap-4 lg:grid-cols-2">
      <InfoCard title="Thông tin">
        <InfoList
          rows={[
            ...rows.map(([label, value]) => ({ label, value })),
            {
              label: 'Đang ở',
              value:
                person.currentOccupancies.length === 0
                ? null
                : person.currentOccupancies.map((stay, index) => (
                    <span key={stay.id}>
                      {index > 0 && ', '}
                      <Link to={`/w/${ws}/places/${stay.placeId}`} className="font-medium hover:underline">
                        {stay.placeName}
                      </Link>{' '}
                      <span className="text-muted-foreground">({RELATION_LABEL[stay.relation]})</span>
                    </span>
                  )),
            },
          ]}
        />
      </InfoCard>
      {person.note && (
        <InfoCard title="Ghi chú">
          <p className="text-sm whitespace-pre-line">{person.note}</p>
        </InfoCard>
      )}
      {defs.length > 0 && (
        <InfoCard title="Mục tuỳ biến" className="lg:col-span-2">
          <AttributeView defs={defs} values={person.attributes} />
        </InfoCard>
      )}
      <p className="text-xs text-muted-foreground lg:col-span-2">Sửa lần cuối {formatDateTime(person.updatedAt)}</p>
    </div>
  )
}
