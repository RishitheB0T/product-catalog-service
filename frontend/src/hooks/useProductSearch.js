// BUILD FLOW 12.15 — frontend/src/hooks/useProductSearch.js
// First introduced: Phase 12
// Purpose: Loads one page of search results for an API query string. It cancels the previous request when the query
//          changes, and reports whether it is loading, the results, or an error, with a way to try again.
// Depends on: searchProducts (12.12).
// Used by: CatalogPage (13.07) and AdminPage (13.09).
// Runtime role: One request per distinct query, from the browser to /api/v1/products.

import { useEffect, useState } from 'react'
import { searchProducts } from '../api/products.js'

// BUILD FLOW 12.15.03 — Recent answers, for Back (added in Phase 13)
// Returning to a results page from a product shows the last answer at once, so the page has its full height and the
// remembered scroll position can return (12.13.03); a fresh copy still loads quietly in the background. The admin
// pages clear this memory after every change they save, so nobody sees a product they have just changed in its old state.
const recent = new Map()
const RECENT_LIMIT = 20

function remember(query, data) {
  recent.delete(query)
  recent.set(query, data)
  if (recent.size > RECENT_LIMIT) recent.delete(recent.keys().next().value)
}

export function forgetRecentSearches() {
  recent.clear()
}

export function useProductSearch(query) {
  // "attempt" changes only for "Try again", which repeats the same query.
  const [attempt, setAttempt] = useState(0)
  const [result, setResult] = useState(() => (recent.has(query)
    ? { key: `${query}#0`, data: recent.get(query), error: null }
    : { key: null, data: null, error: null }))
  const key = `${query}#${attempt}`

  // BUILD FLOW 12.15.01 — Fetch in an effect, cancel in its cleanup
  // An effect runs after React has updated the page. When the query changes, React first runs the previous effect's
  // cleanup, which aborts the older request. Its answer can therefore never arrive late and overwrite newer results:
  // typing fast or clicking through filters always ends with the results of the last request.
  useEffect(() => {
    const controller = new AbortController()
    searchProducts(query, { signal: controller.signal })
      .then((data) => {
        remember(query, data)
        setResult({ key, data, error: null })
      })
      .catch((error) => {
        if (error.name !== 'AbortError') setResult({ key, data: null, error })
      })
    return () => controller.abort()
  }, [query, key])

  // BUILD FLOW 12.15.02 — Loading is derived, not stored
  // The result remembers which request it answers. While it belongs to an older request, the newest one is still
  // loading, and the previous results stay on screen (dimmed) instead of flashing to a skeleton. An error is
  // shown only once its own request has finished.
  const loading = result.key !== key
  return {
    loading,
    data: result.data,
    error: loading ? null : result.error,
    retry: () => setAttempt((n) => n + 1),
  }
}
