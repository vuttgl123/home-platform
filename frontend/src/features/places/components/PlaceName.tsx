import { usePlace } from '../queries'

/** Tên hiện tại của một địa điểm theo id; không mở được (đã xoá, không có quyền) thì hiện `#id`. */
export function PlaceName({ ws, id }: { ws: string; id: string }) {
  const place = usePlace(ws, id)
  return <>{place.data?.name ?? (place.error ? `#${id}` : '…')}</>
}
