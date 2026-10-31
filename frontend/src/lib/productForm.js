// BUILD FLOW 13.14 — frontend/src/lib/productForm.js
// First introduced: Phase 13
// Purpose: The product form's rules: the fields and their labels, checks that mirror the API's validation, and the
//          conversions between a product, the form's text values, and the JSON body the API expects.
// Depends on: The rules in ProductCreateRequest (05.02) and ProductUpdateRequest (05.03); CATEGORIES (12.10).
// Used by: ProductFormPage (13.10) and productForm.test.js (13.15).
// Runtime role: Pure functions, run while a form is filled in and submitted.

import { CATEGORIES } from './catalog.js'

// BUILD FLOW 13.14.01 — The fields, in the order the form shows them
export const FIELDS = [
  { name: 'name', label: 'Name' },
  { name: 'sku', label: 'SKU' },
  { name: 'category', label: 'Category' },
  { name: 'price', label: 'Price (₹)' },
  { name: 'stockQuantity', label: 'Stock quantity' },
  { name: 'description', label: 'Description' },
  { name: 'imageUrl', label: 'Image address' },
]

export const EMPTY_FORM = Object.freeze({
  name: '', sku: '', category: '', price: '', stockQuantity: '', description: '', imageUrl: '',
})

// Form fields hold text, so numbers become text on the way in.
export function toFormValues(product) {
  return {
    name: product.name, sku: product.sku, category: product.category, price: String(product.price),
    stockQuantity: String(product.stockQuantity), description: product.description, imageUrl: product.imageUrl ?? '',
  }
}

// BUILD FLOW 13.14.02 — Checks that mirror the API
// The same limits as the API's Bean Validation rules, so most mistakes are explained before anything is sent; the
// server still checks everything again, and its answer wins (ProductFormPage shows it next to the field). The messages
// say what to do, not only what is wrong. Returns { field: message } for every field with a problem.
const SKU = /^[A-Z0-9][A-Z0-9-]*$/
const PRICE = /^\d{1,8}(\.\d{1,2})?$/
const WHOLE_NUMBER = /^\d{1,9}$/
const IMAGE = /^(https?:\/\/\S+|\/images\/products\/[a-z0-9-]+\.(jpg|png|webp))$/

export function validateProduct(values) {
  const errors = {}
  const name = values.name.trim()
  const description = values.description.trim()
  const imageUrl = values.imageUrl.trim()

  if (!name) errors.name = 'Enter a name.'
  else if (name.length > 120) errors.name = 'Use 120 characters or fewer.'

  if (!values.sku) errors.sku = 'Enter a SKU.'
  else if (values.sku.length > 32) errors.sku = 'Use 32 characters or fewer.'
  else if (!SKU.test(values.sku)) errors.sku = 'Use capital letters, digits, and hyphens, starting with a letter or digit.'

  if (!CATEGORIES.some((category) => category.value === values.category)) errors.category = 'Choose a category.'

  if (!values.price.trim()) errors.price = 'Enter a price.'
  else if (!PRICE.test(values.price.trim())) errors.price = 'Enter rupees with up to two decimals, for example 1490 or 1490.50.'

  if (!values.stockQuantity.trim()) errors.stockQuantity = 'Enter how many are in stock (0 or more).'
  else if (!WHOLE_NUMBER.test(values.stockQuantity.trim())) errors.stockQuantity = 'Enter a whole number, 0 or more.'

  if (!description) errors.description = 'Enter a description.'
  else if (description.length > 2000) errors.description = 'Use 2,000 characters or fewer.'

  if (imageUrl && (imageUrl.length > 500 || !IMAGE.test(imageUrl))) {
    errors.imageUrl = 'Use a web address (https://…) or a path under /images/products/, or leave it empty.'
  }
  return errors
}

// BUILD FLOW 13.14.03 — The JSON body
// Text is trimmed; numbers become numbers; an empty image address becomes null (no photo). An update also sends the
// version the form was loaded with, so the server can refuse it if someone changed the product in the meantime.
export function toRequestBody(values, version) {
  const body = {
    sku: values.sku, name: values.name.trim(), description: values.description.trim(), category: values.category,
    price: Number(values.price.trim()), stockQuantity: Number(values.stockQuantity.trim()),
    imageUrl: values.imageUrl.trim() || null,
  }
  return version === undefined ? body : { ...body, version }
}

// The server's field messages ("must not be blank") as sentences under a field.
export function sentence(message) {
  const text = message.charAt(0).toUpperCase() + message.slice(1)
  return text.endsWith('.') ? text : `${text}.`
}
