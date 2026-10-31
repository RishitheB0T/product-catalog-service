<!--
BUILD FLOW 01.07 — docs/REQUEST_FLOWS.md
First introduced: Phase 01
Purpose: Numbered runtime steps for every important request flow: search, uncached detail,
         cache-hit detail, successful write, rollback, and Redis outage.
Depends on: docs/PROJECT_PLAN.md §7 (backend) and §9 (caching).
Read by: Anyone tracing a request, and code comments that cite step IDs such as "Runtime: READ-04".
Runtime role: None. It documents runtime order; it is not executed.
-->

# Request Flows — what happens at runtime, step by step

> **Status:** Flow 0 (health check) has been real since Phase 02. Flows 2, 4, and 5 have been real since Phase 05.
> Their cache steps arrived in Phases 09 (reads, Flow 3) and 10 (eviction after writes, `WRITE-07`, and the outage
> handling of Flow 6); the cache is on by default since Phase 10.
> Flow 1 (search) has been real since Phase 06, and its browser steps since Phase 12; the browser steps of reads and
> writes (`READ-01`, `WRITE-01`, `WRITE-08`) since Phase 13. Steps name the real classes, methods, hooks, and
> components once their phase has landed.

Step IDs describe **runtime order**, not file-creation order (see "Two numbering systems" in `BUILD_GUIDE.md`).

| Prefix | Flow |
|---|---|
| `HEALTH-xx` | Health check (implemented) |
| `SEARCH-xx` | Paginated search |
| `READ-xx` | Product detail for one id, including the cache-hit (`H`), cache-miss, and Redis-error (`X`) branches |
| `WRITE-xx` | Create, update, and delete, including the rollback branch (`R`) |

<!-- BUILD FLOW 01.07.01 — Where requests travel -->
## Where requests travel

- **Development (Phases 03–13):** browser → Vite dev server on `127.0.0.1:24173` → proxies `/api` → Spring Boot on
  `127.0.0.1:24081` (running directly on Windows with the `dev` profile) → MySQL on `127.0.0.1:24306` and Redis on
  `127.0.0.1:24379`, both in Docker containers. Port choices: README "Ports".
- **Production-style (Phase 14 onwards):** browser → Nginx (the only published port) → `app:8080` on the Compose
  network → `mysql:3306` and `redis:6379`, reached by service name. The database and cache cannot be reached from outside.

<!-- BUILD FLOW 01.07.06 — Flow 0: health check (added in Phase 02) -->
## Flow 0 — Health check (implemented in Phase 02)

Example: `curl.exe http://localhost:24081/actuator/health` (dev profile). Docker health checks will call the
readiness URL in Phase 14.

| Step | Where | What happens | Phase |
|---|---|---|---|
| HEALTH-01 | Client (curl, a test, later Docker) | Sends `GET /actuator/health`, `/actuator/health/liveness`, or `/actuator/health/readiness` | 02 |
| HEALTH-02 | Embedded Tomcat | Accepts the connection on the server port (8080 by default, `127.0.0.1:24081` with `dev`) and hands the request to a worker thread | 02 |
| HEALTH-03 | Spring MVC `DispatcherServlet` | Finds the handler. Actuator's health endpoint is mapped because `exposure.include: health`. An unexposed endpoint such as `/actuator/env` has no handler → `404` | 02 |
| HEALTH-04 | Actuator health endpoint | Asks each health indicator (diskSpace, ping, ssl, livenessState, readinessState, and since Phase 04 `db`, which checks that a pooled MySQL connection is valid) and combines them: any `DOWN` makes the whole result `DOWN` | 02 / 04 |
| HEALTH-05 | Actuator → client | Applies `show-details` (`never` hides the components) and writes JSON. HTTP `200` for `UP`, `503` for `DOWN` | 02 |

<!-- BUILD FLOW 01.07.02 — Flow 1: search -->
## Flow 1 — Search

Example: `GET /api/v1/products?q=linen&category=HOME&page=0&size=20&sort=price_asc`

| Step | Where | What happens | Phase |
|---|---|---|---|
| SEARCH-01 | Browser: `useUrlState` → `App` → `useProductSearch` → `searchProducts` | A change (typing, paused for 300 ms by `SearchBox`; a category; a filter; the sort; a page) updates the state and the address bar with `pushState`. Typing uses `replaceState`, and every change except a page change returns to page 1. `toApiQuery` turns the state into the API's query string, with a zero-based page and `size=12`. The new query's effect aborts the previous request (`AbortController`), so a late answer can never overwrite newer results | 12 |
| SEARCH-02 | Vite proxy or Nginx | Forwards `/api/**` to Spring Boot (development: `vite.config.js`) | 12 / 14 |
| SEARCH-03 | Spring MVC data binding → `ProductController.searchProducts` | Builds a `ProductSearchRequest` from the query string: converts each value (`ProductSortConverter` for `sort`; a blank value counts as not sent), fills the defaults (page 0, size 20, `newest`), and validates it (`@Valid`: ranges, lengths, `@ValidPriceRange`). Values that cannot be converted → 400 `validation-failed` naming them (the rules run only once every value converts); broken rules → 400 `validation-failed` listing every one. Either way the service is never called and no SQL runs | 06 |
| SEARCH-04 | `ProductService.searchProducts` | The proxy begins a read-only transaction. `ProductSpecifications.matching` builds the WHERE clause from the filters that were sent (bound parameters; `%`, `_`, and `!` escaped for LIKE). `ProductSort.toSort()` supplies the ORDER BY, always ending with `id` | 06 |
| SEARCH-05 | `ProductRepository.findAll(specification, pageable)` → MySQL | The page query (`… WHERE … ORDER BY … LIMIT`), then a `COUNT` with the same WHERE clause unless the page itself reveals the total. Since Phase 08 the V2 indexes let a typical page query read its rows already in order and stop after `size` rows (0.08–0.66 ms on 100,000 products); counts still visit every matching row (`docs/QUERY_OPTIMIZATION.md`) | 06 / 08 |
| SEARCH-06 | `ProductMapper.toResponse`, `PageResponse.from` | Entities become DTOs inside the transaction, the page becomes `{content, page, size, totalElements, totalPages}`, the transaction ends, and Jackson writes `200 OK` | 06 |
| SEARCH-07 | Browser: `App` and its components | Renders the grid (each photo on a mat, or a placeholder), "Showing 1–12 of N products", the filter chips, and pagination. Until the first answer it shows a skeleton; while a newer request loads, the old results stay dimmed (`aria-busy`). A 400 shows the API's field messages in plain words and a 5xx a general message, both with "Try again". An empty page offers "Clear all filters" or "Go to page 1" | 12 |

Search never touches Redis. That is deliberate (plan §9).

<!-- BUILD FLOW 01.07.03 — Flows 2, 3, and 6: product detail -->
## Flow 2 — Product detail, cache miss (uncached)

Example: `GET /api/v1/products/42`

| Step | Where | What happens | Phase |
|---|---|---|---|
| READ-01 | Browser: `ProductPage` → `useProduct` → `getProduct` | A card's link (or an address typed in) leads to `/products/42`; `matchRoute` picks the product page, which sends `GET /api/v1/products/42` and aborts it if the shopper leaves first. The edit form (`/admin/products/42/edit`) loads the product the same way | 05 / 13 |
| READ-02 | `ProductController.getProduct` | Spring converts `{id}` to a `long` (non-numeric → 400) and calls the service | 05 |
| READ-03 | `ProductService.getProduct` | Since Phase 09 no transaction yet: the service first asks `ProductDetailCache` (when `catalog.cache.enabled` is true) | 05 / 09 |
| READ-04 | `ProductDetailCache.get` → Redis | `GET catalog:v1:product:42`. Hit → `READ-04H`. Miss → `READ-05` (counted as `miss`). Error → `READ-04X` (counted as `error`, logged) | 09 / 10 |
| READ-05 | `ProductRepository.findById` → MySQL | In a read-only transaction that covers only this read and `READ-06`: `SELECT … FROM product WHERE id = 42`. No row → `ProductNotFoundException` → 404 `product-not-found`, and nothing is cached | 05 / 09 |
| READ-06 | `ProductMapper.toResponse` | Entity → `ProductResponse` inside the transaction, which then ends | 05 |
| READ-07 | `ProductDetailCache.put` → Redis | After the transaction: `SET catalog:v1:product:42 <json>` with a 60-second TTL. A failure is logged and counted; the response is unaffected | 09 |
| READ-08 | Controller → client | `200 OK` with JSON. In the browser, `ProductPage` shows the photo and the facts; a 404 shows "This product doesn't exist" with a link to the catalog | 05 / 13 |

## Flow 3 — Product detail, cache hit

`READ-01` to `READ-04` as above. At **`READ-04H`** Redis has the key, so the JSON is turned back into a
`ProductResponse`, and the flow skips straight to `READ-08`. MySQL is not queried. (Phase 09)

## Flow 6 — Redis outage during a product-detail read

`READ-01` to `READ-04` as above. At **`READ-04X`** the Redis command gives up after its 200 ms timeout (a hung or
stopped Redis) and Spring reports a `QueryTimeoutException`. `ProductDetailCache` logs it and counts it as `error`; it
is *not* treated as "product missing". The flow continues with `READ-05` (MySQL) and `READ-06`. `READ-07` times out
too and is counted, and `READ-08` returns the correct product, just more slowly: about 430 ms instead of 10–15 ms,
measured with Redis stopped. (Phase 10; `ProductCacheOutageTest` pauses Redis to prove it.)

<!-- BUILD FLOW 01.07.04 — Flows 4 and 5: writes -->
## Flow 4 — Successful write

Example: `PUT /api/v1/products/42`

| Step | Where | What happens | Phase |
|---|---|---|---|
| WRITE-01 | Browser: `ProductFormPage` → `updateProduct` | Shown only when `GET /api/v1/settings` says writes are on. The form checks every field with the API's own rules (`validateProduct`); if anything is wrong, nothing is sent and focus moves to the error summary. Otherwise it sends the complete product plus the `version` it loaded. (Create sends `POST` without a version; delete sends `DELETE` after a confirmation dialog) | 05 / 13 |
| WRITE-02 | Spring MVC: Jackson and Bean Validation | Reads the JSON into `ProductUpdateRequest` and checks it (`@Valid`). Unreadable → 400 `malformed-request`; invalid → 400 `validation-failed`. The service is never called | 05 |
| WRITE-03 | `ProductController.updateProduct` | Calls the service | 05 |
| WRITE-04 | `ProductService.updateProduct` | The proxy begins a transaction. `requireWritesEnabled` refuses the request with 403 `writes-disabled` when `catalog.writes-enabled` is false; nothing is written | 05 |
| WRITE-05 | `ProductRepository.findById` → MySQL | Loads row 42 (missing → 404). The request's version must equal the stored one (otherwise 409 `version-conflict`). `ProductMapper.applyUpdate` copies the new values | 05 |
| WRITE-06 | `saveAndFlush` → MySQL, then commit | Hibernate sends `UPDATE … SET …, version = 8 WHERE id = 42 AND version = 7`. No matching row (a concurrent change) → 409 `version-conflict`; another product's SKU → 409 `duplicate-sku`. Otherwise the method returns and the transaction commits | 05 |
| WRITE-07 | `ProductCacheEvictionListener` → `ProductDetailCache.evict` → Redis | The service published `ProductChangedEvent(42)` inside the transaction; only after the commit does Spring call the listener, which deletes `catalog:v1:product:42` (for create, the generated id). If that fails, it is logged and counted, and the committed write is still reported as a success | 10 |
| WRITE-08 | Controller → client | `200 OK` with the new state (update), `201 Created` with `Location` (create), or `204 No Content` (delete). In the browser: the remembered search results are cleared, and the admin list shows "Saved …" or "Deleted …". A 400 is shown next to the fields it names, a 409 `duplicate-sku` on the SKU field, a 409 `version-conflict` as an explanation with "Load the latest version", and a 403 as "Changes are turned off" | 05 / 13 |

Note on the order: request validation (`WRITE-02`) happens before the write switch (`WRITE-04`), so an invalid
request to a read-only deployment receives 400, not 403.

## Flow 5 — Rollback

`WRITE-01` to `WRITE-05` as above; then at `WRITE-06` MySQL rejects the change, for example because the new SKU
belongs to another product (error 1062). `ProductService` translates the `DataIntegrityViolationException` into
`DuplicateSkuException`, which leaves the `@Transactional` method, so at **`WRITE-06R`** the proxy rolls the
transaction back: every change made in it is undone, including fields such as the name that were valid on their own
(`ProductServiceTest.failedUpdateIsRolledBackCompletely` proves this). After-commit work never runs, so the cache is
untouched; it needs no change, because MySQL did not change either (`ProductCacheInvalidationTest` proves this).
`ApiExceptionHandler` answers 409 `duplicate-sku`.

<!-- BUILD FLOW 01.07.05 — Known limits -->
## Known limit: the stale-read race

A stale read is still possible. Request A misses the cache and reads version 7 from MySQL. Request B updates the
product to version 8, commits, and evicts the key, which is not in Redis yet, so nothing is removed. Request A then
stores its old version-7 JSON in Redis. Readers get the stale copy until the TTL expires, at most 60 seconds.
`ProductCacheInvalidationTest.aSlowReaderCanStillStoreAnOldCopyAfterAWrite` plays these steps in order; possible
mitigations are in `docs/CACHING.md`.

<!-- BUILD FLOW 01.07.07 — Tests that walk through each flow (added in Phase 07) -->
## Tests that walk through each flow

| Flow | Tests, and the steps they cover |
|---|---|
| 0 — Health | `HealthEndpointTest`: every step over real HTTP, with no profile, `dev`, and `prod` |
| 1 — Search | `ProductControllerTest` (`SEARCH-03`, service replaced by a test double); `ProductSearchTest` (`SEARCH-04` to `SEARCH-06` on MySQL); `ProductSearchApiIntegrationTest` (`SEARCH-03` to `SEARCH-06` from a real query string to JSON) |
| 2 — Detail | `ProductControllerTest` (`READ-02`, the JSON of `READ-08`); `ProductServiceTest` (`READ-03` to `READ-06`); `ProductApiIntegrationTest` (every step over HTTP, including 404 after a delete) |
| 4 — Write | `ProductServiceTest` (`WRITE-04` to `WRITE-06`); `ProductApiIntegrationTest` (`WRITE-01` to `WRITE-08` over HTTP: the lifecycle, 403 under `prod`, 400 before 403, and ten simultaneous updates that end as one 200 and nine 409s) |
| 5 — Rollback | `ProductServiceTest.failedUpdateIsRolledBackCompletely`; `ProductApiIntegrationTest.aFailedUpdateChangesNothing` (the same rollback, seen over HTTP) |
| 3 — Cache hit | `ProductDetailCacheTest` (`READ-04` to `READ-07` and `READ-04H` against real Redis: miss, hit without MySQL, JSON round trip, expiry, an unreadable value) |
| 4 and 5 — Cache after writes | `ProductCacheInvalidationTest` (`WRITE-07`: update, delete, and create evict after the commit; a rolled-back update evicts nothing; the stale-read race played step by step) |
| 6 — Redis outage | `ProductCacheOutageTest` (Redis paused: reads answered from MySQL within the timeouts, a write committed and reported as a success while its eviction failed and was counted, and the cache working again afterwards) |
