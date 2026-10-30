// BUILD FLOW 12.10 — frontend/src/lib/catalog.js
// First introduced: Phase 12
// Purpose: The storefront's rules about catalog data, kept out of the components: the categories and sort orders the
//          API accepts, the search state read from and written to the address bar, the API query built from it,
//          price checks, the page numbers to show, and INR formatting.
// Depends on: The contract of GET /api/v1/products: Category (04.02), ProductSort (06.01), ProductSearchRequest (06.04).
// Used by: App (12.29), useUrlState (12.13), the components that show or change the search, catalog.test.js (12.31).
// Runtime role: Pure functions and constants: no network, no React, so they are easy to test.

// BUILD FLOW 12.10.01 — Categories and sort orders, spelled exactly as the API expects them
export const CATEGORIES = [
  { value: 'APPAREL', label: 'Apparel' },
  { value: 'FOOTWEAR', label: 'Footwear' },
  { value: 'BAGS', label: 'Bags' },
  { value: 'HOME', label: 'Home' },
  { value: 'KITCHEN', label: 'Kitchen' },
  { value: 'STATIONERY', label: 'Stationery' },
]

export const SORTS = [
  { value: 'newest', label: 'Newest' },
  { value: 'price_asc', label: 'Price: low to high' },
  { value: 'price_desc', label: 'Price: high to low' },
  { value: 'name_asc', label: 'Name: A to Z' },
]

// Twelve products per page fill rows of two, three, or four cards without a gap.
export const PAGE_SIZE = 12
// The API accepts zero-based pages up to 10,000 (ProductSearchRequest), which is page 10,001 when counting from one.
export const MAX_PAGE = 10_001

export function categoryLabel(value) {
  return CATEGORIES.find((category) => category.value === value)?.label ?? value
}

// BUILD FLOW 12.10.02 — The search state and the address bar
// The address bar is the single source of truth for what is shown, so a refresh or a shared link restores it exactly.
// readSearchState accepts any query string, including a hand-edited one: unknown or malformed values fall back to
// their defaults instead of becoming a request the API would reject. Prices stay text, exactly as typed.
export const DEFAULT_STATE = Object.freeze({
  q: '', category: '', minPrice: '', maxPrice: '', inStock: false, sort: 'newest', page: 1,
})

// The same limits as the API: at most 8 digits before the decimal point and 2 after (DECIMAL(10,2)), never negative.
const PRICE = /^\d{1,8}(\.\d{1,2})?$/

export function readSearchState(search) {
  const params = new URLSearchParams(search)
  const pick = (name, isValid) => {
    const value = params.get(name) ?? ''
    return isValid(value) ? value : DEFAULT_STATE[name]
  }
  const page = Number(params.get('page') ?? '1')
  return {
    q: (params.get('q') ?? '').trim().slice(0, 100),
    category: pick('category', (value) => CATEGORIES.some((category) => category.value === value)),
    minPrice: pick('minPrice', (value) => PRICE.test(value)),
    maxPrice: pick('maxPrice', (value) => PRICE.test(value)),
    inStock: params.get('inStock') === 'true',
    sort: pick('sort', (value) => SORTS.some((sort) => sort.value === value)),
    page: Number.isInteger(page) && page >= 1 && page <= MAX_PAGE ? page : 1,
  }
}

// Only values that differ from the defaults are written, so the plain storefront keeps a clean address ("/").
export function writeSearchState(state) {
  const params = new URLSearchParams()
  if (state.q) params.set('q', state.q)
  if (state.category) params.set('category', state.category)
  if (state.minPrice) params.set('minPrice', state.minPrice)
  if (state.maxPrice) params.set('maxPrice', state.maxPrice)
  if (state.inStock) params.set('inStock', 'true')
  if (state.sort !== DEFAULT_STATE.sort) params.set('sort', state.sort)
  if (state.page > 1) params.set('page', String(state.page))
  const text = params.toString()
  return text ? `?${text}` : ''
}

// Whether any filter is active (the sort and the page do not count): used for "Clear all" and the empty state.
export function hasFilters(state) {
  return Boolean(state.q || state.category || state.minPrice || state.maxPrice || state.inStock)
}

// BUILD FLOW 12.10.03 — From the search state to the API's query string
// People count pages from 1 and the API from 0. The sort, page, and size are always sent, so every request spells
// out exactly what it asks for.
export function toApiQuery(state) {
  const params = new URLSearchParams()
  if (state.q) params.set('q', state.q)
  if (state.category) params.set('category', state.category)
  if (state.minPrice) params.set('minPrice', state.minPrice)
  if (state.maxPrice) params.set('maxPrice', state.maxPrice)
  if (state.inStock) params.set('inStock', 'true')
  params.set('sort', state.sort)
  params.set('page', String(state.page - 1))
  params.set('size', String(PAGE_SIZE))
  return params.toString()
}

// BUILD FLOW 12.10.04 — Checking a price range before sending it
// The API would answer a bad range with a 400; checking first lets the form explain the problem next to the fields.
// Returns a message, or an empty string when the range is fine (empty fields mean "no limit").
export function priceRangeProblem(min, max) {
  if ((min && !PRICE.test(min)) || (max && !PRICE.test(max))) {
    return 'Enter prices as whole rupees or with up to two decimals, for example 1500 or 1499.50.'
  }
  if (min && max && Number(min) > Number(max)) return 'The minimum price is higher than the maximum.'
  return ''
}

// BUILD FLOW 12.10.05 — Which page numbers to show
// Always the first and the last page, and the current one with its neighbours; "…" marks every gap. For example,
// page 5 of 20 gives [1, '…', 4, 5, 6, '…', 20].
export function pageWindow(page, lastPage) {
  const wanted = new Set([1, lastPage, page - 1, page, page + 1])
  const pages = [...wanted].filter((n) => n >= 1 && n <= lastPage).sort((a, b) => a - b)
  const result = []
  for (const n of pages) {
    const previous = result[result.length - 1]
    if (previous !== undefined && n - previous === 2) result.push(n - 1)
    else if (previous !== undefined && n - previous > 2) result.push('…')
    result.push(n)
  }
  return result
}

// BUILD FLOW 12.10.06 — Rupees and counts, the Indian way
// The en-IN locale groups digits in lakhs and crores (1,23,456) and puts the ₹ sign first. Paise appear only when
// there are any: ₹1,290 but ₹1,290.50 ("stripIfInteger").
const rupees = new Intl.NumberFormat('en-IN', {
  style: 'currency', currency: 'INR', minimumFractionDigits: 2, trailingZeroDisplay: 'stripIfInteger',
})
const counts = new Intl.NumberFormat('en-IN')

export function formatPrice(value) {
  return rupees.format(Number(value))
}

export function formatCount(value) {
  return counts.format(value)
}
