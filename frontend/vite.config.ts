import path from "path"
import tailwindcss from "@tailwindcss/vite"
import react from "@vitejs/plugin-react"
import { defineConfig } from "vite"

/** Chuyển `/api/<service>/...` tới service ở `port`, bỏ tiền tố. */
function service(name: string, port: number) {
  const prefix = `/api/${name}`
  return {
    [prefix]: {
      target: `http://localhost:${port}`,
      rewrite: (requestPath: string) => requestPath.slice(prefix.length),
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      "@": path.resolve(import.meta.dirname, "./src"),
    },
  },
  server: {
    // Trong lúc phát triển, trình duyệt gọi ba service qua cùng origin với trang.
    proxy: {
      ...service("identity", 8081),
      ...service("registry", 8082),
      ...service("media", 8083),
    },
  },
})
