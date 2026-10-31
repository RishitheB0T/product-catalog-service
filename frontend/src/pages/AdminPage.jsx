// BUILD FLOW 13.09 — frontend/src/pages/AdminPage.jsx
// First introduced: Phase 13
// Purpose: The local admin's product list at /admin: find a product, add one, edit one, or delete one after
//          confirming. Shows a message after each change, and explains when the server allows no changes at all.
// Depends on: useProductSearch (12.15), deleteProduct (12.12), ConfirmDialog (13.12), SearchBox (12.18), Pagination
//             (12.26), Link (13.03), paths (13.02); styles in app.css (12.09, section 12.09.21).
// Used by: App (12.29) for the path /admin.
// Runtime role: A search request per list page; a DELETE per confirmed deletion (runtime steps WRITE-01 onwards).

import { useEffect, useRef, useState } from 'react'
import { deleteProduct } from '../api/products.js'
import ConfirmDialog from '../components/ConfirmDialog.jsx'
import Link from '../components/Link.jsx'
import Pagination from '../components/Pagination.jsx'
import SearchBox from '../components/SearchBox.jsx'
import { usePageTitle } from '../hooks/usePageTitle.js'
import { forgetRecentSearches, useProductSearch } from '../hooks/useProductSearch.js'
import { categoryLabel, formatCount, formatPrice, toApiQuery } from '../lib/catalog.js'
import { paths } from '../lib/routes.js'

// BUILD FLOW 13.09.01 — When the server allows no changes
// The admin link is hidden then (App), but the address can still be typed. The page says plainly why nothing can be
// changed; the server would refuse every write anyway (403 writes-disabled).
export function ReadOnlyNotice() {
  return (
    <div className="status">
      <h1 className="status__title">Changes are turned off on this server.</h1>
      <p className="status__text">Products can be browsed, but not added, edited, or deleted here.</p>
      <Link className="button button--primary" to={paths.catalog}>Go to the shop</Link>
    </div>
  )
}

export default function AdminPage({ search, update, writesEnabled }) {
  usePageTitle('Manage products')
  const { loading, data, error, retry } = useProductSearch(toApiQuery(search))

  // BUILD FLOW 13.09.02 — A message after each change
  // A saved form arrives here with its message in the history entry's state (navigate(..., { state })). It is shown
  // once and then removed from the entry, so a reload doesn't repeat it. <output> has the "status" role, so screen
  // readers announce it politely. After a deletion the message also receives focus, because the button that had
  // focus is gone.
  const [flash, setFlash] = useState(() => {
    const text = window.history.state?.flash
    return text ? { text, focus: false } : null
  })
  const flashRef = useRef(null)
  useEffect(() => {
    if (window.history.state?.flash) window.history.replaceState({ ...window.history.state, flash: null }, '')
  }, [])
  useEffect(() => {
    if (flash?.focus) flashRef.current?.focus()
  }, [flash])

  // BUILD FLOW 13.09.03 — Deleting, after a question
  const [target, setTarget] = useState(null)
  const [deleting, setDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState(null)

  const closeDialog = () => {
    setTarget(null)
    setDeleteError(null)
  }

  const confirmDelete = async () => {
    setDeleting(true)
    setDeleteError(null)
    try {
      await deleteProduct(target.id)
      setFlash({ text: `Deleted “${target.name}”.`, focus: true })
    } catch (failure) {
      // Already gone (404) is the outcome that was asked for; anything else is explained in the dialog.
      if (failure.status !== 404) {
        setDeleteError(failure.type === 'writes-disabled' ? 'Changes are turned off on this server.' : failure.message)
        setDeleting(false)
        return
      }
      setFlash({ text: `“${target.name}” had already been deleted.`, focus: true })
    }
    setDeleting(false)
    setTarget(null)
    forgetRecentSearches()
    retry()
  }

  if (!writesEnabled) return <ReadOnlyNotice />

  return (
    <div className="admin">
      <div className="admin__header">
        <h1 className="admin__title">Manage products</h1>
        <Link className="button button--primary" to={paths.newProduct}>Add a product</Link>
      </div>

      {flash && <output ref={flashRef} className="flash" tabIndex={-1}>{flash.text}</output>}

      <SearchBox
        id="admin-search"
        label="Find products"
        value={search.q}
        onSearch={(q, options) => update({ q }, options)}
      />

      {error && (
        <div className="status status--error" role="alert">
          <p className="status__text">{error.message}</p>
          <button type="button" className="button button--primary" onClick={retry}>Try again</button>
        </div>
      )}
      {!error && !data && <output className="admin__note">Loading products…</output>}
      {!error && data && data.content.length === 0 && <p className="admin__note">No products match.</p>}

      {/* BUILD FLOW 13.09.04 — A table that scrolls on its own
          A product list is tabular data, so it is a <table> with a caption and column headers. On a narrow screen the
          wrapper scrolls sideways instead of the page. Keyboard users reach every cell that matters through its link
          or button, and focusing one scrolls it into view. Each action names its product ("Edit Ceramic Bud Vase"),
          because a screen reader may list links out of context. */}
      {!error && data && data.content.length > 0 && (
        <>
          <section className="table-wrap" aria-labelledby="admin-table-caption">
            <table className="admin-table" aria-busy={loading}>
              <caption id="admin-table-caption" className="admin-table__caption">
                {formatCount(data.totalElements)} {data.totalElements === 1 ? 'product' : 'products'}
              </caption>
              <thead>
                <tr>
                  <th scope="col">Product</th>
                  <th scope="col">SKU</th>
                  <th scope="col">Category</th>
                  <th scope="col" className="admin-table__number">Price</th>
                  <th scope="col" className="admin-table__number">Stock</th>
                  <th scope="col"><span className="visually-hidden">Actions</span></th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((product) => (
                  <tr key={product.id}>
                    <th scope="row"><Link to={paths.product(product.id)}>{product.name}</Link></th>
                    <td className="admin-table__sku">{product.sku}</td>
                    <td>{categoryLabel(product.category)}</td>
                    <td className="admin-table__number">{formatPrice(product.price)}</td>
                    <td className="admin-table__number">{product.inStock ? formatCount(product.stockQuantity) : 'Sold out'}</td>
                    <td className="admin-table__actions">
                      <Link className="link-button" to={paths.editProduct(product.id)} aria-label={`Edit ${product.name}`}>Edit</Link>
                      <button
                        type="button"
                        className="link-button link-button--danger"
                        aria-label={`Delete ${product.name}`}
                        onClick={() => setTarget({ id: product.id, name: product.name })}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>
          <Pagination page={search.page} totalPages={data.totalPages} onPage={(page) => update({ page })} />
        </>
      )}

      <ConfirmDialog
        open={target !== null}
        title={target ? `Delete “${target.name}”?` : 'Delete?'}
        confirmLabel="Delete product"
        busyLabel="Deleting…"
        busy={deleting}
        error={deleteError}
        onConfirm={confirmDelete}
        onCancel={closeDialog}
      >
        <p>It disappears from the shop at once. This cannot be undone.</p>
      </ConfirmDialog>
    </div>
  )
}
