/*
 * BUILD FLOW 07.01 — ProductApiIntegrationTest.java
 * First introduced: Phase 07
 * Purpose: Full-stack tests of the product API over real HTTP: the whole application runs on a random port with a
 *          real MySQL, and every request passes through Tomcat, Spring MVC, Jackson, validation, the service and its
 *          transaction, Hibernate, and back. Covers the write lifecycle, the JSON contract, every kind of error,
 *          a rollback, Unicode, races between simultaneous requests, and the read-only production profile.
 * Depends on: The whole application, TestcontainersConfiguration (04.07), and the RestTestClient that Spring Boot
 *             configures (spring-boot-resttestclient, part of the Web MVC test starter).
 * Called by: Maven Surefire. Docker must be running.
 * Runtime role: Test only.
 */
package com.example.catalog.product;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.example.catalog.TestcontainersConfiguration;
import com.example.catalog.api.PageResponse;
import com.jayway.jsonpath.JsonPath;

// BUILD FLOW 07.01.01 — The real server, called like a client
// webEnvironment = RANDOM_PORT starts the embedded Tomcat on a free port, so these tests open real network connections,
// unlike the MockMvc tests (05.13), which hand requests to Spring MVC inside the test. @AutoConfigureRestTestClient
// gives each test a RestTestClient already pointed at that port; expectStatus(), expectHeader(), and jsonPath() check
// what comes back.
// Nothing here runs in a test transaction: every request commits or rolls back exactly as in production, and the rows
// stay in the shared test database. Every test therefore uses SKUs and names that no other test uses.
class ProductApiIntegrationTest {

    private static final String PRODUCTS = "/api/v1/products";
    private static final String PROBLEMS = "https://catalog.example/problems/";
    private static final ParameterizedTypeReference<PageResponse<ProductResponse>> PAGE_OF_PRODUCTS =
            new ParameterizedTypeReference<>() {
            };
    private static final AtomicInteger SKU_NUMBER = new AtomicInteger();

    // A SKU no other test uses, such as "API-LIFE-1". These rows are committed, so a SKU must never repeat.
    private static String sku(String label) {
        return "API-" + label + "-" + SKU_NUMBER.incrementAndGet();
    }

    private static ProductCreateRequest newProduct(String sku, String name) {
        return new ProductCreateRequest(sku, name, "Created by ProductApiIntegrationTest.", Category.HOME,
                new BigDecimal("1890"), 12, null);
    }

    private static ProductUpdateRequest changed(ProductResponse product, String sku, String name, long version) {
        return new ProductUpdateRequest(sku, name, product.description(), product.category(), product.price(),
                product.stockQuantity(), product.imageUrl(), version);
    }

    private static ProductResponse create(RestTestClient client, ProductCreateRequest request) {
        return client.post().uri(PRODUCTS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductResponse.class).returnResult().getResponseBody();
    }

    private static ProductResponse read(RestTestClient client, long id) {
        return client.get().uri(PRODUCTS + "/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProductResponse.class).returnResult().getResponseBody();
    }

    private static List<Long> idsFound(RestTestClient client, String q) {
        return client.get().uri(PRODUCTS + "?q={q}", q)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PAGE_OF_PRODUCTS).returnResult().getResponseBody()
                .content().stream().map(ProductResponse::id).toList();
    }

    // BUILD FLOW 07.01.02 — Writes enabled (as in development)
    @Nested
    @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "catalog.writes-enabled=true")
    @AutoConfigureRestTestClient
    @Import(TestcontainersConfiguration.class)
    class WithWritesEnabled {

        @Autowired
        private RestTestClient client;

        @LocalServerPort
        private int port;

        @Autowired
        private JdbcTemplate jdbc;

        @Test
        void requestsTravelOverARealNetworkConnection() {
            // If the application had a MockMvc bean, the configured client would call MockMvc instead of the server.
            // The URL of the exchange proves that the request went to the port Tomcat opened.
            URI url = client.get().uri(PRODUCTS + "?size=1")
                    .exchange()
                    .expectStatus().isOk()
                    .returnResult().getUrl();

            assertThat(url.getScheme()).isEqualTo("http");
            assertThat(url.getPort()).isEqualTo(port);
        }

        @Test
        void aProductLivesThroughCreateReadUpdateAndDelete() {
            String sku = sku("LIFE");

            // Create: 201 Created, a Location header naming the new product, and the stored state in the body.
            EntityExchangeResult<ProductResponse> post = client.post().uri(PRODUCTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(newProduct(sku, "  Hand-thrown Planter  "))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody(ProductResponse.class).returnResult();
            ProductResponse created = post.getResponseBody();
            URI location = post.getResponseHeaders().getLocation();

            assertThat(location).hasToString(PRODUCTS + "/" + created.id());
            assertThat(created.name()).isEqualTo("Hand-thrown Planter");
            assertThat(created.price()).isEqualTo(new BigDecimal("1890.00"));
            assertThat(created.version()).isZero();

            // Read: following the Location header returns exactly what the POST returned.
            ProductResponse read = client.get().uri(location.toString())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(ProductResponse.class).returnResult().getResponseBody();
            assertThat(read).isEqualTo(created);

            // Update, sending the version just read: 200 with the new state and version 1.
            ProductUpdateRequest change = changed(created, sku, "Hand-thrown Planter, Large", created.version());
            client.put().uri(PRODUCTS + "/{id}", created.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(change)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.name").isEqualTo("Hand-thrown Planter, Large")
                    .jsonPath("$.version").isEqualTo(1);

            // The same change again is based on version 0, which is now outdated: 409, and the reason names both versions.
            client.put().uri(PRODUCTS + "/{id}", created.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(change)
                    .exchange()
                    .expectStatus().isEqualTo(409)
                    .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .expectBody()
                    .jsonPath("$.type").isEqualTo(PROBLEMS + "version-conflict")
                    .jsonPath("$.detail").isEqualTo("Product " + created.id()
                            + " is at version 1, but this change was based on version 0. Reload it and try again.");

            // Delete: 204 with an empty body. Afterwards the product is gone, for reading and for deleting.
            client.delete().uri(PRODUCTS + "/{id}", created.id())
                    .exchange()
                    .expectStatus().isNoContent()
                    .expectBody().isEmpty();
            client.get().uri(PRODUCTS + "/{id}", created.id())
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody().jsonPath("$.type").isEqualTo(PROBLEMS + "product-not-found");
            client.delete().uri(PRODUCTS + "/{id}", created.id())
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        void theJsonShapeIsTheDocumentedContract() {
            ProductResponse created = create(client, newProduct(sku("JSON"), "Contract Bowl"));

            String json = new String(client.get().uri(PRODUCTS + "/{id}", created.id())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody().returnResult().getResponseBody(), UTF_8);
            Map<String, Object> fields = JsonPath.read(json, "$");

            // Exactly these twelve fields: nothing internal leaks out, and nothing goes missing.
            assertThat(fields).containsOnlyKeys("id", "sku", "name", "description", "category", "price",
                    "stockQuantity", "inStock", "imageUrl", "createdAt", "updatedAt", "version");
            // Prices keep their two decimals in the JSON text, and timestamps are ISO-8601 in UTC (the final "Z").
            assertThat(json).contains("\"price\":1890.00");
            assertThat((String) fields.get("createdAt")).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,6})?Z");
        }

        @Test
        void clientsCannotSetTheIdVersionOrTimestamps() {
            // Fields the request record does not have are ignored ("mass assignment" protection, 05.02).
            String body = """
                    {"id": 999999, "version": 7, "createdAt": "2000-01-01T00:00:00Z", "inStock": false,
                     "sku": "%s", "name": "Mass Assignment Mug", "description": "Extra fields must be ignored.",
                     "category": "KITCHEN", "price": 990, "stockQuantity": 4}
                    """.formatted(sku("MASS"));

            ProductResponse created = client.post().uri(PRODUCTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(ProductResponse.class).returnResult().getResponseBody();

            assertThat(created.id()).isNotEqualTo(999999L);
            assertThat(created.version()).isZero();
            assertThat(created.createdAt()).isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
            assertThat(created.inStock()).isTrue();
        }

        // BUILD FLOW 07.01.03 — Errors, as a client receives them
        @Test
        void badBodiesGetProblemDetailsWithoutJavaDetails() {
            String invalid = """
                    {"sku": "bad sku", "name": " ", "description": "Text", "category": "HOME", "price": -1, "stockQuantity": 2}
                    """;
            client.post().uri(PRODUCTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(invalid)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .expectBody()
                    .jsonPath("$.type").isEqualTo(PROBLEMS + "validation-failed")
                    .jsonPath("$.errors[*].field").isEqualTo(List.of("name", "price", "sku"));

            client.post().uri(PRODUCTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"sku\": ")
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.type").isEqualTo(PROBLEMS + "malformed-request")
                    .consumeWith(result -> assertThat(new String(result.getResponseBody(), UTF_8))
                            .doesNotContain("Exception", "jackson", "java."));
        }

        @Test
        void aSecondProductWithTheSameSkuIsAConflict() {
            String sku = sku("DUP");
            create(client, newProduct(sku, "First"));

            client.post().uri(PRODUCTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(newProduct(sku, "Second"))
                    .exchange()
                    .expectStatus().isEqualTo(409)
                    .expectBody()
                    .jsonPath("$.type").isEqualTo(PROBLEMS + "duplicate-sku")
                    .jsonPath("$.detail").isEqualTo("A product with SKU '" + sku + "' already exists");
        }

        @Test
        void unknownProductsAndUnknownPathsAreNotFound() {
            client.get().uri(PRODUCTS + "/{id}", Long.MAX_VALUE)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .expectBody().jsonPath("$.type").isEqualTo(PROBLEMS + "product-not-found");

            // No endpoint at all: Spring MVC's own 404, in the same format (no "type" means about:blank).
            client.get().uri("/api/v1/no-such-endpoint")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(404)
                    .jsonPath("$.type").doesNotExist();
        }

        @Test
        void aMethodTheEndpointDoesNotSupportIsA405() {
            client.method(HttpMethod.PATCH).uri(PRODUCTS + "/{id}", 1)
                    .exchange()
                    .expectStatus().isEqualTo(405)
                    .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .expectHeader().value("Allow", allow -> assertThat(allow).contains("GET", "PUT", "DELETE"));
        }

        @Test
        void aBodyThatIsNotJsonIsA415() {
            client.post().uri(PRODUCTS)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("sku=FRM-TXT-001")
                    .exchange()
                    .expectStatus().isEqualTo(415)
                    .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
        }

        // BUILD FLOW 07.01.04 — A rollback, seen from outside
        // The update renames product B AND gives it product A's SKU. The UPDATE breaks the unique key, the exception
        // leaves the transactional service method, and the whole transaction rolls back, the new name included.
        @Test
        void aFailedUpdateChangesNothing() {
            String skuOfA = sku("RBK");
            create(client, newProduct(skuOfA, "Product A"));
            ProductResponse productB = create(client, newProduct(sku("RBK"), "Product B"));

            client.put().uri(PRODUCTS + "/{id}", productB.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(changed(productB, skuOfA, "Renamed B", productB.version()))
                    .exchange()
                    .expectStatus().isEqualTo(409)
                    .expectBody().jsonPath("$.type").isEqualTo(PROBLEMS + "duplicate-sku");

            assertThat(read(client, productB.id())).isEqualTo(productB);
        }

        // BUILD FLOW 07.01.05 — Unicode from the request to MySQL and back
        // Accents, a Devanagari word, the rupee sign, and an emoji (four bytes in UTF-8, which needs utf8mb4) travel
        // through JSON, Hibernate, and MySQL unchanged. In the query string they are percent-encoded UTF-8.
        @Test
        void unicodeTextSurvivesTheRoundTrip() {
            ProductCreateRequest request = new ProductCreateRequest(sku("UNI"), "Crème Brûlée Ramekin 🍮",
                    "Glazed in Khurja (खुर्जा); prices in ₹ include tax.", Category.KITCHEN, new BigDecimal("650"),
                    9, null);
            ProductResponse created = create(client, request);

            ProductResponse read = read(client, created.id());
            assertThat(read.name()).isEqualTo("Crème Brûlée Ramekin 🍮");
            assertThat(read.description()).isEqualTo("Glazed in Khurja (खुर्जा); prices in ₹ include tax.");
            // The search ignores accents and case: "creme brulee" finds "Crème Brûlée".
            assertThat(idsFound(client, "creme brulee ramekin 🍮")).contains(created.id());
        }

        // BUILD FLOW 07.01.06 — Races: ten requests at the same moment
        // Each test releases ten requests at once. Whichever request the database serves first, exactly one may succeed:
        // for updates, the version checks (05.10.04) stop the other nine; for creates, the unique SKU key does.
        @Test
        void simultaneousUpdatesOfTheSameVersionLetExactlyOneWin() throws Exception {
            ProductResponse product = create(client, newProduct(sku("RACE"), "Race Lamp"));

            List<String> outcomes = atTheSameTime(10, i -> client.put().uri(PRODUCTS + "/{id}", product.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(changed(product, product.sku(), "Race Lamp " + i, product.version()))
                    .exchange()
                    .expectBody().returnResult());

            assertThat(count(outcomes)).containsExactlyInAnyOrderEntriesOf(Map.of("200", 1L, "409 version-conflict", 9L));
            ProductResponse stored = read(client, product.id());
            assertThat(stored.version()).isEqualTo(1);
            assertThat(stored.name()).startsWith("Race Lamp ");
        }

        @Test
        void simultaneousCreatesWithTheSameSkuLetExactlyOneWin() throws Exception {
            String sku = sku("RACE");

            List<String> outcomes = atTheSameTime(10, i -> client.post().uri(PRODUCTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(newProduct(sku, "Race Mug " + i))
                    .exchange()
                    .expectBody().returnResult());

            assertThat(count(outcomes)).containsExactlyInAnyOrderEntriesOf(Map.of("201", 1L, "409 duplicate-sku", 9L));
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product WHERE sku = ?", Integer.class, sku)).isOne();
        }

        // Starts the requests on separate threads, waits until every thread is ready, then releases them together.
        // Each answer is described as its status, plus the problem type for errors: "200", "409 version-conflict".
        private List<String> atTheSameTime(int count, IntFunction<EntityExchangeResult<byte[]>> request)
                throws Exception {
            CountDownLatch ready = new CountDownLatch(count);
            CountDownLatch go = new CountDownLatch(1);
            try (ExecutorService threads = Executors.newFixedThreadPool(count)) {
                List<Future<EntityExchangeResult<byte[]>>> answers = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    int number = i;
                    answers.add(threads.submit(() -> {
                        ready.countDown();
                        go.await();
                        return request.apply(number);
                    }));
                }
                ready.await();
                go.countDown();
                List<String> outcomes = new ArrayList<>();
                for (Future<EntityExchangeResult<byte[]>> answer : answers) {
                    outcomes.add(describe(answer.get(30, TimeUnit.SECONDS)));
                }
                return outcomes;
            }
        }

        private static String describe(EntityExchangeResult<byte[]> result) {
            int status = result.getStatus().value();
            if (status < 400) {
                return String.valueOf(status);
            }
            String type = JsonPath.read(new String(result.getResponseBody(), UTF_8), "$.type");
            return status + " " + type.substring(PROBLEMS.length());
        }

        private static Map<String, Long> count(List<String> outcomes) {
            return outcomes.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        }
    }

    // BUILD FLOW 07.01.07 — The production profile, over HTTP
    // The real production settings (application-prod.yml) behind a real server: every write is refused with 403, reads
    // keep working, and validation still runs first (REQUEST_FLOWS, note under Flow 4).
    @Nested
    @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("prod")
    @AutoConfigureRestTestClient
    @Import(TestcontainersConfiguration.class)
    class WithProductionSettings {

        @Autowired
        private RestTestClient client;

        @Autowired
        private ProductRepository repository;

        @Test
        void everyWriteIsRefusedWhileReadsKeepWorking() {
            // Inserted through the repository, because the API itself cannot write here.
            Product existing = repository.save(new Product(sku("PRD"), "Read-only Vase", "Exists before the requests.",
                    Category.HOME, new BigDecimal("1490.00"), 5, null));
            long rowsBefore = repository.count();
            ProductResponse current = read(client, existing.getId());

            client.post().uri(PRODUCTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(newProduct(sku("PRD"), "Should not exist"))
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .expectBody().jsonPath("$.type").isEqualTo(PROBLEMS + "writes-disabled");
            client.put().uri(PRODUCTS + "/{id}", existing.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(changed(current, current.sku(), "Changed", current.version()))
                    .exchange()
                    .expectStatus().isForbidden();
            client.delete().uri(PRODUCTS + "/{id}", existing.getId())
                    .exchange()
                    .expectStatus().isForbidden();

            assertThat(repository.count()).isEqualTo(rowsBefore);
            assertThat(read(client, existing.getId())).isEqualTo(current);
            assertThat(idsFound(client, "Read-only Vase")).contains(existing.getId());
        }

        @Test
        void anInvalidWriteIsStillReportedAsInvalid() {
            client.post().uri(PRODUCTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"sku\": \"bad sku\"}")
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody().jsonPath("$.type").isEqualTo(PROBLEMS + "validation-failed");
        }
    }
}
