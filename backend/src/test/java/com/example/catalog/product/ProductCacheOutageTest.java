/*
 * BUILD FLOW 10.04 — ProductCacheOutageTest.java
 * First introduced: Phase 10
 * Purpose: Proves the behavior during a Redis outage: reads fall back to MySQL within a fraction of a second, a write
 *          still commits and succeeds although its eviction fails (which is counted), and the cache works again once
 *          Redis is back.
 * Depends on: ProductService (05.10), ProductDetailCache (09.02), the 200 ms Redis timeouts (application.yml, 02.06.08),
 *             and the shared Redis test container (04.07), which this test pauses.
 * Called by: Maven Surefire. Docker must be running.
 * Runtime role: Test only.
 */
package com.example.catalog.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;

import com.example.catalog.TestcontainersConfiguration;

import io.micrometer.core.instrument.MeterRegistry;

// BUILD FLOW 10.04.01 — Pausing the real Redis
// "docker pause" freezes the Redis process: connections stay open, but nothing answers. That is the worst case for a
// client, because it cannot tell "down" from "slow"; only the timeout ends the wait. The container is shared with other
// test classes, so every test unpauses it in a finally block, whatever happens.
// A client-side timeout does not cancel what was already sent: once Redis runs again, it executes the commands that were
// waiting in its connection. The tests therefore check behavior during the outage, and use fresh keys afterwards.
@SpringBootTest(properties = "catalog.writes-enabled=true")
@AutoConfigureMetrics
@Import(TestcontainersConfiguration.class)
class ProductCacheOutageTest {

    private static final AtomicInteger SKU_NUMBER = new AtomicInteger();
    private static final Duration WELL_BELOW_DEFAULT_TIMEOUT = Duration.ofSeconds(2);

    @Autowired
    private ProductService service;

    @Autowired
    private ProductRepository repository;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private MeterRegistry meters;

    // The bean named after its @Bean method in TestcontainersConfiguration (the MySQL container is a GenericContainer too).
    @Autowired
    @Qualifier("redisContainer")
    private GenericContainer<?> redisContainer;

    private ProductResponse create(String name) {
        return service.createProduct(new ProductCreateRequest("OUTAGE-" + SKU_NUMBER.incrementAndGet(), name,
                "Created by ProductCacheOutageTest.", Category.HOME, new BigDecimal("2190"), 6, null));
    }

    private void pauseRedis() {
        redisContainer.getDockerClient().pauseContainerCmd(redisContainer.getContainerId()).exec();
    }

    private void unpauseRedis() {
        redisContainer.getDockerClient().unpauseContainerCmd(redisContainer.getContainerId()).exec();
    }

    private double count(String name, String tag, String value) {
        return meters.counter(name, tag, value).count();
    }

    // BUILD FLOW 10.04.02 — During the outage
    @Test
    void whileRedisHangsReadsAndWritesStillSucceedQuickly() {
        ProductResponse product = create("Rattan Basket");
        double cacheErrors = count("catalog.product.cache", "result", "error");
        double failedEvictions = count("catalog.product.cache.evictions", "outcome", "failed");

        pauseRedis();
        long readStarted;
        Duration readTook;
        Duration writeTook;
        ProductResponse read;
        ProductResponse updated;
        try {
            readStarted = System.nanoTime();
            read = service.getProduct(product.id());
            readTook = Duration.ofNanos(System.nanoTime() - readStarted);

            long writeStarted = System.nanoTime();
            updated = service.updateProduct(product.id(), new ProductUpdateRequest(product.sku(),
                    "Rattan Basket, Large", product.description(), product.category(), product.price(),
                    product.stockQuantity(), product.imageUrl(), product.version()));
            writeTook = Duration.ofNanos(System.nanoTime() - writeStarted);
        } finally {
            unpauseRedis();
        }

        // The read came from MySQL: the lookup and the store each gave up after the 200 ms timeout.
        assertThat(read.name()).isEqualTo("Rattan Basket");
        assertThat(readTook).isLessThan(WELL_BELOW_DEFAULT_TIMEOUT);
        assertThat(count("catalog.product.cache", "result", "error")).isEqualTo(cacheErrors + 2);
        // The write committed and was reported as a success; only its eviction failed, and that was counted.
        assertThat(updated.version()).isEqualTo(1);
        assertThat(writeTook).isLessThan(WELL_BELOW_DEFAULT_TIMEOUT);
        assertThat(count("catalog.product.cache.evictions", "outcome", "failed")).isEqualTo(failedEvictions + 1);
        assertThat(repository.findById(product.id()).orElseThrow().getName()).isEqualTo("Rattan Basket, Large");
    }

    // BUILD FLOW 10.04.03 — After the outage
    @Test
    void theCacheWorksAgainOnceRedisIsBack() {
        ProductResponse duringOutage = create("Woven Rug");
        pauseRedis();
        try {
            service.getProduct(duringOutage.id());
        } finally {
            unpauseRedis();
        }

        // A product nobody touched during the outage: its first read stores it, its second read is a hit.
        ProductResponse fresh = create("Woven Rug, Round");
        double hits = count("catalog.product.cache", "result", "hit");
        service.getProduct(fresh.id());
        service.getProduct(fresh.id());

        assertThat(count("catalog.product.cache", "result", "hit")).isEqualTo(hits + 1);
        assertThat(redis.hasKey(ProductDetailCache.key(fresh.id()))).isTrue();
    }
}
