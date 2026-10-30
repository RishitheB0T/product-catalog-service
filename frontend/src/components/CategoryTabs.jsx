// BUILD FLOW 12.19 — frontend/src/components/CategoryTabs.jsx
// First introduced: Phase 12
// Purpose: The row of categories ("All" plus the six), one of which is selected. On small screens the row scrolls
//          sideways instead of wrapping.
// Depends on: CATEGORIES (12.10); styles in app.css (12.09, section 12.09.07).
// Used by: App (12.29).
// Runtime role: Each click changes the category filter.

import { CATEGORIES } from '../lib/catalog.js'

// BUILD FLOW 12.19.01 — Toggle buttons, not links
// The categories filter the current page instead of leading somewhere else, so they are buttons. aria-pressed tells
// screen readers which one is selected; the same attribute drives the selected style in CSS.
export default function CategoryTabs({ value, onChange }) {
  const options = [{ value: '', label: 'All' }, ...CATEGORIES]
  return (
    <nav className="categories" aria-label="Categories">
      <ul className="categories__list">
        {options.map((option) => (
          <li key={option.value || 'all'}>
            <button
              type="button"
              className="categories__button"
              aria-pressed={value === option.value}
              onClick={() => onChange(option.value)}
            >
              {option.label}
            </button>
          </li>
        ))}
      </ul>
    </nav>
  )
}
