/*
 * BUILD FLOW 05.04 — ProductResponse.java
 * First introduced: Phase 05
 * Purpose: The JSON a client receives for one product. Built from the entity, never the entity itself.
 * Depends on: Category (04.02).
 * Used by: ProductMapper (05.05) builds it; ProductController (05.11) returns it; from Phase 09 it is also what the
 *          Redis cache stores.
 * Runtime role: Serialized to JSON by Jackson for every successful GET, POST, and PUT.
 */
package com.example.catalog.product;

import java.math.BigDecimal;
import java.time.Instant;

// BUILD FLOW 05.04.01 — The public shape of a product
// Returning a DTO instead of the JPA entity keeps the API stable when the table changes, never exposes internal fields
// by accident, and can never trigger database loading while JSON is written.
// inStock is derived (stockQuantity > 0) so the storefront doesn't need to repeat that rule.
// version is included because a client must send it back with PUT.
// Instants become ISO-8601 text in UTC, for example "2026-10-21T05:12:00.123456Z"; prices keep two decimals (1290.00).
public record ProductResponse(
        long id,
        String sku,
        String name,
        String description,
        Category category,
        BigDecimal price,
        int stockQuantity,
        boolean inStock,
        String imageUrl,
        Instant createdAt,
        Instant updatedAt,
        long version) {
}
