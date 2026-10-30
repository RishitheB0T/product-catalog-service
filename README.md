<!--
BUILD FLOW 01.04 — README.md
First introduced: Phase 01
Purpose: The repository's front page: what works right now, the architecture, how to run it,
         where the evidence is, and the deployment status. It describes reality and labels plans as plans.
Depends on: docs/PROJECT_PLAN.md (scope) and docs/PROGRESS.md (status).
Read by: Anyone opening the repository (GitHub shows it first), including interviewers.
Runtime role: None. Documentation only.
-->

# FORM — The Everyday Edit · Product Catalog Service

A product catalog built from an empty folder in documented phases, with every file explained: a layered
**Spring Boot** REST API over **MySQL**, **Redis** caching for product details, and a **React** storefront that
uses the real API.

> **Status: Phase 11 of 16 complete.** The product API searches the catalog (filters, sorting, pagination) and reads,
> creates, updates, and deletes single products, with validation, optimistic locking, and one error format; production
> settings are read-only. Search queries were measured on 100,000 products and indexed from the measured plans.
> Product details are cached in Redis, cleared after every committed write, with MySQL as the fallback when Redis
> fails. 107 tests cover it, including full-stack tests over real HTTP and a Redis outage. Load tests with k6 measured
> the whole requests: the cache serves product details about 3.3 times faster than MySQL alone. This README always
> describes what actually works; anything not built yet is labeled *planned*.

<!-- BUILD FLOW 01.04.01 — What works today -->
## What works today

| Area | Status |
|---|---|
| Plan, architecture, verified version baseline | Done (Phase 01) |
| Spring Boot skeleton: build, tests, profiles, health check | Done (Phase 02) |
| Local MySQL 8.4 and Redis 8.10 in Docker Compose, with a one-command helper | Done (Phase 03) |
| Product schema (Flyway), JPA entity and repository, dev demo catalog, tests on real MySQL | Done (Phase 04) |
| Product API: get, create, update, delete; validation; RFC 9457 errors; read-only production mode | Done (Phase 05) |
| Search with filters, sorting, and pagination; validated parameters | Done (Phase 06) |
| Full-stack API tests over real HTTP, including simultaneous requests; example requests | Done (Phase 07) |
| Measured query optimization: 100,000-product benchmark data, `EXPLAIN ANALYZE` before and after, search indexes (V2) | Done (Phase 08) |
| Redis product-detail cache: cache-aside reads, versioned keys, JSON, 60-second TTL, metrics | Done (Phase 09) |
| Cache eviction after committed writes, 200 ms Redis timeouts with MySQL fallback; cache switched on | Done (Phase 10) |
| Reproducible k6 benchmarks: cache off, cold, and warm; search without and with indexes; raw results and report | Done (Phase 11) |
| React storefront and local admin | Planned (Phases 12–13) |
| Production containers and CI | Planned (Phase 14) |
| AWS EC2 deployment | Planned (Phase 15; needs AWS access and approval) |

<!-- BUILD FLOW 01.04.02 — Architecture -->
## Architecture (planned)

```text
Browser: React app "FORM"
   │  HTTP  /api/v1/...
   ▼
Vite dev server (development)  ·  Nginx (production; also serves the built React files)
   │  forwards /api/**
   ▼
Spring Boot API
   ProductController   HTTP in and out: parse, validate, choose status codes
   ProductService      business rules, transactions, cache decisions
   ProductRepository   database access through Spring Data JPA
   │                       │
   ▼                       ▼
 Redis                   MySQL 8.4
 short-lived copies      source of truth
 of product details      (schema owned by Flyway migrations)
```

- **Reading one product** checks Redis first. On a miss, it reads MySQL and stores a copy with a short TTL
  (the cache-aside pattern).
- **Searches** always read MySQL, using indexes chosen from measured query plans.
- **Writes** run inside a database transaction. Only after it commits is the cached copy removed.
- The public deployment is **read-only**: the server itself rejects writes.

The numbered runtime steps of each flow are in [`docs/REQUEST_FLOWS.md`](docs/REQUEST_FLOWS.md).

<!-- BUILD FLOW 01.04.03 — Tech stack -->
## Tech stack

Java 21 · Spring Boot 4.1.1 · Spring Data JPA (Hibernate 7) · Bean Validation · Actuator · Flyway 12 ·
MySQL 8.4 LTS · Redis 8.10 · Testcontainers 2 · React 19 · Vite 8 · plain CSS · Docker Compose · Nginx · k6 ·
GitHub Actions. The exact versions, verified against official sources on 2026-10-20, are listed in
[`docs/PROJECT_PLAN.md` §4](docs/PROJECT_PLAN.md#4-stack-and-verified-version-baseline).

<!-- BUILD FLOW 01.04.04 — Repository layout -->
## Repository layout

| Path | Contents | Arrives in |
|---|---|---|
| `backend/` | Spring Boot service | Phase 02 |
| `frontend/` | React + Vite storefront | Phase 12 |
| `infra/` | Compose files, Nginx configuration, EC2 helpers | Phase 03 |
| `scripts/` | PowerShell helpers (Bash only for Linux targets) | Phase 03 |
| `benchmarks/` | Captured search SQL and measured query plans (Phase 08); k6 scripts and results (Phase 11) | Phase 08 |
| `docs/` | Plan, guides, decisions, evidence | Phase 01 |
| `.github/workflows/` | Continuous integration | Phase 14 |

<!-- BUILD FLOW 01.04.05 — Prerequisites -->
## Prerequisites (Windows 11 development machine)

| Tool | Why it is needed | Notes |
|---|---|---|
| JDK 21 | Compiles and runs the backend | Any JDK 21 distribution works |
| Docker Desktop (WSL 2 engine) | Runs MySQL and Redis locally, builds images, and powers Testcontainers | Must be running |
| Git for Windows | Version control. Its Git Credential Manager handles GitHub sign-in in the browser | — |
| Node.js and npm | Frontend tooling only: Vite dev server, tests, production build | Node 24 LTS or newer |
| Maven | **Not required.** The Maven Wrapper (`mvnw.cmd`) downloads the pinned version | — |
| k6 | Benchmarks | Planned to run from its Docker image, so nothing to install |

<!-- BUILD FLOW 01.04.08 — Ports (added in Phase 03) -->
## Ports

The development machine runs several projects, so this one never binds framework defaults (8080, 3306, 6379,
5173, 80) on the host. It uses its own **24xxx** block: `24` followed by the last three digits of the usual port.
Local services listen on `127.0.0.1` only, so other devices on the network cannot reach them.

| Service | Host address | Inside its container | Since |
|---|---|---|---|
| Backend, `dev` profile | `127.0.0.1:24081` | — (runs directly on Windows) | Phase 03 |
| Backend jar, manual production-style run | `127.0.0.1:24082` (passed on the command line) | — | Phase 03 |
| Backend, `bench` profile (100,000-product measurement database) | `127.0.0.1:24083` | — | Phase 08 |
| MySQL 8.4 | `127.0.0.1:24306` | 3306 | Phase 03 |
| Redis 8.10 | `127.0.0.1:24379` | 6379 | Phase 03 |
| Vite dev server | 24173 (planned) | — | Phase 12 |
| Production-style stack through Nginx | 24080 (planned) | 80 | Phase 14 |

<!-- BUILD FLOW 01.04.06 — Running, testing, evidence, deployment -->
## Quick start

Run these in PowerShell from the repository root. You need JDK 21 and Docker Desktop (running). The first runs
download Maven, the Java libraries, and the two container images.

**1. Start MySQL and Redis.** The first `up` creates `infra/.env` with new random passwords (git-ignored) and waits
until both containers report healthy.

```powershell
.\scripts\dev-services.ps1 up
```

Other actions: `status`, `logs`, `down` (stops and removes the containers; the database volume is kept), and
`reset -Force` (also deletes the database volume).

**2. Run the backend** with the dev profile:

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

**3. Check it** from a second terminal:

```powershell
curl.exe http://localhost:24081/actuator/health
```

You should see `"status":"UP"` together with component details, including `db` (MySQL), which only the dev profile
shows. On the first start Flyway creates the product table, and the dev profile loads 24 demo products (log line
`Loaded 24 demo products`). Stop the app with Ctrl+C. Product details are cached in the local Redis; how to look at
the cache is in [`docs/CACHING.md`](docs/CACHING.md).

- Keep the quotes around `-Dspring-boot.run.profiles=dev`. Windows PowerShell splits an unquoted argument like
  this at its first dot, so Maven would receive the wrong setting.
- The backend reads the database password from `infra/.env`, so step 1 must have run at least once.

**4. Look at the data** (the SQL goes in through standard input, which avoids Windows PowerShell's quoting problems):

```powershell
"SELECT sku, name, category, price, stock_quantity FROM product LIMIT 5;" | docker compose --env-file infra\.env -f infra\compose.dev.yaml exec -T mysql sh -c 'MYSQL_PWD=$MYSQL_PASSWORD mysql -u $MYSQL_USER $MYSQL_DATABASE --table'
```

**Production-style run of the packaged jar.** The `prod` profile contains no database or Redis settings on purpose:
a server supplies them as environment variables, which Spring Boot maps onto its properties (`SPRING_DATASOURCE_URL`
becomes `spring.datasource.url`). Here they point at the local MySQL and Redis containers:

```powershell
cd backend
.\mvnw.cmd clean verify
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://127.0.0.1:24306/catalog?connectionTimeZone=UTC"
$env:SPRING_DATASOURCE_USERNAME = "catalog"
$env:SPRING_DATASOURCE_PASSWORD = (Select-String -LiteralPath ..\infra\.env -Pattern '^MYSQL_PASSWORD=(.*)$').Matches[0].Groups[1].Value
$env:SPRING_DATA_REDIS_HOST = "127.0.0.1"
$env:SPRING_DATA_REDIS_PORT = "24379"
java -jar target\catalog-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod --server.port=24082 --server.address=127.0.0.1
```

Without the two Redis variables the application still works: every product read falls back to MySQL after the
200 ms Redis timeout, and the cache counts errors.

Expected: health without component details, no banner, and no demo data. The `$env:` variables last until you close
that terminal. Without the last two options the jar would listen on Spring Boot's default, port 8080 on every network
interface, which suits a container but not a shared development machine.

<!-- BUILD FLOW 01.04.09 — API (added in Phase 05) -->
## API

Base path `/api/v1/products`, JSON in and out. Ready-to-send examples for VS Code's REST Client extension are in
[`backend/api-examples.http`](backend/api-examples.http): searches, then one product from creation to deletion.

| Method and path | Success | Typical errors |
|---|---|---|
| `GET /api/v1/products` | 200 with one page of products | 400 invalid parameters |
| `GET /api/v1/products/{id}` | 200 with the product | 404 unknown id; 400 non-numeric id |
| `POST /api/v1/products` | 201 with the product and a `Location` header | 400 invalid fields or JSON; 409 duplicate SKU; 403 read-only |
| `PUT /api/v1/products/{id}` | 200 with the product (version + 1) | 400; 404; 409 duplicate SKU or outdated version; 403 |
| `DELETE /api/v1/products/{id}` | 204 | 404; 409 changed meanwhile; 403 |

**Search.** Every parameter is optional, and all filters that are sent must match.

| Parameter | Meaning | Default and limits |
|---|---|---|
| `q` | The name contains this text. Case and accents are ignored; `%` and `_` match literally | At most 100 characters |
| `category` | `APPAREL`, `FOOTWEAR`, `BAGS`, `HOME`, `KITCHEN`, or `STATIONERY` | — |
| `minPrice`, `maxPrice` | Price range in rupees, both ends included | At least 0, at most two decimals, `minPrice` ≤ `maxPrice` |
| `inStock` | `true`: only products in stock; `false`: only sold-out products | Not sent: both |
| `page` | Zero-based page number | 0; at most 10,000 |
| `size` | Products per page | 20; from 1 to 100 |
| `sort` | `newest`, `price_asc`, `price_desc`, or `name_asc`. Products that tie are ordered by `id` | `newest` |

The response is `{"content": [...products...], "page": 0, "size": 12, "totalElements": 2, "totalPages": 1}`. A page
after the last one returns empty `content` with the real totals. Blank values count as not sent, unknown parameters
are ignored, and a repeated parameter uses its first value.

```powershell
$page = Invoke-RestMethod "http://localhost:24081/api/v1/products?category=BAGS&maxPrice=3000&inStock=true&sort=price_asc&size=12"
"$($page.totalElements) products, page $($page.page + 1) of $($page.totalPages)"
$page.content | Format-Table id, name, price, stockQuantity
```

Invalid parameters get a 400 `validation-failed` that names them, for example
`curl.exe -s "http://localhost:24081/api/v1/products?sort=popular"` answers with
`{"field": "sort", "message": "must be one of [newest, price_asc, price_desc, name_asc]"}` in `errors`. A value that
cannot be converted at all (such as `?page=abc`) is reported on its own; the range rules are checked once every value
converts.

Creating, updating, and deleting with Windows PowerShell (dev profile running):

```powershell
$body = @{ sku = "FRM-HOM-101"; name = "Hand-thrown Planter"; description = "Stoneware planter with a drainage hole and a matching saucer."; category = "HOME"; price = 1890; stockQuantity = 12 } | ConvertTo-Json
$created = Invoke-RestMethod -Method Post -Uri http://localhost:24081/api/v1/products -ContentType "application/json" -Body $body

# PUT sends the complete product plus the version you last read; an outdated version gets 409.
$update = @{ sku = $created.sku; name = "Hand-thrown Planter, Large"; description = $created.description; category = "HOME"; price = 2190; stockQuantity = 9; version = $created.version } | ConvertTo-Json
Invoke-RestMethod -Method Put -Uri "http://localhost:24081/api/v1/products/$($created.id)" -ContentType "application/json" -Body $update

Invoke-RestMethod -Method Delete -Uri "http://localhost:24081/api/v1/products/$($created.id)"
```

**Errors** follow RFC 9457 "Problem Details" (`Content-Type: application/problem+json`). Windows PowerShell 5.1's
`Invoke-RestMethod` throws on an error status without showing this body, so use `curl.exe` to see it, for example
`curl.exe -s http://localhost:24081/api/v1/products/999999`.

```json
{
  "type": "https://catalog.example/problems/validation-failed",
  "title": "Validation failed",
  "status": 400,
  "detail": "2 fields are invalid.",
  "instance": "/api/v1/products",
  "errors": [
    { "field": "price", "message": "must be greater than or equal to 0.00" },
    { "field": "sku", "message": "must use only upper-case letters, digits, and hyphens, starting with a letter or digit" }
  ]
}
```

| `type` ends with | Status | Meaning |
|---|---|---|
| `validation-failed` | 400 | Fields broke the rules; `errors` lists each one |
| `malformed-request` | 400 | Not valid JSON for this endpoint: bad syntax, a wrong type, or an unknown category |
| `invalid-data` | 400 | The database rejected data that passed validation |
| `product-not-found` | 404 | No product with that id |
| `duplicate-sku` | 409 | Another product already uses the SKU |
| `version-conflict` | 409 | Someone else changed the product since you read it; reload and try again |
| `writes-disabled` | 403 | This deployment is read-only |

Spring's own errors (an unknown path, a wrong method, a non-numeric id) leave `type` out, which means `about:blank`.

**Read-only deployments.** Writes need `catalog.writes-enabled: true`. Only the `dev` profile sets it. The default
and the `prod` profile keep it `false`, so a public deployment answers every write with 403. The server enforces this
itself, so hiding the admin buttons in the UI is not the only protection.

## Tests

Docker Desktop must be running: the tests start their own MySQL 8.4.11 and Redis 8.10.2 containers through
Testcontainers, separate from the development services.

```powershell
cd backend
.\mvnw.cmd verify
```

107 tests in fourteen classes. The first two start the whole application on a random port and call it over real HTTP,
exactly as a client would:
- `ProductApiIntegrationTest` (15): a product from creation to deletion, the exact JSON shape, fields a client may not
  set, every error type (400, 404, 405, 409, 415), a rollback seen through the API, Unicode text, ten simultaneous
  updates of the same version (exactly one 200, nine 409s), ten simultaneous creates with one SKU (one 201, nine
  409s), and the read-only `prod` profile.
- `ProductSearchApiIntegrationTest` (6): filters from a real query string, the page envelope, ties across pages,
  percent-encoded search text, and the binding behavior documented above.
- `ProductControllerTest` (22), a web-layer test with the service replaced by a Mockito test double: status codes,
  JSON shapes, validation messages, every Problem Details type, and the search parameters (defaults, binding, and
  every kind of invalid value).
- `ProductSearchTest` (13) searches real MySQL: each filter and their combination, case- and accent-insensitive names,
  `%` and `_` matched literally, inclusive price bounds, every sort with its `id` tie-breaker, and pages that never
  overlap or skip a product.
- `ProductServiceTest` (8) runs real transactions on real MySQL: commits, a failed update rolled back completely,
  outdated versions and duplicate SKUs refused, and every write refused under the `prod` profile.
- `HealthEndpointTest` (6) starts the real application with no profile, `dev`, and `prod`, and checks over HTTP that
  health is UP, that details appear only with `dev`, that the probes answer, and that `/actuator/env` returns 404.
- `ProductRepositoryTest` (13) runs against real MySQL: products round-trip, timestamps are stored in UTC, duplicate
  SKUs and six kinds of invalid rows are rejected by the database itself, and optimistic locking refuses an update
  based on an outdated version.
- `DemoDataLoaderTest` (2) checks that the demo catalog loads only with `dev`, only into an empty table, and only once.
- `ProductDetailCacheTest` (7) runs against real Redis: a miss stores the JSON with its TTL, a hit answers without
  MySQL, every field survives the round trip, missing products and a switched-off cache store nothing, an unreadable
  value is counted and replaced, and keys expire. `TestcontainersConfigurationTest` (2) checks the shared test setup.
- `ProductCacheInvalidationTest` (5) proves that committed updates, deletes, and creates clear the cached copy, that a
  rolled-back update clears nothing, and plays the remaining stale-read race step by step. `ProductCacheOutageTest`
  (2) pauses the Redis container: reads still answer from MySQL within the timeouts, a write still succeeds while its
  failed eviction is counted, and the cache works again afterwards.
- `BenchmarkProductGeneratorTest` (4) checks that the benchmark data is reproducible (a pinned fingerprint for all
  100,000 products), valid by the API's own rules, and shaped like a real catalog; `BenchmarkDataSeederTest` (2)
  checks that it loads only with the `bench` profile, only into an empty table, and only once.

## Performance evidence

**Search queries (Phase 08)**, measured inside MySQL on 100,000 generated products with `EXPLAIN ANALYZE` (median of
three warm runs), before and after the four indexes of migration V2. Every query returned exactly the same rows.

| Request | Before | After |
|---|---:|---:|
| Home page, newest first (`GET /api/v1/products`) | 42.2 ms | 0.087 ms |
| Category page, newest first (`?category=HOME`) | 37.9 ms | 0.136 ms |
| Category page by price (`?category=HOME&sort=price_asc`) | 37.8 ms | 0.084 ms |
| Price range in stock (`?minPrice=1000&maxPrice=2000&inStock=true&sort=price_asc`) | 41.3 ms | 0.112 ms |
| Name search, common word (`?q=linen`) | 48.6 ms | 0.664 ms |
| Name search, no match (`?q=zebra`) | 44.5 ms | **193 ms (slower)** |

Counts and the deepest page barely changed, and the no-match search got slower; why, and what each index costs, is in
[`docs/QUERY_OPTIMIZATION.md`](docs/QUERY_OPTIMIZATION.md), with the raw plans in `benchmarks/query-plans/` and the
steps to reproduce them.

**Whole HTTP requests (Phase 11)**, measured with k6 (10 users, each sending its next request as soon as the last one
answered; three 30-second trials after a warm-up) on the same 100,000 products, with everything on one laptop.
Medians of the three trials:

| Scenario | Throughput | Median | p95 |
|---|---:|---:|---:|
| Product detail, cache off (MySQL every time) | 1,016 req/s | 8.31 ms | 16.75 ms |
| Product detail, cache on, cold (every request a miss) | 579 req/s | 15.99 ms | 23.82 ms |
| Product detail, cache on, warm (99.6% hits) | 3,310 req/s | 2.35 ms | 5.35 ms |
| Search mix, without the V2 indexes | 44.8 req/s | 189–253 ms | 296–383 ms |
| Search mix, with the V2 indexes | 144.1 req/s | 33–91 ms | 74–149 ms |

The search rows give the range over the four searches in the mix. The indexes help less end to end than inside MySQL,
because every search also runs a `COUNT`. The method, every trial's range, the limits of a one-laptop benchmark, and
four discarded runs are in [`docs/PERFORMANCE.md`](docs/PERFORMANCE.md). The raw k6 summaries are in
`benchmarks/results/`, and `.\scripts\run-benchmarks.ps1` reproduces them in about ten minutes.

## Deployment status

Not deployed. Phase 15 prepares the EC2 workflow. Deploying needs an AWS account and an explicitly approved choice of
instance, region, and cost.

<!-- BUILD FLOW 01.04.07 — Documentation map -->
## Documentation

| Read this | For |
|---|---|
| [`docs/PROGRESS.md`](docs/PROGRESS.md) | Where the build is right now |
| [`docs/PROJECT_PLAN.md`](docs/PROJECT_PLAN.md) | The plan, the rules, and the 16 phases |
| [`docs/BUILD_GUIDE.md`](docs/BUILD_GUIDE.md) | The project in construction order |
| [`docs/FILE_GUIDE.md`](docs/FILE_GUIDE.md) | Every file and its role |
| [`docs/REQUEST_FLOWS.md`](docs/REQUEST_FLOWS.md) | What happens at runtime, step by step |
| [`docs/DECISIONS.md`](docs/DECISIONS.md) | Choices and trade-offs |
| [`docs/QUERY_OPTIMIZATION.md`](docs/QUERY_OPTIMIZATION.md) | How search was measured and indexed, with the evidence |
| [`docs/CACHING.md`](docs/CACHING.md) | The Redis product-detail cache: keys, TTL, flows, failures, limits |
| [`docs/PERFORMANCE.md`](docs/PERFORMANCE.md) | Load tests with k6: the cache and the indexes measured end to end, and how far the numbers go |
| [`docs/LEARNING_LOG.md`](docs/LEARNING_LOG.md) | Concepts and exercises for each phase |
