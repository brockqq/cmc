import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

/**
 * Content Security Policy for the built app (not the dev server, whose tooling injects inline scripts).
 * Receipts are shown from blob: URLs; nothing is loaded from other origins.
 */
const CSP = [
  "default-src 'self'",
  "script-src 'self'",
  "style-src 'self' 'unsafe-inline'",
  "img-src 'self' blob: data:",
  'frame-src blob:',
  // Chrome treats its PDF viewer (receipts shown in an iframe) as plugin content governed by object-src;
  // allow only the blob: URLs the page itself creates
  'object-src blob:',
  "base-uri 'self'",
  "form-action 'self'",
].join('; ')

const contentSecurityPolicy = () => ({
  name: 'content-security-policy',
  apply: 'build',
  transformIndexHtml: (html) =>
    html.replace('<head>', `<head>\n    <meta http-equiv="Content-Security-Policy" content="${CSP}" />`),
})

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    contentSecurityPolicy(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    // Forward API calls to the Spring Boot backend during development
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
