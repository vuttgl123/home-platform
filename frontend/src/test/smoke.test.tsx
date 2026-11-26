import { screen } from '@testing-library/react'
import { expect, test, vi } from 'vitest'

import { renderWithProviders } from './render'

test('rendersWithProviders', () => {
  renderWithProviders(<p>xin chào</p>)

  expect(screen.getByText('xin chào')).toBeInTheDocument()
})

test('unhandledRequestFails', async () => {
  // Không handler nào nhận request này: MSW phải từ chối thay vì để nó ra mạng.
  const logged = vi.spyOn(console, 'error').mockImplementation(() => {})

  await expect(fetch('/api/registry/x')).rejects.toThrow()

  expect(logged.mock.calls.join()).toContain('without a matching request handler')
  logged.mockRestore()
})

test('testsRunInVietnamTime', () => {
  // Bộ lọc ngày, nhãn hạn tính theo ngày ở Việt Nam; test không được đổi kết quả theo máy chạy.
  // ICU có thể gọi vùng này bằng tên cũ "Asia/Saigon".
  expect(['Asia/Ho_Chi_Minh', 'Asia/Saigon']).toContain(Intl.DateTimeFormat().resolvedOptions().timeZone)
})
