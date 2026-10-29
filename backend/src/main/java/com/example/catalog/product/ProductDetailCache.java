/*
 * BUILD FLOW 09.02 — ProductDetailCache.java
 * First introduced: Phase 09
 * Purpose: The Redis side of the product-detail cache: reads and writes one product's response as JSON under the key
 *          catalog:v1:product:{id}, with a TTL, and counts every hit, miss, and error. Since Phase 10 it also removes
 *          a product's key after a committed write, and counts those evictions.
 * Depends on: StringRedisTemplate (Spring Data Redis, configured by Spring Boot), the application's Jackson JsonMapper,
 *             ProductCacheProperties (09.01), and Micrometer's MeterRegistry.
 * Used by: ProductService.getProduct (05.10) for reads; ProductCacheEvictionListener (10.02) for evictions.
 * Runtime role: Runtime steps READ-04 (look up), READ-07 (store), and WRITE-07 (evict); a Redis failure becomes
 *               READ-04X.
 */
package com.example.catalog.product;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.example.catalog.config.ProductCacheProperties;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

// BUILD FLOW 09.02.01 — What is stored, and under which key
// Key: "catalog:v1:product:" + id. "catalog" keeps this application's keys apart from anything else in the same Redis;
// "v1" names the format of the value, so a future incompatible format can use "v2" keys instead of misreading old
// ones. Value: the ProductResponse as JSON text, written by the same Jackson mapper that writes API responses, so a
// cached product looks exactly like the API's answer. Text is easy to inspect with redis-cli.
// Only response DTOs are cached, never entities: a DTO is plain data with no database connection behind it.
@Component
public class ProductDetailCache {

    private static final Logger log = LoggerFactory.getLogger(ProductDetailCache.class);
    static final String KEY_PREFIX = "catalog:v1:product:";

    private final StringRedisTemplate redis;
    private final JsonMapper json;
    private final ProductCacheProperties properties;
    private final Counter hits;
    private final Counter misses;
    private final Counter errors;
    private final Counter evictions;
    private final Counter failedEvictions;

    // BUILD FLOW 09.02.02 — Metrics
    // One counter, catalog.product.cache, with a "result" tag of hit, miss, or error. The hit rate is
    // hits / (hits + misses); errors show Redis trouble. Micrometer collects them; the dev profile shows them at
    // /actuator/metrics/catalog.product.cache. Since Phase 10 a second counter, catalog.product.cache.evictions, has an
    // "outcome" tag of done or failed: a failed eviction can leave an outdated copy behind until its TTL ends.
    public ProductDetailCache(StringRedisTemplate redis, JsonMapper json, ProductCacheProperties properties,
            MeterRegistry meters) {
        this.redis = redis;
        this.json = json;
        this.properties = properties;
        this.hits = counter(meters, "hit");
        this.misses = counter(meters, "miss");
        this.errors = counter(meters, "error");
        this.evictions = evictionCounter(meters, "done");
        this.failedEvictions = evictionCounter(meters, "failed");
    }

    // BUILD FLOW 09.02.03 — Reading (runtime step READ-04)
    // Empty means "ask MySQL": the key is missing (a miss), the cache is switched off, or Redis failed. Only two kinds of
    // failure are caught: Spring's DataAccessException, into which Spring Data Redis translates connection and command
    // errors, and JacksonException for a value that is not readable JSON. Both are logged and counted, and the request
    // continues with MySQL. Any other exception is a bug and still surfaces. Since Phase 10, 200 ms Redis timeouts
    // (application.yml, 02.06.08) bound how long a hung Redis can delay a request.
    public Optional<ProductResponse> get(long id) {
        if (!properties.enabled()) {
            return Optional.empty();
        }
        String key = key(id);
        try {
            String cached = redis.opsForValue().get(key);
            if (cached == null) {
                misses.increment();
                return Optional.empty();
            }
            ProductResponse product = json.readValue(cached, ProductResponse.class);
            hits.increment();
            return Optional.of(product);
        } catch (DataAccessException | JacksonException e) {
            errors.increment();
            log.warn("Product cache read failed for {}; reading MySQL instead: {}", key, e.toString());
            return Optional.empty();
        }
    }

    // BUILD FLOW 09.02.04 — Writing (runtime step READ-07)
    // SET with an expiry: Redis deletes the key by itself after the TTL. A failed write is logged and counted but never
    // fails the request: the caller already has the product from MySQL. It also overwrites an unreadable value.
    public void put(ProductResponse product) {
        if (!properties.enabled()) {
            return;
        }
        String key = key(product.id());
        try {
            redis.opsForValue().set(key, json.writeValueAsString(product), properties.productTtl());
        } catch (DataAccessException | JacksonException e) {
            errors.increment();
            log.warn("Product cache write failed for {}: {}", key, e.toString());
        }
    }

    // BUILD FLOW 09.02.05 — Evicting (runtime step WRITE-07; added in Phase 10)
    // DEL removes the key; the next read misses and stores the current product. Deleting a key that does not exist is
    // harmless, which is why a create may evict its new id too. A failure is logged and counted, never thrown: the write
    // has already committed, and reporting it as failed would be false. The outdated copy then lives until its TTL ends.
    // When the cache is switched off, nothing is evicted either: after switching it back on, wait one TTL (60 s) before
    // relying on cached products, because changes made in between did not clear older copies.
    public void evict(long id) {
        if (!properties.enabled()) {
            return;
        }
        String key = key(id);
        try {
            redis.delete(key);
            evictions.increment();
        } catch (DataAccessException e) {
            failedEvictions.increment();
            log.warn("Product cache eviction failed for {}; the cached copy may stay outdated until its TTL ends: {}",
                    key, e.toString());
        }
    }

    static String key(long id) {
        return KEY_PREFIX + id;
    }

    private static Counter counter(MeterRegistry meters, String result) {
        return Counter.builder("catalog.product.cache")
                .description("Product-detail cache lookups and failures")
                .tag("result", result)
                .register(meters);
    }

    private static Counter evictionCounter(MeterRegistry meters, String outcome) {
        return Counter.builder("catalog.product.cache.evictions")
                .description("Product-detail cache evictions after committed writes")
                .tag("outcome", outcome)
                .register(meters);
    }
}
