// Vue 플러그인을 활성화하고 `@`를 src 절대 별칭처럼 사용할 수 있게 하는 Vite 설정입니다.
import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

const projectRoot = fileURLToPath(new URL("..", import.meta.url));

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
      "@shared": fileURLToPath(new URL("./shared", import.meta.url)),
    },
  },
  server: {
    host: "127.0.0.1",
    port: 5174,
    strictPort: true,
    fs: { allow: [projectRoot] },
    proxy: {
      // 로컬 개발 중 Spring API는 같은 출처의 /api 경로로 전달해 CORS 설정 차이를 줄입니다.
      "/api": {
        target: "http://127.0.0.1:8080",
        changeOrigin: true,
      },
      "/ai": {
        target: "http://127.0.0.1:8000",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/ai/, ""),
      },
      "/uploads/products": {
        target: "http://127.0.0.1:8080",
        changeOrigin: true,
      },
      // 리뷰 사진도 Spring이 저장한 공개 경로에서 그대로 제공한다.
      "/uploads/images": {
        target: "http://127.0.0.1:8080",
        changeOrigin: true,
      },
    },
  },
});

