-- BUILD FLOW 08.13 — benchmarks/queries/index-alternatives.sql
-- First introduced: Phase 08
-- Purpose: "What if" variants of the captured queries (08.08), measured after V2: each one tells MySQL to ignore or to
--          force one index, or changes one optimizer setting, to show what that index contributes and why the
--          optimizer chose as it did. Hints never change results, only the plan.
-- Depends on: The search indexes from V2__add_search_indexes.sql (08.11) in catalog_bench.
-- Used by: scripts/measure-queries.ps1 -Label index-alternatives -QueryFile benchmarks\queries\index-alternatives.sql
-- Runtime role: None. Measurement input only; the application never sends hints.
--
-- Each query names the captured query it varies; its result must have the same fingerprint as that query's.

-- BUILD FLOW 08.13.01 — Does each index earn its place?

-- query: category-newest-without-composite
-- request: category-newest, with ix_product_category_created_at ignored
select p1_0.id,p1_0.category,p1_0.created_at,p1_0.description,p1_0.image_url,p1_0.name,p1_0.price,p1_0.sku,p1_0.stock_quantity,p1_0.updated_at,p1_0.version from product p1_0 IGNORE INDEX (ix_product_category_created_at) where p1_0.category='HOME' order by p1_0.created_at desc,p1_0.id desc limit 0,20;

-- query: name-contains-common-without-created-at
-- request: name-contains-common, with ix_product_created_at ignored
select p1_0.id,p1_0.category,p1_0.created_at,p1_0.description,p1_0.image_url,p1_0.name,p1_0.price,p1_0.sku,p1_0.stock_quantity,p1_0.updated_at,p1_0.version from product p1_0 IGNORE INDEX (ix_product_created_at) where p1_0.name like '%linen%' escape '!' order by p1_0.created_at desc,p1_0.id desc limit 0,20;

-- BUILD FLOW 08.13.02 — The rare-word search that V2 made slower

-- query: name-contains-rare-without-created-at
-- request: name-contains-rare, with ix_product_created_at ignored (the plan before V2)
select p1_0.id,p1_0.category,p1_0.created_at,p1_0.description,p1_0.image_url,p1_0.name,p1_0.price,p1_0.sku,p1_0.stock_quantity,p1_0.updated_at,p1_0.version from product p1_0 IGNORE INDEX (ix_product_created_at) where p1_0.name like '%zebra%' escape '!' order by p1_0.created_at desc,p1_0.id desc limit 0,20;

-- query: name-contains-rare-no-ordering-preference
-- request: name-contains-rare, with the optimizer setting prefer_ordering_index switched off for this statement
select /*+ SET_VAR(optimizer_switch = 'prefer_ordering_index=off') */ p1_0.id,p1_0.category,p1_0.created_at,p1_0.description,p1_0.image_url,p1_0.name,p1_0.price,p1_0.sku,p1_0.stock_quantity,p1_0.updated_at,p1_0.version from product p1_0 where p1_0.name like '%zebra%' escape '!' order by p1_0.created_at desc,p1_0.id desc limit 0,20;

-- BUILD FLOW 08.13.03 — Why the deep page does not use the index

-- query: deep-page-newest-forced-index
-- request: deep-page-newest, forced to read ix_product_created_at
select p1_0.id,p1_0.category,p1_0.created_at,p1_0.description,p1_0.image_url,p1_0.name,p1_0.price,p1_0.sku,p1_0.stock_quantity,p1_0.updated_at,p1_0.version from product p1_0 FORCE INDEX (ix_product_created_at) order by p1_0.created_at desc,p1_0.id desc limit 99980,20;
