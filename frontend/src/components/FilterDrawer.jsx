// BUILD FLOW 12.21 — frontend/src/components/FilterDrawer.jsx
// First introduced: Phase 12
// Purpose: The filter panel for narrow screens: a modal drawer that slides in from the side, with a close button and a
//          "Show N products" button.
// Depends on: The browser's <dialog> element; styles in app.css (12.09, section 12.09.14).
// Used by: App (12.29), which passes FilterPanel (12.20) in as children.
// Runtime role: Opens and closes on request; the filters inside apply immediately, like the sidebar's.

import { useEffect, useRef } from 'react'
import { formatCount } from '../lib/catalog.js'

export default function FilterDrawer({ open, onClose, resultCount, children }) {
  const dialogRef = useRef(null)

  // BUILD FLOW 12.21.01 — A native modal dialog
  // showModal() gives the drawer what an accessible dialog needs without extra code: focus moves inside, the rest of
  // the page becomes inert (it can't be clicked or tabbed to), Escape closes it, and focus returns to the button
  // that opened it. React's "open" value is mirrored onto the element here. The dialog's own "close" event (for
  // example from Escape) reports back through onClose, so both always agree.
  useEffect(() => {
    const dialog = dialogRef.current
    if (open && !dialog.open) dialog.showModal?.()
    if (!open && dialog.open) dialog.close?.()
  }, [open])

  const count = resultCount === undefined ? '' : ` ${formatCount(resultCount)}`
  return (
    <dialog ref={dialogRef} className="drawer" aria-labelledby="drawer-title" onClose={onClose}>
      <div className="drawer__panel">
        <div className="drawer__header">
          <h2 id="drawer-title" className="drawer__title">Filters</h2>
          <button type="button" className="icon-button" aria-label="Close filters" onClick={onClose}>
            <span aria-hidden="true">×</span>
          </button>
        </div>
        {/* The fields exist only while the drawer is open, so the page never holds two copies of each one. */}
        <div className="drawer__body">{open && children}</div>
        <div className="drawer__footer">
          <button type="button" className="button button--primary button--block" onClick={onClose}>
            Show{count} {resultCount === 1 ? 'product' : 'products'}
          </button>
        </div>
      </div>
    </dialog>
  )
}
