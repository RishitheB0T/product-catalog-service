-- BUILD FLOW 08.11 — V2__add_search_indexes.sql
-- First introduced: Phase 08
-- Purpose: Indexes for the storefront's search queries, each chosen from a measured EXPLAIN ANALYZE plan
--          (benchmarks/query-plans/before-v2.txt and after-v2.txt; docs/QUERY_OPTIMIZATION.md).
-- Depends on: V1__create_product_table.sql (04.01). V1 is never edited: an applied migration is history, so every
--             schema change after it is a new migration.
-- Used by: Flyway at startup and in tests; MySQL's optimizer for every search.
-- Runtime role: Schema change. Speeds up reads; every INSERT, and every UPDATE of an indexed column, also updates
--               these indexes.

-- BUILD FLOW 08.11.01 — Four indexes in one statement
-- InnoDB stores the primary key (id) at the end of every secondary index entry, so ix_product_created_at is really
-- sorted by (created_at, id): exactly the "newest" order with its tie-breaker, read backwards for DESC (decision D-032).
-- One ALTER TABLE: MySQL 8 runs a single DDL statement atomically, so either all four indexes exist or none do; MySQL
-- cannot roll back separate DDL statements the way it rolls back data changes.
-- ALGORITHM = INPLACE, LOCK = NONE: build the indexes while reads and writes continue. If MySQL could not do that, the
-- statement fails instead of silently locking the table.
ALTER TABLE product
    -- The home page and name searches, newest first: ORDER BY created_at DESC, id DESC.
    ADD INDEX ix_product_created_at (created_at),
    -- A category page, newest first: equality on category, then already in created_at order inside it.
    ADD INDEX ix_product_category_created_at (category, created_at),
    -- A category page sorted by price, in either direction; also category counts and category + price ranges.
    ADD INDEX ix_product_category_price (category, price),
    -- A price range across all categories, cheapest first.
    ADD INDEX ix_product_price (price),
    ALGORITHM = INPLACE,
    LOCK = NONE;
