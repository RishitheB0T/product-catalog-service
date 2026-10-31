// BUILD FLOW 13.05 — frontend/src/hooks/useProduct.js
// First introduced: Phase 13
// Purpose: Loads one product by id, cancels the request if the id changes or the page closes, and reports loading,
//          the product, or an error (a 404 included), with a way to try again.
// Depends on: getProduct (12.12).
// Used by: ProductPage (13.08) and ProductFormPage (13.10, when editing).
// Runtime role: One GET /api/v1/products/{id} per id (runtime steps READ-01 onwards).

import { useEffect, useState } from 'react'
import { getProduct } from '../api/products.js'

// BUILD FLOW 13.05.01 — The same pattern as useProductSearch (12.15)
// The result remembers which request it answers, so "loading" is derived instead of stored, and the effect's cleanup
// aborts a request whose answer is no longer wanted.
export function useProduct(id) {
  const [attempt, setAttempt] = useState(0)
  const [result, setResult] = useState({ key: null, product: null, error: null })
  const key = `${id}#${attempt}`

  useEffect(() => {
    const controller = new AbortController()
    getProduct(id, { signal: controller.signal })
      .then((product) => setResult({ key, product, error: null }))
      .catch((error) => {
        if (error.name !== 'AbortError') setResult({ key, product: null, error })
      })
    return () => controller.abort()
  }, [id, key])

  const loading = result.key !== key
  return {
    loading,
    product: loading ? null : result.product,
    error: loading ? null : result.error,
    reload: () => setAttempt((n) => n + 1),
  }
}
