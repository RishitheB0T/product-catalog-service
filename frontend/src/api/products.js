// BUILD FLOW 12.12 — frontend/src/api/products.js
// First introduced: Phase 12
// Purpose: The one place that talks to the backend: search (Phase 12); since Phase 13 also one product, create,
//          update, delete, and the server's settings. Every failure becomes an ApiError that the screen can explain.
// Depends on: The product API (ProductController 05.11), SettingsController (13.01), and their Problem Details errors
//             (ApiExceptionHandler 05.12); in development the Vite proxy (12.03) forwards /api to the backend.
// Used by: useProductSearch (12.15), useProduct (13.05), useSettings (13.04), and the admin pages (13.09, 13.10).
// Runtime role: Runs in the browser for every API call (the SEARCH, READ, and WRITE runtime steps then run on the server).

// BUILD FLOW 12.12.01 — One error type for every way a request can fail
// status 0 means no answer arrived at all. type is the last part of the Problem Details type URI, for example
// "version-conflict", so callers can react to the kind of problem. fieldErrors is the server's list of
// {field, message} for invalid input.
export class ApiError extends Error {
  constructor(message, { status = 0, type = null, fieldErrors = [] } = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.type = type
    this.fieldErrors = fieldErrors
  }
}

// BUILD FLOW 12.12.02 — One request function for every call
// fetch() rejects only when no response arrives: the network is down, the server is unreachable, or the caller
// aborted. An HTTP error status still resolves, so response.ok is checked separately. An abort is passed on unchanged
// (its name is "AbortError"): the caller cancelled on purpose, so it is not an error to show. 204 No Content (a
// successful delete) has no body to read.
async function request(path, { method = 'GET', body, signal } = {}) {
  const headers = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  let response
  try {
    response = await fetch(path, {
      method, headers, signal, body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new ApiError("We couldn't reach the catalog. Check your connection and try again.")
  }
  if (!response.ok) throw await toApiError(response)
  return response.status === 204 ? null : response.json()
}

// BUILD FLOW 12.12.03 — Reading a Problem Details error (RFC 9457)
// The backend describes errors as application/problem+json: a type, a title, a detail sentence written to be safe for
// clients, and for invalid input an "errors" list. A 5xx tells a person nothing useful, and an error page from a proxy
// is not JSON at all, so both get a general sentence instead.
async function toApiError(response) {
  let problem = null
  try {
    problem = await response.json()
  } catch {
    problem = null
  }
  const type = typeof problem?.type === 'string' ? problem.type.split('/').pop() : null
  const fieldErrors = Array.isArray(problem?.errors) ? problem.errors : []
  const message = response.status < 500 && problem?.detail
    ? problem.detail
    : 'The catalog is not available right now. Please try again in a moment.'
  return new ApiError(message, { status: response.status, type, fieldErrors })
}

// BUILD FLOW 12.12.04 — Search
// A 400 here is about the search itself, so its field messages are shown with readable names instead of parameter
// names: "Maximum price: must be greater than or equal to minPrice."
const SEARCH_FIELDS = {
  q: 'Search', category: 'Category', minPrice: 'Minimum price', maxPrice: 'Maximum price', inStock: 'Availability',
  sort: 'Sort order', page: 'Page', size: 'Page size',
}

export async function searchProducts(query, { signal } = {}) {
  try {
    return await request(`/api/v1/products?${query}`, { signal })
  } catch (error) {
    if (error.status === 400 && error.fieldErrors.length) {
      error.message = error.fieldErrors
        .map((problem) => `${SEARCH_FIELDS[problem.field] ?? problem.field}: ${problem.message}.`).join(' ')
    }
    throw error
  }
}

// BUILD FLOW 12.12.05 — One product, writes, and settings (added in Phase 13)
export function getProduct(id, { signal } = {}) {
  return request(`/api/v1/products/${id}`, { signal })
}

export function createProduct(product) {
  return request('/api/v1/products', { method: 'POST', body: product })
}

export function updateProduct(id, product) {
  return request(`/api/v1/products/${id}`, { method: 'PUT', body: product })
}

export function deleteProduct(id) {
  return request(`/api/v1/products/${id}`, { method: 'DELETE' })
}

export function getSettings({ signal } = {}) {
  return request('/api/v1/settings', { signal })
}
