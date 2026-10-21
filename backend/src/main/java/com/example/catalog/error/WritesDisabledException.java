/*
 * BUILD FLOW 05.09 — WritesDisabledException.java
 * First introduced: Phase 05
 * Purpose: Signals that this deployment is read-only, so creating, updating, and deleting is refused.
 * Depends on: Nothing.
 * Thrown by: ProductService (05.10) when catalog.writes-enabled is false (CatalogProperties, 05.01).
 * Handled by: ApiExceptionHandler (05.12), which answers 403 Forbidden.
 * Runtime role: Stops a write before any database work happens.
 */
package com.example.catalog.error;

// BUILD FLOW 05.09.01 — Read-only is a server rule, not a hidden button
// Hiding the admin buttons in the UI would not stop anyone from sending POST, PUT, or DELETE requests directly, so the
// server enforces the rule. 403 Forbidden means "understood, but not allowed here" (decision D-026).
public class WritesDisabledException extends RuntimeException {

    public WritesDisabledException() {
        super("This deployment is read-only: creating, updating, and deleting products is disabled.");
    }
}
