/*
 * BUILD FLOW 05.06 — ProductNotFoundException.java
 * First introduced: Phase 05
 * Purpose: Signals that no product exists with the requested id.
 * Depends on: Nothing.
 * Thrown by: ProductService (05.10). Handled by: ApiExceptionHandler (05.12), which answers 404 Not Found.
 * Runtime role: Ends a request early; nothing in the database changes.
 */
package com.example.catalog.product;

// BUILD FLOW 05.06.01 — An unchecked domain exception
// Extending RuntimeException makes it "unchecked": methods need not declare it, and Spring rolls back the current
// transaction when it escapes a @Transactional method (the default rule). The message is safe to show to clients.
public class ProductNotFoundException extends RuntimeException {

    private final long productId;

    public ProductNotFoundException(long productId) {
        super("No product with id " + productId);
        this.productId = productId;
    }

    public long getProductId() {
        return productId;
    }
}
