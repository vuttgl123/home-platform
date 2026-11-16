import type { PersonDetail } from '@/features/people'

import { useItemsOwnedBy } from '../queries'
import { ItemList } from './ItemList'

/** Tab "Đồ đạc" của một người: món chưa gỡ là của riêng họ, kèm chỗ đặt. */
export function PersonItemsTab({ ws, person }: { ws: string; person: PersonDetail }) {
  const items = useItemsOwnedBy(ws, person.id)
  return (
    <div className="flex max-w-4xl flex-col gap-3">
      <ItemList ws={ws} list={items} showPlace empty="Không có món đồ nào là của riêng người này." />
    </div>
  )
}
