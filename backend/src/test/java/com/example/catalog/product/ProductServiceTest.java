/*
 * BUILD FLOW 05.14 — ProductServiceTest.java
 * First introduced: Phase 05
 * Purpose: Tests ProductService with real transactions on a real MySQL: commits, rollbacks, version checks,
 *          duplicate SKUs, missing products, and the read-only switch under the production profile.
 * Depends on: ProductService (05.10) and everything it uses, TestcontainersConfiguration (04.07).
 * Called by: Maven Surefire. Docker must be running.
 * Runtime role: Test only.
 */
package com.example.catalog.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.example.catalog.TestcontainersConfiguration;
import com.example.catalog.error.WritesDisabledException;

// BUILD FLOW 05.14.01 — Real transactions, not test transactions
// These tests are NOT annotated @Transactional. Each service call therefore runs in its own real transaction that
// truly commits or rolls back, which is exactly what is being tested. The rows they commit stay in the shared test
// database, so every test uses its own SKUs and never relies on the table being empty.
class ProductServiceTest {

    private static ProductCreateRequest createRequest(String sku, String name) {
        return new ProductCreateRequest(sku, name, "Created by ProductServiceTest.", Category.HOME,
                new BigDecimal("1499"), 3, null);
    }

    private static ProductUpdateRequest updateRequest(ProductResponse current, String sku, String name, long version) {
        return new ProductUpdateRequest(sku, name, current.description(), current.category(), current.price(),
                current.stockQuantity(), current.imageUrl(), version);
    }

    // BUILD FLOW 05.14.02 — Writes enabled (as in development)
    // properties = ... sets catalog.writes-enabled=true for this test application only.
    @Nested
    @SpringBootTest(properties = "catalog.writes-enabled=true")
    @Import(TestcontainersConfiguration.class)
    class WithWritesEnabled {

        @Autowired
        private ProductService service;

        @Test
        void createsAProductAndReadsItBack() {
            ProductResponse created = service.createProduct(createRequest("SVC-CRT-001", "  Linen Cushion  "));

            ProductResponse read = service.getProduct(created.id());

            assertThat(read.sku()).isEqualTo("SVC-CRT-001");
            assertThat(read.name()).isEqualTo("Linen Cushion");
            // 1499 was sent; the mapper and DECIMAL(10,2) store two decimals.
            assertThat(read.price()).isEqualTo(new BigDecimal("1499.00"));
            assertThat(read.inStock()).isTrue();
            assertThat(read.version()).isZero();
        }

        @Test
        void updateWritesTheChangeAndIncrementsTheVersion() {
            ProductResponse created = service.createProduct(createRequest("SVC-UPD-001", "Old name"));

            ProductResponse updated = service.updateProduct(created.id(),
                    updateRequest(created, "SVC-UPD-001", "New name", created.version()));

            assertThat(updated.name()).isEqualTo("New name");
            assertThat(updated.version()).isEqualTo(1);
            assertThat(service.getProduct(created.id()).name()).isEqualTo("New name");
        }

        @Test
        void updateBasedOnAnOldVersionIsRefusedAndChangesNothing() {
            ProductResponse created = service.createProduct(createRequest("SVC-VER-001", "Original"));
            service.updateProduct(created.id(), updateRequest(created, "SVC-VER-001", "First change", 0));

            // A second client still holds version 0.
            assertThatThrownBy(() -> service.updateProduct(created.id(),
                    updateRequest(created, "SVC-VER-001", "Second change", 0)))
                    .isInstanceOf(VersionConflictException.class)
                    .hasMessageContaining("is at version 1");

            ProductResponse stored = service.getProduct(created.id());
            assertThat(stored.name()).isEqualTo("First change");
            assertThat(stored.version()).isEqualTo(1);
        }

        @Test
        void duplicateSkuOnCreateIsRefused() {
            service.createProduct(createRequest("SVC-DUP-001", "First"));

            assertThatThrownBy(() -> service.createProduct(createRequest("SVC-DUP-001", "Second")))
                    .isInstanceOf(DuplicateSkuException.class)
                    .hasMessage("A product with SKU 'SVC-DUP-001' already exists");
        }

        // BUILD FLOW 05.14.03 — Rollback undoes the whole change
        // The update changes the name AND the SKU; the SKU collides with another product, so the UPDATE fails. Because
        // the exception leaves the @Transactional method, the transaction rolls back and the name change is undone too.
        @Test
        void failedUpdateIsRolledBackCompletely() {
            service.createProduct(createRequest("SVC-RBK-001", "Product A"));
            ProductResponse productB = service.createProduct(createRequest("SVC-RBK-002", "Product B"));

            assertThatThrownBy(() -> service.updateProduct(productB.id(),
                    updateRequest(productB, "SVC-RBK-001", "Renamed B", productB.version())))
                    .isInstanceOf(DuplicateSkuException.class);

            ProductResponse stored = service.getProduct(productB.id());
            assertThat(stored.sku()).isEqualTo("SVC-RBK-002");
            assertThat(stored.name()).isEqualTo("Product B");
            assertThat(stored.version()).isZero();
        }

        @Test
        void deleteRemovesTheProduct() {
            ProductResponse created = service.createProduct(createRequest("SVC-DEL-001", "Short-lived"));

            service.deleteProduct(created.id());

            assertThatThrownBy(() -> service.getProduct(created.id())).isInstanceOf(ProductNotFoundException.class);
        }

        @Test
        void missingProductsAreReportedForEveryOperation() {
            long missing = Long.MAX_VALUE;
            ProductResponse template = service.createProduct(createRequest("SVC-MIS-001", "Template"));

            assertThatThrownBy(() -> service.getProduct(missing)).isInstanceOf(ProductNotFoundException.class);
            assertThatThrownBy(() -> service.updateProduct(missing, updateRequest(template, "SVC-MIS-002", "X", 0)))
                    .isInstanceOf(ProductNotFoundException.class);
            assertThatThrownBy(() -> service.deleteProduct(missing)).isInstanceOf(ProductNotFoundException.class);
        }
    }

    // BUILD FLOW 05.14.04 — The production profile is read-only
    // The real production settings (application-prod.yml) must refuse every write, while reads keep working.
    // A product is inserted directly through the repository: the switch guards the service, not the database.
    @Nested
    @SpringBootTest
    @ActiveProfiles("prod")
    @Import(TestcontainersConfiguration.class)
    class WithProductionSettings {

        @Autowired
        private ProductService service;

        @Autowired
        private ProductRepository repository;

        @Test
        void refusesEveryWriteButStillReads() {
            Product existing = repository.save(new Product("SVC-PRD-001", "Read-only lamp", "Exists before the test.",
                    Category.HOME, new BigDecimal("2999.00"), 4, null));
            long rowsBefore = repository.count();
            ProductResponse current = service.getProduct(existing.getId());

            assertThatThrownBy(() -> service.createProduct(createRequest("SVC-PRD-002", "New")))
                    .isInstanceOf(WritesDisabledException.class);
            assertThatThrownBy(() -> service.updateProduct(existing.getId(),
                    updateRequest(current, "SVC-PRD-001", "Changed", current.version())))
                    .isInstanceOf(WritesDisabledException.class);
            assertThatThrownBy(() -> service.deleteProduct(existing.getId()))
                    .isInstanceOf(WritesDisabledException.class);

            assertThat(repository.count()).isEqualTo(rowsBefore);
            assertThat(service.getProduct(existing.getId()).name()).isEqualTo("Read-only lamp");
        }
    }
}
