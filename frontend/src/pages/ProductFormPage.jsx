// BUILD FLOW 13.10 — frontend/src/pages/ProductFormPage.jsx
// First introduced: Phase 13
// Purpose: The form to add a product (/admin/products/new) or edit one (/admin/products/{id}/edit): checks the input
//          before sending it, shows the server's own answer next to the right field, explains a conflicting edit or a
//          duplicate SKU, and returns to the list with a message once saved.
// Depends on: productForm.js (13.14), FormField (13.13), useProduct (13.05), createProduct and updateProduct (12.12),
//             navigate (12.13), forgetRecentSearches (12.15), ReadOnlyNotice (13.09); styles in app.css (12.09, 12.09.21).
// Used by: App (12.29).
// Runtime role: POST /api/v1/products or PUT /api/v1/products/{id} on submit (runtime steps WRITE-01 onwards).

import { useEffect, useRef, useState } from 'react'
import { createProduct, updateProduct } from '../api/products.js'
import FormField from '../components/FormField.jsx'
import Link from '../components/Link.jsx'
import { usePageTitle } from '../hooks/usePageTitle.js'
import { useProduct } from '../hooks/useProduct.js'
import { forgetRecentSearches } from '../hooks/useProductSearch.js'
import { navigate } from '../hooks/useUrlState.js'
import { CATEGORIES } from '../lib/catalog.js'
import { EMPTY_FORM, FIELDS, sentence, toFormValues, toRequestBody, validateProduct } from '../lib/productForm.js'
import { paths } from '../lib/routes.js'
import { ReadOnlyNotice } from './AdminPage.jsx'

const fieldId = (name) => `product-${name}`

function ProductForm({ productId, version, initial, onLoadLatest }) {
  const editing = productId !== undefined
  const [values, setValues] = useState(initial)
  const [errors, setErrors] = useState({})
  const [formError, setFormError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  // BUILD FLOW 13.10.01 — The error summary receives focus
  // After a failed submit, focus moves to a list of every problem, each a link to its field (the pattern of the UK
  // government's design system). Keyboard and screen-reader users learn at once that something needs fixing, and
  // where. "showSummary" counts failed submits, so every one of them moves focus again.
  const summaryRef = useRef(null)
  const [showSummary, setShowSummary] = useState(0)
  useEffect(() => {
    if (showSummary > 0) summaryRef.current?.focus()
  }, [showSummary])

  const change = (name) => (event) => {
    // SKUs are capital letters by rule, so typing is converted instead of rejected.
    const value = name === 'sku' ? event.target.value.toUpperCase() : event.target.value
    setValues((current) => ({ ...current, [name]: value }))
  }

  const fail = (fieldErrors) => {
    setErrors(fieldErrors)
    setShowSummary((n) => n + 1)
  }

  // BUILD FLOW 13.10.02 — Submitting, and every answer the server can give
  const submit = async (event) => {
    event.preventDefault()
    setFormError(null)
    const found = validateProduct(values)
    if (Object.keys(found).length > 0) {
      fail(found)
      return
    }
    setErrors({})
    setSubmitting(true)
    try {
      const body = toRequestBody(values, editing ? version : undefined)
      const saved = editing ? await updateProduct(productId, body) : await createProduct(body)
      forgetRecentSearches()
      navigate(paths.admin, { state: { flash: `Saved “${saved.name}”.` } })
    } catch (error) {
      setSubmitting(false)
      if (error.fieldErrors.length > 0) {
        fail(Object.fromEntries(error.fieldErrors.map((problem) => [problem.field, sentence(problem.message)])))
      } else if (error.type === 'duplicate-sku') {
        fail({ sku: `${sentence(error.message)} Choose another SKU.` })
      } else if (error.type === 'version-conflict') {
        setFormError({
          text: 'Someone else changed this product after you opened it, so your changes were not saved. Load the '
            + 'latest version to see theirs; what you typed here will be replaced.',
          conflict: true,
        })
      } else if (error.type === 'writes-disabled') {
        setFormError({ text: 'Changes are turned off on this server, so nothing was saved.' })
      } else if (error.status === 404) {
        setFormError({ text: 'This product no longer exists, so it could not be saved.' })
      } else {
        setFormError({ text: error.message })
      }
    }
  }

  const problems = Object.entries(errors).map(([name, message]) => ({
    name, message, label: FIELDS.find((field) => field.name === name)?.label ?? name,
  }))

  const textInput = (name, extra = {}) => (props) => (
    <input {...props} className="field__input" value={values[name]} onChange={change(name)} {...extra} />
  )

  return (
    <form className="product-form" onSubmit={submit} noValidate>
      {problems.length > 0 && (
        <div ref={summaryRef} className="error-summary" tabIndex={-1} aria-labelledby="error-summary-title">
          <h2 id="error-summary-title" className="error-summary__title">
            {problems.length === 1 ? 'Please correct this:' : `Please correct these ${problems.length} things:`}
          </h2>
          <ul className="error-summary__list">
            {problems.map((problem) => (
              <li key={problem.name}>
                <a
                  href={`#${fieldId(problem.name)}`}
                  onClick={(event) => {
                    event.preventDefault()
                    document.getElementById(fieldId(problem.name))?.focus()
                  }}
                >
                  {problem.label}: {problem.message}
                </a>
              </li>
            ))}
          </ul>
        </div>
      )}

      {formError && (
        <div className="form-alert" role="alert">
          <p>{formError.text}</p>
          {formError.conflict && (
            <button type="button" className="button button--quiet" onClick={onLoadLatest}>Load the latest version</button>
          )}
        </div>
      )}

      <FormField id={fieldId('name')} label="Name" error={errors.name}>
        {textInput('name', { autoComplete: 'off' })}
      </FormField>
      <FormField
        id={fieldId('sku')}
        label="SKU"
        hint="Capital letters, digits, and hyphens, for example FRM-HOM-005."
        error={errors.sku}
      >
        {textInput('sku', { autoComplete: 'off', autoCapitalize: 'characters', spellCheck: false })}
      </FormField>
      <FormField id={fieldId('category')} label="Category" error={errors.category}>
        {(props) => (
          <select {...props} className="field__input" value={values.category} onChange={change('category')}>
            <option value="">Choose a category</option>
            {CATEGORIES.map((category) => <option key={category.value} value={category.value}>{category.label}</option>)}
          </select>
        )}
      </FormField>
      <div className="product-form__row">
        <FormField id={fieldId('price')} label="Price (₹)" hint="For example 1490 or 1490.50." error={errors.price}>
          {textInput('price', { inputMode: 'decimal', autoComplete: 'off' })}
        </FormField>
        <FormField id={fieldId('stockQuantity')} label="Stock quantity" hint="0 means sold out." error={errors.stockQuantity}>
          {textInput('stockQuantity', { inputMode: 'numeric', autoComplete: 'off' })}
        </FormField>
      </div>
      <FormField id={fieldId('description')} label="Description" error={errors.description}>
        {(props) => (
          <textarea {...props} className="field__input field__input--area" rows={5} value={values.description}
            onChange={change('description')} />
        )}
      </FormField>
      <FormField
        id={fieldId('imageUrl')}
        label="Image address (optional)"
        hint="A web address, or one of the shop's photos such as /images/products/frm-hom-002.jpg."
        error={errors.imageUrl}
      >
        {textInput('imageUrl', { autoComplete: 'off', spellCheck: false })}
      </FormField>

      <div className="product-form__actions">
        <button type="submit" className="button button--primary" disabled={submitting}>
          {submitting ? 'Saving…' : (editing ? 'Save changes' : 'Add product')}
        </button>
        <Link className="link-button" to={paths.admin}>Cancel</Link>
      </div>
    </form>
  )
}

// BUILD FLOW 13.10.03 — Editing loads the product first
// key={product.version} gives the form a fresh start whenever a newer version is loaded ("Load the latest version"):
// React then mounts a new form with the new values instead of keeping the old field state.
function EditProduct({ id }) {
  const { loading, product, error, reload } = useProduct(id)
  usePageTitle(product ? `Edit ${product.name}` : 'Edit product')
  if (loading) return <output className="admin__note">Loading the product…</output>
  if (error) {
    return (
      <div className="status status--error" role="alert">
        <p className="status__text">{error.status === 404 ? 'This product no longer exists.' : error.message}</p>
        {error.status !== 404 && <button type="button" className="button button--primary" onClick={reload}>Try again</button>}
      </div>
    )
  }
  return (
    <>
      <h1 className="admin__title">Edit “{product.name}”</h1>
      <ProductForm
        key={product.version}
        productId={product.id}
        version={product.version}
        initial={toFormValues(product)}
        onLoadLatest={reload}
      />
    </>
  )
}

function NewProduct() {
  usePageTitle('Add a product')
  return (
    <>
      <h1 className="admin__title">Add a product</h1>
      <ProductForm initial={EMPTY_FORM} />
    </>
  )
}

export default function ProductFormPage({ id, writesEnabled }) {
  if (!writesEnabled) return <ReadOnlyNotice />
  return (
    <div className="admin admin--form">
      <Link className="back-link" to={paths.admin}>← All products</Link>
      {id === undefined ? <NewProduct /> : <EditProduct id={id} />}
    </div>
  )
}
