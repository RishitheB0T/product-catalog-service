// BUILD FLOW 13.15 — frontend/src/lib/productForm.test.js
// First introduced: Phase 13
// Purpose: Tests the product form's rules: the checks that mirror the API, and the JSON body built from the form.
// Depends on: productForm.js (13.14), Vitest.
// Used by: npm test.
// Runtime role: Test only.

import { describe, expect, it } from 'vitest'
import { EMPTY_FORM, sentence, toFormValues, toRequestBody, validateProduct } from './productForm.js'

const VALID = {
  name: '  Linen Apron ', sku: 'FRM-KIT-005', category: 'KITCHEN', price: '1490.50', stockQuantity: '12',
  description: 'A heavy linen apron with deep pockets. ', imageUrl: '',
}

// BUILD FLOW 13.15.01 — The checks
describe('validateProduct', () => {
  it('accepts a complete product, with or without an image address', () => {
    expect(validateProduct(VALID)).toEqual({})
    expect(validateProduct({ ...VALID, imageUrl: '/images/products/frm-hom-002.jpg' })).toEqual({})
    expect(validateProduct({ ...VALID, imageUrl: 'https://images.example/apron.jpg' })).toEqual({})
  })

  it('asks for every required field', () => {
    expect(Object.keys(validateProduct(EMPTY_FORM))).toEqual(['name', 'sku', 'category', 'price', 'stockQuantity', 'description'])
    expect(validateProduct({ ...VALID, name: '   ' }).name).toBe('Enter a name.')
  })

  it('applies the same limits as the API', () => {
    const errors = validateProduct({
      ...VALID, name: 'x'.repeat(121), sku: 'FRM KIT', price: '12.345', stockQuantity: '-1', imageUrl: 'javascript:alert(1)',
    })
    expect(Object.keys(errors)).toEqual(['name', 'sku', 'price', 'stockQuantity', 'imageUrl'])
    expect(validateProduct({ ...VALID, price: '123456789' }).price).toMatch(/up to two decimals/)
    expect(validateProduct({ ...VALID, imageUrl: '/images/products/../secret.jpg' }).imageUrl).toBeDefined()
  })
})

// BUILD FLOW 13.15.02 — The body sent to the API
describe('the request body', () => {
  it('trims text, turns numbers into numbers, and an empty image address into null', () => {
    expect(toRequestBody(VALID)).toEqual({
      sku: 'FRM-KIT-005', name: 'Linen Apron', description: 'A heavy linen apron with deep pockets.', category: 'KITCHEN',
      price: 1490.5, stockQuantity: 12, imageUrl: null,
    })
  })

  it('adds the version for an update, and round-trips a product', () => {
    const product = { ...toRequestBody(VALID), price: 1490.5, imageUrl: null }
    expect(toRequestBody(VALID, 3).version).toBe(3)
    expect(toFormValues(product)).toEqual({ ...VALID, name: 'Linen Apron', description: 'A heavy linen apron with deep pockets.', price: '1490.5' })
  })

  it('turns the server\'s messages into sentences', () => {
    expect(sentence('must not be blank')).toBe('Must not be blank.')
    expect(sentence("A product with SKU 'X' already exists")).toBe("A product with SKU 'X' already exists.")
  })
})
