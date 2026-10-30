// BUILD FLOW 12.27 — frontend/src/components/StatusViews.jsx
// First introduced: Phase 12
// Purpose: What the results area shows when there are no products to list: a skeleton while the first page loads, an
//          empty state with a way forward, and an error with "Try again".
// Depends on: styles in app.css (12.09, section 12.09.12 for the skeleton and 12.09.15 for the messages).
// Used by: App (12.29).
// Runtime role: Chosen by App from the state of the latest request.

// BUILD FLOW 12.27.01 — Skeleton
// Grey blocks in the shape of the cards hold the layout steady while the first page loads, so nothing jumps when the
// products arrive. The whole skeleton is hidden from screen readers; the toolbar's live count says "Loading products".
export function GridSkeleton({ count }) {
  return (
    <ul className="product-grid product-grid--skeleton" aria-hidden="true">
      {Array.from({ length: count }, (_, index) => (
        <li key={index} className="product-grid__item">
          <div className="card">
            <div className="card__media skeleton" />
            <div className="card__body">
              <div className="skeleton skeleton--line skeleton--short" />
              <div className="skeleton skeleton--line" />
              <div className="skeleton skeleton--line skeleton--short" />
            </div>
          </div>
        </li>
      ))}
    </ul>
  )
}

// BUILD FLOW 12.27.02 — Empty results
// Three different situations, each with its own way forward: a page number past the end of the results (go to page
// 1), filters that match nothing (clear them), or an empty catalog.
export function EmptyState({ pastTheEnd, filtersActive, onFirstPage, onClearAll }) {
  let title = 'The catalog is empty.'
  let text = 'Products will appear here as soon as there are any.'
  let action = null
  if (pastTheEnd) {
    title = 'This page is past the end of the results.'
    text = 'The results have fewer pages than that.'
    action = <button type="button" className="button button--primary" onClick={onFirstPage}>Go to page 1</button>
  } else if (filtersActive) {
    title = 'No products match these filters.'
    text = 'Try a shorter search, another category, or a wider price range.'
    action = <button type="button" className="button button--primary" onClick={onClearAll}>Clear all filters</button>
  }
  return (
    <div className="status">
      <h3 className="status__title">{title}</h3>
      <p className="status__text">{text}</p>
      {action}
    </div>
  )
}

// BUILD FLOW 12.27.03 — Errors
// role="alert" makes screen readers announce the message as soon as it appears. The message comes from ApiError
// (12.12): the API's own explanation for an invalid search, or a plain sentence for anything else.
export function ErrorState({ message, onRetry }) {
  return (
    <div className="status status--error" role="alert">
      <h3 className="status__title">The products could not be loaded.</h3>
      <p className="status__text">{message}</p>
      <button type="button" className="button button--primary" onClick={onRetry}>Try again</button>
    </div>
  )
}
