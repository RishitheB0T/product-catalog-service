// BUILD FLOW 12.18 — frontend/src/components/SearchBox.jsx
// First introduced: Phase 12
// Purpose: The search field. It sends the text after a 300 ms pause in typing, at once on Enter, and clears with one
//          click. It follows changes made elsewhere (a removed chip, the Back button).
// Depends on: useDebouncedCallback (12.14); styles in app.css (12.09, section 12.09.05).
// Used by: App (12.29), inside SiteHeader (12.16).
// Runtime role: Turns keystrokes into search changes; App writes them to the address bar.

import { useState } from 'react'
import { useDebouncedCallback } from '../hooks/useDebouncedCallback.js'

const TYPING_PAUSE_MS = 300

export default function SearchBox({ value, onSearch }) {
  // BUILD FLOW 12.18.01 — A draft for the field, and the value last sent
  // "draft" is what the field shows while typing; "sent" is the search this box last sent, or last took over from
  // outside. When the search changes elsewhere (value differs from sent), the field takes the new value. This runs
  // during rendering, as React's documentation recommends for adjusting state when a prop changes. A change this box
  // sent itself arrives with value equal to sent, so typing that is still going on is never overwritten.
  const [draft, setDraft] = useState(value)
  const [sent, setSent] = useState(value)
  if (value !== sent) {
    setSent(value)
    setDraft(value)
  }

  const send = (text, options) => {
    const trimmed = text.trim()
    setSent(trimmed)
    onSearch(trimmed, options)
  }

  // BUILD FLOW 12.18.02 — When a search becomes a history entry
  // Starting a search from no search adds a history entry, so Back returns to the full catalog. Refining a search
  // replaces the current entry, so Back does not walk through every pause in typing.
  const sendLater = useDebouncedCallback((text) => send(text, { replace: value !== '' }), TYPING_PAUSE_MS)

  // BUILD FLOW 12.18.03 — The <search> landmark
  // The <search> element tells assistive technology that this is the page's search, so screen-reader users can jump
  // straight to it. The <form> inside makes Enter submit.
  return (
    <search className="search">
      <form
        onSubmit={(event) => {
          event.preventDefault()
          sendLater.cancel()
          send(draft)
        }}
      >
        <label htmlFor="site-search" className="visually-hidden">Search products</label>
        <input
          id="site-search"
          className="search__input"
          type="search"
          placeholder="Search products"
          autoComplete="off"
          maxLength={100}
          value={draft}
          onChange={(event) => {
            setDraft(event.target.value)
            sendLater(event.target.value)
          }}
        />
        {draft && (
          <button
            type="button"
            className="search__clear"
            aria-label="Clear search"
            onClick={() => {
              sendLater.cancel()
              setDraft('')
              send('')
            }}
          >
            <span aria-hidden="true">×</span>
          </button>
        )}
      </form>
    </search>
  )
}
