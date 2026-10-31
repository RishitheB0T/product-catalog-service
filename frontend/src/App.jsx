// BUILD FLOW 12.29 — frontend/src/App.jsx
// First introduced: Phase 12
// Purpose: The storefront's frame and router: the header, the page for the current address, and the footer. Since
//          Phase 13 it chooses among the catalog, a product page, the admin pages, and "not found"; the catalog itself
//          moved to CatalogPage (13.07).
// Depends on: useUrlState and navigate (12.13), matchRoute and paths (13.02), useSettings (13.04), catalog.js (12.10),
//             the pages 13.07–13.11, SiteHeader (12.16), SearchBox (12.18), SiteFooter (12.28).
// Used by: main.jsx (12.07).
// Runtime role: Re-renders whenever the address or the server's settings change.

import { useRef } from 'react'
import SearchBox from './components/SearchBox.jsx'
import SiteFooter from './components/SiteFooter.jsx'
import SiteHeader from './components/SiteHeader.jsx'
import { useSettings } from './hooks/useSettings.js'
import { navigate, useNavigationEffects, useUrlState } from './hooks/useUrlState.js'
import { DEFAULT_STATE, readSearchState, writeSearchState } from './lib/catalog.js'
import { matchRoute, paths } from './lib/routes.js'
import AdminPage from './pages/AdminPage.jsx'
import CatalogPage from './pages/CatalogPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'
import ProductFormPage from './pages/ProductFormPage.jsx'
import ProductPage from './pages/ProductPage.jsx'

export default function App() {
  // BUILD FLOW 12.29.01 — Everything follows from the address
  // The path picks the page (matchRoute); the query string is the search, read fresh on every render. Changing the
  // search means writing a new address: every change except a page change returns to page 1, and typing replaces the
  // current history entry instead of adding one (12.13.02).
  const { pathname, search } = useUrlState()
  const route = matchRoute(pathname)
  const searchState = readSearchState(search)
  const update = (changes, { replace = false } = {}) => {
    const next = { ...searchState, ...changes }
    if (!('page' in changes)) next.page = 1
    navigate(`${pathname}${writeSearchState(next)}`, { replace })
  }

  const settings = useSettings()
  const writesEnabled = settings?.writesEnabled === true
  const mainRef = useRef(null)
  useNavigationEffects(mainRef)

  // BUILD FLOW 12.29.02 — One search box for every page
  // On the catalog it refines the current search; anywhere else it starts a new search on the catalog.
  const onSearch = (q, options) => {
    if (route.page === 'catalog') update({ q }, options)
    else navigate(`${paths.catalog}${writeSearchState({ ...DEFAULT_STATE, q })}`)
  }

  // BUILD FLOW 12.29.03 — The page for this address
  let page
  switch (route.page) {
    case 'catalog':
      page = <CatalogPage search={searchState} update={update} />
      break
    case 'product':
      page = <ProductPage key={route.id} id={route.id} writesEnabled={writesEnabled} />
      break
    case 'admin':
      page = settings && <AdminPage search={searchState} update={update} writesEnabled={writesEnabled} />
      break
    case 'new-product':
      page = settings && <ProductFormPage writesEnabled={writesEnabled} />
      break
    case 'edit-product':
      page = settings && <ProductFormPage key={route.id} id={route.id} writesEnabled={writesEnabled} />
      break
    default:
      page = <NotFoundPage />
  }

  // BUILD FLOW 12.29.04 — The frame
  // The skip link jumps past the header to <main>, which can take focus (tabIndex -1) so that a page change can move
  // focus there (12.13.03). The admin pages wait for the settings, so they never flash a form the server would refuse.
  return (
    <>
      <a className="skip-link" href="#main">Skip to main content</a>
      <SiteHeader writesEnabled={writesEnabled}>
        <SearchBox value={route.page === 'catalog' ? searchState.q : ''} onSearch={onSearch} />
      </SiteHeader>
      <main id="main" ref={mainRef} className="page" tabIndex={-1}>{page}</main>
      <SiteFooter />
    </>
  )
}
