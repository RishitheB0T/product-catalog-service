// BUILD FLOW 13.19 — frontend/src/pages/AdminPages.test.jsx
// First introduced: Phase 13
// Purpose: Behaviour tests of the local admin through the whole app: read-only servers, the list, adding a product
//          (checked in the browser first, then by the server), editing with a version conflict, and deleting after
//          a confirmation.
// Depends on: App (12.29), AdminPage (13.09), ProductFormPage (13.10), fakeApi.js (13.17), Testing Library, Vitest.
// Used by: npm test.
// Runtime role: Test only.

import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import App from '../App.jsx'
import { fakeBackend, json, page, problem, product } from '../test/fakeApi.js'

const VASE = product(42, 'Ceramic Bud Vase', { sku: 'FRM-HOM-002', price: 1490, stockQuantity: 25, version: 3 })
const LIST = page([VASE, product(43, 'Oak Table Lamp', { sku: 'FRM-HOM-003' })])

const at = (path) => window.history.replaceState(null, '', path)

async function fillNewProduct(user) {
  await user.type(screen.getByLabelText('Name'), 'Linen Apron')
  await user.type(screen.getByLabelText('SKU'), 'frm-kit-005')
  await user.selectOptions(screen.getByLabelText('Category'), 'KITCHEN')
  await user.type(screen.getByLabelText('Price (₹)'), '1490.50')
  await user.type(screen.getByLabelText('Stock quantity'), '12')
  await user.type(screen.getByLabelText('Description'), 'A heavy linen apron.')
}

describe('the admin', () => {
  // BUILD FLOW 13.19.01 — Read-only servers
  it('stays hidden, and explains itself, when the server allows no changes', async () => {
    at('/admin')
    fakeBackend({ writesEnabled: false })
    render(<App />)

    expect(await screen.findByRole('heading', { name: 'Changes are turned off on this server.' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Admin' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Add a product' })).not.toBeInTheDocument()
  })

  // BUILD FLOW 13.19.02 — The list
  it('lists products with an edit link and a delete button for each', async () => {
    at('/admin')
    fakeBackend({ writesEnabled: true, handle: ({ path }) => (path === '/api/v1/products' ? json(LIST) : undefined) })
    render(<App />)

    const table = await screen.findByRole('table')
    expect(within(table).getByRole('rowheader', { name: 'Ceramic Bud Vase' })).toBeInTheDocument()
    expect(within(table).getByRole('link', { name: 'Edit Ceramic Bud Vase' })).toHaveAttribute('href', '/admin/products/42/edit')
    expect(within(table).getByRole('button', { name: 'Delete Oak Table Lamp' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Admin' })).toHaveAttribute('href', '/admin')
  })

  // BUILD FLOW 13.19.03 — Adding a product
  it('lists every problem before sending anything, and moves focus to that list', async () => {
    at('/admin/products/new')
    const api = fakeBackend({ writesEnabled: true })
    const user = userEvent.setup()
    render(<App />)

    await user.click(await screen.findByRole('button', { name: 'Add product' }))

    const summary = screen.getByRole('heading', { name: 'Please correct these 6 things:' }).parentElement
    expect(summary).toHaveFocus()
    expect(within(summary).getAllByRole('link')).toHaveLength(6)
    expect(screen.getByLabelText('Name')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText('Name')).toHaveAccessibleDescription('Enter a name.')
    expect(api.calls.filter((call) => call.method === 'POST')).toHaveLength(0)
  })

  it('sends a valid product, then returns to the list with a message', async () => {
    at('/admin/products/new')
    const api = fakeBackend({
      writesEnabled: true,
      handle: ({ method, path, body }) => {
        if (method === 'POST') return json({ ...product(44, body.name), ...body }, 201)
        if (path === '/api/v1/products') return json(LIST)
        return undefined
      },
    })
    const user = userEvent.setup()
    render(<App />)
    await screen.findByRole('heading', { name: 'Add a product' })

    await fillNewProduct(user)
    await user.click(screen.getByRole('button', { name: 'Add product' }))

    expect(await screen.findByText('Saved “Linen Apron”.')).toBeInTheDocument()
    expect(window.location.pathname).toBe('/admin')
    expect(api.calls.find((call) => call.method === 'POST').body).toEqual({
      sku: 'FRM-KIT-005', name: 'Linen Apron', description: 'A heavy linen apron.', category: 'KITCHEN',
      price: 1490.5, stockQuantity: 12, imageUrl: null,
    })
  })

  it('shows the server\'s answer next to the field it is about', async () => {
    at('/admin/products/new')
    fakeBackend({
      writesEnabled: true,
      handle: ({ method }) => (method === 'POST'
        ? problem(409, 'duplicate-sku', "A product with SKU 'FRM-KIT-005' already exists")
        : undefined),
    })
    const user = userEvent.setup()
    render(<App />)
    await screen.findByRole('heading', { name: 'Add a product' })

    await fillNewProduct(user)
    await user.click(screen.getByRole('button', { name: 'Add product' }))

    expect(await screen.findByRole('heading', { name: 'Please correct this:' })).toBeInTheDocument()
    expect(screen.getByLabelText('SKU'))
      .toHaveAccessibleDescription(expect.stringContaining("A product with SKU 'FRM-KIT-005' already exists. Choose another SKU."))
    expect(window.location.pathname).toBe('/admin/products/new')
  })

  // BUILD FLOW 13.19.04 — Editing, and losing a race
  it('sends the version it loaded, and offers the latest version after a conflict', async () => {
    at('/admin/products/42/edit')
    let current = VASE
    const api = fakeBackend({
      writesEnabled: true,
      handle: ({ method, path }) => {
        if (method === 'PUT') {
          // Someone else saved first: the stored product moved on to version 4.
          current = { ...VASE, price: 1590, version: 4 }
          return problem(409, 'version-conflict', 'Product 42 was changed by someone else')
        }
        if (path === '/api/v1/products/42') return json(current)
        return undefined
      },
    })
    const user = userEvent.setup()
    render(<App />)

    const price = await screen.findByLabelText('Price (₹)')
    expect(price).toHaveValue('1490')
    await user.clear(price)
    await user.type(price, '1550')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Someone else changed this product after you opened it')
    expect(api.calls.find((call) => call.method === 'PUT').body).toMatchObject({ price: 1550, version: 3 })

    await user.click(screen.getByRole('button', { name: 'Load the latest version' }))
    await waitFor(() => expect(screen.getByLabelText('Price (₹)')).toHaveValue('1590'))
  })

  // BUILD FLOW 13.19.05 — Deleting, after a question
  it('asks before deleting, deletes, and says so', async () => {
    at('/admin')
    let deleted = false
    const api = fakeBackend({
      writesEnabled: true,
      handle: ({ method, path }) => {
        if (method === 'DELETE') {
          deleted = true
          return undefined
        }
        if (path === '/api/v1/products') return json(deleted ? page([LIST.content[1]]) : LIST)
        return undefined
      },
    })
    const user = userEvent.setup()
    render(<App />)

    await user.click(await screen.findByRole('button', { name: 'Delete Ceramic Bud Vase' }))
    const dialog = screen.getByRole('dialog', { name: 'Delete “Ceramic Bud Vase”?' })
    expect(dialog).toHaveAccessibleDescription('It disappears from the shop at once. This cannot be undone.')

    await user.click(within(dialog).getByRole('button', { name: 'Delete product' }))

    expect(await screen.findByText('Deleted “Ceramic Bud Vase”.')).toHaveFocus()
    expect(api.calls.find((call) => call.method === 'DELETE').path).toBe('/api/v1/products/42')
    await waitFor(() => expect(screen.queryByRole('rowheader', { name: 'Ceramic Bud Vase' })).not.toBeInTheDocument())
  })
})
