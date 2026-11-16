import { ChevronRightIcon, UserPlusIcon } from 'lucide-react'
import { cn } from 'cn'
import { useState } from 'react'

import { Button } from '@/components/ui/button'
import { Switch } from '@/components/ui/switch'
import { useChildren, type PlaceDetail } from '@/features/places'

import { useOccupanciesAtPlace, useOccupanciesInSubtree } from '../queries'
import type { Occupancy } from '../types'
import { OccupancyActions } from './OccupancyActions'
import { OccupancyFormDialog } from './OccupancyFormDialog'
import { OccupancyList } from './OccupancyList'

/**
 * Tab "Người ở" của một địa điểm: đang ở, sắp đến, đã rời (chỉ tải khi mở). Địa điểm có chỗ con
 * thì xem được người đang ở trong cả bên trong (ví dụ cả toà nhà).
 */
export function PlaceOccupantsTab({ ws, place }: { ws: string; place: PlaceDetail }) {
  // Cùng cache với cây và tab Bên trong.
  const children = useChildren(ws, place.id)
  const hasInside = children.items.length > 0
  const [whole, setWhole] = useState(false)
  const [showPast, setShowPast] = useState(false)
  const [adding, setAdding] = useState(false)
  const current = useOccupanciesAtPlace(ws, place.id, 'CURRENT', { enabled: !whole })
  const upcoming = useOccupanciesAtPlace(ws, place.id, 'UPCOMING', { enabled: !whole })
  const past = useOccupanciesAtPlace(ws, place.id, 'PAST', { enabled: !whole && showPast })
  const inside = useOccupanciesInSubtree(ws, place.id, whole)
  const actions = (row: Occupancy) => <OccupancyActions ws={ws} row={row} />

  return (
    <div className="flex max-w-4xl flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        {hasInside ? (
          <label className="flex items-center gap-2 text-sm">
            <Switch checked={whole} onCheckedChange={setWhole} aria-label="Cả bên trong" />
            Cả bên trong
          </label>
        ) : (
          <span />
        )}
        <Button variant="outline" onClick={() => setAdding(true)}>
          <UserPlusIcon aria-hidden="true" />
          Thêm người ở
        </Button>
      </div>
      {whole ? (
        <Group title="Đang ở bên trong">
          <OccupancyList ws={ws} list={inside} show="person" showBoth empty="Không ai đang ở bên trong." actions={actions} />
        </Group>
      ) : (
        <>
          <Group title="Đang ở">
            <OccupancyList ws={ws} list={current} show="person" empty="Chưa có ai đang ở." actions={actions} />
          </Group>
          <Group title="Sắp đến">
            <OccupancyList ws={ws} list={upcoming} show="person" empty="Không có ai sắp đến." actions={actions} />
          </Group>
          <div className="flex flex-col gap-2">
            <Button
              variant="ghost"
              className="w-fit px-0 text-sm font-medium"
              aria-expanded={showPast}
              onClick={() => setShowPast((open) => !open)}
            >
              <ChevronRightIcon
                className={cn('size-4 transition-transform motion-reduce:transition-none', showPast && 'rotate-90')}
                aria-hidden="true"
              />
              Đã rời
            </Button>
            {showPast && (
              <section aria-label="Đã rời">
                <OccupancyList ws={ws} list={past} show="person" empty="Chưa ai rời đi." actions={actions} />
              </section>
            )}
          </div>
        </>
      )}
      <OccupancyFormDialog
        ws={ws}
        mode="create"
        place={{ id: place.id, name: place.name }}
        open={adding}
        onOpenChange={setAdding}
      />
    </div>
  )
}

function Group({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section aria-label={title} className="flex flex-col gap-2">
      <h2 className="text-sm font-medium">{title}</h2>
      {children}
    </section>
  )
}
