/*
 * BUILD FLOW 10.01 — ProductChangedEvent.java
 * First introduced: Phase 10
 * Purpose: The message "product N was created, updated, or deleted", published inside the write's transaction.
 * Depends on: Nothing; it is plain data.
 * Used by: ProductService (05.10) publishes it; ProductCacheEvictionListener (10.02) reacts to it after the commit.
 * Runtime role: Runtime step WRITE-07: the link between a committed write and the cache.
 */
package com.example.catalog.product;

// BUILD FLOW 10.01.01 — An event instead of a direct call
// The service announces what happened instead of calling the cache itself. Spring holds the event until the
// transaction ends and delivers it only if the transaction committed (see the listener, 10.02). For a create the id is
// the one MySQL generated, known only after the INSERT.
public record ProductChangedEvent(long productId) {
}
