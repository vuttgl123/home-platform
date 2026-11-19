import type { RouteObject } from 'react-router'

import { ActivityPage, TrashPage } from '@/features/activity'
import { CatalogPage } from '@/features/catalog'
import { ItemPage, ItemsPage, PersonItemsTab, PlaceItemsTab } from '@/features/items'
import { PeoplePage, PersonPage, PlaceOccupantsTab, type PersonTabSlots } from '@/features/people'
import { PersonOwnershipsTab, PlaceOwnersTab } from '@/features/ownership'
import { PlacePage, type PlaceTabSlots } from '@/features/places'
import { PlacePrintPage } from '@/features/print'

import { AppLayout } from './layout/AppLayout'
import { NotFoundPage } from './layout/NotFoundPage'
import { StartPage } from './layout/StartPage'
import { WorkspaceHomePage } from './layout/WorkspaceHomePage'
import { RequireUser } from './session/RequireUser'

/** Các tab của trang địa điểm thuộc nghiệp vụ khác; trang địa điểm không nhập các feature đó. */
const placeTabs: PlaceTabSlots = {
  owners: (ws, place) => <PlaceOwnersTab ws={ws} place={place} />,
  people: (ws, place) => <PlaceOccupantsTab ws={ws} place={place} />,
  items: (ws, place) => <PlaceItemsTab ws={ws} place={place} />,
}

/** Các tab của trang một người thuộc nghiệp vụ khác; trang người không nhập các feature đó. */
const personTabs: PersonTabSlots = {
  ownerships: (ws, person) => <PersonOwnershipsTab ws={ws} person={person} />,
  items: (ws, person) => <PersonItemsTab ws={ws} person={person} />,
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
      // Trang in đứng riêng: không có thanh bên, cây địa điểm.
      { path: 'places/:placeId/print', element: <PlacePrintPage /> },
      {
        element: <AppLayout />,
        children: [
          { index: true, element: <WorkspaceHomePage /> },
          { path: 'places/:placeId', element: <PlacePage tabs={placeTabs} /> },
          { path: 'people', element: <PeoplePage /> },
          { path: 'people/:personId', element: <PersonPage tabs={personTabs} /> },
          { path: 'items', element: <ItemsPage /> },
          { path: 'items/:itemId', element: <ItemPage /> },
          { path: 'catalog', element: <CatalogPage /> },
          { path: 'activity', element: <ActivityPage /> },
          { path: 'trash', element: <TrashPage /> },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
]
