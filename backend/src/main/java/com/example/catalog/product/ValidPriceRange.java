/*
 * BUILD FLOW 06.03 — ValidPriceRange.java
 * First introduced: Phase 06
 * Purpose: A custom Bean Validation rule for search: maxPrice must not be lower than minPrice. It concerns two fields
 *          together, which single-field annotations such as @DecimalMin cannot express.
 * Depends on: ProductSearchRequest (06.04) and the Jakarta Bean Validation API.
 * Used by: ProductSearchRequest carries the annotation; Hibernate Validator runs the check during @Valid.
 * Runtime role: Runtime step SEARCH-03.
 */
package com.example.catalog.product;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

// BUILD FLOW 06.03.01 — The annotation
// @Constraint names the class that performs the check. message, groups, and payload are required by the Bean
// Validation specification on every constraint annotation. @Target(TYPE): it goes on the record itself, because the
// check needs two of its fields at once. RUNTIME retention lets the validator find it while the application runs.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPriceRange.Validator.class)
public @interface ValidPriceRange {

    String message() default "must be greater than or equal to minPrice";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    // BUILD FLOW 06.03.02 — The check
    // A missing bound is fine: each bound is optional, and an open range is valid. compareTo compares the values
    // (BigDecimal's equals would also compare the scale, so 1000 and 1000.00 would differ). The error is attached to
    // maxPrice, so the client sees {"field": "maxPrice", ...} like any other field error.
    class Validator implements ConstraintValidator<ValidPriceRange, ProductSearchRequest> {

        @Override
        public boolean isValid(ProductSearchRequest request, ConstraintValidatorContext context) {
            if (request.minPrice() == null || request.maxPrice() == null
                    || request.maxPrice().compareTo(request.minPrice()) >= 0) {
                return true;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("maxPrice")
                    .addConstraintViolation();
            return false;
        }
    }
}
