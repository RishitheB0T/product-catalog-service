// BUILD FLOW 13.11 — frontend/src/pages/NotFoundPage.jsx
// First introduced: Phase 13
// Purpose: The page for an address the storefront doesn't know, such as /products/abc or a mistyped path.
// Depends on: Link (13.03), paths (13.02), usePageTitle (13.06).
// Used by: App (12.29) when matchRoute (13.02) finds no page.
// Runtime role: Static content; no request is sent.

import Link from '../components/Link.jsx'
import { usePageTitle } from '../hooks/usePageTitle.js'
import { paths } from '../lib/routes.js'

// BUILD FLOW 13.11.01 — A dead end with a way out
// The server answers every page address with the same index.html (Vite in development; Nginx's fallback in Phase 14),
// so unknown paths reach the app, which shows this page instead of an empty screen.
export default function NotFoundPage() {
  usePageTitle('Page not found')
  return (
    <div className="status">
      <h1 className="status__title">This page doesn't exist.</h1>
      <p className="status__text">The address may be mistyped, or the page may have moved.</p>
      <Link className="button button--primary" to={paths.catalog}>Go to the shop</Link>
    </div>
  )
}
