// BUILD FLOW 12.20 — frontend/src/components/FilterPanel.jsx
// First introduced: Phase 12
// Purpose: The price range and availability filters. On wide screens it sits beside the products; on narrow screens
//          the same component appears inside the filter drawer.
// Depends on: priceRangeProblem (12.10), useDebouncedCallback (12.14); styles in app.css (12.09, section 12.09.09).
// Used by: App (12.29), once in the sidebar and once inside FilterDrawer (12.21).
// Runtime role: Turns form input into filter changes.

import { useState } from 'react'
import { useDebouncedCallback } from '../hooks/useDebouncedCallback.js'
import { priceRangeProblem } from '../lib/catalog.js'

const TYPING_PAUSE_MS = 500

export default function FilterPanel({ state, onChange, idPrefix }) {
  // BUILD FLOW 12.20.01 — Draft prices, like the search box (12.18.01)
  // The price fields keep their own text while typing. A range is sent after a pause, and only if it makes sense; a
  // bad one is explained next to the fields instead of being sent to the API.
  const [min, setMin] = useState(state.minPrice)
  const [max, setMax] = useState(state.maxPrice)
  const [sent, setSent] = useState({ min: state.minPrice, max: state.maxPrice })
  if (state.minPrice !== sent.min || state.maxPrice !== sent.max) {
    setSent({ min: state.minPrice, max: state.maxPrice })
    setMin(state.minPrice)
    setMax(state.maxPrice)
  }

  const sendLater = useDebouncedCallback((nextMin, nextMax) => {
    if (priceRangeProblem(nextMin, nextMax)) return
    setSent({ min: nextMin, max: nextMax })
    onChange({ minPrice: nextMin, maxPrice: nextMax })
  }, TYPING_PAUSE_MS)

  const problem = priceRangeProblem(min.trim(), max.trim())
  const problemId = `${idPrefix}-price-problem`

  // BUILD FLOW 12.20.02 — Labels and grouping
  // Every field has a <label>, so clicking the text focuses the field and screen readers announce its name. The two
  // price fields share a <fieldset> whose <legend> names the group. inputMode="decimal" brings up a number keypad on
  // phones, while the field still accepts what the user types, so the message above can explain a mistake.
  return (
    <div className="filters">
      <fieldset className="filters__group">
        <legend className="filters__legend">Price (₹)</legend>
        <div className="filters__prices">
          <label className="field">
            <span className="field__label">Min</span>
            <input
              id={`${idPrefix}-min-price`}
              className="field__input"
              inputMode="decimal"
              placeholder="0"
              value={min}
              aria-invalid={Boolean(problem)}
              aria-describedby={problem ? problemId : undefined}
              onChange={(event) => {
                setMin(event.target.value)
                sendLater(event.target.value.trim(), max.trim())
              }}
            />
          </label>
          <span className="filters__dash" aria-hidden="true">–</span>
          <label className="field">
            <span className="field__label">Max</span>
            <input
              id={`${idPrefix}-max-price`}
              className="field__input"
              inputMode="decimal"
              placeholder="Any"
              value={max}
              aria-invalid={Boolean(problem)}
              aria-describedby={problem ? problemId : undefined}
              onChange={(event) => {
                setMax(event.target.value)
                sendLater(min.trim(), event.target.value.trim())
              }}
            />
          </label>
        </div>
        {problem && <p id={problemId} className="field__problem">{problem}</p>}
      </fieldset>

      <fieldset className="filters__group">
        <legend className="filters__legend">Availability</legend>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={state.inStock}
            onChange={(event) => onChange({ inStock: event.target.checked })}
          />
          <span>In stock only</span>
        </label>
      </fieldset>
    </div>
  )
}
