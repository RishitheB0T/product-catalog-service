/*
 * BUILD FLOW 04.02 — Category.java
 * First introduced: Phase 04
 * Purpose: The fixed set of product categories, as a Java enum.
 * Depends on: Nothing.
 * Used by: Product (04.03) and the demo data (04.05); from Phase 05 also request validation and the API.
 * Runtime role: Every category value in Java comes from this list, and the database accepts only these values.
 */
package com.example.catalog.product;

// BUILD FLOW 04.02.01 — Why an enum, and not a categories table
// An enum is a type with a fixed list of named values. The catalog has a small, stable set of categories, so a
// separate table plus a management service would be overkill (plan §7). The same list appears in the database
// as the CHECK constraint ck_product_category (V1 migration), so invalid values are rejected in both places.
// Adding a category therefore means changing this enum AND writing a new migration that updates the constraint.
public enum Category {
    APPAREL,
    FOOTWEAR,
    BAGS,
    HOME,
    KITCHEN,
    STATIONERY
}
