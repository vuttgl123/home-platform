import '@testing-library/jest-dom/vitest'

import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'

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
