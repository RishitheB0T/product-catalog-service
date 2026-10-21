/*
 * BUILD FLOW 05.01 — CatalogProperties.java
 * First introduced: Phase 05
 * Purpose: Typed access to this application's own settings, the "catalog.*" keys in the YAML files. For now there
 *          is one switch: whether creating, updating, and deleting products is allowed.
 * Depends on: catalog.writes-enabled in application.yml (false), application-dev.yml (true), application-prod.yml (false).
 * Used by: ProductService (05.10), which refuses every write while the switch is off.
 * Runtime role: Bound once at startup; read on every write request.
 */
package com.example.catalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// BUILD FLOW 05.01.01 — Configuration properties as a record
// @ConfigurationProperties(prefix = "catalog") makes Spring Boot fill this record from keys that start with "catalog.".
// Relaxed binding maps catalog.writes-enabled (YAML), CATALOG_WRITES_ENABLED (environment variable), and
// --catalog.writes-enabled=true (command line) onto writesEnabled. The record's constructor is the binding target,
// so the value can never change after startup, and a missing key leaves the boolean false: the safe default.
// @ConfigurationPropertiesScan on CatalogApplication finds this record and registers it as a bean.
@ConfigurationProperties(prefix = "catalog")
public record CatalogProperties(boolean writesEnabled) {
}
