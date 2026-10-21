/*
 * BUILD FLOW 06.06 — ProductSpecifications.java
 * First introduced: Phase 06
 * Purpose: Turns the search filters into a WHERE clause: one small predicate per filter, joined with AND. Values always
 *          travel as bound parameters, never as SQL text.
 * Depends on: Product (04.03), Category (04.02), ProductSearchRequest (06.04), Spring Data JPA's Specification, and
 *             the JPA Criteria API.
 * Used by: ProductService.searchProducts (05.10).
 * Runtime role: Runtime step SEARCH-04: the WHERE clause of both the page query and the count query.
 */
package com.example.catalog.product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

// BUILD FLOW 06.06.01 — What a Specification is
// A Specification<Product> is a function (root, query, builder) -> predicate. "root" stands for the product row being
// tested, and "builder" (the JPA CriteriaBuilder) creates conditions such as "price >= ?". Hibernate turns the result
// into SQL with every value sent as a bound parameter, like the "?" placeholders of a PreparedStatement, so search
// text such as  x' OR '1'='1  is only ever a value to compare, never SQL.
// Five optional filters allow 32 combinations; small predicates combined at runtime replace 32 query methods.
// Attribute names ("name", "price") are the entity's Java field names, not column names. A typo would only fail when
// the query runs, so ProductSearchTest (06.07) runs every predicate against MySQL.
final class ProductSpecifications {

    // The escape character for LIKE patterns (see escapeLike below).
    static final char LIKE_ESCAPE = '!';

    private ProductSpecifications() {
    }

    // BUILD FLOW 06.06.02 — Only the filters that were sent
    // Each filter that is present adds one predicate, and allOf joins them with AND. With no filters at all, allOf
    // returns an unrestricted specification and the query has no WHERE clause.
    static Specification<Product> matching(ProductSearchRequest request) {
        List<Specification<Product>> filters = new ArrayList<>();
        if (request.q() != null) {
            filters.add(nameContains(request.q()));
        }
        if (request.category() != null) {
            filters.add(inCategory(request.category()));
        }
        if (request.minPrice() != null) {
            filters.add(priceAtLeast(request.minPrice()));
        }
        if (request.maxPrice() != null) {
            filters.add(priceAtMost(request.maxPrice()));
        }
        if (request.inStock() != null) {
            filters.add(request.inStock() ? inStock() : soldOut());
        }
        return Specification.allOf(filters);
    }

    // BUILD FLOW 06.06.03 — The predicates
    // "Contains" is LIKE '%text%'. The name column's collation, utf8mb4_0900_ai_ci (V1, 04.01), compares without regard
    // to case or accents, so "LINEN" finds "Linen" and "creme" finds "Crème" without LOWER(), which would only add work.
    // The leading % means an ordinary index cannot narrow down the rows; Phase 08 measures what that costs.
    static Specification<Product> nameContains(String text) {
        String pattern = "%" + escapeLike(text) + "%";
        return (root, query, builder) -> builder.like(root.get("name"), pattern, LIKE_ESCAPE);
    }

    static Specification<Product> inCategory(Category category) {
        return (root, query, builder) -> builder.equal(root.get("category"), category);
    }

    static Specification<Product> priceAtLeast(BigDecimal minPrice) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    static Specification<Product> priceAtMost(BigDecimal maxPrice) {
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    static Specification<Product> inStock() {
        return (root, query, builder) -> builder.greaterThan(root.get("stockQuantity"), 0);
    }

    static Specification<Product> soldOut() {
        return (root, query, builder) -> builder.equal(root.get("stockQuantity"), 0);
    }

    // BUILD FLOW 06.06.04 — Wildcards in the search text are matched literally
    // In a LIKE pattern, % matches any run of characters and _ matches any single character. Left alone, a search for
    // "100%" would find every name containing "100", and a search for "_" would find every product. Each special
    // character therefore gets the escape character in front of it. The escape character itself is doubled first;
    // otherwise a "!" typed by the user would escape whatever follows it. "!" is used instead of the usual backslash
    // because MySQL also treats a backslash as special inside string literals, which makes it easy to get wrong.
    static String escapeLike(String text) {
        return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
