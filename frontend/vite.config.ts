import path from "path"
import tailwindcss from "@tailwindcss/vite"
import react from "@vitejs/plugin-react"
import { defineConfig } from "vite"

/**
 * Chuyển `/api/<service>/...` tới service ở `port`, bỏ tiền tố. Biến môi trường
 * `HP_<SERVICE>_PORT` (ví dụ `HP_REGISTRY_PORT=18082`) đổi cổng, để chạy một bộ service thứ hai
 * song song với bộ đang dùng.
 */
function service(name: string, port: number) {
  const prefix = `/api/${name}`
  const target = process.env[`HP_${name.toUpperCase()}_PORT`] ?? String(port)
  return {
    [prefix]: {
      target: `http://localhost:${target}`,
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
  build: {
    rolldownOptions: {
      output: {
        // Thư viện đổi ít hơn code của ứng dụng: tách riêng để trình duyệt giữ được cache của
        // chúng qua các lần phát hành.
        codeSplitting: {
          groups: [
            { name: "react", test: /node_modules[\\/](react|react-dom|react-router|scheduler)[\\/]/, priority: 2 },
            { name: "vendor", test: /node_modules[\\/]/, priority: 1 },
          ],
        },
      },
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
