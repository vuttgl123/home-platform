import { ChevronRightIcon, PlusIcon } from 'lucide-react'
import { cn } from 'cn'
import { useState } from 'react'

import { Button } from '@/components/ui/button'
import { Switch } from '@/components/ui/switch'
import { useChildren, type PlaceDetail } from '@/features/places'

import { useItemsAtPlace, useItemsInSubtree } from '../queries'
import { ItemFormDialog } from './ItemFormDialog'
import { ItemList } from './ItemList'

/**
 * Tab "Đồ đạc" của một địa điểm: đang có (theo tên) và đã gỡ (mới gỡ trước, chỉ tải khi mở).
 * Địa điểm có chỗ con thì xem được đồ trong cả bên trong (ví dụ cả toà nhà).
 */
export function PlaceItemsTab({ ws, place }: { ws: string; place: PlaceDetail }) {
  // Cùng cache với cây và tab Bên trong.
  const children = useChildren(ws, place.id)
  const hasInside = children.items.length > 0
  const [whole, setWhole] = useState(false)
  const [showRemoved, setShowRemoved] = useState(false)
  const [adding, setAdding] = useState(false)
  const current = useItemsAtPlace(ws, place.id, false, { enabled: !whole })
  const removed = useItemsAtPlace(ws, place.id, true, { enabled: !whole && showRemoved })
  const inside = useItemsInSubtree(ws, place.id, whole)

  return (
    <div className="flex flex-col gap-6">
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
          <PlusIcon aria-hidden="true" />
          Thêm đồ
        </Button>
      </div>
      {whole ? (
        <section aria-label="Đang có bên trong" className="flex flex-col gap-2">
          <h2 className="text-sm font-medium">Đang có bên trong</h2>
          <ItemList ws={ws} list={inside} showPlace empty="Không có đồ nào bên trong." />
        </section>
      ) : (
        <>
          <section aria-label="Đang có" className="flex flex-col gap-2">
            <h2 className="text-sm font-medium">Đang có</h2>
            <ItemList ws={ws} list={current} empty="Chưa có đồ nào ở đây." />
          </section>
          <div className="flex flex-col gap-2">
            <Button
              variant="ghost"
              className="w-fit px-0 text-sm font-medium"
              aria-expanded={showRemoved}
              onClick={() => setShowRemoved((open) => !open)}
            >
              <ChevronRightIcon
                className={cn('size-4 transition-transform motion-reduce:transition-none', showRemoved && 'rotate-90')}
                aria-hidden="true"
              />
              Đã gỡ
            </Button>
            {showRemoved && (
              <section aria-label="Đã gỡ">
                <ItemList ws={ws} list={removed} removed empty="Chưa gỡ món nào." />
              </section>
            )}
          </div>
        </>
      )}
      <ItemFormDialog ws={ws} mode="create" place={{ id: place.id, name: place.name }} open={adding} onOpenChange={setAdding} />
    </div>
  )
}
