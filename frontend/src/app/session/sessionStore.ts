import { setCurrentUser } from '@/api'

// Chưa có đăng nhập thật: "phiên" chỉ là id của người dùng đang chọn, giữ ở localStorage.
// Khi có đăng nhập, chỉ thư mục này và `api/headers.ts` phải đổi.

const USER_KEY = 'hp.userId'
const LAST_WORKSPACE_KEY = 'hp.lastWorkspaceId'

function read(key: string): string | null {
  try {
    return localStorage.getItem(key)
  } catch {
    // Trình duyệt chặn localStorage (chế độ riêng tư): coi như chưa lưu gì.
    return null
  }
}

function write(key: string, value: string | null): void {
  try {
    if (value == null) {
      localStorage.removeItem(key)
    } else {
      localStorage.setItem(key, value)
    }
  } catch {
    // Không lưu được thì phiên chỉ sống tới khi tải lại trang.
  }
}

let userId = read(USER_KEY)
const listeners = new Set<() => void>()

// Đặt người gọi cho lớp API ngay khi module được nạp, không chờ component nào render: request
// đầu tiên của trang phải mang đúng header.
setCurrentUser(userId)

function setUser(next: string | null): void {
  userId = next
  setCurrentUser(next)
  write(USER_KEY, next)
  listeners.forEach((listener) => listener())
}

/** Kho giữ người dùng đang chọn, nằm ngoài React. Component đọc nó qua `useSession`. */
export const sessionStore = {
  getUserId: (): string | null => userId,
  signIn: (id: string): void => setUser(id),
  signOut: (): void => {
    write(LAST_WORKSPACE_KEY, null)
    setUser(null)
  },
  subscribe: (listener: () => void): (() => void) => {
    listeners.add(listener)
    return () => {
      listeners.delete(listener)
    }
  },
  /** Không gian mở gần nhất, để lần sau vào thẳng đó. */
  getLastWorkspaceId: (): string | null => read(LAST_WORKSPACE_KEY),
  rememberWorkspace: (workspaceId: string): void => write(LAST_WORKSPACE_KEY, workspaceId),
}
