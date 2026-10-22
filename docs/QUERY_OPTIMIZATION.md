<!--
BUILD FLOW 08.15 — docs/QUERY_OPTIMIZATION.md
First introduced: Phase 08
Purpose: How search was measured and then optimized: the dataset, the captured SQL, the plans before and after the
         V2 indexes, what each index costs, what did not get faster (and why), and how to reproduce every number.
Depends on: benchmarks/queries/*.sql, benchmarks/query-plans/*.txt, V2__add_search_indexes.sql, scripts/bench-db.ps1,
            scripts/measure-queries.ps1.
Read by: Anyone asking "why these indexes?", including interviewers.
Runtime role: None. Documentation only.
-->

# Query optimization — measured, not guessed

<!-- BUILD FLOW 08.15.01 — Summary -->
## Summary

On 100,000 generated products, four indexes (migration `V2`) made every storefront page query **73 to 483 times
faster**: from 37–49 ms to 0.08–0.66 ms inside MySQL, with exactly the same results. They cost 12–18 MB of index
space. Three kinds of query did not improve: counts that must visit most rows, the deepest page, and, slower than
before, a name search for a word that matches nothing (44.5 → 193 ms). Every number comes from the files in
[`benchmarks/query-plans/`](../benchmarks/query-plans/), measured on 2026-10-22.

| Query (API request) | Before | After | Faster | How MySQL answers after V2 |
|---|---:|---:|---:|---|
| home-newest (`GET /api/v1/products`) | 42.2 ms | 0.087 ms | 483× | Reads `ix_product_created_at` backwards: 20 entries |
| category-newest (`?category=HOME`) | 37.9 ms | 0.136 ms | 279× | Looks up `HOME` in `ix_product_category_created_at`, backwards: 20 entries |
| category-price-asc (`?category=HOME&sort=price_asc`) | 37.8 ms | 0.084 ms | 451× | `ix_product_category_price`, forwards |
| category-price-desc (`?category=HOME&sort=price_desc`) | 37.1 ms | 0.117 ms | 317× | The same index, backwards |
| price-range-in-stock (`?minPrice=1000&maxPrice=2000&inStock=true&sort=price_asc`) | 41.3 ms | 0.112 ms | 369× | Range scan on `ix_product_price` from 1,000: 22 entries, 2 sold-out ones skipped |
| name-contains-common (`?q=linen`) | 48.6 ms | 0.664 ms | 73× | Reads `ix_product_created_at` backwards until 20 names match: 258 entries |
| category-count | 22.4 ms | 3.89 ms | 5.8× | Counts the 19,878 `HOME` entries of a category index without reading the table |
| price-range-in-stock-count | 25.8 ms | 22.1 ms | 1.2× | Range scan on `ix_product_price` (11,509 entries), each checked for stock in the table |
| home-count | 7.96 ms | 8.12 ms | same plan | InnoDB counts the whole table |
| name-contains-common-count | 30.0 ms | 33.3 ms | same plan | Full table scan |
| deep-page-newest (`?page=4999`) | 90.7 ms | 105 ms | same plan | Full table scan and sort |
| name-contains-rare (`?q=zebra`) | 44.5 ms | **193 ms** | **4.3× slower** | Reads all 100,000 entries of `ix_product_created_at`, finding nothing |

Times are medians of three warm `EXPLAIN ANALYZE` runs, measured inside MySQL. The same plan varies by a few
milliseconds from run to run: deep-page-newest took 90.7 ms before and 105 ms after with an identical plan.

<!-- BUILD FLOW 08.15.02 — Method -->
## How the numbers were produced

1. **A separate database with realistic data.** The `bench` profile (decision D-037) runs the application against
   `catalog_bench`, a second database in the development MySQL 8.4.11 container (128 MB buffer pool). It holds 100,000
   products from `BenchmarkProductGenerator` with seed 20261022 (D-038):
   - six categories with uneven shares (24, 15, 13, 20, 16, and 12 %);
   - prices from ₹90 to ₹18,990, all ending in 90;
   - 7 % sold out;
   - creation times spread over two years.

   The same seed produces the same data on any machine. A test pins the dataset's fingerprint, and the three loads
   whose queries were measured in this phase gave identical results. The table is 20.5 MB, so everything fits in memory: these
   numbers measure CPU and memory work, not disk reads.
2. **The SQL the application really sends.** MySQL's general query log recorded every statement while eight typical
   API requests were sent to the bench server (`scripts/bench-db.ps1 log-on`, `log-show`, `log-off`). That gave 15
   statements, 12 of them distinct, copied verbatim into
   [`benchmarks/queries/search-queries.sql`](../benchmarks/queries/search-queries.sql). The values appear inside the
   SQL because MySQL Connector/J writes them into the statement text, escaped, before sending it.
3. **`EXPLAIN ANALYZE`, warm, three times.** `scripts/measure-queries.ps1` first refreshes the table's statistics
   (`ANALYZE TABLE`). Then, for each query, it runs one warm-up execution, three `EXPLAIN ANALYZE` executions (the
   median counts), and one final execution whose complete result it fingerprints with SHA-256.
4. **The same results, proven.** After V2, every query's fingerprint equals its fingerprint before V2: the same rows,
   values, and order. The plans changed completely, so this is also a test of the `id` tie-breaker (D-032): without
   a total order, a different plan may return tied rows in a different order.
5. **"What if" checks.** [`benchmarks/queries/index-alternatives.sql`](../benchmarks/queries/index-alternatives.sql)
   repeats some queries with an index hint: `IGNORE INDEX`, `FORCE INDEX`, or a per-statement optimizer setting.
   Each one shows what a single index contributes, without dropping anything. Hints change plans, never results:
   each fingerprint matches its original query's.

<!-- BUILD FLOW 08.15.03 — Reading a plan -->
## Reading `EXPLAIN ANALYZE`

The home page before V2 ([`before-v2.txt`](../benchmarks/query-plans/before-v2.txt)):

```text
-> Limit: 20 row(s)  (cost=10208 rows=20) (actual time=42.2..42.2 rows=20 loops=1)
    -> Sort: p1_0.created_at DESC, p1_0.id DESC, limit input to 20 row(s) per chunk  (cost=10208 rows=98793) (actual time=42..42 rows=20 loops=1)
        -> Table scan on p1_0  (cost=10208 rows=98793) (actual time=0.041..30 rows=100000 loops=1)
```

- **Read it from the innermost line outwards.** Each step feeds the step above it.
- **`cost` and the first `rows` are estimates**, made before the query ran.
- **`actual time=A..B`:** milliseconds until the first row (A) and until the last row (B).
- **`rows` after it:** what really came out.
- **`loops`:** how often the step ran.

Here MySQL read all 100,000 rows (30 ms), kept the 20 newest while sorting, and returned them after 42 ms. Every
page query before V2 looked like this, because no index could provide the requested order or narrow the rows. After
V2, the same request reads 20 index entries:

```text
-> Limit: 20 row(s)  (actual time=0.0433..0.0873 rows=20 loops=1)
    -> Index scan on p1_0 using ix_product_created_at (reverse)  (actual time=0.0417..0.085 rows=20 loops=1)
```

(Shortened: the estimates are left out here; the measurement files keep every plan in full.)

<!-- BUILD FLOW 08.15.04 — The four indexes -->
## The four indexes, and why each one

An index is a sorted copy of some columns plus a pointer to the row; in InnoDB the pointer is the primary key. MySQL
can therefore jump to a position in the sort order (a "seek"), and read forwards or backwards from there. Because
InnoDB appends `id` to every secondary index entry, an index on `(created_at)` is really sorted by
`(created_at, id)`: exactly the API's "newest" order including its tie-breaker.

| Index (V2) | Serves | Why this column order |
|---|---|---|
| `ix_product_created_at (created_at)` | Home page; name searches (newest first) | Already in `created_at DESC, id DESC` order when read backwards; MySQL stops after 20 |
| `ix_product_category_created_at (category, created_at)` | A category page, newest first | Equality on the first column, then the order on the second: one direct lookup, 20 entries |
| `ix_product_category_price (category, price)` | A category sorted by price, either direction; category counts; category with a price range | Same pattern; `price DESC, id DESC` is a backward read |
| `ix_product_price (price)` | A price range across all categories, cheapest first | The range and the order on the same column |

The rules behind these choices:
- **Equality columns first, then the range or sort column.** In `(category, price)`, all `HOME` entries are next to
  each other, already sorted by price. In `(price, category)` they would be scattered across the whole index.
- **The leftmost-prefix rule.** A composite index can be used for its first column alone, or its first two, but not
  for its second column alone. `(category, created_at)` therefore serves "category = HOME" and
  "category = HOME, newest first", but not "everything, newest first": its entries are grouped by category. That is
  why `(created_at)` exists separately. A separate `(category)` index would be redundant: both composite indexes
  already start with `category`.
- **Filtering versus sorting.** An index can narrow the rows (the WHERE clause), deliver them already in order (the
  ORDER BY), or both. The big wins here come from order: with rows arriving sorted, `LIMIT 20` stops after 20
  instead of sorting everything first.
- **The sort direction must match.** `ORDER BY price DESC, id DESC` is a backward read of `(category, price, id)`.
  `price DESC, id ASC` would match no reading direction and need a sort step. This is why every sort in
  `ProductSort` ends with `id` in the same direction (D-032).
- **No index on `stock_quantity`.** 93 % of products are in stock, so "in stock" filters out almost nothing. Adding
  it to `ix_product_price` as `(price, stock_quantity)` would let the price-range count avoid the table. But ties in
  price would then be ordered by stock before `id`, so the page query would need a sort again.
- **No index on `name`.** See "Why `LIKE '%term%'` cannot seek" below.

**Does every index earn its place?** The what-if file answers this for the least obvious one. Without
`ix_product_category_created_at`, category-newest still takes only 0.273 ms: MySQL walks `ix_product_created_at` and
keeps the `HOME` rows (97 entries read), because `HOME` is 20 % of the catalog. The index stays anyway. For a small
category, the same walk would read most of the 100,000 entries, which is exactly what makes the rare-word search
slow (below). With the composite index, any category's first page is one direct lookup, whatever the category's size.

<!-- BUILD FLOW 08.15.05 — What did not improve -->
## What did not get faster, and what got slower

**Counts.** `COUNT` must visit every matching row, so an index helps only in two cases:
- **It can count without reading the table.** The category count reads only `HOME` entries from a category index:
  5.8× faster.
- **It narrows the rows a lot.** The price-range count uses `ix_product_price` but must still look up each of the
  11,509 rows to check stock: only 1.2× faster.

The home count uses InnoDB's own whole-table count (about 8 ms either way), and no index can narrow `%linen%`.
Counts now cost more than the pages they accompany. Options for later: show "10,000+" instead of an exact total,
cache totals briefly, or show "next page" without a total. Phase 11 measures their share end to end first.

**Deep pages.** `LIMIT 99980, 20` means producing 99,980 rows in order just to skip them. With the index, MySQL would
read 100,000 index entries and look up 100,000 rows; forced to do so, it took 170 ms. The optimizer instead kept the
table scan and sort (90–105 ms), correctly. The page limit (D-033) bounds the cost. Keyset pagination ("the 20 rows
after the last one seen") would make deep pages as cheap as the first; it is a documented extension (plan §8).

**Searching for a rare word got slower.** `?q=zebra` matches nothing.
- **Before V2:** MySQL read the table once and found no match (44.5 ms).
- **After V2:** MySQL bets that walking `ix_product_created_at` newest-first will find 20 matches early. For "linen",
  8 % of names, it wins: 258 entries, 0.66 ms. For "zebra" it walks all 100,000 entries and looks up every row:
  193 ms.
- **Why the optimizer can't avoid it:** it cannot know in advance how many names contain `%zebra%`.
- **Ignoring the index** restores 44.4 ms. Turning off the optimizer setting `prefer_ordering_index` for the
  statement changes nothing (186 ms).

This trade-off is accepted for now: typical searches are 73× faster, and a no-match search costs 0.2 s at this
catalog size. That cost grows with the catalog. The remedies, each a deliberate change and none a silent one:
- a `FULLTEXT` index (word-based matching with its own rules);
- a dedicated search engine;
- deciding the plan from a first count of matches.

<!-- BUILD FLOW 08.15.06 — LIKE and B-trees -->
## Why `LIKE '%term%'` cannot seek

A B-tree index is sorted by the beginning of the value, like a dictionary. `name LIKE 'lin%'` can seek: the matching
names sit together, from "lin" up to "lio". In `name LIKE '%linen%'` the word may start anywhere, so the matching
names are spread over the whole index, and every name must be checked. The API's search is deliberately a contains
search (D-034). Turning it into a prefix search would make it indexable, but "Stonewashed Linen Throw" would no
longer match "linen". That would be a different feature, so it is not done silently.

<!-- BUILD FLOW 08.15.07 — The cost of indexes -->
## What the indexes cost

- **Storage** (`scripts/bench-db.ps1 status`). The table is 20.5 MB. When `V2` built the four indexes in bulk over
  100,000 existing rows, they took 12.1 MB together (2.5 + 3.5 + 3.5 + 2.5 MB). When the same 100,000 rows were
  inserted into a table that already had them, they took 18.1 MB (4.5 + 4.5 + 5.5 + 3.5 MB). Rows arriving in random
  order split index pages and leave them partly empty; a bulk build fills pages in order. InnoDB counts this space
  in whole 1 MB extents after the first half megabyte, so small differences are rounded up.
- **Writes.** Every INSERT now adds an entry to six indexes instead of two. Every UPDATE of `price`, `category`, or
  `created_at` moves entries in the indexes containing them.
  - Building `V2` over 100,000 rows took 3.4 s, online: reads and writes can continue meanwhile.
  - Loading 100,000 rows took 3.8, 5.6, and 4.6 s without the four indexes, and 6.4 and 4.6 s with them. At this
    size the difference is smaller than the variation between runs, so these few runs cannot show it.
  - A catalog is read far more often than it is written, so the trade is worth it here.

<!-- BUILD FLOW 08.15.08 — Reproducing -->
## How to reproduce

```powershell
.\scripts\dev-services.ps1 up
.\scripts\bench-db.ps1 create                     # add "drop -Force" first to start from nothing
cd backend
.\mvnw.cmd clean package -DskipTests
# Before: Flyway stops at V1. Wait for "Loaded 100000 benchmark products", then press Ctrl+C.
java -jar target\catalog-0.0.1-SNAPSHOT.jar --spring.profiles.active=bench --spring.flyway.target=1
cd ..
.\scripts\measure-queries.ps1 -Label before-v2
# After: a normal start applies V2 to the loaded rows. Wait for "Started", then press Ctrl+C.
cd backend
java -jar target\catalog-0.0.1-SNAPSHOT.jar --spring.profiles.active=bench
cd ..
.\scripts\measure-queries.ps1 -Label after-v2 -Baseline before-v2
.\scripts\measure-queries.ps1 -Label index-alternatives -QueryFile benchmarks\queries\index-alternatives.sql
.\scripts\bench-db.ps1 status
```

The measuring script needs only MySQL, so the backend can be stopped while it runs. `-Baseline` ends with an error
if any result differs. The `before-v2` state is always rebuilt this way, from a fresh database with
`--spring.flyway.target=1`; indexes are never dropped to recreate it.

<!-- BUILD FLOW 08.15.09 — Limits -->
## Limits of these numbers

- They are measured inside MySQL. JSON, HTTP, and the network are not included; Phase 11 measures whole requests
  with k6.
- One laptop, one query at a time, data in memory. There is no concurrency and no disk pressure.
- 100,000 rows. Index lookups for a first page stay almost constant as the catalog grows; scans, counts, and the
  rare-word search grow with it.
- Times vary by a few milliseconds between runs of the same plan. Compare plans first, then times.
