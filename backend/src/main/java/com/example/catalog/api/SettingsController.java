/*
 * BUILD FLOW 13.01 — SettingsController.java
 * First introduced: Phase 13
 * Purpose: GET /api/v1/settings tells the storefront which features this server allows; for now, whether products may
 *          be created, changed, and deleted. The storefront uses it to show or hide its admin area.
 * Depends on: CatalogProperties (05.01).
 * Called by: The storefront's useSettings hook (13.04), once per page load.
 * Runtime role: A read-only answer built from configuration; it touches neither MySQL nor Redis.
 */
package com.example.catalog.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.catalog.config.CatalogProperties;

// BUILD FLOW 13.01.01 — Telling the client, while the server keeps enforcing
// Hiding admin buttons is a courtesy, not protection: ProductService still refuses every write while the switch is
// off (403 writes-disabled), whatever a client does. This endpoint only lets the storefront avoid offering actions
// that would fail. It reveals a single yes/no that any write attempt would reveal anyway.
@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

    // The JSON is {"writesEnabled": true} or {"writesEnabled": false}.
    public record ClientSettings(boolean writesEnabled) {
    }

    private final CatalogProperties properties;

    public SettingsController(CatalogProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public ClientSettings settings() {
        return new ClientSettings(properties.writesEnabled());
    }
}
