// BUILD FLOW 12.12 — frontend/src/api/products.js
// First introduced: Phase 12
// Purpose: The one place that talks to the backend: GET /api/v1/products with a query string. Every failure becomes
//          an ApiError whose message a shopper can read.
// Depends on: The product API (ProductController 05.11) and its Problem Details errors (ApiExceptionHandler 05.12);
//             in development the Vite proxy (12.03) forwards /api to the backend.
// Used by: useProductSearch (12.15).
// Runtime role: Runs in the browser for every search (runtime steps SEARCH-01 to SEARCH-06 then run on the server).

// BUILD FLOW 12.12.01 — One error type for every way a request can fail
// status 0 means no answer arrived at all. problem keeps the server's Problem Details body, when there was one.
export class ApiError extends Error {
  constructor(message, { status = 0, problem = null } = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

// BUILD FLOW 12.12.02 — The search request
// fetch() rejects only when no response arrives: the network is down, the server is unreachable, or the caller
// aborted. An HTTP error status still resolves, so response.ok is checked separately. An abort is passed on unchanged
// (its name is "AbortError"): the caller cancelled on purpose, so it is not an error to show.
export async function searchProducts(query, { signal } = {}) {
  let response
  try {
    response = await fetch(`/api/v1/products?${query}`, { signal, headers: { Accept: 'application/json' } })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new ApiError("We couldn't reach the catalog. Check your connection and try again.")
  }
  if (!response.ok) throw await toApiError(response)
  return response.json()
}

// BUILD FLOW 12.12.03 — Reading a Problem Details error (RFC 9457)
// The backend describes errors as application/problem+json: a title, a detail sentence, and for a 400 an "errors" list
// of {field, message}. A 400 is about the search itself, so its field messages are worth showing, with readable names
// instead of parameter names. A 5xx tells a shopper nothing useful, and an error page from a proxy is not JSON at all.
const FIELD_NAMES = {
  q: 'Search', category: 'Category', minPrice: 'Minimum price', maxPrice: 'Maximum price', inStock: 'Availability',
  sort: 'Sort order', page: 'Page', size: 'Page size',
}

async function toApiError(response) {
  let problem = null
  try {
    problem = await response.json()
  } catch {
    problem = null
  }
  if (response.status === 400 && problem) {
    const fields = Array.isArray(problem.errors) ? problem.errors : []
    const message = fields.length
      ? fields.map((error) => `${FIELD_NAMES[error.field] ?? error.field}: ${error.message}.`).join(' ')
      : (problem.detail ?? 'Some of the search settings are not valid.')
    return new ApiError(message, { status: 400, problem })
  }
  return new ApiError('The catalog is not available right now. Please try again in a moment.',
    { status: response.status, problem })
}
