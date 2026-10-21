/*
 * BUILD FLOW 05.05 — ProductMapper.java
 * First introduced: Phase 05
 * Purpose: Copies data between the API's DTOs and the Product entity, explicitly and field by field.
 * Depends on: Product (04.03), ProductCreateRequest (05.02), ProductUpdateRequest (05.03), ProductResponse (05.04).
 * Used by: ProductService (05.10).
 * Runtime role: Runs inside the service's transaction for every read and write.
 */
package com.example.catalog.product;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

// BUILD FLOW 05.05.01 — Explicit mapping instead of a mapping library
// Hand-written mapping is a few lines per direction, easy to read and debug, and makes every copied field visible.
// Mapping libraries (MapStruct, ModelMapper) save typing but hide the rules this project wants to show.
// @Component makes it a bean so ProductService receives it through constructor injection.
@Component
public class ProductMapper {

    // BUILD FLOW 05.05.02 — Request to entity
    // Text is trimmed so " Linen Shirt " is stored as "Linen Shirt". Validation already ran on the raw input, so a
    // value made only of spaces was rejected by @NotBlank before it got here.
    public Product toNewEntity(ProductCreateRequest request) {
        return new Product(request.sku(), request.name().trim(), request.description().trim(), request.category(),
                twoDecimals(request.price()), request.stockQuantity(), request.imageUrl());
    }

    // Copies the new state onto an entity that the current transaction already loaded. Hibernate notices the changes
    // (dirty checking) and writes one UPDATE when the transaction flushes; no save() call is needed for that.
    public void applyUpdate(Product product, ProductUpdateRequest request) {
        product.setSku(request.sku());
        product.setName(request.name().trim());
        product.setDescription(request.description().trim());
        product.setCategory(request.category());
        product.setPrice(twoDecimals(request.price()));
        product.setStockQuantity(request.stockQuantity());
        product.setImageUrl(request.imageUrl());
    }

    // BUILD FLOW 05.05.03 — Entity to response
    public ProductResponse toResponse(Product product) {
        return new ProductResponse(product.getId(), product.getSku(), product.getName(), product.getDescription(),
                product.getCategory(), product.getPrice(), product.getStockQuantity(), product.getStockQuantity() > 0,
                product.getImageUrl(), product.getCreatedAt(), product.getUpdatedAt(), product.getVersion());
    }

    // Two decimal places, like DECIMAL(10,2): 1290 becomes 1290.00. setScale(2) without a rounding mode would throw if
    // digits had to be dropped, which @Digits(fraction = 2) already rules out.
    private static BigDecimal twoDecimals(BigDecimal price) {
        return price.setScale(2);
    }
}
