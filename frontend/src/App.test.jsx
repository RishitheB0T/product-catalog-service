// BUILD FLOW 12.32 — frontend/src/App.test.jsx
// First introduced: Phase 12
// Purpose: Behaviour tests of the catalog page against a fake API: what a shopper sees, what is requested, and what
//          the address bar holds after searching, filtering, failing, and clearing.
// Depends on: App (12.29) and everything it renders; fakeApi.js (13.17, since Phase 13); Testing Library; Vitest.
// Used by: npm test.
// Runtime role: Test only. No backend is needed: fetch is replaced in every test.

import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import App from './App.jsx'
import { fakeBackend, json, page, problem, product } from './test/fakeApi.js'

// BUILD FLOW 12.32.01 — Shared answers
// Since Phase 13 the fake backend lives in test/fakeApi.js; it also answers the settings request the app now sends,
// and searches() leaves that request out.
const TWO_PRODUCTS = page([
  product(1, 'Ceramic Bud Vase'),
  product(2, 'Oak Table Lamp', { price: 5990, stockQuantity: 0, inStock: false }),
])

const answerSearches = (body) => ({ path }) => (path === '/api/v1/products' ? json(body) : undefined)
const lastQuery = (api) => api.searches().at(-1).query.toString()
const searchBox = () => screen.getByRole('searchbox', { name: 'Search products' })

describe('the catalog page', () => {
  // BUILD FLOW 12.32.02 — Showing results
  it('lists the products and the count from the API', async () => {
    const api = fakeBackend({ handle: answerSearches(TWO_PRODUCTS) })
    render(<App />)

    expect(await screen.findByRole('heading', { name: 'Ceramic Bud Vase' })).toBeInTheDocument()
    expect(screen.getByText('Showing 1–2 of 2 products')).toBeInTheDocument()
    expect(screen.getByText('₹5,990')).toBeInTheDocument()
    expect(screen.getByText('Sold out')).toBeInTheDocument()
    expect(api.searches()).toHaveLength(1)
    expect(api.searches()[0].url).toBe('/api/v1/products?sort=newest&page=0&size=12')
  })

  it('restores a shared search from the address bar', async () => {
    window.history.replaceState(null, '', '/?q=vase&category=HOME&sort=price_asc&page=2')
    const api = fakeBackend({ handle: answerSearches(page([product(13, 'Ceramic Bud Vase')], { number: 1, total: 13 })) })
    render(<App />)

    expect(await screen.findByText('Showing 13–13 of 13 products')).toBeInTheDocument()
    expect(api.searches()[0].url).toBe('/api/v1/products?q=vase&category=HOME&sort=price_asc&page=1&size=12')
    expect(searchBox()).toHaveValue('vase')
    expect(screen.getByRole('button', { name: 'Home' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: 'Remove filter: “vase”' })).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: 'Sort by' })).toHaveValue('price_asc')
    expect(screen.getByRole('button', { name: 'Page 2' })).toHaveAttribute('aria-current', 'page')
  })

  // BUILD FLOW 12.32.03 — Searching and filtering
  it('searches once the typing pauses, and keeps the search in the address', async () => {
    const api = fakeBackend({ handle: answerSearches(TWO_PRODUCTS) })
    const user = userEvent.setup()
    render(<App />)
    await screen.findByText('Showing 1–2 of 2 products')

    await user.type(searchBox(), 'linen')

    await waitFor(() => expect(lastQuery(api)).toBe('q=linen&sort=newest&page=0&size=12'))
    const searches = api.searches().map((call) => call.query.get('q')).filter(Boolean)
    expect(searches).toEqual(['linen'])
    expect(window.location.search).toBe('?q=linen')
  })

  it('cancels the previous request when the search changes', async () => {
    const api = fakeBackend({
      handle: ({ path, signal }) => {
        if (path !== '/api/v1/products') return undefined
        // The first search never answers on its own; it can only be aborted.
        if (api.searches().length === 1) {
          return new Promise((resolve, reject) => {
            signal.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError')))
          })
        }
        return json(page([product(3, 'Cast Iron Skillet', { category: 'KITCHEN' })]))
      },
    })
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: 'Kitchen' }))

    expect(await screen.findByRole('heading', { name: 'Cast Iron Skillet' })).toBeInTheDocument()
    expect(api.searches()[0].signal.aborted).toBe(true)
    expect(api.searches()[1].query.get('category')).toBe('KITCHEN')
  })

  it('goes back to page 1 when a filter changes', async () => {
    window.history.replaceState(null, '', '/?page=3')
    const api = fakeBackend({ handle: answerSearches(page([product(1, 'Ceramic Bud Vase')], { number: 2, total: 30 })) })
    const user = userEvent.setup()
    render(<App />)
    await screen.findByRole('heading', { name: 'Ceramic Bud Vase' })

    await user.click(screen.getByRole('checkbox', { name: 'In stock only' }))

    await waitFor(() => expect(lastQuery(api)).toBe('inStock=true&sort=newest&page=0&size=12'))
    expect(window.location.search).toBe('?inStock=true')
  })

  it('removes one filter with its chip, and all of them with "Clear all"', async () => {
    window.history.replaceState(null, '', '/?q=vase&category=HOME&inStock=true')
    const api = fakeBackend({ handle: answerSearches(TWO_PRODUCTS) })
    const user = userEvent.setup()
    render(<App />)
    await screen.findByRole('heading', { name: 'Ceramic Bud Vase' })

    await user.click(screen.getByRole('button', { name: 'Remove filter: Home' }))
    await waitFor(() => expect(window.location.search).toBe('?q=vase&inStock=true'))

    await user.click(screen.getByRole('button', { name: 'Clear all' }))
    await waitFor(() => expect(window.location.search).toBe(''))
    expect(searchBox()).toHaveValue('')
    await waitFor(() => expect(lastQuery(api)).toBe('sort=newest&page=0&size=12'))
  })

  // BUILD FLOW 12.32.04 — Failures
  it('explains an invalid search in plain words and can try again', async () => {
    window.history.replaceState(null, '', '/?minPrice=2000&maxPrice=1000')
    let failing = true
    const api = fakeBackend({
      handle: ({ path }) => {
        if (path !== '/api/v1/products') return undefined
        return failing
          ? problem(400, 'validation-failed', '1 field is invalid.',
            [{ field: 'maxPrice', message: 'must be greater than or equal to minPrice' }])
          : json(TWO_PRODUCTS)
      },
    })
    const user = userEvent.setup()
    render(<App />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Maximum price: must be greater than or equal to minPrice.')

    failing = false
    await user.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('heading', { name: 'Ceramic Bud Vase' })).toBeInTheDocument()
    expect(api.searches()).toHaveLength(2)
  })

  it('shows a placeholder when a photo fails to load', async () => {
    fakeBackend({ handle: answerSearches(page([product(1, 'Oak Table Lamp', { imageUrl: '/images/products/missing.jpg' })])) })
    const { container } = render(<App />)
    await screen.findByRole('heading', { name: 'Oak Table Lamp' })

    const photo = container.querySelector('.card__photo')
    expect(photo).toHaveAttribute('src', '/images/products/missing.jpg')
    fireEvent.error(photo)

    expect(container.querySelector('.card__photo')).toBeNull()
    expect(container.querySelector('.card__placeholder')).toHaveTextContent('OT')
  })
})
