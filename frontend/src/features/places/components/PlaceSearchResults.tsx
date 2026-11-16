import { Link } from 'react-router'

import { LoadMoreButton } from '@/components/common/LoadMoreButton'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'

import { usePlaceSearch } from '../queries'
import { placeUrl } from '../routes'
import { AncestorTrail } from './AncestorTrail'

/** Kết quả tìm địa điểm theo tên bắt đầu bằng `term`, thay cho cây trong lúc đang tìm. */
export function PlaceSearchResults({ ws, term }: { ws: string; term: string }) {
  const results = usePlaceSearch(ws, term)

  if (results.isLoading) {
    return (
      <div className="flex flex-col gap-1.5 px-2 py-1.5" aria-label="Đang tìm">
        <Skeleton className="h-4 w-3/4" />
        <Skeleton className="h-4 w-1/2" />
      </div>
    )
  }
  if (results.error) {
    return (
      <div className="flex items-center justify-between gap-2 px-2 py-1 text-sm text-muted-foreground">
        Không tìm được
        <Button variant="outline" size="xs" onClick={results.refetch}>
          Thử lại
        </Button>
      </div>
    )
  }
  if (results.items.length === 0) {
    return (
      <p className="px-2 py-1.5 text-sm text-muted-foreground">
        Không có địa điểm nào có tên bắt đầu bằng “{term}”.
      </p>
    )
  }
  return (
    <ul className="flex flex-col">
      {results.items.map((place) => (
        <li key={place.id}>
          <Link
            to={placeUrl(ws, place.id)}
            className="flex items-start gap-2 rounded-md px-2 py-1 text-sm outline-none hover:bg-sidebar-accent hover:text-sidebar-accent-foreground focus-visible:ring-2 focus-visible:ring-sidebar-ring"
          >
            <span className="min-w-0 flex-1">
              <span className="block truncate">{place.name}</span>
              <AncestorTrail ancestors={place.ancestors} />
            </span>
            {place.code && <span className="shrink-0 text-xs text-muted-foreground">{place.code}</span>}
          </Link>
        </li>
      ))}
      {results.hasMore && (
        <li>
          <LoadMoreButton onClick={results.loadMore} isLoading={results.isLoadingMore} />
        </li>
      )}
    </ul>
  )
}
