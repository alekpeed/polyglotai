import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { viteSingleFile } from "vite-plugin-singlefile";

/**
 * Builds a single, self-contained HTML file of the app for design/demo QA — NOT the shipped
 * product. `@tauri-apps/plugin-sql` (real IPC to Tauri's Rust backend) is aliased to
 * src/preview/tauriSqlPluginShim.ts (in-browser sql.js, persisted to localStorage) so the same
 * screens/components run in a plain browser tab. The real build (vite.config.ts, used by
 * `pnpm dev`/`pnpm build`/`tauri dev`) never references this file or the shim.
 *
 * Run with: pnpm preview:build  → dist-preview/preview.html
 */
export default defineConfig({
  plugins: [react(), viteSingleFile()],
  build: {
    outDir: "dist-preview",
    // Inline every asset (fonts, the sql.js wasm binary) as data URIs so the whole app is one file.
    assetsInlineLimit: 100_000_000,
    rollupOptions: {
      input: "preview.html",
    },
  },
  resolve: {
    alias: {
      "@tauri-apps/plugin-sql": new URL("./src/preview/tauriSqlPluginShim.ts", import.meta.url).pathname,
    },
  },
});
