// BUILD FLOW 12.26 — frontend/src/components/Pagination.jsx
// First introduced: Phase 12
// Purpose: Previous and Next, and a short window of page numbers, below the grid. Hidden when there is only one page.
// Depends on: pageWindow and MAX_PAGE (12.10); styles in app.css (12.09, section 12.09.13).
// Used by: App (12.29).
// Runtime role: Each click asks for another page of the same search.

import { MAX_PAGE, pageWindow } from '../lib/catalog.js'

// BUILD FLOW 12.26.01 — Buttons and the current page
// The page numbers change what is shown here instead of going elsewhere, so they are buttons. aria-current="page"
// marks the current one for screen readers (CSS styles it from the same attribute). The last page is capped at the
// highest page the API accepts.
export default function Pagination({ page, totalPages, onPage }) {
  const lastPage = Math.min(totalPages, MAX_PAGE)
  if (lastPage <= 1) return null
  return (
    <nav className="pagination" aria-label="Pages">
      <button
        type="button"
        className="pagination__step"
        disabled={page <= 1}
        onClick={() => onPage(page - 1)}
      >
        Previous
      </button>
      <ul className="pagination__pages">
        {pageWindow(page, lastPage).map((item, index) =>
          item === '…' ? (
            <li key={`gap-${index}`} className="pagination__gap" aria-hidden="true">…</li>
          ) : (
            <li key={item}>
              <button
                type="button"
                className="pagination__page"
                aria-label={`Page ${item}`}
                aria-current={item === page ? 'page' : undefined}
                onClick={() => onPage(item)}
              >
                {item}
              </button>
            </li>
          ),
        )}
      </ul>
      <button
        type="button"
        className="pagination__step"
        disabled={page >= lastPage}
        onClick={() => onPage(page + 1)}
      >
        Next
      </button>
    </nav>
  )
}
