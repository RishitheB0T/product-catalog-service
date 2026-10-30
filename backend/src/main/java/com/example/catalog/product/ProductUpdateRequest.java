/*
 * BUILD FLOW 05.03 — ProductUpdateRequest.java
 * First introduced: Phase 05
 * Purpose: The JSON body of PUT /api/v1/products/{id}: the complete new state of the product plus the version the
 *          client last read, so an update based on outdated data can be refused.
 * Depends on: Category (04.02) and the Jakarta Bean Validation annotations.
 * Used by: ProductController (05.11) receives it; ProductService (05.10) checks the version; ProductMapper (05.05)
 *          copies the fields onto the entity.
 * Runtime role: Created by Jackson from the request body, then validated before the controller method runs.
 */
package com.example.catalog.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// BUILD FLOW 05.03.01 — Full replacement plus the expected version
// PUT replaces the whole product, so every field is required, with the same rules as ProductCreateRequest. The rules
// are repeated on purpose: two short, explicit records are easier to read than a shared base type or validation groups.
// version: the value from the last GET. If the stored product has moved on, the service answers 409 Conflict rather
// than silently overwriting someone else's change (optimistic locking, decision D-027).
public record ProductUpdateRequest(
        @NotBlank
        @Size(max = 32)
        @Pattern(regexp = "^[A-Z0-9][A-Z0-9-]*$",
                message = "must use only upper-case letters, digits, and hyphens, starting with a letter or digit")
        String sku,

        @NotBlank
        @Size(max = 120)
        String name,

        @NotBlank
        @Size(max = 2000)
        String description,

        @NotNull
        Category category,

        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 8, fraction = 2)
        BigDecimal price,

        @NotNull
        @PositiveOrZero
        Integer stockQuantity,

        // The same rule as in ProductCreateRequest: a web address or one of the storefront's own photos.
        @Size(max = 500)
        @Pattern(regexp = "^(https?://\\S+|/images/products/[a-z0-9-]+\\.(jpg|png|webp))$",
                message = "must be an http or https URL, or a path under /images/products/")
        String imageUrl,

        @NotNull
        @PositiveOrZero
        Long version) {
}
