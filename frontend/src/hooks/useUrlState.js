// BUILD FLOW 12.13 — frontend/src/hooks/useUrlState.js
// First introduced: Phase 12
// Purpose: Keeps the search state in the address bar. It reads the state when the page loads and on Back or Forward,
//          and writes every change as a new history entry (or over the current one, while typing).
// Depends on: readSearchState and writeSearchState (12.10); the browser's History API.
// Used by: App (12.29).
// Runtime role: Runs on every search change; never contacts the server itself.

import { useCallback, useEffect, useState } from 'react'
import { readSearchState, writeSearchState } from '../lib/catalog.js'

export function useUrlState() {
  // A function passed to useState runs only on the first render, so the address is parsed once, not on every render.
  const [state, setState] = useState(() => readSearchState(window.location.search))

  // BUILD FLOW 12.13.01 — Back and Forward
  // The browser fires "popstate" when the person moves through the history; the address is then read again. The
  // effect's cleanup removes the listener when the component goes away, so listeners never pile up.
  useEffect(() => {
    const onPopState = () => setState(readSearchState(window.location.search))
    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [])

  // BUILD FLOW 12.13.02 — Changing the search
  // Every change except a page change returns to page 1: page 3 of the old results means nothing for new ones.
  // pushState adds a history entry, so Back undoes the change; replaceState overwrites the current entry and is used
  // while typing, so that Back does not step through every letter. An unchanged address writes nothing at all.
  const update = useCallback((changes, { replace = false } = {}) => {
    const next = { ...state, ...changes }
    if (!('page' in changes)) next.page = 1
    const url = `${window.location.pathname}${writeSearchState(next)}`
    if (url !== `${window.location.pathname}${window.location.search}`) {
      if (replace) window.history.replaceState(null, '', url)
      else window.history.pushState(null, '', url)
    }
    setState(next)
  }, [state])

  return [state, update]
}
