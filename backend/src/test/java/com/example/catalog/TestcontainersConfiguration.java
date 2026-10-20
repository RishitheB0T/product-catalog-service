/*
 * BUILD FLOW 04.07 — TestcontainersConfiguration.java
 * First introduced: Phase 04
 * Purpose: Gives every test that starts Spring a real MySQL 8.4.11 running in Docker, started once for the
 *          whole test run and shared by all test classes.
 * Depends on: Docker Desktop (must be running), Testcontainers 2, and Spring Boot's @ServiceConnection support.
 * Used by: Test classes that need a database: @Import(TestcontainersConfiguration.class).
 * Runtime role: Test only. Never part of the application jar.
 */
package com.example.catalog;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
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

    // BUILD FLOW 04.07.02 — @ServiceConnection
    // Spring Boot starts the container if it is not running yet, reads its random host port, database name, user,
    // and password, and configures the DataSource from them. These connection details take priority over any
    // spring.datasource.* in the YAML files, so a test can never touch the development database.
    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return MYSQL;
    }
}
