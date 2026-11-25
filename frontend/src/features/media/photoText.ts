import type { Photo } from './types'

/** Chữ thay ảnh: chú thích, hay "Ảnh N" (đếm từ 1) khi không có. */
export function photoAlt(photo: Photo, index: number): string {
  return photo.caption ?? `Ảnh ${index + 1}`
}
