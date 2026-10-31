// BUILD FLOW 13.13 — frontend/src/components/FormField.jsx
// First introduced: Phase 13
// Purpose: One form field: its label, an optional hint, the control itself, and its error message, all connected for
//          assistive technology.
// Depends on: Styles in app.css (12.09, section 12.09.21).
// Used by: ProductFormPage (13.10).
// Runtime role: Renders on every keystroke in the form.

// BUILD FLOW 13.13.01 — A render prop for the control
// The field doesn't know whether it holds an <input>, a <select>, or a <textarea>, so the caller passes a function that
// receives the attributes the control needs: its id (which the <label> points at), aria-describedby (the hint and the
// error, read out after the label), and aria-invalid (announced as "invalid entry" while there is an error).
export default function FormField({ id, label, hint, error, children }) {
  const hintId = hint ? `${id}-hint` : null
  const errorId = error ? `${id}-error` : null
  const describedBy = [hintId, errorId].filter(Boolean).join(' ') || undefined
  return (
    <div className={`form-field${error ? ' form-field--error' : ''}`}>
      <label className="form-field__label" htmlFor={id}>{label}</label>
      {hint && <p id={hintId} className="form-field__hint">{hint}</p>}
      {error && <p id={errorId} className="form-field__error">{error}</p>}
      {children({ id, 'aria-describedby': describedBy, 'aria-invalid': Boolean(error) })}
    </div>
  )
}
