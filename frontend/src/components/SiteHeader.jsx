// BUILD FLOW 12.16 — frontend/src/components/SiteHeader.jsx
// First introduced: Phase 12
// Purpose: The top bar: the FORM wordmark (a link to the start page) and, passed in as children, the search box.
// Depends on: Styles in app.css (12.09, section 12.09.04).
// Used by: App (12.29).
// Runtime role: Rendered once per page; no state of its own.

// BUILD FLOW 12.16.01 — Composition through children
// The header doesn't know about searching. App puts the SearchBox inside it, the way HTML elements nest, which keeps
// this component a pure layout piece.
export default function SiteHeader({ children }) {
  return (
    <header className="site-header">
      <div className="site-header__inner">
        <a className="brand" href="/">
          <span className="brand__mark">FORM</span>
          <span className="brand__tagline">The Everyday Edit</span>
        </a>
        {children}
      </div>
    </header>
  )
}
