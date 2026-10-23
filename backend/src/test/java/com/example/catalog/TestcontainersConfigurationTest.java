/*
 * BUILD FLOW 09.04 — TestcontainersConfigurationTest.java
 * First introduced: Phase 09
 * Purpose: Checks that the shared test setup works as described: each test application keeps a single idle MySQL
 *          connection, and Redis answers from the test container.
 * Depends on: TestcontainersConfiguration (04.07), HikariCP, Spring Data Redis.
 * Called by: Maven Surefire. Docker must be running.
 * Runtime role: Test only.
 */
package com.example.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.zaxxer.hikari.HikariDataSource;

// BUILD FLOW 09.04.01 — Testing the tests' own setup
// A setting that silently did not apply would only show up later, as "too many connections" in an unrelated test.
// The same configuration as most test classes, so Spring reuses their application and this costs no startup.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TestcontainersConfigurationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private StringRedisTemplate redis;

    @Test
    void eachTestApplicationKeepsOnlyOneIdleDatabaseConnection() {
        HikariDataSource pool = (HikariDataSource) dataSource;

        assertThat(pool.getMinimumIdle()).isEqualTo(1);
        assertThat(pool.getMaximumPoolSize()).isEqualTo(10);
    }

    @Test
    void redisAnswersFromTheTestContainer() {
        redis.opsForValue().set("catalog:test:ping", "pong", Duration.ofSeconds(5));

        assertThat(redis.opsForValue().get("catalog:test:ping")).isEqualTo("pong");
    }
}
