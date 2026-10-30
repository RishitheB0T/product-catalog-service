// BUILD FLOW 12.17 — frontend/src/components/Hero.jsx
// First introduced: Phase 12
// Purpose: The compact editorial introduction above the catalog: what the shop is, in one heading and one sentence.
// Depends on: Styles in app.css (12.09, section 12.09.06).
// Used by: App (12.29).
// Runtime role: Static content.

// BUILD FLOW 12.17.01 — One h1 per page
// The heading is the page's main title for screen readers and search engines; every other heading is an h2 or h3
// below it. The text makes no claims the shop cannot back up: no ratings, no sales figures, no fake urgency.
export default function Hero() {
  return (
    <section className="hero" aria-labelledby="hero-title">
      <p className="hero__eyebrow">Everyday essentials</p>
      <h1 id="hero-title" className="hero__title">Considered things for everyday use.</h1>
      <p className="hero__text">
        Clothing, footwear, bags, and pieces for the home, kitchen, and desk, chosen to be used often and kept for years.
      </p>
    </section>
  )
}
