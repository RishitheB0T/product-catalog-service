// BUILD FLOW 12.23 — frontend/src/components/ResultsToolbar.jsx
// First introduced: Phase 12
// Purpose: The bar above the products: how many there are, the sort order, and (on narrow screens) the button that
//          opens the filter drawer.
// Depends on: SORTS (12.10); styles in app.css (12.09, section 12.09.10).
// Used by: App (12.29).
// Runtime role: Shows the count from the latest response; changing the sort starts a new search.

import { SORTS } from '../lib/catalog.js'

// BUILD FLOW 12.23.01 — A live count
// aria-live="polite" makes screen readers announce the new count after each search, once the reader is idle, so a
// person who can't see the grid still hears that the results changed.
export default function ResultsToolbar({ summary, sort, onSort, onOpenFilters, drawerFilterCount }) {
  return (
    <div className="toolbar">
      <p className="toolbar__count" aria-live="polite">{summary}</p>
      <div className="toolbar__actions">
        <button type="button" className="button button--quiet toolbar__filters" onClick={onOpenFilters}>
          Filters{drawerFilterCount > 0 ? ` (${drawerFilterCount})` : ''}
        </button>
        <label className="sort">
          <span className="sort__label">Sort by</span>
          <select className="sort__select" value={sort} onChange={(event) => onSort(event.target.value)}>
            {SORTS.map((option) => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>
        </label>
      </div>
    </div>
  )
}
