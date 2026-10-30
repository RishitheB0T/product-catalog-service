// BUILD FLOW 12.30 — frontend/src/test/setup.js
// First introduced: Phase 12
// Purpose: Runs before every test file: adds the DOM matchers and empties the simulated document after each test.
// Depends on: @testing-library/jest-dom, @testing-library/react, Vitest.
// Used by: vite.config.js (12.03, test.setupFiles).
// Runtime role: Test only.

import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// BUILD FLOW 12.30.01 — A clean page for every test
// jest-dom adds readable assertions such as toBeInTheDocument() and toHaveValue(). Testing Library cleans up by
// itself only when test functions are global, which this project doesn't enable, so cleanup runs here. The address
// is reset too, because the storefront keeps its state in it.
afterEach(() => {
  cleanup()
  window.history.replaceState(null, '', '/')
})
