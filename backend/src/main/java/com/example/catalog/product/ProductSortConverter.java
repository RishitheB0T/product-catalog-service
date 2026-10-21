/*
 * BUILD FLOW 06.02 — ProductSortConverter.java
 * First introduced: Phase 06
 * Purpose: Converts the text of the "sort" query parameter, such as "price_asc", into a ProductSort.
 * Depends on: ProductSort (06.01) and Spring's Converter interface.
 * Used by: Spring MVC's data binding, which picks up every Converter bean through Spring Boot's auto-configuration.
 * Runtime role: Runtime step SEARCH-03, while the query string becomes a ProductSearchRequest.
 */
package com.example.catalog.product;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

// BUILD FLOW 06.02.01 — Plugging into Spring's type conversion
// Spring converts query-string text into the type each parameter needs: "20" into an Integer, "HOME" into a Category
// (by the enum constant's name). ProductSort's API values are not its constant names, so it needs its own converter.
// As a @Component it is registered automatically for request binding.
// A blank value means "not given", as with Spring's built-in enum conversion. An unknown value makes fromApiValue
// throw; Spring records that as a type mismatch on the "sort" field, and ApiExceptionHandler (05.12) answers 400.
@Component
public class ProductSortConverter implements Converter<String, ProductSort> {

    @Override
    public ProductSort convert(String source) {
        String value = source.strip();
        return value.isEmpty() ? null : ProductSort.fromApiValue(value);
    }
}
