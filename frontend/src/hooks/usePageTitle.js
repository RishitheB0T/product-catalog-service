// BUILD FLOW 13.06 — frontend/src/hooks/usePageTitle.js
// First introduced: Phase 13
// Purpose: Sets the browser tab's title for the current page, for example "Ceramic Bud Vase — FORM".
// Depends on: Nothing.
// Used by: Every page component (13.07–13.11).
// Runtime role: Runs when a page appears or its title changes.

import { useEffect } from 'react'

// BUILD FLOW 13.06.01 — Why titles matter in a single-page app
// The browser never loads a new document here, so the title would stay the same on every page. Screen readers announce
// the title after navigation, and the tab, the history list, and bookmarks all show it.
export function usePageTitle(title) {
  useEffect(() => {
    document.title = title ? `${title} — FORM` : 'FORM — The Everyday Edit'
  }, [title])
}
