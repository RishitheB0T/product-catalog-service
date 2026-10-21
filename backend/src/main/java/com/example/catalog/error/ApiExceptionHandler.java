/*
 * BUILD FLOW 05.12 — ApiExceptionHandler.java
 * First introduced: Phase 05
 * Purpose: Turns every error into one JSON format: RFC 9457 "Problem Details" (media type application/problem+json),
 *          with an "errors" list when individual fields or (since Phase 06) query parameters are wrong.
 * Depends on: Spring MVC's ResponseEntityExceptionHandler and ProblemDetail, the domain exceptions 05.06–05.09,
 *             Jackson 3's exception types for unreadable JSON, and Spring's binding errors for query parameters.
 * Used by: Spring MVC, whenever a controller or Spring itself (unknown URL, wrong method, bad JSON) throws.
 * Runtime role: The last step of every failed request.
 */
package com.example.catalog.error;

import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.example.catalog.product.DuplicateSkuException;
import com.example.catalog.product.ProductNotFoundException;
import com.example.catalog.product.VersionConflictException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

// BUILD FLOW 05.12.01 — One error format for the whole API
// @RestControllerAdvice: Spring applies the @ExceptionHandler methods below to every controller and writes their
// return values as JSON. Extending ResponseEntityExceptionHandler adds ready-made handling for Spring MVC's own errors
// (unknown URL, wrong HTTP method, a non-numeric id, ...), also rendered as ProblemDetail. Two of those are overridden.
// A ProblemDetail has standard fields:
//   type      a URI naming the kind of problem. Our own kinds live under https://catalog.example/problems/; ".example"
//             is reserved for documentation, so the URI never points at someone else's site. Spring's generic errors
//             use "about:blank" with the standard HTTP title (for example "Not Found").
//   title     a short summary of the kind; status: the HTTP status code; detail: what happened this time;
//   instance  the request path. Extra properties, such as "errors", appear at the top level of the JSON.
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    static final String PROBLEM_BASE = "https://catalog.example/problems/";

    // BUILD FLOW 05.12.02 — Domain errors from ProductService
    // Expected outcomes of normal use, so nothing is logged. Each message was written to be safe to show to clients.
    @ExceptionHandler(ProductNotFoundException.class)
    ProblemDetail handleNotFound(ProductNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "product-not-found", "Product not found", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DuplicateSkuException.class)
    ProblemDetail handleDuplicateSku(DuplicateSkuException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "duplicate-sku", "Duplicate SKU", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(VersionConflictException.class)
    ProblemDetail handleVersionConflict(VersionConflictException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "version-conflict", "Version conflict", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(WritesDisabledException.class)
    ProblemDetail handleWritesDisabled(WritesDisabledException ex, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "writes-disabled", "Writes disabled", ex.getMessage(), request.getRequestURI());
    }

    // BUILD FLOW 05.12.03 — Database rules that request validation did not catch
    // Validation mirrors the database constraints, so reaching this handler reveals a gap between the two. The client
    // gets a clear 400 without database details; the developer gets the full message in the log.
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Database rejected data that passed request validation: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.BAD_REQUEST, "invalid-data", "Invalid product data",
                "The data breaks a rule that the database enforces.", request.getRequestURI());
    }

    // BUILD FLOW 05.12.04 — Validation failures (@Valid)
    // Every invalid field is reported at once, sorted so the order is stable for clients and tests. Since Phase 06 the
    // same response covers search parameters, including values that could not be converted at all (05.12.08).
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        List<FieldProblem> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldProblem(error.getField(), message(error)))
                .sorted(Comparator.comparing(FieldProblem::field).thenComparing(FieldProblem::message))
                .toList();
        String detail = errors.size() == 1 ? "1 field is invalid." : errors.size() + " fields are invalid.";
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "validation-failed", "Validation failed", detail, path(request));
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    // BUILD FLOW 05.12.05 — Unreadable JSON
    // Jackson fails before validation can run: broken JSON syntax, text where a number belongs, or an unknown category.
    // Jackson's own message names internal Java classes, so the client gets a neutral explanation instead, plus the
    // offending field and, for enums such as category, the accepted values, when Jackson can tell us.
    // Jackson 3 exceptions are unchecked and carry the JSON path of the failing value (getPath()).
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "malformed-request", "Malformed request body",
                "The request body is not valid JSON for this endpoint. Check the syntax and the field types.", path(request));
        if (ex.getMostSpecificCause() instanceof JacksonException jackson && !jackson.getPath().isEmpty()) {
            body.setProperty("errors", List.of(new FieldProblem(fieldPath(jackson), describe(jackson))));
        }
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    // BUILD FLOW 05.12.06 — Anything unexpected
    // A bug or an outage: log the full stack trace for the developer, but never send it to the client.
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error for {} {}", request.getMethod(), request.getRequestURI(), ex);
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. It has been logged.");
        body.setInstance(URI.create(request.getRequestURI()));
        return body;
    }

    // BUILD FLOW 05.12.07 — Helpers
    // One field problem in the "errors" list. A record is serialized by Jackson as {"field": ..., "message": ...}.
    public record FieldProblem(String field, String message) {
    }

    private static ProblemDetail problem(HttpStatus status, String slug, String title, String detail, String path) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setType(URI.create(PROBLEM_BASE + slug));
        body.setTitle(title);
        body.setInstance(URI.create(path));
        return body;
    }

    private static String path(WebRequest request) {
        return ((ServletWebRequest) request).getRequest().getRequestURI();
    }

    // Builds "category" or "items[2].price" from Jackson's path of property names and array positions.
    private static String fieldPath(JacksonException ex) {
        StringBuilder path = new StringBuilder();
        for (JacksonException.Reference reference : ex.getPath()) {
            if (reference.getPropertyName() != null) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(reference.getPropertyName());
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        return path.toString();
    }

    private static String describe(JacksonException ex) {
        return describe(ex instanceof InvalidFormatException invalid ? invalid.getTargetType() : null);
    }

    // BUILD FLOW 05.12.08 — Query values that cannot be converted (added in Phase 06)
    // ?minPrice=abc or ?sort=popular fails while Spring converts the text, before any validation rule runs. Spring
    // records a field error that wraps the TypeMismatchException; its default message names Java classes, so it is
    // replaced with the same neutral wording as for JSON bodies. For enums the accepted values are listed as each
    // constant prints itself: Category by its name, ProductSort by its API value.
    private static String message(FieldError error) {
        if (error.contains(TypeMismatchException.class)) {
            return describe(error.unwrap(TypeMismatchException.class).getRequiredType());
        }
        return error.getDefaultMessage();
    }

    private static String describe(Class<?> targetType) {
        if (targetType != null && targetType.isEnum()) {
            return "must be one of " + Arrays.toString(targetType.getEnumConstants());
        }
        return "has a value of the wrong type or format";
    }
}
