/**
 * Kiểu của một response, dựng từ kiểu sinh tự động `T`.
 *
 * OpenAPI của backend chưa ghi trường nào là bắt buộc, nên trong `T` mọi trường đều tuỳ chọn.
 * Thực tế backend luôn gửi đủ khoá, và trường không có giá trị mang `null`. `Present` nói lại
 * điều đó: các trường `K` luôn có giá trị, các trường còn lại có thể là `null`.
 *
 * ```ts
 * type PlaceRef = Present<RegistrySchemas['PlaceRef'], 'id' | 'name'>
 * ```
 */
export type Present<T, K extends keyof T> = { [P in K]-?: NonNullable<T[P]> } & {
  [P in Exclude<keyof T, K>]-?: NonNullable<T[P]> | null
}

/** Một trang của danh sách phân trang theo vị trí; `nextCursor` là `null` ở trang cuối. */
export type CursorPage<T> = { items: T[]; nextCursor: string | null }
