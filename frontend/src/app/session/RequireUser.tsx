import { useState } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router'

import { useSession } from './useSession'

/**
 * Chặn các trang cần người dùng: chưa chọn ai thì về trang đầu.
 *
 * Mở thẳng một đường dẫn khi chưa chọn người dùng thì đường dẫn đó được nhớ lại, để vào xong là
 * tới đúng chỗ. Đang dùng mà đổi người dùng thì không nhớ: trang đang xem thuộc về người cũ.
 */
export function RequireUser() {
  const { userId } = useSession()
  const location = useLocation()
  const [hadUserOnArrival] = useState(userId != null)
  if (userId == null) {
    const state = hadUserOnArrival ? null : { from: location.pathname + location.search }
    return <Navigate to="/" replace state={state} />
  }
  return <Outlet />
}
