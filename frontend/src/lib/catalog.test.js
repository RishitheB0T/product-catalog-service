// BUILD FLOW 12.31 — frontend/src/lib/catalog.test.js
// First introduced: Phase 12
// Purpose: Tests the pure catalog rules: reading and writing the address, the API query, price checks, page
//          numbers, and rupee formatting.
// Depends on: catalog.js (12.10), Vitest.
// Used by: npm test.
// Runtime role: Test only.

import { describe, expect, it } from 'vitest'
import {
  DEFAULT_STATE, formatCount, formatPrice, pageWindow, priceRangeProblem, readSearchState, toApiQuery,
  writeSearchState,
} from './catalog.js'

// BUILD FLOW 12.31.01 — The address bar
describe('the search state in the address bar', () => {
  it('reads an empty address as the defaults', () => {
    expect(readSearchState('')).toEqual(DEFAULT_STATE)
  })

  it('reads every filter, and writes it back unchanged', () => {
    const search = '?q=linen&category=HOME&minPrice=1000&maxPrice=2499.50&inStock=true&sort=price_asc&page=3'
    const state = readSearchState(search)
    expect(state).toEqual({
      q: 'linen', category: 'HOME', minPrice: '1000', maxPrice: '2499.50', inStock: true, sort: 'price_asc', page: 3,
    })
    expect(writeSearchState(state)).toBe(search)
  })

  it('replaces values the API would reject with the defaults', () => {
    const state = readSearchState('?category=GARDEN&sort=cheapest&minPrice=-5&maxPrice=abc&inStock=yes&page=0')
    expect(state).toEqual(DEFAULT_STATE)
    expect(readSearchState('?page=2.5').page).toBe(1)
    expect(readSearchState('?page=10002').page).toBe(1)
  })

  it('leaves defaults out of the address, so the plain storefront is just "/"', () => {
    expect(writeSearchState(DEFAULT_STATE)).toBe('')
    expect(writeSearchState({ ...DEFAULT_STATE, sort: 'name_asc' })).toBe('?sort=name_asc')
  })
})

// BUILD FLOW 12.31.02 — The API query
describe('the API query', () => {
  it('counts pages from zero and always sends the sort, page, and size', () => {
    expect(toApiQuery({ ...DEFAULT_STATE, page: 2 })).toBe('sort=newest&page=1&size=12')
    expect(toApiQuery({ ...DEFAULT_STATE, q: 'linen shirt', inStock: true }))
      .toBe('q=linen+shirt&inStock=true&sort=newest&page=0&size=12')
  })
})

// BUILD FLOW 12.31.03 — Prices, pages, and numbers
describe('price ranges', () => {
  it('accepts empty, whole, and two-decimal prices in order', () => {
    expect(priceRangeProblem('', '')).toBe('')
    expect(priceRangeProblem('1000', '1499.50')).toBe('')
  })

  it('explains malformed prices and a reversed range', () => {
    expect(priceRangeProblem('12,000', '')).toMatch(/whole rupees/)
    expect(priceRangeProblem('', '1.999')).toMatch(/whole rupees/)
    expect(priceRangeProblem('2000', '1000')).toBe('The minimum price is higher than the maximum.')
  })
})

describe('page numbers', () => {
  it('shows the first, the last, and the neighbours of the current page', () => {
    expect(pageWindow(1, 1)).toEqual([1])
    expect(pageWindow(1, 4)).toEqual([1, 2, 3, 4])
    expect(pageWindow(5, 20)).toEqual([1, '…', 4, 5, 6, '…', 20])
    expect(pageWindow(20, 20)).toEqual([1, '…', 19, 20])
  })
})

describe('rupees and counts', () => {
  it('groups digits the Indian way and shows paise only when there are any', () => {
    expect(formatPrice(1290)).toBe('₹1,290')
    expect(formatPrice('123456.5')).toBe('₹1,23,456.50')
    expect(formatCount(100000)).toBe('1,00,000')
  })
})
