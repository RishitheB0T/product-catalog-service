// BUILD FLOW 12.07 — frontend/src/main.jsx
// First introduced: Phase 12
// Purpose: The JavaScript entry point: loads the styles and renders <App /> into the page's #root element.
// Depends on: App (12.29), styles/tokens.css (12.08), styles/app.css (12.09).
// Used by: index.html (12.05), through its <script type="module"> tag.
// Runtime role: Runs once when the page loads.

import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.jsx'
import './styles/tokens.css'
import './styles/app.css'

// BUILD FLOW 12.07.01 — Mounting React
// createRoot takes over one DOM element; from then on React owns everything inside it. StrictMode is a development
// aid: it runs components and effects twice to expose code that is not safe to repeat. One visible example is
// useProductSearch (12.15), whose first request is cancelled at once in development; production builds skip it.
createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
