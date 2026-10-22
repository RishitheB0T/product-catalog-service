/*
 * BUILD FLOW 08.07 — BenchmarkDataSeederTest.java
 * First introduced: Phase 08
 * Purpose: Proves that benchmark data loads only with the "bench" profile, only into an empty table, exactly as the
 *          generator produced it, and never twice; and that without the profile the seeder does not exist.
 * Depends on: BenchmarkDataSeeder (08.05), BenchmarkProductGenerator (08.04), BenchmarkProperties (08.03),
 *             TestcontainersConfiguration (04.07).
 * Called by: Maven Surefire. Docker must be running.
 * Runtime role: Test only.
 */
package com.example.catalog.bench;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.example.catalog.TestcontainersConfiguration;
import com.example.catalog.bench.BenchmarkProductGenerator.GeneratedProduct;

// BUILD FLOW 08.07.01 — Same pattern as DemoDataLoaderTest (04.09)
// The bench profile's own database settings are replaced by the shared test container (@ServiceConnection, 04.07),
// and catalog.bench.products=500 keeps the test fast.
class BenchmarkDataSeederTest {

    @Nested
    @SpringBootTest(properties = "catalog.bench.products=500")
    @ActiveProfiles("bench")
    @Import(TestcontainersConfiguration.class)
    class WithBenchProfile {

        @Autowired
        private BenchmarkDataSeeder seeder;

        @Autowired
        private BenchmarkProperties properties;

        @Autowired
        private JdbcTemplate jdbc;

        @Test
        void fillsAnEmptyTableWithTheGeneratedProductsExactlyOnce() {
            // The test container is shared with other test classes, so start from a known state.
            jdbc.update("DELETE FROM product");

            seeder.run(null);

            assertThat(count()).isEqualTo(500);
            // The stored row matches what the generator produced, field by field, including the UTC timestamp.
            GeneratedProduct first = new BenchmarkProductGenerator(properties.seed()).next();
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT name, category, price, stock_quantity, created_at FROM product WHERE sku = ?", first.sku());
            assertThat(row.get("name")).isEqualTo(first.name());
            assertThat(row.get("category")).isEqualTo(first.category().name());
            assertThat((BigDecimal) row.get("price")).isEqualByComparingTo(first.price());
            assertThat(row.get("stock_quantity")).isEqualTo(first.stockQuantity());
            assertThat(((LocalDateTime) row.get("created_at")).toInstant(ZoneOffset.UTC)).isEqualTo(first.createdAt());

            // A second run finds a non-empty table and must not add anything.
            seeder.run(null);

            assertThat(count()).isEqualTo(500);
        }

        private long count() {
            return jdbc.queryForObject("SELECT COUNT(*) FROM product", Long.class);
        }
    }

    @Nested
    @SpringBootTest
    @Import(TestcontainersConfiguration.class)
    class WithoutBenchProfile {

        @Autowired
        private ApplicationContext context;

        @Test
        void hasNoBenchmarkDataSeeder() {
            assertThat(context.getBeanNamesForType(BenchmarkDataSeeder.class)).isEmpty();
        }
    }
}
