/*
 * BUILD FLOW 04.04 — ProductRepository.java
 * First introduced: Phase 04
 * Purpose: Database access for Product. We declare the interface; Spring Data JPA writes the implementation
 *          (save, findById, findAll, deleteById, count, ...) when the application starts.
 * Depends on: Product (04.03), Spring Data JPA, and the DataSource that Spring Boot configures.
 * Used by: ProductService (05.10) and the tests. Since Phase 06 it also runs the search queries, built from
 *          ProductSpecifications (06.06).
 * Runtime role: The only layer that talks to MySQL. Its calls join the caller's transaction, if there is one.
 */
package com.example.catalog.product;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

// BUILD FLOW 04.04.01 — Why this one is an interface
// The plan avoids "an interface for every class", and this is the exception with a real reason: Spring Data
// generates the implementation from the interface, so there is no class for us to write at all.
// JpaRepository<Product, Long> names the entity type and the type of its @Id. It brings about twenty ready-made
// methods, and it converts database errors into Spring's DataAccessException family
// (for example DataIntegrityViolationException), so callers do not depend on MySQL-specific exceptions.
// BUILD FLOW 04.04.03 — Queries assembled at runtime (added in Phase 06)
// JpaSpecificationExecutor<Product> adds findAll(specification, pageable): Spring Data runs one page query for any
// combination of predicates built while the application runs, plus a count query when the total is needed.
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // BUILD FLOW 04.04.02 — A derived query
    // Spring Data reads the method name: "findBy" + "Sku" becomes SELECT ... FROM product WHERE sku = ?.
    // Optional: the product may not exist, and the caller decides what that means (a 404 from Phase 05 on).
    Optional<Product> findBySku(String sku);
}
