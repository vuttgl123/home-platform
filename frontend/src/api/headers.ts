// Backend nhận người gọi qua hai header (chưa có đăng nhập). Mỗi hàm gọi API tự truyền chúng,
// vì OpenAPI khai báo chúng là tham số bắt buộc của từng API.

let currentUser: string | null = null

/**
 * Đặt người dùng của mọi lời gọi sau đó. Phiên (`app/session`) gọi hàm này một cách đồng bộ,
 * trước khi component nào kịp gửi request.
 */
export function setCurrentUser(userId: string | null): void {
  currentUser = userId
}

/**
 * Header của một API không gắn không gian (identity).
 * @throws Error nếu chưa có người dùng: thà hỏng ở đây còn hơn gửi một request thiếu header
 */
export function userHeaders(): { 'X-User-Id': string } {
  if (currentUser == null) {
    throw new Error('No user is signed in')
  }
  return { 'X-User-Id': currentUser }
}

/** Header của một API trong một không gian (registry). */
export function workspaceHeaders(workspaceId: string): { 'X-Workspace-Id': string; 'X-User-Id': string } {
  return { 'X-Workspace-Id': workspaceId, ...userHeaders() }
}

/** Người dùng của phiên, hoặc `null` khi chưa chọn ai. Dùng để hiện "Bạn" thay cho một `id`. */
export function currentUserId(): string | null {
  return currentUser
}
