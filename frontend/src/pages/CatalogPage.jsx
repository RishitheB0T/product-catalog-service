// BUILD FLOW 13.07 — frontend/src/pages/CatalogPage.jsx
// First introduced: Phase 13 (the catalog itself was App in Phase 12)
// Purpose: The catalog page at "/": hero, categories, filters (sidebar or drawer), the result count and sort, chips,
//          the results, and pagination. The search state comes from the address bar, through App.
// Depends on: useProductSearch (12.15), catalog.js (12.10), and the catalog components (12.17–12.27).
// Used by: App (12.29) for the path "/".
// Runtime role: Re-renders whenever the search in the address bar or the latest response changes.

import { useState } from 'react'
import ActiveFilters from '../components/ActiveFilters.jsx'
import CategoryTabs from '../components/CategoryTabs.jsx'
import FilterDrawer from '../components/FilterDrawer.jsx'
import FilterPanel from '../components/FilterPanel.jsx'
import Hero from '../components/Hero.jsx'
import Pagination from '../components/Pagination.jsx'
import ProductGrid from '../components/ProductGrid.jsx'
import ResultsToolbar from '../components/ResultsToolbar.jsx'
import { EmptyState, ErrorState, GridSkeleton } from '../components/StatusViews.jsx'
import { usePageTitle } from '../hooks/usePageTitle.js'
import { useProductSearch } from '../hooks/useProductSearch.js'
import { DEFAULT_STATE, PAGE_SIZE, formatCount, hasFilters, toApiQuery, writeSearchState } from '../lib/catalog.js'
import { paths } from '../lib/routes.js'

// BUILD FLOW 13.07.01 — The count above the grid, from the server's numbers
function summarize(data) {
  if (!data) return 'Loading products…'
  const total = data.totalElements
  if (total === 0) return 'No products found'
  const noun = total === 1 ? 'product' : 'products'
  if (data.content.length === 0) return `${formatCount(total)} ${noun}`
  const first = data.page * data.size + 1
  const last = first + data.content.length - 1
  return `Showing ${formatCount(first)}–${formatCount(last)} of ${formatCount(total)} ${noun}`
}

export default function CatalogPage({ search, update }) {
  // BUILD FLOW 13.07.02 — State: one source of truth, everything else derived
  // The search lives in the address bar; the API query is computed from it on every render, and the request follows
  // the query. The only state of this page's own is whether the filter drawer is open.
  const { loading, data, error, retry } = useProductSearch(toApiQuery(search))
  const [drawerOpen, setDrawerOpen] = useState(false)
  const filtersActive = hasFilters(search)
  usePageTitle(search.q ? `“${search.q}”` : null)

  const clearAll = () => update({
    q: DEFAULT_STATE.q, category: DEFAULT_STATE.category, minPrice: DEFAULT_STATE.minPrice,
    maxPrice: DEFAULT_STATE.maxPrice, inStock: DEFAULT_STATE.inStock,
  })

  // A new page starts at the top of the results, not at the pagination where the click happened.
  const goToPage = (page) => {
    update({ page })
    const motion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth'
    document.getElementById('results')?.scrollIntoView?.({ behavior: motion, block: 'start' })
  }

  // BUILD FLOW 13.07.03 — Choosing what the results area shows
  // An error wins; then the skeleton until the first answer; then an empty state or the grid. While a newer request
  // is loading, whatever is shown stays visible: the wrapper's aria-busy tells screen readers that it is being
  // updated, and CSS dims it. Each card links to its product page and remembers this address, so the product page
  // can offer a way back to exactly these results.
  let results
  if (error) results = <ErrorState message={error.message} onRetry={retry} />
  else if (!data) results = <GridSkeleton count={PAGE_SIZE} />
  else if (data.content.length === 0) {
    results = (
      <EmptyState
        pastTheEnd={data.totalElements > 0}
        filtersActive={filtersActive}
        onFirstPage={() => goToPage(1)}
        onClearAll={clearAll}
      />
    )
  } else results = <ProductGrid products={data.content} from={`${paths.catalog}${writeSearchState(search)}`} />

  return (
    <>
      <Hero />
      <CategoryTabs value={search.category} onChange={(category) => update({ category })} />

      <div className="catalog">
        <aside className="catalog__filters" aria-labelledby="filters-title">
          <h2 id="filters-title" className="catalog__filters-title">Filters</h2>
          <FilterPanel idPrefix="sidebar" state={search} onChange={update} />
        </aside>

        <section id="results" className="catalog__results" aria-labelledby="results-title" tabIndex={-1}>
          <h2 id="results-title" className="visually-hidden">Products</h2>
          <ResultsToolbar
            summary={summarize(data)}
            sort={search.sort}
            onSort={(sort) => update({ sort })}
            onOpenFilters={() => setDrawerOpen(true)}
            drawerFilterCount={[search.minPrice || search.maxPrice, search.inStock].filter(Boolean).length}
          />
          <ActiveFilters state={search} onRemove={update} onClearAll={clearAll} />
          <div className="results" aria-busy={loading}>{results}</div>
          {data && !error && <Pagination page={search.page} totalPages={data.totalPages} onPage={goToPage} />}
        </section>
      </div>

      <FilterDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} resultCount={data?.totalElements}>
        <FilterPanel idPrefix="drawer" state={search} onChange={update} />
      </FilterDrawer>
    </>
  )
}
