/*
 * BUILD FLOW 10.02 — ProductCacheEvictionListener.java
 * First introduced: Phase 10
 * Purpose: Removes a product's cached copy after the transaction that changed it has committed, and never after a
 *          rollback.
 * Depends on: ProductChangedEvent (10.01), ProductDetailCache.evict (09.02), Spring's transactional event support.
 * Called by: Spring, once the transaction that published the event has committed.
 * Runtime role: Runtime step WRITE-07, after the commit and before the response is sent.
 */
package com.example.catalog.product;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// BUILD FLOW 10.02.01 — Why after the commit, and only then
// Spring keeps the event with the transaction and calls this method once it has ended with a commit; after a rollback
// it never calls it. Evicting earlier, inside the transaction, would leave a gap: another request could miss the cache,
// read the not-yet-committed old row from MySQL, and store it again before the commit.
// The method runs in the request's own thread, after the commit and before the HTTP response is written, so the client
// that made the change reads the new version next (unless the eviction itself failed).
// An exception here could not turn the committed write into an error: Spring runs after-commit listeners in its
// afterCompletion callback, which logs exceptions instead of rethrowing them. ProductDetailCache.evict also catches
// Redis failures itself, so they are counted.
@Component
class ProductCacheEvictionListener {

    private final ProductDetailCache cache;

    ProductCacheEvictionListener(ProductDetailCache cache) {
        this.cache = cache;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductChanged(ProductChangedEvent event) {
        cache.evict(event.productId());
    }
}
