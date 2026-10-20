/*
 * BUILD FLOW 04.08 — ProductRepositoryTest.java
 * First introduced: Phase 04
 * Purpose: Prove the persistence layer against a real MySQL 8.4: Flyway's V1 migration applies, Hibernate's
 *          schema validation passes, products round-trip, timestamps are stored in UTC, and the database itself
 *          rejects duplicate SKUs, invalid rows, and updates based on an outdated version.
 * Depends on: Product (04.03), ProductRepository (04.04), V1 migration (04.01), TestcontainersConfiguration (04.07).
 * Called by: Maven Surefire during .\mvnw.cmd test or verify. Docker must be running.
 * Runtime role: Test only.
 */
package com.example.catalog.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import javax.sql.DataSource;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.example.catalog.TestcontainersConfiguration;

// BUILD FLOW 04.08.01 — A persistence "slice" test on real MySQL
// @DataJpaTest starts only the persistence part of the application (entities, repositories, Flyway, the
// DataSource), not the web layer, and wraps every test in a transaction that is rolled back afterwards, so tests
// cannot leave rows behind. By default it would swap in an in-memory database; Replace.NONE keeps the real MySQL
// from @Import(TestcontainersConfiguration.class), because only MySQL behaves exactly like MySQL.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ProductRepositoryTest {

    // In tests, Spring fills @Autowired fields after creating the test instance. (Application code uses
    // constructor injection instead; test classes are created by JUnit, so field injection is the simple option.)
    @Autowired
    private ProductRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    // Plain SQL access, used to look at the stored values directly and to bypass Java-side rules on purpose.
    private JdbcTemplate jdbc;

    @BeforeEach
    void createJdbcTemplate() {
        jdbc = new JdbcTemplate(dataSource);
    }

    private static Product newProduct(String sku) {
        return new Product(sku, "Test Linen Shirt", "A shirt that exists only in tests.", Category.APPAREL,
                new BigDecimal("2499.50"), 7, null);
    }

    // BUILD FLOW 04.08.02 — Round trip
    @Test
    void savesAndReloadsAProduct() {
        Product saved = repository.saveAndFlush(newProduct("TEST-SAVE-001"));
        // Forget every cached object, so the next read really comes from MySQL instead of memory.
        entityManager.clear();

        Product reloaded = repository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getId()).isNotNull();
        assertThat(reloaded.getSku()).isEqualTo("TEST-SAVE-001");
        assertThat(reloaded.getCategory()).isEqualTo(Category.APPAREL);
        // isEqualByComparingTo ignores BigDecimal scale differences: 2499.5 equals 2499.50.
        assertThat(reloaded.getPrice()).isEqualByComparingTo("2499.50");
        assertThat(reloaded.getVersion()).isZero();
        assertThat(reloaded.getCreatedAt()).isEqualTo(saved.getCreatedAt()).isEqualTo(reloaded.getUpdatedAt());
    }

    @Test
    void findsAProductBySku() {
        repository.saveAndFlush(newProduct("TEST-FIND-001"));

        assertThat(repository.findBySku("TEST-FIND-001")).isPresent();
        assertThat(repository.findBySku("TEST-MISSING-001")).isEmpty();
    }

    // BUILD FLOW 04.08.03 — Timestamps are stored in UTC
    // The raw DATETIME value, read with plain SQL, must equal the Instant converted to UTC. This matters because
    // the JVM on the development machine runs in IST (UTC+05:30): without the UTC settings, the stored value
    // would be 5.5 hours off from what another time zone would read back.
    @Test
    void storesTimestampsInUtc() {
        Product saved = repository.saveAndFlush(newProduct("TEST-UTC-001"));

        LocalDateTime stored = jdbc.queryForObject(
                "SELECT created_at FROM product WHERE id = ?", LocalDateTime.class, saved.getId());

        assertThat(stored).isEqualTo(LocalDateTime.ofInstant(saved.getCreatedAt(), ZoneOffset.UTC));
    }

    // BUILD FLOW 04.08.04 — Constraints enforced by MySQL itself
    // A duplicate key is MySQL error 1062, which Spring reports as DataIntegrityViolationException. Its message
    // names the violated constraint, which the API will turn into a clear 409 in Phase 05.
    @Test
    void rejectsADuplicateSku() {
        repository.saveAndFlush(newProduct("TEST-DUP-001"));

        assertThatThrownBy(() -> repository.saveAndFlush(newProduct("TEST-DUP-001")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_product_sku");
    }

    // Each row is written with plain SQL, deliberately bypassing all Java-side checks, so only the database's
    // CHECK constraints can stop it. @CsvSource supplies one case per line; quoteCharacter '"' lets a value be
    // a single space (" "), while unquoted values are trimmed.
    // Finding from the first test run: MySQL reports a CHECK violation as error 3819, and Spring's built-in table
    // of MySQL error codes does not list 3819, so JdbcTemplate wraps it in a generic UncategorizedSQLException
    // (a DataAccessException) rather than DataIntegrityViolationException. The test asserts exactly that: some
    // DataAccessException whose root cause is MySQL error 3819 naming the constraint. The application itself goes
    // through Hibernate, which does classify it; see the next test.
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', quoteCharacter = '"', textBlock = """
            negative price    | TEST-CHK-001 | Name | Text | HOME   | -1.00 | 1  | ck_product_price_not_negative
            negative stock    | TEST-CHK-002 | Name | Text | HOME   | 10.00 | -1 | ck_product_stock_not_negative
            unknown category  | TEST-CHK-003 | Name | Text | GARDEN | 10.00 | 1  | ck_product_category
            lower-case SKU    | test-chk-004 | Name | Text | HOME   | 10.00 | 1  | ck_product_sku_format
            blank name        | TEST-CHK-005 | " "  | Text | HOME   | 10.00 | 1  | ck_product_name_not_blank
            blank description | TEST-CHK-006 | Name | " "  | HOME   | 10.00 | 1  | ck_product_description_not_blank
            """)
    void databaseRejectsInvalidRows(String rule, String sku, String name, String description, String category,
            BigDecimal price, int stock, String constraint) {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO product (sku, name, description, category, price, stock_quantity) VALUES (?, ?, ?, ?, ?, ?)",
                sku, name, description, category, price, stock))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining(constraint)
                .rootCause()
                .isInstanceOfSatisfying(SQLException.class, e -> assertThat(e.getErrorCode()).isEqualTo(3819));
    }

    // Through JPA, which is how the application writes, Hibernate's MySQL dialect recognises error 3819 as a
    // constraint violation, and Spring reports it as DataIntegrityViolationException, ready for the API to map.
    @Test
    void checkViolationsThroughJpaAreDataIntegrityViolations() {
        Product product = newProduct("TEST-CHK-JPA-001");
        product.setPrice(new BigDecimal("-1.00"));

        assertThatThrownBy(() -> repository.saveAndFlush(product))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_product_price_not_negative");
    }

    // BUILD FLOW 04.08.05 — Optimistic locking
    @Test
    void successfulUpdateIncrementsTheVersion() {
        Product product = repository.saveAndFlush(newProduct("TEST-VER-001"));
        Instant createdAt = product.getCreatedAt();

        product.setStockQuantity(99);
        Product updated = repository.saveAndFlush(product);

        assertThat(updated.getVersion()).isEqualTo(1L);
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(createdAt);
    }

    @Test
    void rejectsAnUpdateBasedOnAnOutdatedVersion() {
        Product product = repository.saveAndFlush(newProduct("TEST-LOCK-001"));

        // Simulate another user whose change committed in the meantime: the row's version moves from 0 to 1,
        // while our copy of the product still remembers version 0.
        jdbc.update("UPDATE product SET name = ?, version = version + 1 WHERE id = ?", "Changed elsewhere",
                product.getId());

        product.setName("My change");

        // Hibernate sends UPDATE ... WHERE id = ? AND version = 0, which now matches no row.
        assertThatThrownBy(() -> repository.saveAndFlush(product))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
