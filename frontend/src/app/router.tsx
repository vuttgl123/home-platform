import type { RouteObject } from 'react-router'

import { PeoplePage, PersonPage } from '@/features/people'
import { PlacePage } from '@/features/places'

import { AppLayout } from './layout/AppLayout'
import { ComingSoonPage } from './layout/ComingSoonPage'
import { NotFoundPage } from './layout/NotFoundPage'
import { StartPage } from './layout/StartPage'
import { WorkspaceHomePage } from './layout/WorkspaceHomePage'
import { RequireUser } from './session/RequireUser'

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
          { path: 'places/:placeId', element: <PlacePage /> },
          { path: 'people', element: <PeoplePage /> },
          { path: 'people/:personId', element: <PersonPage /> },
          { path: 'catalog', element: <ComingSoonPage /> },
          { path: 'activity', element: <ComingSoonPage /> },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
]
