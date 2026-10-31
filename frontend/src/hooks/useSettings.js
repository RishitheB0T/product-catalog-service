// BUILD FLOW 13.04 — frontend/src/hooks/useSettings.js
// First introduced: Phase 13
// Purpose: The server's settings (whether products may be changed), fetched once per page load and shared by every
//          component that asks.
// Depends on: getSettings (12.12), and through it GET /api/v1/settings (13.01).
// Used by: App (12.29), which shows the admin link and pages only when writes are enabled.
// Runtime role: One request when the storefront starts.

import { useEffect, useState } from 'react'
import { getSettings } from '../api/products.js'

// BUILD FLOW 13.04.01 — One request, many readers
// The promise is kept at module level, so every caller waits for the same request instead of starting its own. If the
// settings can't be read, the storefront behaves as read-only: offering changes the server might refuse is worse than
// hiding them. The server enforces its switch either way.
let settings = null

export function loadSettings() {
  settings ??= getSettings().catch(() => ({ writesEnabled: false }))
  return settings
}

// Lets tests start from a fresh page load.
export function forgetSettings() {
  settings = null
}

// null while the answer is on its way.
export function useSettings() {
  const [value, setValue] = useState(null)
  useEffect(() => {
    let current = true
    loadSettings().then((loaded) => {
      if (current) setValue(loaded)
    })
    return () => {
      current = false
    }
  }, [])
  return value
}
