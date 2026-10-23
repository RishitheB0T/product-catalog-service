/*
 * BUILD FLOW 04.07 — TestcontainersConfiguration.java
 * First introduced: Phase 04
 * Purpose: Gives every test that starts Spring a real MySQL 8.4.11 and (since Phase 09) a real Redis 8.10.2, both
 *          running in Docker, started once for the whole test run and shared by all test classes.
 * Depends on: Docker Desktop (must be running), Testcontainers 2, and Spring Boot's @ServiceConnection support.
 * Used by: Test classes that need a database: @Import(TestcontainersConfiguration.class).
 * Runtime role: Test only. Never part of the application jar.
 */
package com.example.catalog;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

// BUILD FLOW 04.07.01 — One container for the whole test run
// @TestConfiguration: extra beans for tests only. proxyBeanMethods = false: Spring calls the @Bean method directly,
// without the proxy it would add for @Configuration classes (not needed here, and slightly faster).
// Starting MySQL takes several seconds and about 400 MB of memory, so every Spring test context shares this single
// instance ("static" means one per JVM). Tests therefore never assume an empty table: each creates its own uniquely
// named rows, and @DataJpaTest rolls its changes back. The image tag matches infra/compose.dev.yaml; change both together.
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final MySQLContainer MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.4.11"));

    // A plain container is enough for Redis; @ServiceConnection(name = "redis") below tells Spring Boot what it is.
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:8.10.2-alpine")).withExposedPorts(6379);

    // BUILD FLOW 04.07.02 — @ServiceConnection
    // Spring Boot starts the container if it is not running yet, reads its random host port, database name, user,
    // and password, and configures the DataSource from them. These connection details take priority over any
    // spring.datasource.* in the YAML files, so a test can never touch the development database.
    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return MYSQL;
    }

    // BUILD FLOW 04.07.03 — Redis for the cache tests (added in Phase 09)
    // The same mechanism replaces spring.data.redis.*: tests talk to this container, never to the development Redis.
    // For a plain GenericContainer, Spring Boot cannot tell what runs inside, so the connection is named explicitly.
    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return REDIS;
    }

    // BUILD FLOW 04.07.04 — Small connection pools (added in Phase 09)
    // Spring keeps every distinct test configuration's application alive for reuse, and each one has its own pool of
    // MySQL connections, by default 10 kept open even when idle. With a dozen configurations that approaches MySQL's
    // limit of 151 connections (decision D-036). Here each pool keeps one idle connection and still grows to 10 when a
    // test needs them, such as the ten simultaneous requests in ProductApiIntegrationTest.
    @Bean
    DynamicPropertyRegistrar smallConnectionPools() {
        return registry -> registry.add("spring.datasource.hikari.minimum-idle", () -> 1);
    }
}
