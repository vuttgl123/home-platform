import { ChevronRightIcon, UserPlusIcon } from 'lucide-react'
import { cn } from 'cn'
import { useState } from 'react'

import { Button } from '@/components/ui/button'
import { Switch } from '@/components/ui/switch'
import { useChildren, type PlaceDetail } from '@/features/places'

import { useOwnershipsAtPlace, useOwnershipsInSubtree } from '../queries'
import type { Ownership } from '../types'
import { OwnershipActions } from './OwnershipActions'
import { OwnershipFormDialog } from './OwnershipFormDialog'
import { OwnershipList } from './OwnershipList'

/**
 * Tab "Sở hữu" của một địa điểm: đang sở hữu (kèm tổng tỷ lệ), sắp tới, đã thôi (chỉ tải khi mở).
 * Địa điểm có chỗ con thì xem được ai đang sở hữu các chỗ bên trong (ví dụ các căn của một toà).
 */
export function PlaceOwnersTab({ ws, place }: { ws: string; place: PlaceDetail }) {
  const children = useChildren(ws, place.id)
  const hasInside = children.items.length > 0
  const [whole, setWhole] = useState(false)
  const [showPast, setShowPast] = useState(false)
  const [adding, setAdding] = useState(false)
  const current = useOwnershipsAtPlace(ws, place.id, 'CURRENT', { enabled: !whole })
  const upcoming = useOwnershipsAtPlace(ws, place.id, 'UPCOMING', { enabled: !whole })
  const past = useOwnershipsAtPlace(ws, place.id, 'PAST', { enabled: !whole && showPast })
  const inside = useOwnershipsInSubtree(ws, place.id, whole)
  const actions = (row: Ownership) => <OwnershipActions ws={ws} row={row} />

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
          Thêm chủ
        </Button>
      </div>
      {whole ? (
        <Group title="Đang sở hữu bên trong">
          <OwnershipList ws={ws} list={inside} show="person" showBoth empty="Không ai đang sở hữu bên trong." actions={actions} />
        </Group>
      ) : (
        <>
          <Group title="Đang sở hữu">
            <OwnershipList ws={ws} list={current} show="person" showTotal empty="Chưa có ai đang sở hữu." actions={actions} />
          </Group>
          <Group title="Sắp tới">
            <OwnershipList ws={ws} list={upcoming} show="person" empty="Không có ai sắp sở hữu." actions={actions} />
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
              Đã thôi
            </Button>
            {showPast && (
              <section aria-label="Đã thôi">
                <OwnershipList ws={ws} list={past} show="person" empty="Chưa ai thôi sở hữu." actions={actions} />
              </section>
            )}
          </div>
        </>
      )}
      <OwnershipFormDialog ws={ws} mode="create" place={{ id: place.id, name: place.name }} open={adding} onOpenChange={setAdding} />
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
