/*
 * BUILD FLOW 08.05 — BenchmarkDataSeeder.java
 * First introduced: Phase 08
 * Purpose: With the "bench" profile only: fills an EMPTY product table with the generated benchmark products in one
 *          transaction, then refreshes MySQL's statistics about the table.
 * Depends on: BenchmarkProductGenerator (08.04), BenchmarkProperties (08.03), the JdbcTemplate and TransactionTemplate
 *             that Spring Boot configures, and the product table created by Flyway.
 * Called by: Spring Boot, once, after the application has started (ApplicationRunner), like DemoDataLoader (04.06).
 * Runtime role: Measurement setup on the development machine; absent in every other profile.
 */
package com.example.catalog.bench;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.catalog.bench.BenchmarkProductGenerator.GeneratedProduct;

// BUILD FLOW 08.05.01 — A bench-only Spring bean
// The same pattern as the dev-only DemoDataLoader: @Profile("bench") means the bean exists only when that profile is
// active, and ApplicationRunner means Spring Boot calls run(...) once the application has started, after Flyway.
@Component
@Profile("bench")
class BenchmarkDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkDataSeeder.class);
    private static final int BATCH_SIZE = 1_000;
    private static final String INSERT = """
            INSERT INTO product (sku, name, description, category, price, stock_quantity, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)""";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final BenchmarkProperties properties;

    BenchmarkDataSeeder(JdbcTemplate jdbc, TransactionTemplate transaction, BenchmarkProperties properties) {
        this.jdbc = jdbc;
        this.transaction = transaction;
        this.properties = properties;
    }

    // BUILD FLOW 08.05.02 — Only into an empty table, all or nothing
    // A non-empty table is left alone, so restarting the bench profile never duplicates data. The whole load is one
    // transaction (TransactionTemplate is the programmatic form of @Transactional): if anything fails, no half-filled
    // table is left behind to be mistaken for a complete one.
    // MySQL chooses indexes using statistics about the data. They refresh by themselves after large changes, but in
    // the background; ANALYZE TABLE refreshes them now, so the first measurement sees the real distribution.
    @Override
    public void run(ApplicationArguments args) {
        Long existing = jdbc.queryForObject("SELECT COUNT(*) FROM product", Long.class);
        if (existing != null && existing > 0) {
            log.info("Benchmark data not loaded: the product table already has {} rows", existing);
            return;
        }
        long started = System.nanoTime();
        transaction.executeWithoutResult(status -> insertProducts());
        jdbc.execute("ANALYZE TABLE product");
        log.info("Loaded {} benchmark products (seed {}) in {} ms", properties.products(), properties.seed(),
                (System.nanoTime() - started) / 1_000_000);
    }

    // BUILD FLOW 08.05.03 — Batched inserts
    // batchUpdate sends 1,000 rows per round trip to MySQL; with rewriteBatchedStatements=true (08.01) the driver turns
    // each batch into a single multi-row INSERT. Timestamps are written as UTC LocalDateTime values, the convention the
    // application uses for every DATETIME(6) column (decision D-021).
    private void insertProducts() {
        BenchmarkProductGenerator generator = new BenchmarkProductGenerator(properties.seed());
        List<GeneratedProduct> batch = new ArrayList<>(BATCH_SIZE);
        for (int i = 0; i < properties.products(); i++) {
            batch.add(generator.next());
            if (batch.size() == BATCH_SIZE || i == properties.products() - 1) {
                jdbc.batchUpdate(INSERT, batch, batch.size(), (statement, product) -> {
                    LocalDateTime createdAt = LocalDateTime.ofInstant(product.createdAt(), ZoneOffset.UTC);
                    statement.setString(1, product.sku());
                    statement.setString(2, product.name());
                    statement.setString(3, product.description());
                    statement.setString(4, product.category().name());
                    statement.setBigDecimal(5, product.price());
                    statement.setInt(6, product.stockQuantity());
                    statement.setObject(7, createdAt);
                    statement.setObject(8, createdAt);
                });
                batch.clear();
            }
        }
    }
}
