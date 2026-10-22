-- BUILD FLOW 08.08 — benchmarks/queries/search-queries.sql
-- First introduced: Phase 08
-- Purpose: The representative search queries, exactly as MySQL received them from the application: captured with
--          MySQL's general query log (scripts/bench-db.ps1 log-on / log-show) while the API requests below were sent
--          to the backend running with the bench profile.
-- Depends on: The product table in catalog_bench (V1, and V2 from Phase 08) holding the 100,000 generated products.
-- Used by: scripts/measure-queries.ps1 (08.09), which runs each query with EXPLAIN ANALYZE and fingerprints its result.
-- Runtime role: None. Measurement input only.
--
-- Format: "-- query: <name>", then "-- request: <API request>", then the statement, ending with ";".
-- The values are written into the SQL text because MySQL Connector/J, by default, sends prepared statements that way
-- (with every value properly escaped); the server never sees a "?".

-- BUILD FLOW 08.08.01 — The storefront's home page: everything, newest first (the API's defaults)

-- query: home-newest
-- request: GET /api/v1/products
select p1_0.id,p1_0.category,p1_0.created_at,p1_0.description,p1_0.image_url,p1_0.name,p1_0.price,p1_0.sku,p1_0.stock_quantity,p1_0.updated_at,p1_0.version from product p1_0 order by p1_0.created_at desc,p1_0.id desc limit 0,20;

-- query: home-count
-- request: GET /api/v1/products (the total for "page 1 of 5,000")
select count(p1_0.id) from product p1_0;

-- BUILD FLOW 08.08.02 — A category page: newest first, or sorted by price

-- query: category-newest
-- request: GET /api/v1/products?category=HOME
select p1_0.id,p1_0.category,p1_0.created_at,p1_0.description,p1_0.image_url,p1_0.name,p1_0.price,p1_0.sku,p1_0.stock_quantity,p1_0.updated_at,p1_0.version from product p1_0 where p1_0.category='HOME' order by p1_0.created_at desc,p1_0.id desc limit 0,20;

-- query: category-count
-- request: GET /api/v1/products?category=HOME (the same count also runs for the two price sorts below)
select count(p1_0.id) from product p1_0 where p1_0.category='HOME';

-- query: category-price-asc
-- request: GET /api/v1/products?category=HOME&sort=price_asc
select p1_0.id,p1_0.category,p1_0.created_at,p1_0.description,p1_0.image_url,p1_0.name,p1_0.price,p1_0.sku,p1_0.stock_quantity,p1_0.updated_at,p1_0.version from product p1_0 where p1_0.category='HOME' order by p1_0.price,p1_0.id limit 0,20;

-- query: category-price-desc
-- request: GET /api/v1/products?category=HOME&sort=price_desc
select p1_0.id,p1_0.category,p1_0.created_at,p1_0.description,p1_0.image_url,p1_0.name,p1_0.price,p1_0.sku,p1_0.stock_quantity,p1_0.updated_at,p1_0.version from product p1_0 where p1_0.category='HOME' order by p1_0.price desc,p1_0.id desc limit 0,20;

-- BUILD FLOW 08.08.03 — A price range across all categories, in stock only, cheapest first

-- query: price-range-in-stock
-- request: GET /api/v1/products?minPrice=1000&maxPrice=2000&inStock=true&sort=price_asc
select p1_0.id,p1_0.category,p1_0.created_at,p1_0.description,p1_0.image_url,p1_0.name,p1_0.price,p1_0.sku,p1_0.stock_quantity,p1_0.updated_at,p1_0.version from product p1_0 where p1_0.price>=1000 and p1_0.price<=2000 and p1_0.stock_quantity>0 order by p1_0.price,p1_0.id limit 0,20;

-- query: price-range-in-stock-count
-- request: GET /api/v1/products?minPrice=1000&maxPrice=2000&inStock=true&sort=price_asc
select count(p1_0.id) from product p1_0 where p1_0.price>=1000 and p1_0.price<=2000 and p1_0.stock_quantity>0;

-- BUILD FLOW 08.08.04 — Name search: a common word (8,308 matches) and a word that matches nothing

-- query: name-contains-common
-- request: GET /api/v1/products?q=linen
select p1_0.id,p1_0.category,p1_0.created_at,p1_0.description,p1_0.image_url,p1_0.name,p1_0.price,p1_0.sku,p1_0.stock_quantity,p1_0.updated_at,p1_0.version from product p1_0 where p1_0.name like '%linen%' escape '!' order by p1_0.created_at desc,p1_0.id desc limit 0,20;

-- query: name-contains-common-count
-- request: GET /api/v1/products?q=linen
select count(p1_0.id) from product p1_0 where p1_0.name like '%linen%' escape '!';

-- query: name-contains-rare
-- request: GET /api/v1/products?q=zebra (no product matches, so the empty first page is the total: no count query)
select p1_0.id,p1_0.category,p1_0.created_at,p1_0.description,p1_0.image_url,p1_0.name,p1_0.price,p1_0.sku,p1_0.stock_quantity,p1_0.updated_at,p1_0.version from product p1_0 where p1_0.name like '%zebra%' escape '!' order by p1_0.created_at desc,p1_0.id desc limit 0,20;

-- BUILD FLOW 08.08.05 — The last page: how deep offsets behave

-- query: deep-page-newest
-- request: GET /api/v1/products?page=4999 (rows 99,981 to 100,000; its count is home-count)
select p1_0.id,p1_0.category,p1_0.created_at,p1_0.description,p1_0.image_url,p1_0.name,p1_0.price,p1_0.sku,p1_0.stock_quantity,p1_0.updated_at,p1_0.version from product p1_0 order by p1_0.created_at desc,p1_0.id desc limit 99980,20;
