/*
 * BUILD FLOW 07.02 — ProductSearchApiIntegrationTest.java
 * First introduced: Phase 07
 * Purpose: Full-stack tests of search over real HTTP: filters arriving from a real query string, ties across pages,
 *          percent-encoded search text, the exact page envelope, and the binding behavior the README documents
 *          (a repeated parameter uses its first value; conversion errors are reported before rule violations).
 * Depends on: The whole application, TestcontainersConfiguration (04.07), and Spring Boot's RestTestClient.
 * Called by: Maven Surefire. Docker must be running.
 * Runtime role: Test only.
 */
package com.example.catalog.product;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.example.catalog.TestcontainersConfiguration;
import com.example.catalog.api.PageResponse;
import com.jayway.jsonpath.JsonPath;

// BUILD FLOW 07.02.01 — Same application as the API tests, different questions
// These annotations equal those of ProductApiIntegrationTest.WithWritesEnabled (07.01), so Spring reuses that running
// application from its test-context cache instead of starting another one.
// Products are created through the API and stay committed. Every test gives its products a name starting with "Zq4"
// plus a word of its own ("Zq4 Filter", "Zq4 Page", ...) and searches for exactly that prefix, so no other rows match.
// ProductSearchTest (06.07) already proves each filter on MySQL; here the question is whether a real query string
// reaches it intact.
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "catalog.writes-enabled=true")
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class ProductSearchApiIntegrationTest {

    private static final String PRODUCTS = "/api/v1/products";
    private static final ParameterizedTypeReference<PageResponse<ProductResponse>> PAGE_OF_PRODUCTS =
            new ParameterizedTypeReference<>() {
            };
    private static final AtomicInteger SKU_NUMBER = new AtomicInteger();

    @Autowired
    private RestTestClient client;

    // BUILD FLOW 07.02.02 — Helpers: create through the API, search through the API
    private long create(String name, Category category, String price, int stock) {
        ProductCreateRequest request = new ProductCreateRequest("SRCHAPI-" + SKU_NUMBER.incrementAndGet(), name,
                "Created by ProductSearchApiIntegrationTest.", category, new BigDecimal(price), stock, null);
        return client.post().uri(PRODUCTS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductResponse.class).returnResult().getResponseBody().id();
    }

    // The values replace the {placeholders} in the URI template and are percent-encoded on the way, as a browser does.
    private PageResponse<ProductResponse> search(String uriTemplate, Object... values) {
        return client.get().uri(uriTemplate, values)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PAGE_OF_PRODUCTS).returnResult().getResponseBody();
    }

    private static List<Long> ids(PageResponse<ProductResponse> page) {
        return page.content().stream().map(ProductResponse::id).toList();
    }

    // BUILD FLOW 07.02.03 — Filters and the page envelope
    @Test
    void everyFilterArrivesFromTheQueryString() {
        create("Zq4 Filter Linen Shirt", Category.APPAREL, "1500", 5);
        long soldOut = create("Zq4 Filter Linen Throw", Category.HOME, "2500", 0);
        long cushion = create("Zq4 Filter Linen Cushion", Category.HOME, "1800", 3);
        create("Zq4 Filter Wool Throw", Category.HOME, "2200", 7);
        create("Zq4 Filter Linen Napkins", Category.HOME, "900", 2);

        String allFilters = PRODUCTS + "?q={q}&category=HOME&minPrice=1000&maxPrice=3000&inStock={inStock}&sort=price_asc";

        assertThat(ids(search(allFilters, "Zq4 Filter Linen", true))).containsExactly(cushion);
        assertThat(ids(search(allFilters, "Zq4 Filter Linen", false))).containsExactly(soldOut);
    }

    @Test
    void theResponseHasExactlyTheFiveEnvelopeFields() {
        String json = new String(client.get().uri(PRODUCTS + "?size=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody(), UTF_8);
        Map<String, Object> envelope = JsonPath.read(json, "$");

        assertThat(envelope).containsOnlyKeys("content", "page", "size", "totalElements", "totalPages");
    }

    // BUILD FLOW 07.02.04 — Ties and pages
    @Test
    void pagesOfTiedProductsNeverOverlapOrSkip() {
        // Five products with the same name and price, created one after another, so their ids increase.
        List<Long> created = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            created.add(create("Zq4 Page Notebook", Category.STATIONERY, "490", 10));
        }

        List<Long> seen = new ArrayList<>();
        for (int number = 0; number < 3; number++) {
            PageResponse<ProductResponse> page = search(PRODUCTS + "?q={q}&sort=price_asc&size=2&page={page}",
                    "Zq4 Page Notebook", number);
            assertThat(page.totalElements()).isEqualTo(5);
            assertThat(page.totalPages()).isEqualTo(3);
            seen.addAll(ids(page));
        }
        // Pages of 2, 2, and 1: each product exactly once, in id order.
        assertThat(seen).containsExactlyElementsOf(created);

        PageResponse<ProductResponse> afterTheLast = search(PRODUCTS + "?q={q}&size=2&page=3", "Zq4 Page Notebook");
        assertThat(afterTheLast.content()).isEmpty();
        assertThat(afterTheLast.totalElements()).isEqualTo(5);
    }

    // BUILD FLOW 07.02.05 — Search text inside a URL
    @Test
    void searchTextIsDecodedAndThenMatchedLiterally() {
        long percent = create("Zq4 Wild 100% Linen", Category.HOME, "700", 5);
        create("Zq4 Wild 1000 Linen", Category.HOME, "700", 5);
        long accented = create("Zq4 Wild Crème Jug", Category.KITCHEN, "700", 5);

        // The client sends % as %25 and spaces as %20; the server decodes them, and the % is then matched literally.
        assertThat(ids(search(PRODUCTS + "?q={q}", "Zq4 Wild 100%"))).containsExactly(percent);
        // Case and accents are ignored on the server: "CREME" finds "Crème".
        assertThat(ids(search(PRODUCTS + "?q={q}", "zq4 wild CREME"))).containsExactly(accented);
    }

    // BUILD FLOW 07.02.06 — Binding behavior that the README documents
    // If a framework upgrade ever changes either behavior, these tests fail, and the README and D-034 must change too.
    @Test
    void aRepeatedParameterUsesItsFirstValue() {
        long tote = create("Zq4 Repeat Tote", Category.BAGS, "1200", 3);
        create("Zq4 Repeat Vase", Category.HOME, "1200", 3);

        assertThat(ids(search(PRODUCTS + "?q={q}&category=BAGS&category=HOME", "Zq4 Repeat"))).containsExactly(tote);
    }

    @Test
    void conversionErrorsAreReportedBeforeTheRules() {
        // "popular" cannot become a ProductSort, so the rules (such as size ≤ 100) are not checked yet.
        client.get().uri(PRODUCTS + "?sort=popular&size=500")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors[*].field").isEqualTo(List.of("sort"));

        // Once every value converts, every broken rule is reported together.
        client.get().uri(PRODUCTS + "?sort=price_asc&size=500&page=-1")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors[*].field").isEqualTo(List.of("page", "size"));
    }
}
