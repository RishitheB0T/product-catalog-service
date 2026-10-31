// BUILD FLOW 13.17 — frontend/src/test/fakeApi.js
// First introduced: Phase 13
// Purpose: A fake backend for the page tests: replaces fetch, records every call (method, path, query, body), and
//          answers with objects shaped exactly like the API's ProductResponse, PageResponse, and Problem Details.
// Depends on: Vitest (vi), the Fetch API's Response class.
// Used by: App.test.jsx (12.32), ProductPage.test.jsx (13.18), AdminPages.test.jsx (13.19).
// Runtime role: Test only.

import { vi } from 'vitest'

// BUILD FLOW 13.17.01 — Building answers
export function product(id, name, changes = {}) {
  return {
    id, sku: `FRM-TST-${id}`, name, description: 'A test product.', category: 'HOME', price: 1290, stockQuantity: 5,
    inStock: true, imageUrl: null, createdAt: '2026-10-30T00:00:00Z', updatedAt: '2026-10-30T00:00:00Z', version: 0,
    ...changes,
  }
}

export function page(content, { number = 0, size = 12, total = content.length } = {}) {
  return { content, page: number, size, totalElements: total, totalPages: Math.ceil(total / size) }
}

export function json(body, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

export function problem(status, type, detail, errors) {
  const body = { type: `https://catalog.example/problems/${type}`, title: type, status, detail }
  return json(errors ? { ...body, errors } : body, status)
}

// BUILD FLOW 13.17.02 — The fake backend
// handle(call) answers a call, or returns nothing to fall back to the defaults: the settings
// ({ writesEnabled }), 204 for a DELETE, and 404 for anything else. It may also return a promise that never settles,
// to keep a request pending. searches() lists only the catalog searches, ignoring the settings request.
export function fakeBackend({ writesEnabled = false, handle } = {}) {
  const calls = []
  const fetchMock = vi.fn(async (url, options = {}) => {
    const address = new URL(url, 'http://localhost')
    const call = {
      url, method: options.method ?? 'GET', path: address.pathname, query: address.searchParams,
      body: options.body ? JSON.parse(options.body) : undefined, signal: options.signal,
    }
    calls.push(call)
    const answer = await handle?.(call)
    if (answer) return answer
    if (call.path === '/api/v1/settings') return json({ writesEnabled })
    if (call.method === 'DELETE') return new Response(null, { status: 204 })
    return problem(404, 'product-not-found', 'No such product')
  })
  vi.stubGlobal('fetch', fetchMock)
  return {
    calls,
    searches: () => calls.filter((call) => call.path === '/api/v1/products' && call.method === 'GET'),
  }
}
