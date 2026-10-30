// BUILD FLOW 12.32 — frontend/src/App.test.jsx
// First introduced: Phase 12
// Purpose: Behaviour tests of the catalog page against a fake API: what a shopper sees, what is requested, and what
//          the address bar holds after searching, filtering, failing, and clearing.
// Depends on: App (12.29) and everything it renders; Testing Library; Vitest.
// Used by: npm test.
// Runtime role: Test only. No backend is needed: fetch is replaced in every test.

import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import App from './App.jsx'

// BUILD FLOW 12.32.01 — A fake API
// fetch is replaced with a function that records each request and answers it, with objects shaped exactly like the
// API's ProductResponse and PageResponse.
function product(id, name, changes = {}) {
  return {
    id, sku: `FRM-TST-${id}`, name, description: 'A test product.', category: 'HOME', price: 1290, stockQuantity: 5,
    inStock: true, imageUrl: null, createdAt: '2026-10-30T00:00:00Z', updatedAt: '2026-10-30T00:00:00Z', version: 0,
    ...changes,
  }
}

function page(content, { number = 0, size = 12, total = content.length } = {}) {
  return { content, page: number, size, totalElements: total, totalPages: Math.ceil(total / size) }
}

function json(body, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

const TWO_PRODUCTS = page([
  product(1, 'Ceramic Bud Vase'),
  product(2, 'Oak Table Lamp', { price: 5990, stockQuantity: 0, inStock: false }),
])

function fakeApi(answer = () => json(TWO_PRODUCTS)) {
  const fetchMock = vi.fn((url, options) => Promise.resolve(answer(url, options)))
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

function requestedQueries(fetchMock) {
  return fetchMock.mock.calls.map(([url]) => new URL(url, 'http://localhost').searchParams)
}

function lastQuery(fetchMock) {
  return requestedQueries(fetchMock).at(-1).toString()
}

const searchBox = () => screen.getByRole('searchbox', { name: 'Search products' })

describe('the catalog page', () => {
  // BUILD FLOW 12.32.02 — Showing results
  it('lists the products and the count from the API', async () => {
    const fetchMock = fakeApi()
    render(<App />)

    expect(await screen.findByRole('heading', { name: 'Ceramic Bud Vase' })).toBeInTheDocument()
    expect(screen.getByText('Showing 1–2 of 2 products')).toBeInTheDocument()
    expect(screen.getByText('₹5,990')).toBeInTheDocument()
    expect(screen.getByText('Sold out')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(fetchMock.mock.calls[0][0]).toBe('/api/v1/products?sort=newest&page=0&size=12')
  })

  it('restores a shared search from the address bar', async () => {
    window.history.replaceState(null, '', '/?q=vase&category=HOME&sort=price_asc&page=2')
    const fetchMock = fakeApi(() => json(page([product(13, 'Ceramic Bud Vase')], { number: 1, total: 13 })))
    render(<App />)

    expect(await screen.findByText('Showing 13–13 of 13 products')).toBeInTheDocument()
    expect(fetchMock.mock.calls[0][0]).toBe('/api/v1/products?q=vase&category=HOME&sort=price_asc&page=1&size=12')
    expect(searchBox()).toHaveValue('vase')
    expect(screen.getByRole('button', { name: 'Home' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: 'Remove filter: “vase”' })).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: 'Sort by' })).toHaveValue('price_asc')
    expect(screen.getByRole('button', { name: 'Page 2' })).toHaveAttribute('aria-current', 'page')
  })

  // BUILD FLOW 12.32.03 — Searching and filtering
  it('searches once the typing pauses, and keeps the search in the address', async () => {
    const fetchMock = fakeApi()
    const user = userEvent.setup()
    render(<App />)
    await screen.findByText('Showing 1–2 of 2 products')

    await user.type(searchBox(), 'linen')

    await waitFor(() => expect(lastQuery(fetchMock)).toBe('q=linen&sort=newest&page=0&size=12'))
    const searches = requestedQueries(fetchMock).map((params) => params.get('q')).filter(Boolean)
    expect(searches).toEqual(['linen'])
    expect(window.location.search).toBe('?q=linen')
  })

  it('cancels the previous request when the search changes', async () => {
    const fetchMock = vi.fn((url, { signal }) => {
      if (fetchMock.mock.calls.length === 1) {
        // The first request never answers on its own; it can only be aborted.
        return new Promise((resolve, reject) => {
          signal.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError')))
        })
      }
      return Promise.resolve(json(page([product(3, 'Cast Iron Skillet', { category: 'KITCHEN' })])))
    })
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: 'Kitchen' }))

    expect(await screen.findByRole('heading', { name: 'Cast Iron Skillet' })).toBeInTheDocument()
    expect(fetchMock.mock.calls[0][1].signal.aborted).toBe(true)
    expect(requestedQueries(fetchMock)[1].get('category')).toBe('KITCHEN')
  })

  it('goes back to page 1 when a filter changes', async () => {
    window.history.replaceState(null, '', '/?page=3')
    const fetchMock = fakeApi(() => json(page([product(1, 'Ceramic Bud Vase')], { number: 2, total: 30 })))
    const user = userEvent.setup()
    render(<App />)
    await screen.findByRole('heading', { name: 'Ceramic Bud Vase' })

    await user.click(screen.getByRole('checkbox', { name: 'In stock only' }))

    await waitFor(() => expect(lastQuery(fetchMock)).toBe('inStock=true&sort=newest&page=0&size=12'))
    expect(window.location.search).toBe('?inStock=true')
  })

  it('removes one filter with its chip, and all of them with "Clear all"', async () => {
    window.history.replaceState(null, '', '/?q=vase&category=HOME&inStock=true')
    const fetchMock = fakeApi()
    const user = userEvent.setup()
    render(<App />)
    await screen.findByRole('heading', { name: 'Ceramic Bud Vase' })

    await user.click(screen.getByRole('button', { name: 'Remove filter: Home' }))
    await waitFor(() => expect(window.location.search).toBe('?q=vase&inStock=true'))

    await user.click(screen.getByRole('button', { name: 'Clear all' }))
    await waitFor(() => expect(window.location.search).toBe(''))
    expect(searchBox()).toHaveValue('')
    await waitFor(() => expect(lastQuery(fetchMock)).toBe('sort=newest&page=0&size=12'))
  })

  // BUILD FLOW 12.32.04 — Failures
  it('explains an invalid search in plain words and can try again', async () => {
    window.history.replaceState(null, '', '/?minPrice=2000&maxPrice=1000')
    let failing = true
    const fetchMock = fakeApi(() => (failing
      ? json({
        title: 'Validation failed', status: 400, detail: '1 field is invalid.',
        errors: [{ field: 'maxPrice', message: 'must be greater than or equal to minPrice' }],
      }, 400)
      : json(TWO_PRODUCTS)))
    const user = userEvent.setup()
    render(<App />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Maximum price: must be greater than or equal to minPrice.')

    failing = false
    await user.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('heading', { name: 'Ceramic Bud Vase' })).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('shows a placeholder when a photo fails to load', async () => {
    fakeApi(() => json(page([product(1, 'Oak Table Lamp', { imageUrl: '/images/products/missing.jpg' })])))
    const { container } = render(<App />)
    await screen.findByRole('heading', { name: 'Oak Table Lamp' })

    const photo = container.querySelector('.card__photo')
    expect(photo).toHaveAttribute('src', '/images/products/missing.jpg')
    fireEvent.error(photo)

    expect(container.querySelector('.card__photo')).toBeNull()
    expect(container.querySelector('.card__placeholder')).toHaveTextContent('OT')
  })
})
