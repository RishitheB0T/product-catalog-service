// BUILD FLOW 12.25 — frontend/src/components/ProductCard.jsx
// First introduced: Phase 12
// Purpose: One product in the grid: its photo (or a placeholder), category, name, price in rupees, and a "Sold out"
//          mark when it is out of stock.
// Depends on: categoryLabel and formatPrice (12.10); styles in app.css (12.09, section 12.09.12).
// Used by: ProductGrid (12.24).
// Runtime role: Renders one ProductResponse from the API.

import { useState } from 'react'
import { categoryLabel, formatPrice } from '../lib/catalog.js'

// The placeholder's monogram: the first letters of the first two words ("Oak Table Lamp" gives "OT").
function monogram(name) {
  return name.split(/\s+/).slice(0, 2).map((word) => word[0]).join('').toUpperCase()
}

export default function ProductCard({ product }) {
  // BUILD FLOW 12.25.01 — An image that may fail
  // Many products have no photo (imageUrl is null), and a photo can fail to load. Both show the same designed
  // placeholder, so the grid never shows a broken-image icon. onError is the browser telling React the image failed.
  const [imageFailed, setImageFailed] = useState(false)
  const showPhoto = Boolean(product.imageUrl) && !imageFailed

  // BUILD FLOW 12.25.02 — Photos on a mat
  // The photos have different shapes, so each one is shown whole, centred on the same tinted mat (object-fit: contain
  // in CSS) instead of being cropped. The image is decorative here (alt=""), because the product's name follows as a
  // heading; saying it twice would only make screen readers repeat themselves. loading="lazy" fetches photos only as
  // they come near the screen.
  return (
    <article className="card">
      <div className="card__media">
        {showPhoto ? (
          <img
            className="card__photo"
            src={product.imageUrl}
            alt=""
            loading="lazy"
            decoding="async"
            onError={() => setImageFailed(true)}
          />
        ) : (
          <div className="card__placeholder" aria-hidden="true">{monogram(product.name)}</div>
        )}
        {!product.inStock && <span className="card__badge">Sold out</span>}
      </div>
      <div className="card__body">
        <p className="card__category">{categoryLabel(product.category)}</p>
        <h3 className="card__name">{product.name}</h3>
        <p className="card__price">{formatPrice(product.price)}</p>
      </div>
    </article>
  )
}
