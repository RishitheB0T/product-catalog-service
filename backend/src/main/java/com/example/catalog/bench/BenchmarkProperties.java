/*
 * BUILD FLOW 08.03 — BenchmarkProperties.java
 * First introduced: Phase 08
 * Purpose: Typed access to the "catalog.bench.*" settings: how many products to generate for measurements, and the
 *          seed that makes them identical on every machine.
 * Depends on: catalog.bench.* in application-bench.yml (08.01); the defaults below everywhere else.
 * Used by: BenchmarkDataSeeder (08.05).
 * Runtime role: Bound at every startup, but only read when the "bench" profile is active.
 */
package com.example.catalog.bench;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

// BUILD FLOW 08.03.01 — Settings with defaults and limits
// Like CatalogProperties (05.01), but under "catalog.bench". @ConfigurationPropertiesScan registers this record in
// every profile, so each value has a @DefaultValue: dev and prod start without setting anything here.
// @Validated checks the limits at startup, so a typo such as products: 1000000000 stops the application immediately
// with a clear message instead of filling the disk.
@ConfigurationProperties(prefix = "catalog.bench")
@Validated
public record BenchmarkProperties(
        @DefaultValue("100000") @Min(1) @Max(1_000_000) int products,
        @DefaultValue("20261022") long seed) {
}
