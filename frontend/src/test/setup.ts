import '@testing-library/jest-dom/vitest'

import { cleanup } from '@testing-library/react'
import { createElement, type ReactNode } from 'react'
import { afterAll, afterEach, beforeAll, vi } from 'vitest'

import { sessionStore } from '@/app/session/sessionStore'

import { server } from './server'

// Request không có handler là lỗi của test, không được lặng lẽ đi ra mạng.
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))

afterEach(() => {
  cleanup()
  server.resetHandlers()
  server.events.removeAllListeners()
  sessionStore.signOut()
  localStorage.clear()
})

afterAll(() => server.close())

// jsdom thiếu vài API của trình duyệt mà component shadcn (base-ui) cần.
if (!window.matchMedia) {
  window.matchMedia = (query: string) =>
    ({
      matches: false,
      media: query,
      onchange: null,
      addEventListener: () => {},
      removeEventListener: () => {},
      addListener: () => {},
      removeListener: () => {},
      dispatchEvent: () => false,
    }) as MediaQueryList
}

if (!globalThis.ResizeObserver) {
  globalThis.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  }
}

if (!Element.prototype.scrollIntoView) {
  Element.prototype.scrollIntoView = () => {}
}

// Vùng cuộn (scroll-area) hỏi các hiệu ứng đang chạy của phần tử; jsdom không có hiệu ứng nào.
if (!Element.prototype.getAnimations) {
  Element.prototype.getAnimations = () => []
}

// jsdom không đo được kích thước, nên biểu đồ (recharts) đo ra 0 và ghi cảnh báo ở mỗi lần vẽ. Cho
// vùng chứa một kích thước cố định: biểu đồ vẽ thật (có SVG để test nhìn vào) và không còn cảnh báo.
vi.mock('recharts', async (importOriginal) => {
  const actual = await importOriginal<typeof import('recharts')>()
  return {
    ...actual,
    ResponsiveContainer: ({ children }: { children: ReactNode }) =>
      createElement(actual.ResponsiveContainer, { width: 600, height: 300 }, children),
  }
})

