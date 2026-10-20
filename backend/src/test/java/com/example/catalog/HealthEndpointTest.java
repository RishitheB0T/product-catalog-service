/*
 * BUILD FLOW 02.09 — HealthEndpointTest.java
 * First introduced: Phase 02
 * Purpose: Prove that the application starts, answers a real HTTP health check, and that each
 *          configuration (base, dev, prod) exposes exactly the operational information we intend.
 * Depends on: CatalogApplication (02.05); application.yml, application-dev.yml and
 *             application-prod.yml (02.06 to 02.08); JUnit 6, AssertJ and Spring Boot's test
 *             support from the test starters in pom.xml; the JDK's built-in java.net.http.HttpClient;
 *             since Phase 04, TestcontainersConfiguration (04.07) for the database every context now needs.
 * Called by: Maven Surefire during .\mvnw.cmd test or verify (and by CI from Phase 14).
 * Runtime role: Test only. Test classes are never packaged into the application jar.
 */
package com.example.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

// BUILD FLOW 02.09.01 — One outer class, one nested class per configuration
// @Nested (from JUnit) marks an inner class whose tests run as a group. Each nested class below
// carries its own @SpringBootTest, so Spring starts a separate application for each: one with no
// profile, one with "dev", one with "prod". The same HTTP request is checked against all three.
// Since Phase 04 the application needs a database to start, so each nested class also imports the
// shared MySQL test container (@Import(TestcontainersConfiguration.class)); health then includes "db".
class HealthEndpointTest {

    // BUILD FLOW 02.09.02 — A small HTTP helper using the JDK's own client
    // The tests call the application over a real network socket, like curl would, so they cover
    // Tomcat, Spring MVC, Actuator and the YAML files together. HttpClient ships with Java, so no
    // extra test library is needed. "static final" means one shared client that never changes.
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    // Sends GET http://localhost:<port><path> and returns the status code and the body as text.
    // "throws IOException, InterruptedException": a network call can fail or be interrupted;
    // declaring it lets JUnit report such a problem as a failed test.
    static HttpResponse<String> get(int port, String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // BUILD FLOW 02.09.03 — Base configuration (no profile active): the safe defaults
    // WebEnvironment.RANDOM_PORT starts the real embedded Tomcat on a free port chosen at startup,
    // so tests never collide with an application already running on 8080 or 8081.
    @Nested
    @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
    @Import(TestcontainersConfiguration.class)
    class BaseConfiguration {

        // Spring injects the port Tomcat actually received.
        @LocalServerPort
        int port;

        @Test
        void healthIsUpWithoutComponentDetails() throws Exception {
            HttpResponse<String> response = get(port, "/actuator/health");

            assertThat(response.statusCode()).isEqualTo(200);
            // Actual body: {"groups":["liveness","readiness"],"status":"UP"}. Spring Boot 4 enables
            // the liveness/readiness probe groups automatically and lists their names. Group names
            // are harmless; what must stay hidden are the components and their details.
            assertThat(response.body())
                    .contains("\"status\":\"UP\"")
                    .doesNotContain("\"components\"")
                    .doesNotContain("\"details\"");
        }

        @Test
        void probeGroupsAnswerOnTheirOwnUrls() throws Exception {
            // Liveness: "is the process alive?". Readiness: "can it take traffic right now?".
            // The production containers in Phase 14 can use these URLs as health checks.
            for (String probe : new String[] {"/actuator/health/liveness", "/actuator/health/readiness"}) {
                HttpResponse<String> response = get(port, probe);
                assertThat(response.statusCode()).as(probe).isEqualTo(200);
                assertThat(response.body()).as(probe).isEqualTo("{\"status\":\"UP\"}");
            }
        }

        @Test
        void internalEndpointsAreNotExposed() throws Exception {
            // These endpoints exist inside Actuator but are not exposed over HTTP, so the server
            // answers 404 Not Found, exactly as it would for a URL that was never defined.
            assertThat(get(port, "/actuator/env").statusCode()).isEqualTo(404);
            assertThat(get(port, "/actuator/beans").statusCode()).isEqualTo(404);
        }
    }

    // BUILD FLOW 02.09.04 — dev profile: details are shown
    // @ActiveProfiles("dev") makes Spring load application-dev.yml on top of application.yml.
    // Its server.port (8081) does not apply here: RANDOM_PORT overrides the port in tests.
    @Nested
    @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("dev")
    @Import(TestcontainersConfiguration.class)
    class DevProfile {

        @LocalServerPort
        int port;

        @Test
        void healthShowsComponentDetails() throws Exception {
            HttpResponse<String> response = get(port, "/actuator/health");

            assertThat(response.statusCode()).isEqualTo(200);
            // "components" only appears when details are shown (show-details: always).
            assertThat(response.body()).contains("\"status\":\"UP\"").contains("\"components\"");
        }
    }

    // BUILD FLOW 02.09.05 — prod profile: as locked down as the base settings
    @Nested
    @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("prod")
    @Import(TestcontainersConfiguration.class)
    class ProdProfile {

        @LocalServerPort
        int port;

        @Test
        void healthIsUpWithoutComponentDetails() throws Exception {
            HttpResponse<String> response = get(port, "/actuator/health");

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body())
                    .contains("\"status\":\"UP\"")
                    .doesNotContain("\"components\"")
                    .doesNotContain("\"details\"");
        }

        @Test
        void internalEndpointsAreNotExposed() throws Exception {
            assertThat(get(port, "/actuator/env").statusCode()).isEqualTo(404);
        }
    }
}
