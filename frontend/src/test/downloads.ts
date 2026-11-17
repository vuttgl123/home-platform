import { onTestFinished, vi } from 'vitest'

/** Một file mà code đã "lưu về máy" trong test. */
export type SavedFile = { name: string; blob: Blob }

/**
 * Thay phần trình duyệt của việc lưu file, vì jsdom không có `URL.createObjectURL` và không tải
 * được liên kết. Trả về danh sách file đã lưu, theo thứ tự; mọi thứ được trả lại như cũ khi test xong.
 */
export function mockDownloads(): SavedFile[] {
  const saved: SavedFile[] = []
  const blobs = new Map<string, Blob>()
  const original = { create: URL.createObjectURL, revoke: URL.revokeObjectURL }
  URL.createObjectURL = vi.fn((blob: Blob) => {
    const url = `blob:test/${blobs.size}`
    blobs.set(url, blob)
    return url
  })
  URL.revokeObjectURL = vi.fn()
  const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
    const blob = blobs.get(this.getAttribute('href') ?? '')
    if (blob) {
      saved.push({ name: this.download, blob })
    }
  })
  onTestFinished(() => {
    click.mockRestore()
    URL.createObjectURL = original.create
    URL.revokeObjectURL = original.revoke
  })
  return saved
}
