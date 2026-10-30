/*
 * BUILD FLOW 05.02 — ProductCreateRequest.java
 * First introduced: Phase 05
 * Purpose: The JSON body of POST /api/v1/products, with the rules a new product must pass.
 * Depends on: Category (04.02) and the Jakarta Bean Validation annotations (Hibernate Validator, from
 *             spring-boot-starter-validation).
 * Used by: ProductController (05.11) receives it; ProductMapper (05.05) turns it into a Product entity.
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

// BUILD FLOW 05.02.01 — A DTO as a Java record
// A record is a compact, immutable data class: Java generates the constructor, the accessors (sku(), name(), ...),
// equals, hashCode, and toString. As a DTO (data transfer object) it states exactly what a client may send. Fields a
// client must not set, such as id, version, and the timestamps, do not exist here, so they cannot be smuggled in
// ("mass assignment" protection by design); unknown JSON fields are ignored.
// The annotations are Bean Validation rules, checked because the controller parameter is marked @Valid. They mirror
// the database constraints in V1 (04.01), so bad input gets a clear 400 response instead of a database error.
public record ProductCreateRequest(
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

        // Matches DECIMAL(10,2): at most 8 digits before the decimal point and 2 after, never negative.
        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 8, fraction = 2)
        BigDecimal price,

        // Integer rather than int, so a missing value arrives as null and @NotNull can report it.
        @NotNull
        @PositiveOrZero
        Integer stockQuantity,

        // Optional. An http or https URL, or (since Phase 12) a path to one of the storefront's own photos, such as
        // /images/products/frm-app-003.jpg. Nothing else: schemes such as javascript: or data: could be abused when a
        // page renders them, and the path allows no dots or slashes in the name, so it cannot climb out of the folder.
        @Size(max = 500)
        @Pattern(regexp = "^(https?://\\S+|/images/products/[a-z0-9-]+\\.(jpg|png|webp))$",
                message = "must be an http or https URL, or a path under /images/products/")
        String imageUrl) {
}
