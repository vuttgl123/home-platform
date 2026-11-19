import { useQueryClient } from '@tanstack/react-query'

import type { RegistrySchemas } from '@/api'
import { attributeRows, defsOfType, fieldLabelsWith, useAllFieldDefs, useFieldDefs } from '@/features/catalog'
import { HistoryTab, type SnapshotRow } from '@/features/history'
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
  const snapshotRows = usePersonSnapshotRows(ws)
  return (
    <HistoryTab<PersonSnapshot>
      ws={ws}
      kind="people"
      id={person.id}
      currentVersion={person.version}
      fieldLabels={fieldLabelsWith(FIELD_LABELS, defs)}
      snapshotRows={snapshotRows}
      onRestored={(restored) => refreshAfterPersonWrite(queryClient, ws, restored as PersonDetail)}
      onReload={() => fetchFresh(person.id)}
    />
  )
}

/** Các trường của một bản hồ sơ người, rồi mục tuỳ biến; `null` khi mục tuỳ biến còn đang tải. */
function usePersonSnapshotRows(ws: string): ((snapshot: PersonSnapshot) => SnapshotRow[]) | null {
  const defs = useAllFieldDefs(ws, 'PERSON')
  if (defs.isPending) {
    return null
  }
  return (snapshot) => [
    { key: 'fullName', label: 'Họ tên', value: snapshot.fullName, raw: snapshot.fullName },
    { key: 'phone', label: 'Số điện thoại', value: snapshot.phone, raw: snapshot.phone },
    { key: 'email', label: 'Email', value: snapshot.email, raw: snapshot.email },
    {
      key: 'idDocument',
      label: 'Giấy tờ',
      value: [snapshot.idType, snapshot.idNumber].filter(Boolean).join(' '),
      raw: [snapshot.idType ?? null, snapshot.idNumber ?? null],
    },
    {
      key: 'dateOfBirth',
      label: 'Ngày sinh',
      value: snapshot.dateOfBirth ? formatDate(snapshot.dateOfBirth) : null,
      raw: snapshot.dateOfBirth,
    },
    { key: 'gender', label: 'Giới tính', value: snapshot.gender ? GENDER_LABEL[snapshot.gender] : null, raw: snapshot.gender },
    { key: 'note', label: 'Ghi chú', value: snapshot.note, raw: snapshot.note },
    ...attributeRows(defsOfType(defs.data ?? [], '0'), (snapshot.attributes as Record<string, unknown>) ?? null).map((row) => ({
      ...row,
      value: row.display,
      muted: row.archived,
    })),
  ]
}
