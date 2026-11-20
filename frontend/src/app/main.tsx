import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createBrowserRouter } from 'react-router'

import { createQueryClient } from '@/api'

import '../index.css'
import { App } from './App'
import { routes } from './router'
import { themeStore } from './theme'

// Áp chế độ sáng tối trước lần vẽ đầu, để trang không chớp trắng khi đang ở chế độ tối.
themeStore.apply()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App router={createBrowserRouter(routes)} queryClient={createQueryClient()} />
  </StrictMode>,
)
