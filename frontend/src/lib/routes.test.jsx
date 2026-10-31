// BUILD FLOW 13.16 — frontend/src/lib/routes.test.jsx
// First introduced: Phase 13
// Purpose: Tests the router's pieces: which page each path shows, and which clicks Link turns into in-app navigation.
// Depends on: routes.js (13.02), Link (13.03), Testing Library, Vitest.
// Used by: npm test.
// Runtime role: Test only.

import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import Link from '../components/Link.jsx'
import { matchRoute, paths } from './routes.js'

// BUILD FLOW 13.16.01 — Paths to pages
describe('matchRoute', () => {
  it('finds every page, with or without a trailing slash', () => {
    expect(matchRoute('/')).toEqual({ page: 'catalog' })
    expect(matchRoute('/products/42')).toEqual({ page: 'product', id: 42 })
    expect(matchRoute('/admin')).toEqual({ page: 'admin' })
    expect(matchRoute('/admin/')).toEqual({ page: 'admin' })
    expect(matchRoute('/admin/products/new')).toEqual({ page: 'new-product' })
    expect(matchRoute('/admin/products/7/edit')).toEqual({ page: 'edit-product', id: 7 })
  })

  it('treats malformed ids and unknown paths as "not found"', () => {
    for (const path of ['/products/0', '/products/abc', '/products/-1', '/products/1234567890123456', '/shop', '/admin/products/7']) {
      expect(matchRoute(path)).toEqual({ page: 'not-found' })
    }
  })

  it('builds the same paths it recognises', () => {
    expect(matchRoute(paths.product(5))).toEqual({ page: 'product', id: 5 })
    expect(matchRoute(paths.editProduct(5))).toEqual({ page: 'edit-product', id: 5 })
  })
})

// BUILD FLOW 13.16.02 — Which clicks stay in the app
describe('Link', () => {
  it('turns a plain click into in-app navigation, carrying its state', () => {
    render(<Link to="/products/5" state={{ from: '/?q=vase' }}>Vase</Link>)
    const link = screen.getByRole('link', { name: 'Vase' })
    expect(link).toHaveAttribute('href', '/products/5')

    const notPrevented = fireEvent.click(link)

    expect(notPrevented).toBe(false)
    expect(window.location.pathname).toBe('/products/5')
    expect(window.history.state).toEqual({ from: '/?q=vase' })
  })

  it('leaves Ctrl, Cmd, Shift, and Alt clicks to the browser', () => {
    render(<Link to="/products/5">Vase</Link>)
    const link = screen.getByRole('link', { name: 'Vase' })
    // A listener on window runs after React's handler, so it sees whether Link took the click. It then stops the
    // browser's own navigation, which jsdom cannot perform anyway.
    const takenByTheApp = []
    const record = (event) => {
      takenByTheApp.push(event.defaultPrevented)
      event.preventDefault()
    }
    window.addEventListener('click', record)
    try {
      for (const modifier of ['ctrlKey', 'metaKey', 'shiftKey', 'altKey']) fireEvent.click(link, { [modifier]: true })
    } finally {
      window.removeEventListener('click', record)
    }
    expect(takenByTheApp).toEqual([false, false, false, false])
    expect(window.location.pathname).toBe('/')
  })
})
