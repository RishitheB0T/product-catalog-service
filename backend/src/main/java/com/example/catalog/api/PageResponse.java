/*
 * BUILD FLOW 06.05 — PageResponse.java
 * First introduced: Phase 06
 * Purpose: The JSON envelope for one page of results: {content, page, size, totalElements, totalPages}.
 * Depends on: Spring Data's Page.
 * Used by: ProductService (05.10) builds it for search; ProductController (05.11) returns it.
 * Runtime role: Runtime step SEARCH-06.
 */
package com.example.catalog.api;

import java.util.List;

import org.springframework.data.domain.Page;

// BUILD FLOW 06.05.01 — Our own envelope instead of Spring's Page
// Spring Data's Page carries more than clients need (the Pageable, sort details, first/last flags), and its JSON form
// is not a stable contract: Spring Data logs a warning when a PageImpl is serialized as-is. This record is the
// contract, five fields that change only when we decide to change them:
//   content        the items on this page (fewer than size on the last page; empty past the end)
//   page           the zero-based page number that was requested
//   size           the requested page size
//   totalElements  how many items match the filters altogether
//   totalPages     how many pages of this size that makes (0 when nothing matches)
// It is generic (<T>), so a future list endpoint can reuse it.
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    // A record is only shallowly immutable. Copying the list means nobody can change the content afterwards.
    public PageResponse {
        content = List.copyOf(content);
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }
}
