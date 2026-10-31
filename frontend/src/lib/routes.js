// BUILD FLOW 13.02 — frontend/src/lib/routes.js
// First introduced: Phase 13
// Purpose: The storefront's pages and their addresses: which page a path shows, and the paths that links use.
// Depends on: Nothing.
// Used by: App (12.29), which picks the page; every component that links to a page.
// Runtime role: Pure functions; run on every render of App.

// BUILD FLOW 13.02.01 — Addresses, built in one place
// Components ask paths.product(42) instead of writing "/products/42" themselves, so an address can change here alone.
export const paths = {
  catalog: '/',
  product: (id) => `/products/${id}`,
  admin: '/admin',
  newProduct: '/admin/products/new',
  editProduct: (id) => `/admin/products/${id}/edit`,
}

// BUILD FLOW 13.02.02 — From a path to a page
// Ids are positive whole numbers of at most 15 digits, which JavaScript numbers hold exactly; anything else, such as
// /products/abc, is an unknown page rather than a request the API would reject. A trailing slash is ignored.
const ID = '([1-9]\\d{0,14})'
const PRODUCT = new RegExp(`^/products/${ID}$`)
const EDIT_PRODUCT = new RegExp(`^/admin/products/${ID}/edit$`)

export function matchRoute(pathname) {
  const path = pathname.length > 1 ? pathname.replace(/\/+$/, '') : pathname
  if (path === paths.catalog) return { page: 'catalog' }
  if (path === paths.admin) return { page: 'admin' }
  if (path === paths.newProduct) return { page: 'new-product' }
  let match = PRODUCT.exec(path)
  if (match) return { page: 'product', id: Number(match[1]) }
  match = EDIT_PRODUCT.exec(path)
  if (match) return { page: 'edit-product', id: Number(match[1]) }
  return { page: 'not-found' }
}
