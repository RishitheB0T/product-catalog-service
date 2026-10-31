// BUILD FLOW 12.16 — frontend/src/components/SiteHeader.jsx
// First introduced: Phase 12
// Purpose: The top bar: the FORM wordmark (a link to the shop's front page), the search box (passed in as children),
//          and since Phase 13 a link to the admin area when the server allows changes.
// Depends on: Link (13.03), paths (13.02); styles in app.css (12.09, section 12.09.04).
// Used by: App (12.29).
// Runtime role: Rendered once per page; no state of its own.

import { paths } from '../lib/routes.js'
import Link from './Link.jsx'

// BUILD FLOW 12.16.01 — Composition through children
// The header doesn't know about searching. App puts the SearchBox inside it, the way HTML elements nest, which keeps
// this component a pure layout piece. The admin link appears only where it can work: the server says whether writes
// are on (13.01), and still refuses them itself when they are off.
export default function SiteHeader({ writesEnabled, children }) {
  return (
    <header className="site-header">
      <div className="site-header__inner">
        <Link className="brand" to={paths.catalog}>
          <span className="brand__mark">FORM</span>
          <span className="brand__tagline">The Everyday Edit</span>
        </Link>
        {writesEnabled && (
          <nav className="site-nav" aria-label="Site">
            <Link className="site-nav__link" to={paths.admin}>Admin</Link>
          </nav>
        )}
        {children}
      </div>
    </header>
  )
}
