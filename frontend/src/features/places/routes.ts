/** Đường dẫn tới trang của một địa điểm. */
export function placeUrl(ws: string, placeId: string): string {
  return `/w/${ws}/places/${placeId}`
}

/** Mẫu route của trang địa điểm, để một component ngoài trang đó (cây) biết địa điểm đang mở. */
export const PLACE_ROUTE = '/w/:workspaceId/places/:placeId'
