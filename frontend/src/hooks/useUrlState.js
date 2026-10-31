// BUILD FLOW 12.13 — frontend/src/hooks/useUrlState.js
// First introduced: Phase 12
// Purpose: Keeps React in step with the address bar. Since Phase 13 it is the storefront's small router: it reports
//          the current path and query string, moves to a new address (navigate), and after a move restores the scroll
//          position (Back and Forward) or starts the new page at the top with focus on its content.
// Depends on: The browser's History API; React's useSyncExternalStore.
// Used by: App (12.29), Link (13.03), and ProductFormPage (13.10), which moves on after saving.
// Runtime role: Every link click, Back, Forward, and search change passes through here.

import { useEffect, useRef, useSyncExternalStore } from 'react'

const NAVIGATED = 'storefront:navigated'
let lastMove = 'load'

// BUILD FLOW 12.13.01 — The address bar as an external store
// The address lives outside React. useSyncExternalStore is React's tool for such state: it reads a snapshot (here the
// path and query string, one plain string) and renders again whenever subscribe's callback fires. The browser fires
// "popstate" for Back and Forward; pushState and replaceState fire nothing, so navigate() announces its own moves.
function subscribe(onChange) {
  const onPop = () => {
    lastMove = 'pop'
    onChange()
  }
  window.addEventListener('popstate', onPop)
  window.addEventListener(NAVIGATED, onChange)
  return () => {
    window.removeEventListener('popstate', onPop)
    window.removeEventListener(NAVIGATED, onChange)
  }
}

const currentUrl = () => `${window.location.pathname}${window.location.search}`
const pathOf = (url) => url.split('?')[0]

export function useUrlState() {
  const url = useSyncExternalStore(subscribe, currentUrl)
  const query = url.indexOf('?')
  return query === -1 ? { pathname: url, search: '' } : { pathname: url.slice(0, query), search: url.slice(query) }
}

// BUILD FLOW 12.13.02 — Moving to a new address
// pushState adds a history entry, so Back returns here; replaceState overwrites the current one (used while typing a
// search). Before leaving, the current entry remembers how far the page was scrolled. "state" carries facts for the
// next page, such as the results page the shopper came from, or a message to show once a change is saved.
export function navigate(url, { replace = false, state = null } = {}) {
  if (url === currentUrl() && !state) return
  if (replace) {
    lastMove = 'replace'
    window.history.replaceState(window.history.state, '', url)
  } else {
    lastMove = 'push'
    window.history.replaceState({ ...window.history.state, scrollY: window.scrollY }, '')
    window.history.pushState(state, '', url)
  }
  window.dispatchEvent(new Event(NAVIGATED))
}

// BUILD FLOW 12.13.03 — After a move: scroll position and focus
// The browser's own scroll restoration runs before React has drawn the earlier page again, so it is switched off and
// done here instead. After Back or Forward, the position remembered for that entry returns. After a link to another
// page, the page starts at the top, and focus moves to the main region: screen readers then start reading the new page,
// and the next Tab starts there. Search changes on the same page (typing, filters) leave scroll and focus alone.
export function useNavigationEffects(mainRef) {
  const url = useSyncExternalStore(subscribe, currentUrl)
  const previous = useRef(null)

  useEffect(() => {
    window.history.scrollRestoration = 'manual'
  }, [])

  useEffect(() => {
    const before = previous.current
    previous.current = url
    if (before === null) return
    const newPage = pathOf(before) !== pathOf(url)
    if (lastMove === 'pop') window.scrollTo(0, window.history.state?.scrollY ?? 0)
    else if (newPage) window.scrollTo(0, 0)
    if (newPage) mainRef.current?.focus({ preventScroll: true })
  }, [url, mainRef])
}
