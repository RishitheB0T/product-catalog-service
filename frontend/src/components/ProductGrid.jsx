// BUILD FLOW 12.24 — frontend/src/components/ProductGrid.jsx
// First introduced: Phase 12
// Purpose: The responsive grid of product cards for one page of results.
// Depends on: ProductCard (12.25); styles in app.css (12.09, section 12.09.12).
// Used by: App (12.29).
// Runtime role: Renders the content array of the latest response.

import ProductCard from './ProductCard.jsx'

// BUILD FLOW 12.24.01 — A list, keyed by id
// The products are a list (<ul>), so screen readers announce how many there are. key={product.id} lets React match
// each card to the same product across searches, and give a new product a new card (with fresh image state).
export default function ProductGrid({ products }) {
  return (
    <ul className="product-grid">
      {products.map((product) => (
        <li key={product.id} className="product-grid__item">
          <ProductCard product={product} />
        </li>
      ))}
    </ul>
  )
}
