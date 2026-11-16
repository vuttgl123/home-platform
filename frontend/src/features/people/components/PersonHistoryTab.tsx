import { useQueryClient } from '@tanstack/react-query'

import type { RegistrySchemas } from '@/api'
import { AttributeView, fieldLabelsWith, useFieldDefs } from '@/features/catalog'
import { HistoryTab, SnapshotFields } from '@/features/history'
import { formatDate } from '@/lib/format'

import { GENDER_LABEL } from '../labels'
import { refreshAfterPersonWrite, useFetchFreshPerson } from '../queries'
import type { PersonDetail } from '../types'

type PersonSnapshot = RegistrySchemas['PersonSnapshot']

const FIELD_LABELS: Record<string, string> = {
  fullName: 'Họ tên',
  phone: 'Số điện thoại',
  email: 'Email',
  idType: 'Loại giấy tờ',
  idNumber: 'Số giấy tờ',
  dateOfBirth: 'Ngày sinh',
  gender: 'Giới tính',
  note: 'Ghi chú',
  attributes: 'Mục tuỳ biến',
  avatarFileId: 'Ảnh đại diện',
  deletedAt: 'Xoá',
}

/** Tab "Lịch sử" của một người. */
export function PersonHistoryTab({ ws, person }: { ws: string; person: PersonDetail }) {
  const queryClient = useQueryClient()
  const fetchFresh = useFetchFreshPerson(ws)
  const defs = useFieldDefs(ws, 'PERSON', '0').data ?? []
  return (
    <HistoryTab<PersonSnapshot>
      ws={ws}
      kind="people"
      id={person.id}
      currentVersion={person.version}
      fieldLabels={fieldLabelsWith(FIELD_LABELS, defs)}
      renderSnapshot={(snapshot) => <PersonSnapshotView ws={ws} snapshot={snapshot} />}
      onRestored={(restored) => refreshAfterPersonWrite(queryClient, ws, restored as PersonDetail)}
      onReload={() => fetchFresh(person.id)}
    />
  )
}

function PersonSnapshotView({ ws, snapshot }: { ws: string; snapshot: PersonSnapshot }) {
  const defs = useFieldDefs(ws, 'PERSON', '0').data ?? []
  return (
    <div className="flex flex-col gap-4">
      <SnapshotFields
        rows={[
          ['Họ tên', snapshot.fullName],
          ['Số điện thoại', snapshot.phone],
          ['Email', snapshot.email],
          ['Giấy tờ', [snapshot.idType, snapshot.idNumber].filter(Boolean).join(' ')],
          ['Ngày sinh', snapshot.dateOfBirth ? formatDate(snapshot.dateOfBirth) : null],
          ['Giới tính', snapshot.gender ? GENDER_LABEL[snapshot.gender] : null],
          ['Ghi chú', snapshot.note],
        ]}
      />
      {defs.length > 0 && <AttributeView defs={defs} values={(snapshot.attributes as Record<string, unknown>) ?? null} />}
    </div>
  )
}
