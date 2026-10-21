/*
 * BUILD FLOW 05.13 — ProductControllerTest.java
 * First introduced: Phase 05
 * Purpose: Tests the HTTP layer on its own: routes, status codes, JSON shapes, validation, and the Problem Details
 *          error format, with ProductService replaced by a Mockito test double.
 * Depends on: ProductController (05.11), ApiExceptionHandler (05.12), the DTOs and domain exceptions, MockMvc, Mockito.
 * Called by: Maven Surefire. No database and no Docker are needed.
 * Runtime role: Test only.
 */
package com.example.catalog.product;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.catalog.error.WritesDisabledException;

// BUILD FLOW 05.13.01 — A web "slice" with a test double
// @WebMvcTest starts only the web layer: this controller, @RestControllerAdvice classes such as ApiExceptionHandler,
// JSON conversion, and validation. No database, no service. MockMvc sends simulated HTTP requests through the real
// Spring MVC machinery without opening a network port.
// @MockitoBean puts a Mockito test double in place of ProductService. given(...).willReturn(...) scripts its answers,
// so each test controls exactly what the "service" does, and the controller is tested in isolation.
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    private static final String PROBLEMS = "https://catalog.example/problems/";

    private static final String VALID_PRODUCT = """
            {"sku": "FRM-TST-001", "name": "Linen Shirt", "description": "A test shirt.", "category": "APPAREL",
             "price": 2499.50, "stockQuantity": 7, "imageUrl": null}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProductService service;

    private static ProductResponse product(long id, long version) {
        Instant time = Instant.parse("2026-10-21T05:00:00Z");
        return new ProductResponse(id, "FRM-TST-001", "Linen Shirt", "A test shirt.", Category.APPAREL,
                new BigDecimal("2499.50"), 7, true, null, time, time, version);
    }

    // BUILD FLOW 05.13.02 — GET
    @Test
    void getReturnsTheProductAsJson() throws Exception {
        given(service.getProduct(42)).willReturn(product(42, 3));

        mvc.perform(get("/api/v1/products/42"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.sku").value("FRM-TST-001"))
                .andExpect(jsonPath("$.price").value(2499.50))
                .andExpect(jsonPath("$.inStock").value(true))
                .andExpect(jsonPath("$.createdAt").value("2026-10-21T05:00:00Z"))
                .andExpect(jsonPath("$.version").value(3));
    }

    @Test
    void unknownProductIsA404ProblemDetail() throws Exception {
        given(service.getProduct(99)).willThrow(new ProductNotFoundException(99));

        mvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(PROBLEMS + "product-not-found"))
                .andExpect(jsonPath("$.title").value("Product not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("No product with id 99"))
                .andExpect(jsonPath("$.instance").value("/api/v1/products/99"));
    }

    @Test
    void nonNumericIdIsRejectedBeforeTheService() throws Exception {
        mvc.perform(get("/api/v1/products/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(service);
    }

    // BUILD FLOW 05.13.03 — POST
    @Test
    void createReturns201WithALocationHeader() throws Exception {
        given(service.createProduct(any())).willReturn(product(43, 0));

        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(VALID_PRODUCT))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/products/43"))
                .andExpect(jsonPath("$.id").value(43))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void invalidFieldsAreAllReportedAndTheServiceIsNeverCalled() throws Exception {
        String invalid = """
                {"sku": "bad sku", "name": " ", "description": "Text", "price": -1, "stockQuantity": -5}
                """;

        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(PROBLEMS + "validation-failed"))
                .andExpect(jsonPath("$.detail").value("5 fields are invalid."))
                .andExpect(jsonPath("$.errors[*].field",
                        containsInAnyOrder("category", "name", "price", "sku", "stockQuantity")));

        verifyNoInteractions(service);
    }

    @Test
    void unknownCategoryNamesTheAcceptedValues() throws Exception {
        String body = VALID_PRODUCT.replace("\"APPAREL\"", "\"GARDEN\"");

        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "malformed-request"))
                .andExpect(jsonPath("$.errors[0].field").value("category"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("must be one of [APPAREL, FOOTWEAR, BAGS, HOME, KITCHEN, STATIONERY]"));
    }

    @Test
    void brokenJsonIsA400() throws Exception {
        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("{\"sku\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "malformed-request"));
    }

    @Test
    void duplicateSkuIsA409() throws Exception {
        given(service.createProduct(any())).willThrow(new DuplicateSkuException("FRM-TST-001", null));

        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(VALID_PRODUCT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "duplicate-sku"))
                .andExpect(jsonPath("$.detail").value("A product with SKU 'FRM-TST-001' already exists"));
    }

    @Test
    void writesInAReadOnlyDeploymentAreA403() throws Exception {
        given(service.createProduct(any())).willThrow(new WritesDisabledException());

        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(VALID_PRODUCT))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "writes-disabled"));
    }

    // BUILD FLOW 05.13.04 — PUT and DELETE
    @Test
    void updateRequiresTheVersion() throws Exception {
        mvc.perform(put("/api/v1/products/42").contentType(MediaType.APPLICATION_JSON).content(VALID_PRODUCT))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("version"));

        verifyNoInteractions(service);
    }

    @Test
    void updateReturnsTheNewState() throws Exception {
        given(service.updateProduct(eq(42L), any())).willReturn(product(42, 4));

        mvc.perform(put("/api/v1/products/42").contentType(MediaType.APPLICATION_JSON).content(withVersion(3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(4));
    }

    @Test
    void staleVersionIsA409() throws Exception {
        given(service.updateProduct(eq(42L), any())).willThrow(new VersionConflictException(42, 1, 2L));

        mvc.perform(put("/api/v1/products/42").contentType(MediaType.APPLICATION_JSON).content(withVersion(1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "version-conflict"))
                .andExpect(jsonPath("$.detail").value(containsString("is at version 2")));
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/v1/products/42"))
                .andExpect(status().isNoContent());

        then(service).should().deleteProduct(42);
    }

    @Test
    void deletingAnUnknownProductIsA404() throws Exception {
        willThrow(new ProductNotFoundException(99)).given(service).deleteProduct(99);

        mvc.perform(delete("/api/v1/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "product-not-found"));
    }

    private static String withVersion(long version) {
        return VALID_PRODUCT.replace("\"imageUrl\": null}", "\"imageUrl\": null, \"version\": " + version + "}");
    }
}
