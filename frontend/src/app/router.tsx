import type { RouteObject } from 'react-router'

import { CatalogPage } from '@/features/catalog'
import { ItemPage, PlaceItemsTab } from '@/features/items'
import { PeoplePage, PersonPage, PlaceOccupantsTab } from '@/features/people'
import { PlacePage, type PlaceTabSlots } from '@/features/places'

import { AppLayout } from './layout/AppLayout'
import { ComingSoonPage } from './layout/ComingSoonPage'
import { NotFoundPage } from './layout/NotFoundPage'
import { StartPage } from './layout/StartPage'
import { WorkspaceHomePage } from './layout/WorkspaceHomePage'
import { RequireUser } from './session/RequireUser'

/** Các tab của trang địa điểm thuộc nghiệp vụ khác; trang địa điểm không nhập các feature đó. */
const placeTabs: PlaceTabSlots = {
  people: (ws, place) => <PlaceOccupantsTab ws={ws} place={place} />,
  items: (ws, place) => <PlaceItemsTab ws={ws} place={place} />,
}

/**
 * Cây route của ứng dụng (spec mục 6). Không gian đang chọn nằm trên URL, nên chép đường dẫn
 * gửi người khác vẫn mở đúng chỗ.
 */
export const routes: RouteObject[] = [
  { path: '/', element: <StartPage /> },
  {
    path: '/w/:workspaceId',
    element: <RequireUser />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { index: true, element: <WorkspaceHomePage /> },
          { path: 'places/:placeId', element: <PlacePage tabs={placeTabs} /> },
          { path: 'people', element: <PeoplePage /> },
          { path: 'people/:personId', element: <PersonPage /> },
          { path: 'items/:itemId', element: <ItemPage /> },
          { path: 'catalog', element: <CatalogPage /> },
          { path: 'activity', element: <ComingSoonPage /> },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
]
