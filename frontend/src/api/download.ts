import { ApiError } from './errors'
import { workspaceHeaders } from './headers'

/**
 * Tải một file của registry-service rồi lưu về máy dưới tên `fileName`.
 *
 * Không dùng được thẻ `<a href>` thường: request phải mang header người dùng như mọi API khác. Vì vậy
 * file được tải bằng `fetch`, rồi lưu qua một liên kết tạm trỏ vào dữ liệu đã tải.
 * @param path đường dẫn trong registry, ví dụ `/exports/items.xlsx`
 * @param query tham số; khoá có giá trị `undefined` bị bỏ qua
 * @throws ApiError với response không phải 2xx, hoặc khi kết nối hỏng trước khi nhận đủ file
 */
export async function downloadFile(
  ws: string,
  path: string,
  query: Record<string, string | undefined>,
  fileName: string,
): Promise<void> {
  const url = new URL(`/api/registry${path}`, window.location.origin)
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined) {
      url.searchParams.set(key, value)
    }
  }
  const headers = workspaceHeaders(ws)
  let response: Response
  try {
    // Tìm `fetch` lúc gọi, như client của API: trong test MSW thay nó sau khi module này được nạp.
    response = await globalThis.fetch(url, { headers })
  } catch {
    throw ApiError.network()
  }
  if (!response.ok) {
    throw ApiError.from(response.status, await response.json().catch(() => null))
  }
  let blob: Blob
  try {
    blob = await response.blob()
  } catch {
    // Máy chủ ngắt giữa chừng: thà báo lỗi còn hơn lưu một file thiếu.
    throw ApiError.network()
  }
  save(blob, fileName)
}

function save(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.append(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
