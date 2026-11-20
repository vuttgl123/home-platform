import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import { RouterProvider, type createBrowserRouter } from 'react-router'

import { Toaster } from '@/components/ui/toast'
import { TooltipProvider } from '@/components/ui/tooltip'

/** Gốc của ứng dụng. Router và cache được truyền vào để test dựng được bản riêng của mình. */
export function App({
  router,
  queryClient,
}: {
  router: ReturnType<typeof createBrowserRouter>
  queryClient: QueryClient
}) {
  return (
    <QueryClientProvider client={queryClient}>
      {/* Một provider cho mọi chú thích: rê từ nút này sang nút kia thì chú thích hiện ngay. */}
      <TooltipProvider delay={400}>
        <RouterProvider router={router} />
      </TooltipProvider>
      <Toaster />
    </QueryClientProvider>
  )
}
