import createClient from 'openapi-fetch'

import { ApiError } from './errors'
import type { components as IdentityComponents, paths as IdentityPaths } from './schema/identity'
import type { components as RegistryComponents, paths as RegistryPaths } from './schema/registry'

export type IdentitySchemas = IdentityComponents['schemas']
export type RegistrySchemas = RegistryComponents['schemas']

// URL tuyệt đối: openapi-fetch tự tạo Request, và Request của Node (dùng khi chạy test) không
// nhận URL tương đối. Trên trình duyệt kết quả vẫn là cùng origin với trang.
const origin = window.location.origin

// Tìm `fetch` lúc gọi chứ không lúc tạo client: openapi-fetch mặc định giữ lại `fetch` đang có
// khi module này được nạp, tức là trước khi MSW kịp thay nó trong test.
const lateBoundFetch = (request: Request) => globalThis.fetch(request)

/** Client có kiểu của identity-service; đường dẫn, tham số và body được kiểm tra lúc biên dịch. */
export const identity = createClient<IdentityPaths>({ baseUrl: `${origin}/api/identity`, fetch: lateBoundFetch })

/** Client có kiểu của registry-service. */
export const registry = createClient<RegistryPaths>({ baseUrl: `${origin}/api/registry`, fetch: lateBoundFetch })

/**
 * Đổi kết quả của một lời gọi openapi-fetch thành dữ liệu, hoặc ném {@link ApiError}.
 * @returns phần thân của response 2xx; `undefined` với response không có thân (204)
 * @throws ApiError với response không phải 2xx, hoặc khi request không tới được máy chủ
 */
export async function unwrap<T>(call: Promise<{ data?: T; error?: unknown; response: Response }>): Promise<T> {
  let result: Awaited<typeof call>
  try {
    result = await call
  } catch {
    throw ApiError.network()
  }
  if (!result.response.ok) {
    throw ApiError.from(result.response.status, result.error)
  }
  return result.data as T
}
