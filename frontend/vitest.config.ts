import { defineConfig, mergeConfig } from "vitest/config"

import viteConfig from "./vite.config.ts"

export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      environment: "jsdom",
      setupFiles: ["src/test/setup.ts"],
      css: false,
      // Mỗi test dựng cả ứng dụng và đi qua nhiều bước người dùng; chạy song song cả bộ trên máy
      // chậm, vài test vượt 5 giây mặc định dù chạy riêng chỉ mất hơn 1 giây.
      testTimeout: 15_000,
    },
  }),
)
