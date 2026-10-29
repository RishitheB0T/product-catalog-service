/*
 * BUILD FLOW 09.03 — ProductDetailCacheTest.java
 * First introduced: Phase 09
 * Purpose: Proves the product-detail cache against a real Redis: a miss reads MySQL and stores JSON with the TTL; a hit
 *          answers without MySQL; the JSON round-trips every field; missing products and a switched-off cache store
 *          nothing; an unreadable value counts as an error and is replaced; keys expire after the TTL.
 * Depends on: ProductService.getProduct (05.10), ProductDetailCache (09.02), TestcontainersConfiguration (04.07,
 *             MySQL and Redis), and Spring Boot's @AutoConfigureMetrics.
 * Called by: Maven Surefire. Docker must be running.
 * Runtime role: Test only.
 */
package com.example.catalog.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.catalog.TestcontainersConfiguration;

import io.micrometer.core.instrument.MeterRegistry;

// BUILD FLOW 09.03.01 — Real Redis, real MySQL, the real service
// Products are saved through the repository and committed (no test transaction), so the service reads them in its
// own transactions, exactly as in production. Every product has its own id, so every test has its own cache key.
// @AutoConfigureMetrics: Spring Boot tests normally replace metrics with a no-op registry; this keeps real counters,
// so the tests can check hits, misses, and errors.
class ProductDetailCacheTest {

    private static final AtomicInteger SKU_NUMBER = new AtomicInteger();

    private static Product newProduct(String name) {
        return new Product("CACHE-" + SKU_NUMBER.incrementAndGet(), name, "Created by ProductDetailCacheTest.",
                Category.HOME, new BigDecimal("2490.00"), 5, null);
    }

    @Nested
    @SpringBootTest(properties = "catalog.cache.enabled=true")
    @AutoConfigureMetrics
    @Import(TestcontainersConfiguration.class)
    class WithTheCacheOn {

        @Autowired
        private ProductService service;

        @Autowired
        private ProductRepository repository;

        @Autowired
        private StringRedisTemplate redis;

        @Autowired
        private JdbcTemplate jdbc;

        @Autowired
        private MeterRegistry meters;

        // BUILD FLOW 09.03.02 — Miss, then hit
        @Test
        void aMissReadsMySqlAndStoresTheJsonWithTheTtl() {
            Product product = repository.save(newProduct("Cache Lamp"));
            String key = ProductDetailCache.key(product.getId());
            double missesBefore = count("miss");

            service.getProduct(product.getId());

            assertThat(count("miss")).isEqualTo(missesBefore + 1);
            assertThat(key).isEqualTo("catalog:v1:product:" + product.getId());
            assertThat(redis.opsForValue().get(key))
                    .contains("\"sku\":\"" + product.getSku() + "\"")
                    .contains("\"price\":2490.00");
            // Seconds left before Redis deletes the key: the 60-second TTL, minus the moment since it was stored.
            assertThat(redis.getExpire(key, TimeUnit.SECONDS)).isBetween(55L, 60L);
        }

        @Test
        void aHitIsAnsweredFromRedisWithoutReadingMySql() {
            Product product = repository.save(newProduct("Cache Vase"));
            service.getProduct(product.getId());
            // Change the row behind the application's back. Only an answer from Redis can still show the old name.
            jdbc.update("UPDATE product SET name = 'Changed in MySQL only' WHERE id = ?", product.getId());
            double hitsBefore = count("hit");

            ProductResponse second = service.getProduct(product.getId());

            assertThat(second.name()).isEqualTo("Cache Vase");
            assertThat(count("hit")).isEqualTo(hitsBefore + 1);
        }

        @Test
        void theJsonRoundTripKeepsEveryField() {
            Product product = repository.save(newProduct("Cache Bowl"));

            ProductResponse fromMySql = service.getProduct(product.getId());
            ProductResponse fromRedis = service.getProduct(product.getId());

            // Records compare every field: id, price with its two decimals, timestamps to the microsecond, version.
            assertThat(fromRedis).isEqualTo(fromMySql);
        }

        // BUILD FLOW 09.03.03 — What is never cached, and what is repaired
        @Test
        void aMissingProductIsNotCached() {
            long missing = Long.MAX_VALUE;

            assertThatThrownBy(() -> service.getProduct(missing)).isInstanceOf(ProductNotFoundException.class);

            assertThat(redis.hasKey(ProductDetailCache.key(missing))).isFalse();
        }

        @Test
        void anUnreadableValueCountsAsAnErrorAndIsReplaced() {
            Product product = repository.save(newProduct("Cache Jug"));
            String key = ProductDetailCache.key(product.getId());
            redis.opsForValue().set(key, "not json");
            double errorsBefore = count("error");

            ProductResponse read = service.getProduct(product.getId());

            assertThat(read.name()).isEqualTo("Cache Jug");
            assertThat(count("error")).isEqualTo(errorsBefore + 1);
            assertThat(redis.opsForValue().get(key)).contains("\"name\":\"Cache Jug\"");
        }

        private double count(String result) {
            return meters.counter("catalog.product.cache", "result", result).count();
        }
    }

    // BUILD FLOW 09.03.04 — Expiry
    // A one-second TTL, so the test can watch Redis delete the key. Awaitility checks the condition repeatedly until it
    // holds or the time limit passes, instead of sleeping for a guessed duration.
    @Nested
    @SpringBootTest(properties = { "catalog.cache.enabled=true", "catalog.cache.product-ttl=1s" })
    @AutoConfigureMetrics
    @Import(TestcontainersConfiguration.class)
    class WithAOneSecondTtl {

        @Autowired
        private ProductService service;

        @Autowired
        private ProductRepository repository;

        @Autowired
        private StringRedisTemplate redis;

        @Test
        void theKeyExpiresAndTheNextReadGoesToMySqlAgain() {
            Product product = repository.save(newProduct("Cache Rug"));
            String key = ProductDetailCache.key(product.getId());

            service.getProduct(product.getId());
            assertThat(redis.hasKey(key)).isTrue();

            await().atMost(Duration.ofSeconds(5)).until(() -> !redis.hasKey(key));

            service.getProduct(product.getId());
            assertThat(redis.hasKey(key)).isTrue();
        }
    }

    // BUILD FLOW 09.03.05 — Switched off
    // The default in Phase 09; since Phase 10 the cache is on by default, so this test switches it off explicitly.
    @Nested
    @SpringBootTest(properties = "catalog.cache.enabled=false")
    @Import(TestcontainersConfiguration.class)
    class WithTheCacheOff {

        @Autowired
        private ProductService service;

        @Autowired
        private ProductRepository repository;

        @Autowired
        private StringRedisTemplate redis;

        @Test
        void nothingIsStoredAndEveryReadGoesToMySql() {
            Product product = repository.save(newProduct("Cache Tray"));

            service.getProduct(product.getId());

            assertThat(redis.hasKey(ProductDetailCache.key(product.getId()))).isFalse();
        }
    }
}
