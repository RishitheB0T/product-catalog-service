/*
 * BUILD FLOW 04.09 — DemoDataLoaderTest.java
 * First introduced: Phase 04
 * Purpose: Prove that the demo catalog loads with the dev profile, only into an empty table, never twice, and
 *          that without the dev profile the loader does not exist at all.
 * Depends on: DemoDataLoader (04.06), demo-products.sql (04.05), TestcontainersConfiguration (04.07).
 * Called by: Maven Surefire during .\mvnw.cmd test or verify. Docker must be running.
 * Runtime role: Test only.
 */
package com.example.catalog.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.example.catalog.TestcontainersConfiguration;

// BUILD FLOW 04.09.01 — Two configurations, one assertion each
// Same pattern as HealthEndpointTest: each @Nested class starts its own Spring application with different
// profiles, here against the shared MySQL test container.
class DemoDataLoaderTest {

    @Nested
    @SpringBootTest
    @ActiveProfiles("dev")
    @Import(TestcontainersConfiguration.class)
    class WithDevProfile {

        @Autowired
        private DemoDataLoader loader;

        @Autowired
        private JdbcTemplate jdbc;

        @Test
        void loadsTheDemoCatalogIntoAnEmptyTableExactlyOnce() {
            // The test container is shared with other test classes, so start from a known state.
            jdbc.update("DELETE FROM product");

            loader.run(null);

            assertThat(count("SELECT COUNT(*) FROM product")).isEqualTo(24);
            assertThat(count("SELECT COUNT(DISTINCT category) FROM product")).isEqualTo(6);
            assertThat(count("SELECT COUNT(*) FROM product WHERE stock_quantity = 0")).isEqualTo(2);

            // A second run finds a non-empty table and must not add anything.
            loader.run(null);

            assertThat(count("SELECT COUNT(*) FROM product")).isEqualTo(24);
        }

        private long count(String sql) {
            return jdbc.queryForObject(sql, Long.class);
        }
    }

    @Nested
    @SpringBootTest
    @Import(TestcontainersConfiguration.class)
    class WithoutDevProfile {

        @Autowired
        private ApplicationContext context;

        @Test
        void hasNoDemoDataLoader() {
            assertThat(context.getBeanNamesForType(DemoDataLoader.class)).isEmpty();
        }
    }
}
