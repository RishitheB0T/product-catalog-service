/*
 * BUILD FLOW 05.11 — ProductController.java
 * First introduced: Phase 05
 * Purpose: The HTTP endpoints for single products: GET, POST, PUT, and DELETE under /api/v1/products.
 * Depends on: ProductService (05.10) and the DTOs 05.02–05.04.
 * Called by: Spring MVC's DispatcherServlet when a matching request arrives (through the Vite proxy in development
 *            and Nginx in production).
 * Runtime role: The HTTP step of every product request (runtime steps READ-02, WRITE-03, and the final response).
 */
package com.example.catalog.product;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// BUILD FLOW 05.11.01 — HTTP in, HTTP out, nothing else
// @RestController = @Controller + @ResponseBody: return values are written to the response as JSON by Jackson.
// @RequestMapping: every route below starts with /api/v1/products; "v1" leaves room for an incompatible v2 later.
// The controller parses and validates input, calls exactly one service method, and chooses the status code. It has
// no business rules and no transactions; failures become responses in ApiExceptionHandler (05.12).
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    // BUILD FLOW 05.11.02 — GET /api/v1/products/{id} (runtime step READ-02)
    // @PathVariable copies {id} from the URL into the parameter. A non-numeric id is rejected with 400 before this
    // method runs. Returning the DTO itself means 200 OK.
    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable long id) {
        return service.getProduct(id);
    }

    // BUILD FLOW 05.11.03 — POST /api/v1/products
    // @RequestBody: Jackson builds a ProductCreateRequest from the JSON body. @Valid: Bean Validation checks it first;
    // on failure Spring throws MethodArgumentNotValidException, which becomes a 400 response listing the bad fields.
    // 201 Created comes with a Location header naming the new product. It is a path, not a full URL, so it stays
    // correct behind the Nginx proxy (Phase 14); clients resolve it against the URL they called.
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        ProductResponse created = service.createProduct(request);
        return ResponseEntity.created(URI.create("/api/v1/products/" + created.id())).body(created);
    }

    // BUILD FLOW 05.11.04 — PUT /api/v1/products/{id} (runtime step WRITE-03)
    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable long id, @Valid @RequestBody ProductUpdateRequest request) {
        return service.updateProduct(id, request);
    }

    // BUILD FLOW 05.11.05 — DELETE /api/v1/products/{id}
    // 204 No Content: the product is gone and there is nothing to send back.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable long id) {
        service.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
