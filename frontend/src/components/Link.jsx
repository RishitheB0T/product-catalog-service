// BUILD FLOW 13.03 — frontend/src/components/Link.jsx
// First introduced: Phase 13
// Purpose: A link between the storefront's own pages. It is a real <a href>, so it can be opened in a new tab,
//          copied, and read as a link, but a plain click changes the page without reloading the whole app.
// Depends on: navigate (12.13).
// Used by: The header, product cards, the product page, and the admin pages.
// Runtime role: Turns ordinary clicks into in-app navigation.

import { navigate } from '../hooks/useUrlState.js'

// BUILD FLOW 13.03.01 — Only plain clicks are intercepted
// The browser keeps everything else: Ctrl or Cmd (new tab), Shift (new window), Alt (download), a middle click
// (button 1), and a link aimed at another window (target). preventDefault stops the full page load; navigate()
// changes the address and lets React draw the new page.
export default function Link({ to, state, replace, onClick, children, ...props }) {
  return (
    <a
      href={to}
      {...props}
      onClick={(event) => {
        onClick?.(event)
        const plain = event.button === 0 && !event.metaKey && !event.ctrlKey && !event.shiftKey && !event.altKey
        if (event.defaultPrevented || !plain || props.target) return
        event.preventDefault()
        navigate(to, { state, replace })
      }}
    >
      {children}
    </a>
  )
}
