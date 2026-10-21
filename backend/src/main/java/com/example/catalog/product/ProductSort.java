/*
 * BUILD FLOW 06.01 — ProductSort.java
 * First introduced: Phase 06
 * Purpose: The sort allowlist for search. Each value the API accepts (newest, price_asc, price_desc, name_asc) maps to
 *          an order defined here in code, and every order ends with id as the tie-breaker.
 * Depends on: Product's attribute names (04.03) and Spring Data's Sort.
 * Used by: ProductSortConverter (06.02) parses it; ProductSearchRequest (06.04) holds it; ProductService (05.10) turns
 *          it into the ORDER BY of the search query.
 * Runtime role: Runtime steps SEARCH-03 (reading ?sort=) and SEARCH-04 (ordering the query).
 */
package com.example.catalog.product;

import java.util.Arrays;

import org.springframework.data.domain.Sort;

// BUILD FLOW 06.01.01 — An allowlist, not a column name from the URL
// A client picks one of four names; it never sends a column name. Each name maps to an order written below, so no
// text from the URL can reach the ORDER BY clause, and only orders we planned for (and will index in Phase 08) exist.
// Spring Data's generic "?sort=price,desc" parameter would instead let a client sort by any entity attribute,
// including long text columns that no index can help with.
public enum ProductSort {

    // BUILD FLOW 06.01.02 — Why every order ends with id
    // Rows with equal values (two products at the same price; the demo products, which were all created in the same
    // instant) may come back in any order, and that order can change from one query to the next. Every page is a
    // separate query, so a tied row could appear on page 1 and again on page 2 while another row is never shown.
    // id is unique, so adding it gives every row exactly one position. It runs in the same direction as the main
    // column, so that a single index can serve the whole order (InnoDB stores the primary key in every secondary
    // index; Phase 08 measures this).
    NEWEST("newest", Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))),
    PRICE_ASC("price_asc", Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"))),
    PRICE_DESC("price_desc", Sort.by(Sort.Order.desc("price"), Sort.Order.desc("id"))),
    NAME_ASC("name_asc", Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")));

    private final String apiValue;
    private final Sort order;

    ProductSort(String apiValue, Sort order) {
        this.apiValue = apiValue;
        this.order = order;
    }

    // BUILD FLOW 06.01.03 — Between the API value and the order
    // An exact, case-sensitive match. An unknown value throws; Spring reports that as a 400 listing the accepted values.
    public static ProductSort fromApiValue(String value) {
        return Arrays.stream(values())
                .filter(sort -> sort.apiValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown sort value: " + value));
    }

    public Sort toSort() {
        return order;
    }

    // The API value, so a list of accepted values reads [newest, price_asc, price_desc, name_asc] in error responses.
    @Override
    public String toString() {
        return apiValue;
    }
}
