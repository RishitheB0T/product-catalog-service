/*
 * BUILD FLOW 05.07 — DuplicateSkuException.java
 * First introduced: Phase 05
 * Purpose: Signals that another product already uses the requested SKU.
 * Depends on: Nothing.
 * Thrown by: ProductService (05.10), after MySQL's unique key uk_product_sku rejected the INSERT or UPDATE.
 * Handled by: ApiExceptionHandler (05.12), which answers 409 Conflict.
 * Runtime role: The transaction rolls back, so the database keeps its previous state.
 */
package com.example.catalog.product;

// BUILD FLOW 05.07.01 — Translating a database error into a domain error
// The database is the only reliable judge of uniqueness: two requests checking "does this SKU exist?" at the same
// moment would both see "no", but the unique key lets only one INSERT succeed. The service catches the database error
// and throws this exception instead, so the web layer never needs to know about SQL or constraint names.
public class DuplicateSkuException extends RuntimeException {

    private final String sku;

    public DuplicateSkuException(String sku, Throwable cause) {
        super("A product with SKU '" + sku + "' already exists", cause);
        this.sku = sku;
    }

    public String getSku() {
        return sku;
    }
}
