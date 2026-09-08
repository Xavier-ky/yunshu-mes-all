import { fileURLToPath, URL } from "node:url";
import path from "node:path";
import vue from "@vitejs/plugin-vue";
import { createSvgIconsPlugin } from "vite-plugin-svg-icons";
import { defineConfig } from "vite";

const rootDir = fileURLToPath(new URL(".", import.meta.url));

export default defineConfig({
  plugins: [
    vue(),
    createSvgIconsPlugin({
      iconDirs: [path.resolve(rootDir, "src/yunshu-ui/assets/icons/svg")],
      symbolId: "icon-[name]",
    }),
  ],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  optimizeDeps: {
    exclude: ["dhtmlx-gantt"],
  },
  server: {
    host: "127.0.0.1",
    port: 5173,
    proxy: {
      "/api": {
        target: "http://127.0.0.1:8080",
        changeOrigin: true,
      },
      "/ureport": {
        target: "http://127.0.0.1:8080",
        changeOrigin: true,
      },
      "/yunshu-agent": {
        target: "http://127.0.0.1:8090/api",
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/yunshu-agent/, ""),
      },
    },
  },
});
