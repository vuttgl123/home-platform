/**
 * Một lỗi của API. Backend trả mọi lỗi ở dạng Problem Details (RFC 9457); lỗi kiểm tra dữ liệu
 * có thêm `errors: [{ field, message }]`, lỗi 500 có thêm `requestId` để lần về log.
 */
/** Chữ của 409 `BUSY`: lệnh khác (ví dụ một lần nhập Excel) đang giữ dữ liệu; thử lại sau là được. */
export const BUSY_MESSAGE = 'Đang có thao tác khác trên dữ liệu này (ví dụ đang nhập Excel); thử lại sau ít phút'

export class ApiError extends Error {
  /** Mã HTTP; `0` khi request không tới được máy chủ. */
  readonly status: number
  /** Thông báo của backend, bằng tiếng Anh; riêng lỗi `BUSY` đã là chữ tiếng Việt để hiện thẳng. */
  readonly detail: string
  /** Lỗi theo từng trường (`name`, `attributes.area_m2`); nhiều lỗi của một trường nối bằng `; `. */
  readonly fieldErrors: Record<string, string>
  /** Lỗi kiểm tra dữ liệu không gắn với trường nào. */
  readonly formErrors: string[]
  readonly requestId: string | null
  /** Mã máy đọc của lỗi, nếu backend gửi, ví dụ `VERSION_CONFLICT`. */
  readonly code: string | null

  constructor(
    status: number,
    detail: string,
    extra: {
      fieldErrors?: Record<string, string>
      formErrors?: string[]
      requestId?: string | null
      code?: string | null
    } = {},
  ) {
    super(detail)
    this.name = 'ApiError'
    this.status = status
    this.detail = detail
    this.fieldErrors = extra.fieldErrors ?? {}
    this.formErrors = extra.formErrors ?? []
    this.requestId = extra.requestId ?? null
    this.code = extra.code ?? null
  }

  /**
   * Bản ghi đã được người khác sửa sau lần đọc: phải đọc lại rồi mới ghi được. Nhận ra bằng mã
   * `VERSION_CONFLICT`; response không có mã (backend cũ) thì bằng đuôi của `detail`.
   */
  get isVersionConflict(): boolean {
    if (this.code != null) {
      return this.code === 'VERSION_CONFLICT'
    }
    return this.status === 409 && this.detail.endsWith('was changed by someone else')
  }

  /** Không lấy được khoá dòng vì lệnh khác đang giữ: không phải xung đột version, thử lại sau. */
  get isBusy(): boolean {
    return this.code === 'BUSY'
  }

  get isNotFound(): boolean {
    return this.status === 404
  }

  /** @param body phần thân đã đọc của response lỗi; có thể không phải Problem Details (proxy, HTML) */
  static from(status: number, body: unknown): ApiError {
    if (!isRecord(body) || typeof body.detail !== 'string') {
      return new ApiError(status, 'Unexpected error')
    }
    const fieldErrors: Record<string, string> = {}
    const formErrors: string[] = []
    for (const entry of Array.isArray(body.errors) ? body.errors : []) {
      if (!isRecord(entry) || typeof entry.message !== 'string') {
        continue
      }
      if (typeof entry.field === 'string') {
        const earlier = fieldErrors[entry.field]
        fieldErrors[entry.field] = earlier ? `${earlier}; ${entry.message}` : entry.message
      } else {
        formErrors.push(entry.message)
      }
    }
    const requestId = typeof body.requestId === 'string' ? body.requestId : null
    const code = typeof body.code === 'string' ? body.code : null
    const detail = code === 'BUSY' ? BUSY_MESSAGE : body.detail
    return new ApiError(status, detail, { fieldErrors, formErrors, requestId, code })
  }

  /** Request không tới được máy chủ: mất mạng, hoặc backend đang tắt. */
  static network(): ApiError {
    return new ApiError(0, 'Không kết nối được máy chủ')
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}
