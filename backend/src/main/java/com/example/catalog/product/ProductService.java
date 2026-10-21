/*
 * BUILD FLOW 05.10 — ProductService.java
 * First introduced: Phase 05
 * Purpose: The product rules: read, search, create, update, and delete, each inside one database transaction. It
 *          refuses writes in read-only deployments, checks versions, and translates database errors into domain errors.
 * Depends on: ProductRepository (04.04), ProductMapper (05.05), CatalogProperties (05.01), the exceptions 05.06–05.09;
 *             for search ProductSearchRequest (06.04), ProductSpecifications (06.06), and PageResponse (06.05).
 * Called by: ProductController (05.11).
 * Runtime role: The business-logic step of every product request (runtime steps READ-03, SEARCH-04 to SEARCH-06, and
 *               WRITE-04 to WRITE-06).
 */
package com.example.catalog.product;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.catalog.api.PageResponse;
import com.example.catalog.config.CatalogProperties;
import com.example.catalog.error.WritesDisabledException;

// BUILD FLOW 05.10.01 — Where transactions begin, commit, and roll back
// @Service marks a business-logic bean (a @Component with a clearer name). Spring wraps it in a proxy, and for every
// method annotated @Transactional the proxy:
//   1. BEGINS a transaction before the method runs (borrows a connection from the pool, turns off auto-commit);
//   2. COMMITS when the method returns normally;
//   3. ROLLS BACK when an unchecked exception (RuntimeException or Error) leaves the method.
// The proxy only sees calls coming from outside the bean. A call from one method of this class to another bypasses it
// ("self-invocation"), so each public method below is the complete unit of work.
// readOnly = true tells Hibernate to skip dirty checking and the MySQL driver to mark the transaction read-only.
@Service
public class ProductService {

    private final ProductRepository repository;
    private final ProductMapper mapper;
    private final CatalogProperties properties;

    // Constructor injection, as in DemoDataLoader (04.06): Spring passes in the three beans this class needs.
    public ProductService(ProductRepository repository, ProductMapper mapper, CatalogProperties properties) {
        this.repository = repository;
        this.mapper = mapper;
        this.properties = properties;
    }

    // BUILD FLOW 05.10.02 — Reading one product (runtime step READ-03)
    // The DTO is built inside the transaction, so everything it needs is loaded before the transaction ends.
    @Transactional(readOnly = true)
    public ProductResponse getProduct(long id) {
        return mapper.toResponse(findExisting(id));
    }

    // BUILD FLOW 05.10.07 — Searching (runtime steps SEARCH-04 to SEARCH-06; added in Phase 06)
    // One read-only transaction covers both queries Spring Data may run: the page itself (ORDER BY … LIMIT) and a
    // COUNT with the same WHERE clause. The count is skipped when the page itself reveals the total, for example a
    // first page that is not full. PageRequest.of takes the zero-based page, the size, and the order from the sort
    // allowlist. page.map converts each entity to a DTO while the transaction is still open.
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(ProductSearchRequest request) {
        PageRequest pageRequest = PageRequest.of(request.page(), request.size(), request.sort().toSort());
        Page<Product> page = repository.findAll(ProductSpecifications.matching(request), pageRequest);
        return PageResponse.from(page.map(mapper::toResponse));
    }

    // BUILD FLOW 05.10.03 — Creating a product
    // saveAndFlush sends the INSERT now instead of at commit, so a duplicate SKU fails inside this method, where it can
    // be translated. The exception then leaves the method, and the proxy rolls the transaction back.
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        requireWritesEnabled();
        Product saved = saveAndFlush(mapper.toNewEntity(request), request.sku());
        return mapper.toResponse(saved);
    }

    // BUILD FLOW 05.10.04 — Updating a product (runtime steps WRITE-04 to WRITE-06)
    // First check: the version the client read must still be the stored version; otherwise someone else has saved a
    // change since, and overwriting it would lose that change. Second check: Hibernate's @Version condition in the
    // UPDATE catches a change committed between our read and our write. The flush makes the UPDATE (and the version
    // increment) happen now, so the response shows the new version.
    @Transactional
    public ProductResponse updateProduct(long id, ProductUpdateRequest request) {
        requireWritesEnabled();
        Product product = findExisting(id);
        if (!product.getVersion().equals(request.version())) {
            throw new VersionConflictException(id, request.version(), product.getVersion());
        }
        mapper.applyUpdate(product, request);
        try {
            return mapper.toResponse(saveAndFlush(product, request.sku()));
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new VersionConflictException(id, request.version(), null);
        }
    }

    // BUILD FLOW 05.10.05 — Deleting a product
    // Loading first gives a clear 404 for a missing id (Spring Data's deleteById would silently do nothing). Hibernate
    // deletes with "WHERE id = ? AND version = ?", so a concurrent change also makes this fail with a conflict.
    @Transactional
    public void deleteProduct(long id) {
        requireWritesEnabled();
        Product product = findExisting(id);
        try {
            repository.delete(product);
            repository.flush();
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new VersionConflictException(id, product.getVersion(), null);
        }
    }

    // BUILD FLOW 05.10.06 — Shared steps
    private void requireWritesEnabled() {
        if (!properties.writesEnabled()) {
            throw new WritesDisabledException();
        }
    }

    private Product findExisting(long id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    // Translates a unique-SKU violation into a domain error. Any other database error propagates unchanged and still
    // rolls the transaction back. (Optimistic-lock failures are translated by updateProduct and deleteProduct, the
    // only callers where they can happen: an INSERT has no version check.)
    private Product saveAndFlush(Product product, String sku) {
        try {
            return repository.saveAndFlush(product);
        } catch (DataIntegrityViolationException e) {
            if (violatesUniqueSku(e)) {
                throw new DuplicateSkuException(sku, e);
            }
            throw e;
        }
    }

    // MySQL's own message names the violated key: "Duplicate entry 'FRM-APP-001' for key 'product.uk_product_sku'".
    private static boolean violatesUniqueSku(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains("uk_product_sku");
    }
}
