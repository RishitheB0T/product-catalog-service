// BUILD FLOW 12.30 — frontend/src/test/setup.js
// First introduced: Phase 12
// Purpose: Runs before every test file: adds the DOM matchers, fills two gaps in jsdom (since Phase 13), and resets
//          the page, the address, and the app's module-level memory after each test.
// Depends on: @testing-library/jest-dom, @testing-library/react, Vitest; useSettings (13.04), useProductSearch (12.15).
// Used by: vite.config.js (12.03, test.setupFiles).
// Runtime role: Test only.

import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'
import { forgetRecentSearches } from '../hooks/useProductSearch.js'
import { forgetSettings } from '../hooks/useSettings.js'

// BUILD FLOW 12.30.01 — A clean page for every test
// jest-dom adds readable assertions such as toBeInTheDocument() and toHaveValue(). Testing Library cleans up by
// itself only when test functions are global, which this project doesn't enable, so cleanup runs here. The address
// is reset too, because the storefront keeps its state in it, and so are the settings and recent results that the
// app remembers between pages (since Phase 13).
afterEach(() => {
  cleanup()
  window.history.replaceState(null, '', '/')
  forgetSettings()
  forgetRecentSearches()
})

// BUILD FLOW 12.30.02 — What jsdom doesn't do (added in Phase 13)
// jsdom has no layout, so scrolling does nothing; and it has the <dialog> element but not showModal() or close().
// These stand-ins do what the tests need: open and close the dialog (the "open" attribute and the "close" event) and
// accept scroll calls silently. Focus trapping and the inert page are real-browser behaviour, checked in a browser.
window.scrollTo = () => {}
if (!HTMLDialogElement.prototype.showModal) {
  HTMLDialogElement.prototype.showModal = function showModal() {
    this.setAttribute('open', '')
  }
  HTMLDialogElement.prototype.close = function close() {
    if (!this.hasAttribute('open')) return
    this.removeAttribute('open')
    this.dispatchEvent(new Event('close'))
  }
}
