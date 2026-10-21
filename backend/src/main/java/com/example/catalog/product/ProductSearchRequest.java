/*
 * BUILD FLOW 06.04 — ProductSearchRequest.java
 * First introduced: Phase 06
 * Purpose: The query parameters of GET /api/v1/products as one validated record: the filters (q, category, minPrice,
 *          maxPrice, inStock), the page (page, size), and the order (sort), with defaults for anything left out.
 * Depends on: Category (04.02), ProductSort (06.01), ValidPriceRange (06.03), and Bean Validation.
 * Used by: ProductController (05.11) receives it; ProductService (05.10) and ProductSpecifications (06.06) read it.
 * Runtime role: Built and validated in runtime step SEARCH-03; read in SEARCH-04.
 */
package com.example.catalog.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

// BUILD FLOW 06.04.01 — Query parameters bound to a record
// Spring MVC builds this record from the query string: it matches parameter names to component names
// (?minPrice=1000 fills minPrice), converts each text value to the component's type, and passes null for parameters
// that were not sent. Parameters with other names are ignored, and a repeated parameter uses its first value.
// @Valid on the controller parameter then checks the rules below, and every broken rule is reported in one 400
// response. A value that cannot be converted at all (?page=abc) is reported first, on its own: the rules only run
// once every value has been converted.
// All filters are optional: null means "do not filter on this".
@ValidPriceRange
public record ProductSearchRequest(
        // Contains-match on the product name. Names have at most 120 characters, so longer search text is pointless.
        @Size(max = 100)
        String q,

        Category category,

        // Inclusive bounds with the same limits as a product's price, DECIMAL(10,2). @Digits also rejects extreme
        // values such as 1E+400 before they can reach MySQL.
        @DecimalMin("0.00")
        @Digits(integer = 8, fraction = 2)
        BigDecimal minPrice,

        @DecimalMin("0.00")
        @Digits(integer = 8, fraction = 2)
        BigDecimal maxPrice,

        // true: only products in stock; false: only sold-out products; not sent: both.
        Boolean inStock,

        // Zero-based: page 0 is the first page (the storefront shows it as "page 1"). The upper limit keeps the row
        // offset (page × size, at most 1,000,000) far below the 32-bit limit at which Spring Data refuses a query, and
        // bounds the work of OFFSET, which makes MySQL read and discard every skipped row.
        @Min(0)
        @Max(10_000)
        Integer page,

        @Min(1)
        @Max(100)
        Integer size,

        ProductSort sort) {

    public static final int DEFAULT_SIZE = 20;

    // BUILD FLOW 06.04.02 — Defaults and normalization in a compact constructor
    // A compact constructor runs before the fields are assigned and may replace the incoming values. However the
    // record is created, the service never sees a null page, size, or sort, or search text padded with spaces.
    // Text made only of spaces counts as "not sent".
    public ProductSearchRequest {
        q = (q == null || q.isBlank()) ? null : q.strip();
        page = (page == null) ? 0 : page;
        size = (size == null) ? DEFAULT_SIZE : size;
        sort = (sort == null) ? ProductSort.NEWEST : sort;
    }
}
