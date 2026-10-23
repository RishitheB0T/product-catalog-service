/*
 * BUILD FLOW 09.01 — ProductCacheProperties.java
 * First introduced: Phase 09
 * Purpose: Typed access to the product-detail cache settings: whether the cache is used at all, and how long a cached
 *          product lives in Redis (its TTL, "time to live").
 * Depends on: catalog.cache.* in application.yml (02.06.06).
 * Used by: ProductDetailCache (09.02).
 * Runtime role: Bound once at startup; read on every product-detail request.
 */
package com.example.catalog.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// BUILD FLOW 09.01.01 — The switch and the TTL
// enabled is the switch that lets benchmarks compare the same endpoint with and without the cache (Phase 11).
// productTtl accepts Spring Boot's duration syntax: 60s, 2m, 500ms. A zero or negative TTL stops startup with a clear
// message instead of failing later on every write to Redis.
@ConfigurationProperties(prefix = "catalog.cache")
public record ProductCacheProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("60s") Duration productTtl) {

    public ProductCacheProperties {
        if (productTtl.isZero() || productTtl.isNegative()) {
            throw new IllegalArgumentException("catalog.cache.product-ttl must be positive, but was " + productTtl);
        }
    }
}
