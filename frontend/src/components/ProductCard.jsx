// BUILD FLOW 12.25 — frontend/src/components/ProductCard.jsx
// First introduced: Phase 12
// Purpose: One product in the grid: its photo (or a placeholder), category, name, price in rupees, and a "Sold out"
//          mark when it is out of stock.
// Depends on: categoryLabel and formatPrice (12.10); since Phase 13 Link (13.03) and paths (13.02); styles in app.css
//             (12.09, section 12.09.12).
// Used by: ProductGrid (12.24).
// Runtime role: Renders one ProductResponse from the API.

import { useState } from 'react'
import { categoryLabel, formatPrice, monogram } from '../lib/catalog.js'
import { paths } from '../lib/routes.js'
import Link from './Link.jsx'

export default function ProductCard({ product, from }) {
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
      {/* BUILD FLOW 12.25.03 — One link per card (added in Phase 13)
          Only the name is a link, so screen readers hear one short link instead of the whole card's text. CSS
          stretches its click area over the entire card (.card__link::after), and the card shows the focus ring. */}
      <div className="card__body">
        <p className="card__category">{categoryLabel(product.category)}</p>
        <h3 className="card__name">
          <Link className="card__link" to={paths.product(product.id)} state={{ from }}>{product.name}</Link>
        </h3>
        <p className="card__price">{formatPrice(product.price)}</p>
      </div>
    </article>
  )
}
