/*
 * BUILD FLOW 04.03 — Product.java
 * First introduced: Phase 04
 * Purpose: The JPA entity for one row of the product table. Hibernate copies column values into these fields
 *          when reading, and generates INSERT and UPDATE statements from them when writing.
 * Depends on: The product table created by V1__create_product_table.sql (04.01) and Category (04.02).
 * Used by: ProductRepository (04.04) and the tests now; ProductService and the DTO mapper from Phase 05.
 * Runtime role: Lives inside a persistence context, the unit of work of one transaction. It is never sent to
 *               clients directly: from Phase 05 the API returns DTOs built from it.
 */
package com.example.catalog.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

// BUILD FLOW 04.03.01 — Mapping the class to the table
// @Entity: Hibernate manages instances of this class and maps them to rows. @Table names the table explicitly
// instead of relying on naming rules. The jakarta.persistence annotations are the JPA standard; Hibernate 7 is
// the implementation behind them.
@Entity
@Table(name = "product")
public class Product {

    // BUILD FLOW 04.03.02 — Identity and columns
    // @Id marks the primary key. GenerationType.IDENTITY means MySQL's AUTO_INCREMENT assigns it during the
    // INSERT, so a new Product has id == null until it is saved. (Hibernate must therefore run each INSERT
    // immediately to learn the id, which rules out batching inserts; that is fine for this catalog.)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column describes the column: its name, NOT NULL, and maximum length. Flyway created the real columns;
    // with ddl-auto=validate (application.yml), Hibernate compares these mappings with the database at startup
    // and refuses to start on a mismatch such as a missing column or an incompatible type.
    @Column(name = "sku", nullable = false, length = 32)
    private String sku;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    // EnumType.STRING stores the enum's name ("HOME"). The default, ORDINAL, would store its position (3),
    // which silently changes meaning if anyone reorders the enum.
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private Category category;

    // BigDecimal: exact decimal arithmetic for money, matching DECIMAL(10,2) in MySQL.
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // Instant: a point on the UTC time line, independent of any time zone. It is stored in the DATETIME(6)
    // columns as UTC because application.yml sets hibernate.jdbc.time_zone=UTC (the JVM itself may run in IST).
    // updatable = false: Hibernate never includes created_at in an UPDATE statement.
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // BUILD FLOW 04.03.03 — Optimistic locking
    // @Version: Hibernate adds "AND version = <the value it read>" to every UPDATE and DELETE, and increments the
    // value when the statement succeeds. If another transaction changed the row in between, no row matches,
    // Hibernate throws, and Spring reports ObjectOptimisticLockingFailureException; Phase 05 turns that into an
    // HTTP 409. The wrapper type Long lets null mean "never saved".
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // BUILD FLOW 04.03.04 — Constructors
    // JPA needs a constructor without arguments to create objects when it reads rows. "protected" keeps it out of
    // everyday code, which uses the full constructor below.
    protected Product() {
    }

    public Product(String sku, String name, String description, Category category, BigDecimal price,
            int stockQuantity, String imageUrl) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.imageUrl = imageUrl;
    }

    // BUILD FLOW 04.03.05 — Timestamps from lifecycle callbacks
    // JPA calls the @PrePersist method just before the INSERT and the @PreUpdate method just before an UPDATE.
    // Truncating to microseconds matches DATETIME(6), so the value in memory equals the stored value (MySQL would
    // otherwise round away the extra nanoseconds and the two would differ).
    @PrePersist
    void onCreate() {
        Instant now = now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = now();
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    // BUILD FLOW 04.03.06 — Accessors
    // Plain getters and setters instead of Lombok, so every method is visible. There are no setters for id,
    // createdAt, updatedAt, and version: only the database, the callbacks above, and Hibernate set those.
    // equals and hashCode are deliberately not overridden: entities are compared by object identity here, and an
    // equals based on id would treat all unsaved products (id == null) as equal.
    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
