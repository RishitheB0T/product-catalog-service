// BUILD FLOW 13.12 — frontend/src/components/ConfirmDialog.jsx
// First introduced: Phase 13
// Purpose: A modal question with two answers, such as "Delete this product?" with Cancel and Delete. It shows the
//          action in progress and any error, and it can always be dismissed.
// Depends on: The browser's <dialog> element; styles in app.css (12.09, section 12.09.22).
// Used by: AdminPage (13.09) for deleting a product.
// Runtime role: Opens on request; closes on Cancel, Escape, or after the action succeeds.

import { useEffect, useRef } from 'react'

// BUILD FLOW 13.12.01 — A native modal, safe by default
// showModal() moves focus into the dialog and makes the page behind it inert, and Escape closes it; its "close" event
// reports back through onCancel, so React's state and the element always agree. Cancel comes first, so it is the
// button that receives focus: pressing Enter by reflex can't destroy anything. aria-describedby makes screen readers
// read the question's details along with its title.
export default function ConfirmDialog({ open, title, children, confirmLabel, busyLabel, busy, error, onConfirm, onCancel }) {
  const dialogRef = useRef(null)

  useEffect(() => {
    const dialog = dialogRef.current
    if (open && !dialog.open) dialog.showModal?.()
    if (!open && dialog.open) dialog.close?.()
  }, [open])

  return (
    <dialog
      ref={dialogRef}
      className="confirm"
      aria-labelledby="confirm-title"
      aria-describedby="confirm-text"
      onClose={onCancel}
    >
      <h2 id="confirm-title" className="confirm__title">{title}</h2>
      <div id="confirm-text" className="confirm__text">{children}</div>
      {error && <p className="confirm__error" role="alert">{error}</p>}
      <div className="confirm__actions">
        <button type="button" className="button button--quiet" onClick={onCancel}>Cancel</button>
        <button type="button" className="button button--danger" disabled={busy} onClick={onConfirm}>
          {busy ? busyLabel : confirmLabel}
        </button>
      </div>
    </dialog>
  )
}
