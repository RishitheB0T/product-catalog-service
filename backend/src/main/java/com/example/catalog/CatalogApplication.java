/*
 * BUILD FLOW 02.05 — CatalogApplication.java
 * First introduced: Phase 02
 * Purpose: The entry point. Starts Spring Boot, which builds the application context and
 *          starts the embedded Tomcat web server.
 * Depends on: Spring Boot auto-configuration (switched on by the starters in pom.xml) and the
 *             configuration files in src/main/resources (02.06 to 02.08).
 * Called by: The JVM when the jar starts (java -jar), .\mvnw.cmd spring-boot:run, and every
 *            @SpringBootTest test, which looks for this class to know where to start.
 * Runtime role: Runs once at startup. Afterwards, Tomcat's worker threads serve the requests.
 */
package com.example.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// BUILD FLOW 02.05.01 — @SpringBootApplication: three switches in one annotation
// An annotation (the @Name syntax) is a label that frameworks read at runtime. This one combines:
//  - @SpringBootConfiguration: this class may define configuration for Spring.
//  - @EnableAutoConfiguration: Spring Boot inspects the classpath and configures what it finds.
//    spring-boot-starter-webmvc is present, so it sets up Tomcat and Spring MVC; Actuator is
//    present, so it adds the health endpoint. Later phases get JPA, Flyway and Redis the same way.
//  - @ComponentScan: Spring scans this package (com.example.catalog) and its sub-packages for
//    classes marked @Component, @Service, @RestController and so on, and creates them.
//    That is why every class we write later lives under com.example.catalog.
// BUILD FLOW 02.05.03 — Typed settings (added in Phase 05)
// @ConfigurationPropertiesScan finds @ConfigurationProperties records under com.example.catalog, such as
// CatalogProperties (05.01), and registers them as beans filled from the YAML files and environment variables.
@SpringBootApplication
@ConfigurationPropertiesScan
public class CatalogApplication {

    // BUILD FLOW 02.05.02 — main: where the JVM starts the program
    // SpringApplication.run(...) creates the application context: Spring's container that creates
    // our objects (called beans), hands each one the other beans it needs (dependency injection,
    // first used in Phase 04), applies the configuration files and active profiles, and starts
    // Tomcat on the configured port.
    // args carries command-line options, for example --spring.profiles.active=dev.
    public static void main(String[] args) {
        SpringApplication.run(CatalogApplication.class, args);
    }
}
