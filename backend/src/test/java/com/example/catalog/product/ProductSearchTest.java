/*
 * BUILD FLOW 06.07 — ProductSearchTest.java
 * First introduced: Phase 06
 * Purpose: Tests search on a real MySQL: each filter and their combination, case- and accent-insensitive names,
 *          literal wildcards, inclusive price bounds, the in-stock flag, every sort with its id tie-breaker, and pages
 *          that never overlap or skip rows.
 * Depends on: ProductService.searchProducts (05.10) and everything it uses (06.01–06.06), TestcontainersConfiguration (04.07).
 * Called by: Maven Surefire. Docker must be running.
 * Runtime role: Test only.
 */
package com.example.catalog.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.example.catalog.TestcontainersConfiguration;
import com.example.catalog.api.PageResponse;

// BUILD FLOW 06.07.01 — Each test's own rows, rolled back afterwards
// @Transactional on a test class: every test runs inside one transaction that is rolled back when the test ends, so
// the rows it inserts disappear again. The service's read-only transaction joins that transaction and sees the rows.
// The shared test database can also hold rows committed by other test classes (decision D-023). Every product here
// therefore has the marker "Qv7" in its name, and every search except the one that counts everything includes it,
// so only this test's own rows can match.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ProductSearchTest {

    private static final AtomicInteger SKU_NUMBER = new AtomicInteger();
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 10, 21, 6, 0);

    @Autowired
    private ProductService service;

    @Autowired
    private JdbcTemplate jdbc;

    // BUILD FLOW 06.07.02 — Test data and search helpers
    // Rows are inserted with plain SQL, so a test can give several products the same created_at (a tie) on purpose.
    private long insert(String name, Category category, String price, int stock) {
        return insert(name, category, price, stock, CREATED_AT);
    }

    private long insert(String name, Category category, String price, int stock, LocalDateTime createdAt) {
        jdbc.update("""
                INSERT INTO product (sku, name, description, category, price, stock_quantity, created_at, updated_at)
                VALUES (?, ?, 'Inserted by ProductSearchTest.', ?, ?, ?, ?, ?)
                """, "SRCH-" + SKU_NUMBER.incrementAndGet(), name, category.name(), new BigDecimal(price), stock,
                createdAt, createdAt);
        // LAST_INSERT_ID() belongs to the connection, and the test transaction keeps one connection throughout.
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    // The order of ProductSearchRequest's components: q, category, minPrice, maxPrice, inStock, page, size, sort.
    // Filter results are sorted by price (cheapest first), so the expected order is easy to read.
    private List<Long> idsFound(String q, Category category, String minPrice, String maxPrice, Boolean inStock) {
        return ids(service.searchProducts(new ProductSearchRequest(q, category, decimal(minPrice), decimal(maxPrice),
                inStock, null, null, ProductSort.PRICE_ASC)));
    }

    private List<Long> idsSorted(String q, ProductSort sort) {
        return ids(service.searchProducts(new ProductSearchRequest(q, null, null, null, null, null, null, sort)));
    }

    private static List<Long> ids(PageResponse<ProductResponse> page) {
        return page.content().stream().map(ProductResponse::id).toList();
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    // BUILD FLOW 06.07.03 — Filters
    @Test
    void withoutFiltersEveryProductIsFound() {
        insert("Qv7 Plain Mug", Category.KITCHEN, "500", 1);
        long rows = jdbc.queryForObject("SELECT COUNT(*) FROM product", Long.class);
        int size = ProductSearchRequest.DEFAULT_SIZE;

        PageResponse<ProductResponse> page = service.searchProducts(
                new ProductSearchRequest(null, null, null, null, null, null, null, null));

        assertThat(page.totalElements()).isEqualTo(rows);
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(size);
        assertThat(page.content()).hasSize((int) Math.min(rows, size));
        assertThat(page.totalPages()).isEqualTo((int) Math.ceil(rows / (double) size));
    }

    @Test
    void eachFilterNarrowsTheResultAndTheyCombine() {
        long shirt = insert("Qv7 Linen Shirt", Category.APPAREL, "1500", 5);
        long linenThrow = insert("Qv7 Linen Throw", Category.HOME, "2500", 0);
        long cushion = insert("Qv7 Linen Cushion", Category.HOME, "1800", 3);
        insert("Qv7 Wool Throw", Category.HOME, "2200", 7);
        long napkins = insert("Qv7 Linen Napkins", Category.HOME, "900", 2);
        long curtain = insert("Qv7 Linen Curtain", Category.HOME, "3200", 4);

        // Each line adds one filter, and each filter removes exactly one more product.
        assertThat(idsFound("Qv7 Linen", null, null, null, null))
                .containsExactly(napkins, shirt, cushion, linenThrow, curtain);
        assertThat(idsFound("Qv7 Linen", Category.HOME, null, null, null))
                .containsExactly(napkins, cushion, linenThrow, curtain);
        assertThat(idsFound("Qv7 Linen", Category.HOME, "1000", null, null))
                .containsExactly(cushion, linenThrow, curtain);
        assertThat(idsFound("Qv7 Linen", Category.HOME, "1000", "3000", null))
                .containsExactly(cushion, linenThrow);
        assertThat(idsFound("Qv7 Linen", Category.HOME, "1000", "3000", true))
                .containsExactly(cushion);
    }

    @Test
    void nameSearchIgnoresCaseAndAccents() {
        long ramekin = insert("Qv7 Crème Brûlée Ramekin", Category.KITCHEN, "650", 9);

        assertThat(idsFound("qv7 CREME brulee", null, null, null, null)).containsExactly(ramekin);
    }

    @Test
    void wildcardCharactersInTheSearchTextAreMatchedLiterally() {
        long percent = insert("Qv7 100% Linen Napkin", Category.HOME, "700", 5);
        insert("Qv7 1000 Thread Napkin", Category.HOME, "700", 5);
        long underscore = insert("Qv7 Tray_A", Category.HOME, "700", 5);
        insert("Qv7 TrayXA", Category.HOME, "700", 5);
        long exclamation = insert("Qv7 Wow! Mug", Category.KITCHEN, "700", 5);
        insert("Qv7 Wow Mug", Category.KITCHEN, "700", 5);
        long backslash = insert("Qv7 C:\\Temp Box", Category.STATIONERY, "700", 5);

        // Unescaped, % and _ would act as wildcards, and each search would also find the product inserted after it.
        assertThat(idsFound("Qv7 100%", null, null, null, null)).containsExactly(percent);
        assertThat(idsFound("Qv7 Tray_A", null, null, null, null)).containsExactly(underscore);
        // "!" is the escape character, so it must itself be escaped.
        assertThat(idsFound("Qv7 Wow!", null, null, null, null)).containsExactly(exclamation);
        assertThat(idsFound("Qv7 C:\\Temp", null, null, null, null)).containsExactly(backslash);
    }

    @Test
    void escapeLikeDoublesTheEscapeCharacterFirst() {
        assertThat(ProductSpecifications.escapeLike("100%_!")).isEqualTo("100!%!_!!");
    }

    @Test
    void priceBoundsAreInclusive() {
        insert("Qv7 Bound Below", Category.BAGS, "999.99", 1);
        long exact = insert("Qv7 Bound Exact", Category.BAGS, "1000.00", 1);
        insert("Qv7 Bound Above", Category.BAGS, "1000.01", 1);

        // 1000 and 1000.00 are the same price, and both bounds include it.
        assertThat(idsFound("Qv7 Bound", null, "1000", "1000.00", null)).containsExactly(exact);
    }

    @Test
    void inStockSelectsAvailableOrSoldOutProducts() {
        long available = insert("Qv7 Stock Available", Category.FOOTWEAR, "2000", 4);
        long soldOut = insert("Qv7 Stock Sold Out", Category.FOOTWEAR, "2100", 0);

        assertThat(idsFound("Qv7 Stock", null, null, null, true)).containsExactly(available);
        assertThat(idsFound("Qv7 Stock", null, null, null, false)).containsExactly(soldOut);
        assertThat(idsFound("Qv7 Stock", null, null, null, null)).containsExactly(available, soldOut);
    }

    @Test
    void noMatchGivesAnEmptyPage() {
        PageResponse<ProductResponse> page = service.searchProducts(
                new ProductSearchRequest("Qv7 no product has this name", null, null, null, null, null, null, null));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
    }

    // BUILD FLOW 06.07.04 — Sorting and the id tie-breaker
    @Test
    void eachSortOrdersByItsMainColumn() {
        // Inserted in the order B, A, C, so that no expected order below is simply the id order.
        long b = insert("Qv7 Order B", Category.HOME, "3000", 1, CREATED_AT.minusDays(2));
        long a = insert("Qv7 Order A", Category.HOME, "1000", 1, CREATED_AT);
        long c = insert("Qv7 Order C", Category.HOME, "2000", 1, CREATED_AT.minusDays(1));

        assertThat(idsSorted("Qv7 Order", ProductSort.NEWEST)).containsExactly(a, c, b);
        assertThat(idsSorted("Qv7 Order", ProductSort.PRICE_ASC)).containsExactly(a, c, b);
        assertThat(idsSorted("Qv7 Order", ProductSort.PRICE_DESC)).containsExactly(b, c, a);
        assertThat(idsSorted("Qv7 Order", ProductSort.NAME_ASC)).containsExactly(a, b, c);
    }

    @Test
    void tiesAreBrokenByIdInTheDirectionOfTheSort() {
        // Same name, price, and creation time: only the id tells these three apart.
        long first = insert("Qv7 Tie", Category.HOME, "1000", 1);
        long second = insert("Qv7 Tie", Category.HOME, "1000", 1);
        long third = insert("Qv7 Tie", Category.HOME, "1000", 1);

        assertThat(idsSorted("Qv7 Tie", ProductSort.PRICE_ASC)).containsExactly(first, second, third);
        assertThat(idsSorted("Qv7 Tie", ProductSort.NAME_ASC)).containsExactly(first, second, third);
        assertThat(idsSorted("Qv7 Tie", ProductSort.PRICE_DESC)).containsExactly(third, second, first);
        assertThat(idsSorted("Qv7 Tie", ProductSort.NEWEST)).containsExactly(third, second, first);
    }

    @Test
    void everySortEndsWithId() {
        // Also protects sorts added in the future: forgetting the tie-breaker fails here.
        for (ProductSort sort : ProductSort.values()) {
            List<Sort.Order> orders = sort.toSort().toList();
            assertThat(orders.getLast().getProperty()).as("last order of %s", sort).isEqualTo("id");
        }
    }

    // BUILD FLOW 06.07.05 — Paging
    @Test
    void pagesNeverOverlapOrSkipRows() {
        // Seven products that tie on every sort column: without the tie-breaker, page boundaries would be arbitrary.
        List<Long> inserted = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            inserted.add(insert("Qv7 Page", Category.STATIONERY, "500", 1));
        }

        List<Long> seen = new ArrayList<>();
        for (int number = 0; number < 3; number++) {
            PageResponse<ProductResponse> page = service.searchProducts(
                    new ProductSearchRequest("Qv7 Page", null, null, null, null, number, 3, ProductSort.PRICE_ASC));
            assertThat(page.page()).isEqualTo(number);
            assertThat(page.size()).isEqualTo(3);
            assertThat(page.totalElements()).isEqualTo(7);
            assertThat(page.totalPages()).isEqualTo(3);
            seen.addAll(ids(page));
        }

        // Pages of 3, 3, and 1 product: every product exactly once, in id order.
        assertThat(seen).containsExactlyElementsOf(inserted);
    }

    @Test
    void aPageAfterTheLastIsEmptyButKeepsTheTotals() {
        for (int i = 0; i < 4; i++) {
            insert("Qv7 Beyond", Category.BAGS, "800", 1);
        }

        PageResponse<ProductResponse> page = service.searchProducts(
                new ProductSearchRequest("Qv7 Beyond", null, null, null, null, 5, 3, null));

        assertThat(page.content()).isEmpty();
        assertThat(page.page()).isEqualTo(5);
        assertThat(page.totalElements()).isEqualTo(4);
        assertThat(page.totalPages()).isEqualTo(2);
    }
}
