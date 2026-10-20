/*
 * BUILD FLOW 04.06 — DemoDataLoader.java
 * First introduced: Phase 04
 * Purpose: Fills an EMPTY product table with the demo catalog (db/demo/demo-products.sql), and only when the
 *          "dev" profile is active. Production, and tests without the dev profile, never get demo data.
 * Depends on: The DataSource and JdbcTemplate that Spring Boot configures, and the product table from Flyway's V1.
 * Called by: Spring Boot, once, after the application has started (it implements ApplicationRunner).
 * Runtime role: Development convenience only.
 */
package com.example.catalog.demo;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

// BUILD FLOW 04.06.01 — A dev-only Spring bean
// @Component: Spring finds this class during component scanning (it lives under com.example.catalog) and creates
// one shared instance of it, called a bean. @Profile("dev"): it does so only when the dev profile is active;
// otherwise the class is ignored completely. ApplicationRunner: Spring Boot calls run(...) once the application
// has fully started, which is after Flyway has created the table.
// The class is package-private (no "public"): nothing outside this package needs to see it.
@Component
@Profile("dev")
class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);
    private static final ClassPathResource SCRIPT = new ClassPathResource("db/demo/demo-products.sql");

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    // BUILD FLOW 04.06.02 — Constructor injection
    // Nobody writes "new DemoDataLoader(...)". Spring sees that the only constructor needs a DataSource and a
    // JdbcTemplate, finds those beans (Spring Boot built them from the datasource settings), and passes them in.
    // This is dependency injection: the class declares what it needs instead of creating it, which keeps it easy
    // to test and to reconfigure. The fields are final, so they cannot change after construction.
    DemoDataLoader(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    // BUILD FLOW 04.06.03 — Load only into an empty table
    // Running twice must never duplicate anything, so the loader does nothing if any product exists. The script
    // runs through Spring's ResourceDatabasePopulator, which splits it into statements at ";" and skips "--" comments.
    @Override
    public void run(ApplicationArguments args) {
        Long existing = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product", Long.class);
        if (existing != null && existing > 0) {
            log.info("Demo data not loaded: the product table already has {} rows", existing);
            return;
        }
        new ResourceDatabasePopulator(SCRIPT).execute(dataSource);
        Long loaded = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product", Long.class);
        log.info("Loaded {} demo products from {}", loaded, SCRIPT.getPath());
    }
}
