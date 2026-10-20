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

> **Status:** Flow 0 (health check) is implemented (Phase 02). Flows 1–6 are planned: each step names the phase
> that will implement it, and is updated to name the real classes and methods when that phase lands.

Step IDs describe **runtime order**, not file-creation order (see "Two numbering systems" in `BUILD_GUIDE.md`).

| Prefix | Flow |
|---|---|
| `HEALTH-xx` | Health check (implemented) |
| `SEARCH-xx` | Paginated search |
| `READ-xx` | Product detail for one id, including the cache-hit (`H`), cache-miss, and Redis-error (`X`) branches |
| `WRITE-xx` | Create, update, and delete, including the rollback branch (`R`) |

<!-- BUILD FLOW 01.07.01 — Where requests travel -->
## Where requests travel (planned)

- **Development (Phases 03–13):** browser → Vite dev server on `localhost:5173` → proxies `/api` → Spring Boot on
  `localhost:8081` (running directly on Windows with the `dev` profile; 8080 is occupied on the development machine)
  → MySQL and Redis containers through ports bound to `127.0.0.1`.
- **Production-style (Phase 14 onwards):** browser → Nginx (the only published port) → `app:8080` on the Compose
  network → `mysql:3306` and `redis:6379`, reached by service name. The database and cache cannot be reached from outside.

<!-- BUILD FLOW 01.07.06 — Flow 0: health check (added in Phase 02) -->
## Flow 0 — Health check (implemented in Phase 02)

Example: `curl.exe http://localhost:8081/actuator/health` (dev profile). Docker health checks will call the
readiness URL in Phase 14.

| Step | Where | What happens | Phase |
|---|---|---|---|
| HEALTH-01 | Client (curl, a test, later Docker) | Sends `GET /actuator/health`, `/actuator/health/liveness`, or `/actuator/health/readiness` | 02 |
| HEALTH-02 | Embedded Tomcat | Accepts the connection on the server port (8080 by default, 8081 with `dev`) and hands the request to a worker thread | 02 |
| HEALTH-03 | Spring MVC `DispatcherServlet` | Finds the handler. Actuator's health endpoint is mapped because `exposure.include: health`. An unexposed endpoint such as `/actuator/env` has no handler → `404` | 02 |
| HEALTH-04 | Actuator health endpoint | Asks each health indicator (diskSpace, ping, ssl, livenessState, readinessState; the database from Phase 04) and combines them: any `DOWN` makes the whole result `DOWN` | 02 |
| HEALTH-05 | Actuator → client | Applies `show-details` (`never` hides the components) and writes JSON. HTTP `200` for `UP`, `503` for `DOWN` | 02 |

<!-- BUILD FLOW 01.07.02 — Flow 1: search -->
## Flow 1 — Search

Example: `GET /api/v1/products?q=linen&category=HOME&page=0&size=20&sort=price_asc`
(`HOME` and `price_asc` are placeholders until Phases 04 and 06 fix the real values.)

| Step | Where | What happens | Phase |
|---|---|---|---|
| SEARCH-01 | Browser | The catalog page builds the query string from the filter state in the URL. A newer search cancels the older request | 12 |
| SEARCH-02 | Vite proxy or Nginx | Forwards `/api/**` to Spring Boot | 12 / 14 |
| SEARCH-03 | ProductController | Binds and validates parameters (price range, page and size bounds, sort allowlist). Invalid input → 400 with field errors | 06 |
| SEARCH-04 | ProductService | Opens a read-only transaction, turns the criteria into predicates with bound parameters, and maps the sort key to an order defined in code plus the `id` tie-breaker | 06 |
| SEARCH-05 | ProductRepository → MySQL | Runs the page query and the count query, using indexes chosen from measured plans | 06 / 08 |
| SEARCH-06 | ProductService | Maps entities to response DTOs and builds `{content, page, size, totalElements, totalPages}` | 06 |
| SEARCH-07 | Browser | Renders results, the result count, filter chips, and pagination | 12 |

Search never touches Redis. That is deliberate (plan §9).

<!-- BUILD FLOW 01.07.03 — Flows 2, 3, and 6: product detail -->
## Flow 2 — Product detail, cache miss (uncached)

Example: `GET /api/v1/products/42`

| Step | Where | What happens | Phase |
|---|---|---|---|
| READ-01 | Browser | The detail page requests product 42 | 13 |
| READ-02 | ProductController | Parses `{id}` and calls the service | 05 |
| READ-03 | ProductService | Receives the id. If caching is enabled, it asks the detail cache first | 05 / 09 |
| READ-04 | Redis | `GET catalog:v1:product:42`. Hit → `READ-04H`. Miss → `READ-05`. Error → `READ-04X` | 09 / 10 |
| READ-05 | ProductRepository → MySQL | `SELECT … WHERE id = 42`. No row → 404, and nothing is cached | 05 |
| READ-06 | Mapper | Entity → `ProductResponse` DTO | 05 |
| READ-07 | Redis | `SET catalog:v1:product:42 <json>` with a 60-second TTL | 09 |
| READ-08 | Controller → browser | `200 OK` with JSON | 05 |

## Flow 3 — Product detail, cache hit

`READ-01` to `READ-04` as above. At **`READ-04H`** Redis has the key, so the JSON is turned back into a
`ProductResponse`, and the flow skips straight to `READ-08`. MySQL is not queried. (Phase 09)

## Flow 6 — Redis outage during a product-detail read

`READ-01` to `READ-04` as above. At **`READ-04X`** Redis times out quickly or refuses the connection. The error is
logged and counted. It is *not* treated as "product missing". The flow continues with `READ-05` (MySQL) and
`READ-06`. `READ-07` either fails harmlessly or is skipped, and `READ-08` returns the correct product, just more
slowly. (Phase 10)

<!-- BUILD FLOW 01.07.04 — Flows 4 and 5: writes -->
## Flow 4 — Successful write

Example: `PUT /api/v1/products/42`

| Step | Where | What happens | Phase |
|---|---|---|---|
| WRITE-01 | Browser (local admin) | Sends the edited product, including the `version` it last read | 13 |
| WRITE-02 | Server write switch | If writes are disabled (the public deployment), the request is rejected with a clear error and nothing changes | 05 |
| WRITE-03 | ProductController | Validates the body. Invalid → 400 with field errors | 05 |
| WRITE-04 | ProductService | A `@Transactional` method starts a database transaction | 05 |
| WRITE-05 | ProductRepository → MySQL | Loads row 42 (missing → 404), compares the client's version (stale → 409), and applies the changes | 05 |
| WRITE-06 | Commit | Hibernate sends `UPDATE … WHERE id = 42 AND version = 7`. If a concurrent write already changed the version, no row matches → 409. Otherwise MySQL commits | 05 |
| WRITE-07 | After-commit listener | Deletes `catalog:v1:product:42` (for create, the generated id). If that fails, it is logged and counted, and the committed write is still reported as a success | 10 |
| WRITE-08 | Controller → browser | `201 Created` with `Location` (create), `200 OK` (update), or `204 No Content` (delete) | 05 |

## Flow 5 — Rollback

`WRITE-01` to `WRITE-05` as above, then something fails, for example a duplicate SKU violating the unique
constraint. At **`WRITE-06R`** the transaction rolls back, so MySQL keeps the old data. After-commit work never
runs, so the cache is untouched. The client receives a clear error (409 for a duplicate SKU). (Phases 05 and 10)

<!-- BUILD FLOW 01.07.05 — Known limits -->
## Known limit (preview of Phase 10)

A stale read is still possible. Request A misses the cache and reads version 7 from MySQL. Request B updates the
product to version 8, commits, and evicts the key, which is not in Redis yet, so nothing is removed. Request A then
stores its old version-7 JSON in Redis. Readers get the stale copy until the TTL expires. The TTL limits how long
this can last. Details and possible mitigations will be documented in `docs/CACHING.md` (Phases 09–10).
