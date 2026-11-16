import { useState } from 'react'

import { useFetchFreshItem } from '../queries'
import type { ItemDetail } from '../types'

/**
 * "Tải bản mới" sau một lần 409 "đã có người sửa": lấy bản mới nhất vào cache (trang nhận nó, nên
 * `item` của hộp thoại đổi theo), rồi xoá lỗi của lệnh ghi.
 */
export function useItemReload(ws: string, item: ItemDetail, resetError: () => void) {
  const fetchFresh = useFetchFreshItem(ws)
  const [reloading, setReloading] = useState(false)
  async function reload() {
    setReloading(true)
    try {
      await fetchFresh(item.id)
      resetError()
    } finally {
      setReloading(false)
    }
  }
  return { reload: () => void reload(), reloading }
}
