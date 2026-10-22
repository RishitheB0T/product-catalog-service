/*
 * BUILD FLOW 08.06 — BenchmarkProductGeneratorTest.java
 * First introduced: Phase 08
 * Purpose: Proves the benchmark data is reproducible (same seed, same products; a fixed fingerprint for the default
 *          dataset), valid (every product passes the API's own validation rules), and shaped like a shop (category
 *          shares, sold-out share, prices, dates).
 * Depends on: BenchmarkProductGenerator (08.04), ProductCreateRequest (05.02), and Hibernate Validator.
 * Called by: Maven Surefire. A plain unit test: no Spring, no database, no Docker.
 * Runtime role: Test only.
 */
package com.example.catalog.bench;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;

import com.example.catalog.bench.BenchmarkProductGenerator.GeneratedProduct;
import com.example.catalog.product.Category;
import com.example.catalog.product.ProductCreateRequest;

// BUILD FLOW 08.06.01 — The dataset under test
// The default dataset is the one application-bench.yml loads: seed 20261022, 100,000 products. Generating it takes
// well under a second, so every test below checks all of it, not a sample.
class BenchmarkProductGeneratorTest {

    private static final long DEFAULT_SEED = 20261022;
    private static final int DEFAULT_PRODUCTS = 100_000;
    private static final List<GeneratedProduct> DATASET = generate(DEFAULT_SEED, DEFAULT_PRODUCTS);

    private static List<GeneratedProduct> generate(long seed, int count) {
        BenchmarkProductGenerator generator = new BenchmarkProductGenerator(seed);
        return Stream.generate(generator::next).limit(count).toList();
    }

    // BUILD FLOW 08.06.02 — Reproducible
    @Test
    void theSameSeedAlwaysGivesTheSameProducts() {
        assertThat(generate(42, 1_000)).isEqualTo(generate(42, 1_000));
        assertThat(generate(43, 1_000)).isNotEqualTo(generate(42, 1_000));
    }

    // A fingerprint is a SHA-256 hash over every field of every product, in order. Measurements can be compared only
    // while the data stays exactly the same; if this test fails, the generator has changed, and saved measurements no
    // longer describe the data it produces. The expected value was recorded from the first generation of the dataset.
    @Test
    void theDefaultDatasetHasAFixedFingerprint() throws NoSuchAlgorithmException {
        assertThat(fingerprint(DATASET)).isEqualTo("6b78f5088cce4723f102f87a575e89999d9867ee7f180789ac3d6579462c367f");
    }

    // BUILD FLOW 08.06.03 — Valid by the API's own rules
    // Each generated product is turned into the request a client would send and checked with the same Bean Validation
    // rules that guard POST /api/v1/products (05.02), which mirror the database constraints of V1.
    @Test
    void everyProductPassesTheApiValidationRules() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            List<String> problems = DATASET.stream()
                    .flatMap(product -> validator.validate(new ProductCreateRequest(product.sku(), product.name(),
                            product.description(), product.category(), product.price(), product.stockQuantity(), null))
                            .stream()
                            .map(violation -> product.sku() + " " + violation.getPropertyPath() + " " + violation.getMessage()))
                    .toList();

            assertThat(problems).isEmpty();
        }
        assertThat(DATASET).extracting(GeneratedProduct::sku).doesNotHaveDuplicates();
    }

    // BUILD FLOW 08.06.04 — Shaped like a shop
    // Uneven category shares and a few sold-out products make the filters match very different numbers of rows, which
    // is what the measurements need. Shares are checked in percent, with a tolerance far above random variation.
    @Test
    void theDataIsShapedLikeTheCatalog() {
        Map<Category, Long> perCategory = DATASET.stream().collect(groupingBy(GeneratedProduct::category, counting()));
        assertThat(percent(perCategory.get(Category.APPAREL))).isCloseTo(24, within(1.0));
        assertThat(percent(perCategory.get(Category.FOOTWEAR))).isCloseTo(15, within(1.0));
        assertThat(percent(perCategory.get(Category.BAGS))).isCloseTo(13, within(1.0));
        assertThat(percent(perCategory.get(Category.HOME))).isCloseTo(20, within(1.0));
        assertThat(percent(perCategory.get(Category.KITCHEN))).isCloseTo(16, within(1.0));
        assertThat(percent(perCategory.get(Category.STATIONERY))).isCloseTo(12, within(1.0));
        assertThat(percent(DATASET.stream().filter(product -> product.stockQuantity() == 0).count()))
                .isCloseTo(7, within(0.5));

        assertThat(DATASET).allSatisfy(product -> {
            assertThat(product.price().toPlainString()).endsWith("90.00");
            assertThat(product.createdAt())
                    .isBetween(Instant.parse("2024-10-01T00:00:00Z"), Instant.parse("2026-10-01T00:00:00Z"));
        });
    }

    private static double percent(long count) {
        return count * 100.0 / DATASET.size();
    }

    private static String fingerprint(List<GeneratedProduct> products) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        for (GeneratedProduct product : products) {
            String line = String.join("|", product.sku(), product.name(), product.description(),
                    product.category().name(), product.price().toPlainString(),
                    Integer.toString(product.stockQuantity()), product.createdAt().toString()) + "\n";
            sha256.update(line.getBytes(StandardCharsets.UTF_8));
        }
        return HexFormat.of().formatHex(sha256.digest());
    }
}
