import { setupServer } from 'msw/node'

/** Server giả của mọi test. Mỗi test tự đăng ký handler bằng `server.use(...)`. */
export const server = setupServer()
