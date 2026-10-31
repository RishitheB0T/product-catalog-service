// BUILD FLOW 13.18 — frontend/src/pages/ProductPage.test.jsx
// First introduced: Phase 13
// Purpose: Behaviour tests of the product page through the whole app: the facts shown, a missing product, the way
//          from a card to the page and back to the same results, and the edit link only when writes are on.
// Depends on: App (12.29), ProductPage (13.08), fakeApi.js (13.17), Testing Library, Vitest.
// Used by: npm test.
// Runtime role: Test only.

import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import App from '../App.jsx'
import { fakeBackend, json, page, product } from '../test/fakeApi.js'

const VASE = product(42, 'Ceramic Bud Vase', {
  sku: 'FRM-HOM-002', description: 'Hand-thrown stoneware.', price: 1490, stockQuantity: 25,
  imageUrl: '/images/products/frm-hom-002.jpg',
})

function answers({ path }) {
  if (path === '/api/v1/products/42') return json(VASE)
  if (path === '/api/v1/products') return json(page([VASE]))
  return undefined
}

describe('the product page', () => {
  // BUILD FLOW 13.18.01 — The facts
  it('shows the photo, name, price, stock, SKU, and description, and names the browser tab', async () => {
    window.history.replaceState(null, '', '/products/42')
    fakeBackend({ handle: answers })
    render(<App />)

    expect(await screen.findByRole('heading', { level: 1, name: 'Ceramic Bud Vase' })).toBeInTheDocument()
    expect(screen.getByRole('img', { name: 'Ceramic Bud Vase' })).toHaveAttribute('src', '/images/products/frm-hom-002.jpg')
    expect(screen.getByText('₹1,490')).toBeInTheDocument()
    expect(screen.getByText('In stock: 25 available')).toBeInTheDocument()
    expect(screen.getByText('FRM-HOM-002')).toBeInTheDocument()
    expect(screen.getByText('Hand-thrown stoneware.')).toBeInTheDocument()
    // The title is set by an effect, which React runs just after the screen updates, so the test waits for it.
    await waitFor(() => expect(document.title).toBe('Ceramic Bud Vase — FORM'))
    expect(screen.queryByRole('link', { name: 'Edit this product' })).not.toBeInTheDocument()
  })

  it('says so when the product does not exist', async () => {
    window.history.replaceState(null, '', '/products/99')
    fakeBackend()
    render(<App />)

    expect(await screen.findByRole('heading', { name: "This product doesn't exist." })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Browse all products' })).toHaveAttribute('href', '/')
  })

  // BUILD FLOW 13.18.02 — From the results and back
  it('opens from a card and returns to the same results', async () => {
    window.history.replaceState(null, '', '/?q=vase')
    fakeBackend({ handle: answers })
    const user = userEvent.setup()
    render(<App />)

    await user.click(await screen.findByRole('link', { name: 'Ceramic Bud Vase' }))
    expect(window.location.pathname).toBe('/products/42')
    expect(await screen.findByRole('heading', { level: 1, name: 'Ceramic Bud Vase' })).toBeInTheDocument()

    await user.click(screen.getByRole('link', { name: '← Back to results' }))
    await waitFor(() => expect(window.location.search).toBe('?q=vase'))
    expect(await screen.findByRole('searchbox', { name: 'Search products' })).toHaveValue('vase')
  })

  it('offers an edit link when the server allows changes', async () => {
    window.history.replaceState(null, '', '/products/42')
    fakeBackend({ writesEnabled: true, handle: answers })
    render(<App />)

    expect(await screen.findByRole('link', { name: 'Edit this product' })).toHaveAttribute('href', '/admin/products/42/edit')
  })
})
