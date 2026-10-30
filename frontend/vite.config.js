// BUILD FLOW 12.03 — frontend/vite.config.js
// First introduced: Phase 12
// Purpose: Vite's settings for the storefront: the React plugin, the development server on 127.0.0.1:24173 with /api
//          forwarded to the backend on 24081, the same address for "vite preview", and the test settings for Vitest.
// Depends on: @vitejs/plugin-react; the backend running with the dev profile on 127.0.0.1:24081 (README "Ports").
// Used by: npm run dev, build, preview, and test (package.json, 12.01).
// Runtime role: Development and build tooling only; nothing in this file reaches the browser.

import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// BUILD FLOW 12.03.01 — The development proxy
// The browser talks only to the Vite server. Requests whose path starts with /api are passed on to Spring Boot, so the
// page and the API share one origin: no CORS settings are needed, and the same relative URLs (/api/v1/...) will work
// behind Nginx in production (Phase 14).
const proxy = { '/api': { target: 'http://127.0.0.1:24081' } }

export default defineConfig({
  plugins: [react()],

  // BUILD FLOW 12.03.02 — Project-specific ports (README "Ports")
  // strictPort: fail with a clear message instead of quietly moving to another port when 24173 is taken.
  // "vite preview" serves the production build; it reuses 24173 because the two never run at the same time.
  server: { host: '127.0.0.1', port: 24173, strictPort: true, proxy },
  preview: { host: '127.0.0.1', port: 24173, strictPort: true, proxy },

  // BUILD FLOW 12.03.03 — Tests
  // Vitest reads this block. Components render into jsdom, a simulated browser document; setup.js (12.30) adds the
  // DOM matchers and cleans the document after each test. restoreMocks and unstubGlobals undo every spy and every
  // replaced global (such as fetch) after each test, so no test can leak into the next.
  test: { environment: 'jsdom', setupFiles: './src/test/setup.js', restoreMocks: true, unstubGlobals: true },
})
