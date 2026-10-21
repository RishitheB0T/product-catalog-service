/*
 * BUILD FLOW 05.08 — VersionConflictException.java
 * First introduced: Phase 05
 * Purpose: Signals that an update or delete was based on an outdated version of a product.
 * Depends on: Nothing.
 * Thrown by: ProductService (05.10). Handled by: ApiExceptionHandler (05.12), which answers 409 Conflict.
 * Runtime role: The transaction rolls back, so the newer data that someone else saved stays intact.
 */
package com.example.catalog.product;

// BUILD FLOW 05.08.01 — Two ways to detect a conflict
// 1. The client's version (from its last GET) differs from the stored version: we know both numbers.
// 2. Hibernate's @Version check fails during the UPDATE itself, because another transaction committed between our read
//    and our write: we only know that it happened, so currentVersion is null.
public class VersionConflictException extends RuntimeException {

    public VersionConflictException(long productId, long expectedVersion, Long currentVersion) {
        super(currentVersion == null
                ? "Product " + productId + " was changed by someone else while this change was being saved."
                        + " Reload it and try again."
                : "Product " + productId + " is at version " + currentVersion + ", but this change was based on version "
                        + expectedVersion + ". Reload it and try again.");
    }
}
