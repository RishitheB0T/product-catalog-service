// BUILD FLOW 12.22 — frontend/src/components/ActiveFilters.jsx
// First introduced: Phase 12
// Purpose: One removable chip for each active filter (search text, category, price range, availability), plus "Clear
//          all". Shows nothing when no filter is active.
// Depends on: categoryLabel and formatPrice (12.10); styles in app.css (12.09, section 12.09.11).
// Used by: App (12.29).
// Runtime role: Each click removes one filter, or all of them.

import { categoryLabel, formatPrice } from '../lib/catalog.js'

// BUILD FLOW 12.22.01 — Chips built from the state
// Each chip carries the change that removes it, for example { minPrice: '', maxPrice: '' } for the price chip, so one
// handler serves them all. The visible text is short; aria-label says what the button does.
function chipsFor(state) {
  const chips = []
  if (state.q) chips.push({ key: 'q', label: `“${state.q}”`, removal: { q: '' } })
  if (state.category) chips.push({ key: 'category', label: categoryLabel(state.category), removal: { category: '' } })
  if (state.minPrice || state.maxPrice) {
    let label = `${formatPrice(state.minPrice)} – ${formatPrice(state.maxPrice)}`
    if (!state.maxPrice) label = `From ${formatPrice(state.minPrice)}`
    if (!state.minPrice) label = `Up to ${formatPrice(state.maxPrice)}`
    chips.push({ key: 'price', label, removal: { minPrice: '', maxPrice: '' } })
  }
  if (state.inStock) chips.push({ key: 'inStock', label: 'In stock', removal: { inStock: false } })
  return chips
}

export default function ActiveFilters({ state, onRemove, onClearAll }) {
  const chips = chipsFor(state)
  if (chips.length === 0) return null
  return (
    <div className="chips">
      <h3 className="visually-hidden">Active filters</h3>
      <ul className="chips__list">
        {chips.map((chip) => (
          <li key={chip.key}>
            <button
              type="button"
              className="chip"
              aria-label={`Remove filter: ${chip.label}`}
              onClick={() => onRemove(chip.removal)}
            >
              <span>{chip.label}</span>
              <span className="chip__x" aria-hidden="true">×</span>
            </button>
          </li>
        ))}
      </ul>
      <button type="button" className="link-button" onClick={onClearAll}>Clear all</button>
    </div>
  )
}
