import { useState } from 'react'
import { useParams, useSearchParams } from 'react-router'

import { OccupancyFormDialog } from '@/features/people'
import { isId } from '@/lib/ids'

import { VacancyFilters } from '../components/VacancyFilters'
import { VacancyList } from '../components/VacancyList'
import { useVacancies } from '../queries'
import type { Vacancy, VacancyFilter } from '../types'

/**
 * Trang `/w/:workspaceId/vacancies`: chỗ còn xếp thêm người được hôm nay. Bộ lọc nằm trên URL
 * (`?type=`, `?root=`); giá trị lạ bị bỏ qua.
 */
export function VacanciesPage() {
  const { workspaceId = '' } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const type = searchParams.get('type')
  const root = searchParams.get('root')
  const filter: VacancyFilter = { placeTypeId: isId(type) ? type : null, rootId: isId(root) ? root : null }
  const list = useVacancies(workspaceId, filter)
  const [seat, setSeat] = useState<Vacancy | null>(null)

  function update(next: VacancyFilter) {
    const params = new URLSearchParams()
    if (next.placeTypeId) params.set('type', next.placeTypeId)
    if (next.rootId) params.set('root', next.rootId)
    setSearchParams(params, { replace: true })
  }

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">Chỗ trống</h1>
        <p className="text-muted-foreground">Những chỗ còn xếp thêm người được hôm nay.</p>
      </header>
      <VacancyFilters ws={workspaceId} filter={filter} onChange={update} />
      <VacancyList ws={workspaceId} list={list} onSeat={setSeat} />
      {seat && (
        <OccupancyFormDialog
          ws={workspaceId}
          open
          onOpenChange={(open) => !open && setSeat(null)}
          mode="create"
          place={{ id: seat.id, name: seat.name }}
        />
      )}
    </div>
  )
}
