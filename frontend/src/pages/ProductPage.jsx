// BUILD FLOW 13.08 — frontend/src/pages/ProductPage.jsx
// First introduced: Phase 13
// Purpose: One product's page at /products/{id}: the photo (or placeholder), category, name, price, stock, SKU, and
//          description, with a way back to the results the shopper came from, and an edit link when writes are on.
// Depends on: useProduct (13.05), usePageTitle (13.06), catalog.js (12.10), Link (13.03), paths (13.02); styles in
//             app.css (12.09, section 12.09.20).
// Used by: App (12.29) for paths like /products/42.
// Runtime role: One GET /api/v1/products/{id}; the cache serves repeated reads (Phases 09–10).

import { useState } from 'react'
import Link from '../components/Link.jsx'
import { usePageTitle } from '../hooks/usePageTitle.js'
import { useProduct } from '../hooks/useProduct.js'
import { categoryLabel, formatCount, formatPrice, monogram } from '../lib/catalog.js'
import { paths } from '../lib/routes.js'

// BUILD FLOW 13.08.01 — The way back
// A card's link stored the results address in this history entry (Link's "state"). If it is there, Back is the best
// way to return: the browser goes to that entry, and its search and scroll position come back with it. Opened from a
// bookmark or a shared link, the page has no such entry, so it links to the catalog instead.
function BackLink() {
  const from = window.history.state?.from
  if (typeof from === 'string' && from.startsWith('/')) {
    return (
      <a
        className="back-link"
        href={from}
        onClick={(event) => {
          event.preventDefault()
          window.history.back()
        }}
      >
        ← Back to results
      </a>
    )
  }
  return <Link className="back-link" to={paths.catalog}>← All products</Link>
}

function Photo({ product }) {
  const [failed, setFailed] = useState(false)
  // Here the photo is the subject of the page, so it gets a text alternative (the product's name) instead of being
  // decorative. Screen readers already say "image", so the text doesn't repeat "photo of".
  if (product.imageUrl && !failed) {
    return <img className="product__photo" src={product.imageUrl} alt={product.name} onError={() => setFailed(true)} />
  }
  return <div className="product__placeholder" aria-hidden="true">{monogram(product.name)}</div>
}

export default function ProductPage({ id, writesEnabled }) {
  const { loading, product, error, reload } = useProduct(id)
  const notFound = error?.status === 404
  usePageTitle(product?.name ?? (notFound ? 'Product not found' : null))

  // BUILD FLOW 13.08.02 — Loading, missing, failed, found
  if (loading) {
    return (
      <div className="product-page">
        <BackLink />
        <output className="product-page__loading">Loading the product…</output>
      </div>
    )
  }
  if (notFound) {
    return (
      <div className="product-page status">
        <h1 className="status__title">This product doesn't exist.</h1>
        <p className="status__text">It may have been removed, or the address may be mistyped.</p>
        <Link className="button button--primary" to={paths.catalog}>Browse all products</Link>
      </div>
    )
  }
  if (error) {
    return (
      <div className="product-page status status--error" role="alert">
        <h1 className="status__title">The product could not be loaded.</h1>
        <p className="status__text">{error.message}</p>
        <button type="button" className="button button--primary" onClick={reload}>Try again</button>
      </div>
    )
  }

  // BUILD FLOW 13.08.03 — The product
  // The facts come straight from the API: no invented ratings, reviews, or urgency. Stock shows the real quantity.
  return (
    <div className="product-page">
      <BackLink />
      <article className="product">
        <div className="product__media">
          <Photo product={product} />
          {!product.inStock && <span className="card__badge">Sold out</span>}
        </div>
        <div className="product__info">
          <p className="product__category">{categoryLabel(product.category)}</p>
          <h1 className="product__name">{product.name}</h1>
          <p className="product__price">{formatPrice(product.price)}</p>
          <p className={product.inStock ? 'product__stock' : 'product__stock product__stock--out'}>
            {product.inStock ? `In stock: ${formatCount(product.stockQuantity)} available` : 'Sold out'}
          </p>
          <p className="product__description">{product.description}</p>
          <dl className="product__facts">
            <div>
              <dt>SKU</dt>
              <dd>{product.sku}</dd>
            </div>
            <div>
              <dt>Category</dt>
              <dd>{categoryLabel(product.category)}</dd>
            </div>
          </dl>
          {writesEnabled && (
            <Link className="button button--quiet" to={paths.editProduct(product.id)}>Edit this product</Link>
          )}
        </div>
      </article>
    </div>
  )
}
