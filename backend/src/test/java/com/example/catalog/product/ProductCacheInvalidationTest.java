/*
 * BUILD FLOW 10.03 — ProductCacheInvalidationTest.java
 * First introduced: Phase 10
 * Purpose: Proves that committed writes clear the cached copy (update, delete, and create with its generated id), that
 *          a rolled-back write clears nothing, and reproduces the stale-read race that cache-aside still allows.
 * Depends on: ProductService (05.10), ProductDetailCache (09.02), ProductCacheEvictionListener (10.02), real MySQL and
 *             Redis from TestcontainersConfiguration (04.07).
 * Called by: Maven Surefire. Docker must be running.
 * Runtime role: Test only.
 */
package com.example.catalog.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.example.catalog.TestcontainersConfiguration;

import io.micrometer.core.instrument.MeterRegistry;

// BUILD FLOW 10.03.01 — Real transactions, real Redis
// No test transaction: each service call commits or rolls back for real, which is what decides whether the
// after-commit listener runs. Every product gets its own SKU and therefore its own id and cache key.
@SpringBootTest(properties = "catalog.writes-enabled=true")
@AutoConfigureMetrics
@Import(TestcontainersConfiguration.class)
class ProductCacheInvalidationTest {

    private static final AtomicInteger SKU_NUMBER = new AtomicInteger();

    @Autowired
    private ProductService service;

    @Autowired
    private ProductDetailCache cache;

    @Autowired
    private ProductRepository repository;

    @Autowired
    private ProductMapper mapper;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private MeterRegistry meters;

    private ProductResponse create(String sku, String name) {
        return service.createProduct(new ProductCreateRequest(sku, name, "Created by ProductCacheInvalidationTest.",
                Category.KITCHEN, new BigDecimal("1590"), 8, null));
    }

    private ProductResponse rename(ProductResponse product, String sku, String name) {
        return service.updateProduct(product.id(), new ProductUpdateRequest(sku, name, product.description(),
                product.category(), product.price(), product.stockQuantity(), product.imageUrl(), product.version()));
    }

    private static String newSku() {
        return "INVAL-" + SKU_NUMBER.incrementAndGet();
    }

    private boolean cached(long id) {
        return Boolean.TRUE.equals(redis.hasKey(ProductDetailCache.key(id)));
    }

    private double evictions(String outcome) {
        return meters.counter("catalog.product.cache.evictions", "outcome", outcome).count();
    }

    // BUILD FLOW 10.03.02 — Every committed write clears its key
    @Test
    void anUpdateClearsTheCachedCopyOnceItHasCommitted() {
        ProductResponse product = create(newSku(), "Enamel Jug");
        service.getProduct(product.id());
        assertThat(cached(product.id())).isTrue();
        double before = evictions("done");

        rename(product, product.sku(), "Enamel Jug, Large");

        assertThat(cached(product.id())).isFalse();
        assertThat(evictions("done")).isEqualTo(before + 1);
        // The next read misses, reads MySQL, and stores the new version.
        ProductResponse read = service.getProduct(product.id());
        assertThat(read.name()).isEqualTo("Enamel Jug, Large");
        assertThat(read.version()).isEqualTo(1);
    }

    @Test
    void aDeleteClearsTheCachedCopy() {
        ProductResponse product = create(newSku(), "Olive Wood Spoon");
        service.getProduct(product.id());

        service.deleteProduct(product.id());

        assertThat(cached(product.id())).isFalse();
        assertThatThrownBy(() -> service.getProduct(product.id())).isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void aCreateEvictsItsNewlyGeneratedId() {
        double before = evictions("done");

        ProductResponse product = create(newSku(), "Speckled Bowl");

        // Nothing was cached under the new id, so the eviction removed nothing, but it ran with the generated id.
        assertThat(evictions("done")).isEqualTo(before + 1);
        assertThat(cached(product.id())).isFalse();
    }

    // BUILD FLOW 10.03.03 — A rollback clears nothing
    // The update renames product B and gives it product A's SKU. The UPDATE breaks the unique key, the transaction rolls
    // back, and the after-commit listener never runs: B's cached copy stays, and it is still correct, because MySQL is
    // unchanged too.
    @Test
    void aRolledBackUpdateLeavesTheCacheAlone() {
        ProductResponse productA = create(newSku(), "Copper Pan A");
        ProductResponse productB = create(newSku(), "Copper Pan B");
        ProductResponse cachedB = service.getProduct(productB.id());
        double before = evictions("done");

        assertThatThrownBy(() -> rename(productB, productA.sku(), "Renamed B"))
                .isInstanceOf(DuplicateSkuException.class);

        assertThat(evictions("done")).isEqualTo(before);
        assertThat(cached(productB.id())).isTrue();
        assertThat(service.getProduct(productB.id())).isEqualTo(cachedB);
    }

    // BUILD FLOW 10.03.04 — The race that cache-aside still allows (docs/CACHING.md)
    // The steps of two concurrent requests, played one after another so the outcome is certain:
    //   1. a reader misses the cache and reads version 0 from MySQL, but is slow to store it;
    //   2. a writer commits version 1 and clears the key, which is not there yet;
    //   3. the slow reader now stores its version-0 copy.
    // Readers then get version 0 until the TTL ends. This test documents the limit; it does not fix it.
    @Test
    void aSlowReaderCanStillStoreAnOldCopyAfterAWrite() {
        ProductResponse product = create(newSku(), "Glass Carafe");

        ProductResponse readerCopy = mapper.toResponse(repository.findById(product.id()).orElseThrow());
        rename(product, product.sku(), "Glass Carafe, Fluted");
        cache.put(readerCopy);

        ProductResponse served = service.getProduct(product.id());
        assertThat(served.version()).isZero();
        assertThat(served.name()).isEqualTo("Glass Carafe");
        // The TTL is what limits how long the outdated copy can be served.
        assertThat(redis.getExpire(ProductDetailCache.key(product.id()), TimeUnit.SECONDS)).isBetween(1L, 60L);
    }
}
