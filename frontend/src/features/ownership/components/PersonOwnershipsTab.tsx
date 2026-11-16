import { MapPinPlusIcon } from 'lucide-react'
import { useState } from 'react'

import { Button } from '@/components/ui/button'
import type { PersonDetail } from '@/features/people'

import { useOwnershipsOfPerson } from '../queries'
import type { Ownership } from '../types'
import { OwnershipActions } from './OwnershipActions'
import { OwnershipFormDialog } from './OwnershipFormDialog'
import { OwnershipList } from './OwnershipList'

/** Tab "Sở hữu" của một người: những chỗ họ đang và đã sở hữu, mới nhất trước. */
export function PersonOwnershipsTab({ ws, person }: { ws: string; person: PersonDetail }) {
  const owned = useOwnershipsOfPerson(ws, person.id)
  const [adding, setAdding] = useState(false)
  return (
    <div className="flex max-w-4xl flex-col gap-3">
      <div className="flex justify-end">
        <Button variant="outline" onClick={() => setAdding(true)}>
          <MapPinPlusIcon aria-hidden="true" />
          Thêm chỗ sở hữu
        </Button>
      </div>
      <OwnershipList
        ws={ws}
        list={owned}
        show="place"
        showStatus
        empty="Chưa sở hữu chỗ nào."
        actions={(row: Ownership) => <OwnershipActions ws={ws} row={row} />}
      />
      <OwnershipFormDialog
        ws={ws}
        mode="create"
        person={{ id: person.id, fullName: person.fullName }}
        open={adding}
        onOpenChange={setAdding}
      />
    </div>
  )
}
