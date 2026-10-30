// BUILD FLOW 12.28 — frontend/src/components/SiteFooter.jsx
// First introduced: Phase 12
// Purpose: The page footer: what this shop is (a demonstration; nothing is for sale), and the photo credits that the
//          photos' licenses require, in a section that opens on request.
// Depends on: PHOTO_CREDITS (12.11); styles in app.css (12.09, section 12.09.16).
// Used by: App (12.29).
// Runtime role: Static content.

import { PHOTO_CREDITS } from '../lib/photoCredits.js'

// BUILD FLOW 12.28.01 — Credits that are easy to find, and out of the way
// <details> and <summary> make a section that opens and closes without any JavaScript, and that keyboards and screen
// readers already understand. Every credit names the photo, its author, its source, its license, and what changed.
export default function SiteFooter() {
  return (
    <footer className="site-footer">
      <div className="site-footer__inner">
        <p className="site-footer__brand">FORM — The Everyday Edit</p>
        <p className="site-footer__note">
          A demonstration storefront: the products and prices are examples, and nothing here is for sale.
        </p>
        <details className="credits">
          <summary className="credits__summary">Photo credits</summary>
          <p className="credits__intro">
            Photos from Wikimedia Commons. Products without a photo show a placeholder.
          </p>
          <ul className="credits__list">
            {PHOTO_CREDITS.map((credit) => (
              <li key={credit.source}>
                {credit.product}: <a href={credit.source}>“{credit.title}”</a> by {credit.author},{' '}
                {credit.licenseUrl ? <a href={credit.licenseUrl}>{credit.license}</a> : credit.license}, {credit.changes}.
              </li>
            ))}
          </ul>
        </details>
      </div>
    </footer>
  )
}
