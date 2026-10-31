<!--
BUILD FLOW 01.06 — docs/FILE_GUIDE.md
First introduced: Phase 01
Purpose: One row for every tracked file: its BUILD FLOW number, exact path, purpose,
         dependencies, caller/consumer, and when to read it.
Depends on: The list of tracked files (`git ls-files`) and each file's BUILD FLOW header.
Read by: Anyone meeting an unfamiliar file, and the pre-commit check that nothing is undocumented.
Runtime role: None. Documentation only.
-->

# File Guide — every tracked file

**Rules.** Every file that `git ls-files` lists has a row here. Numbers come from each file's BUILD FLOW header.
Generated files are marked **(generated)** and are never edited by hand to add comments; their explanation lives
here instead. Two more kinds of file cannot hold a header: JSON, which has no comment syntax (marked **(JSON)**),
and images (marked **(binary)**). Binary and generated internals are grouped rather than listed line by line.

<!-- BUILD FLOW 01.06.01 — Repository root -->
## Repository root

| BUILD FLOW | Path | Purpose | Depends on | Caller / consumer | When to read |
|---|---|---|---|---|---|
| 01.02 | `.gitignore` | Keeps secrets, build output, dependencies, logs, and editor clutter out of Git | — | Git (`status`, `add`, `commit`) | Before adding tooling that creates new files |
| 01.03 | `.gitattributes` | LF line endings in the repository; CRLF for Windows scripts; binary-file rules | — | Git (checkout and commit) | When a script fails because of its line endings |
| 01.04 | `README.md` | Honest front page: status, architecture, prerequisites, documentation map | `docs/PROJECT_PLAN.md`, `docs/PROGRESS.md` | GitHub visitors, interviewers | First |

<!-- BUILD FLOW 01.06.02 — docs/ -->
## docs/

| BUILD FLOW | Path | Purpose | Depends on | Caller / consumer | When to read |
|---|---|---|---|---|---|
| 01.01 | `docs/PROJECT_PLAN.md` | Canonical plan: working rules, requirements, version baseline, 16 phases | The project requirements (restated in full) | Every other document links to it | Before each phase |
| 01.05 | `docs/BUILD_GUIDE.md` | The project in construction order, phase by phase | Plan, File Guide | Anyone studying the build | To study the build in order |
| 01.06 | `docs/FILE_GUIDE.md` | This table | `git ls-files`, file headers | Readers, the pre-commit check | When you meet an unfamiliar file |
| 01.07 | `docs/REQUEST_FLOWS.md` | Runtime step IDs (HEALTH, SEARCH, READ, WRITE) for every flow: health, search with its browser steps (Phase 12), single-product reads and writes, and the cache branches (Phases 09–10). Since Phase 07 also which tests cover each flow | Plan §7 and §9 | Readers, code comments | To understand what happens during a request |
| 01.08 | `docs/DECISIONS.md` | Important choices, the alternatives, and the trade-offs | Plan | Readers, interview preparation | When asking "why this and not that?" |
| 01.09 | `docs/LEARNING_LOG.md` | Concepts learned and small exercises for each phase | Build Guide | Readers | After each phase |
| 01.10 | `docs/PROGRESS.md` | Current phase, last commit, checks, blockers, next action | All docs | Read first when resuming work | Whenever work resumes |
| 09.05 | `docs/CACHING.md` | The product-detail cache: what is and is not cached, key and value format, TTL, reads, after-commit eviction on writes, Redis failures with measured costs, metrics, and the remaining consistency limits | 09.01, 09.02, 05.10 | Readers, interview preparation | Before changing the cache |
| 11.06 | `docs/PERFORMANCE.md` | The benchmark method and results: product detail with the cache off, cold, and warm; search without and with the indexes; how to read the numbers, and their limits | `benchmarks/results/`, 11.01–11.03 | Readers, interview preparation | When asked "how fast is it, and how do you know?" |
| 12.40 | `docs/IMAGE_CREDITS.md` | The product photos: how they were chosen (free license, no visible brand, matches the product), and each one's source, author, license, and changes | 12.11, 12.33–12.39 | Readers, anyone adding a photo | Before adding or replacing a product photo |
| 08.15 | `docs/QUERY_OPTIMIZATION.md` | How search was measured and optimized: dataset, captured SQL, plans before and after V2, costs, what did not improve, how to reproduce | `benchmarks/`, V2, the two measurement scripts | Readers, interview preparation | When asking "why these indexes?" |

<!-- BUILD FLOW 01.06.04 — backend/ (added in Phase 02) -->
## backend/

| BUILD FLOW | Path | Purpose | Depends on | Caller / consumer | When to read |
|---|---|---|---|---|---|
| 02.01 | `backend/pom.xml` | Maven build: Spring Boot 4.1.1 parent, Java 21, the starters (Web MVC, Validation, Actuator; since Phase 04 also Data JPA, Flyway with its MySQL module, the MySQL driver, and Testcontainers for tests), runnable-jar packaging; since Phase 05 Mockito runs as a Java agent in tests; since Phase 09 Spring Data Redis and its test starter | Maven Wrapper, Maven Central | `mvnw.cmd` / `mvnw`; CI and Docker from Phase 14 | When adding a dependency or changing the build |
| 02.02 | `backend/mvnw` | **(generated)** Maven Wrapper script for Linux and macOS (CI, Docker, EC2): downloads the pinned Maven, checks its SHA-256, runs it. Stored with LF endings and the executable bit | `backend/.mvn/wrapper/maven-wrapper.properties` | Linux builds from Phase 14 | Only if the build tool itself misbehaves |
| 02.03 | `backend/mvnw.cmd` | **(generated)** The same wrapper for Windows, checked out with CRLF endings | `backend/.mvn/wrapper/maven-wrapper.properties` | Windows development machines | Only if the build tool itself misbehaves |
| 02.04 | `backend/.mvn/wrapper/maven-wrapper.properties` | **(generated, plus one setting)** Pins Maven 3.10.0 and wrapper 3.3.4. We added `distributionSha256Sum`, verified against Apache's published SHA-512 | — | `mvnw`, `mvnw.cmd` | When upgrading Maven: change the URL and the checksum together |
| 02.05 | `backend/src/main/java/com/example/catalog/CatalogApplication.java` | Entry point that starts Spring Boot. Its package is where component scanning starts; since Phase 05 it also scans for `@ConfigurationProperties` records | Spring Boot auto-configuration, the YAML files | The JVM, `spring-boot:run`, `@SpringBootTest` | The first Java file to read |
| 02.06 | `backend/src/main/resources/application.yml` | Base configuration with safe defaults: app name, Actuator exposure limited to health, no health details; since Phase 04 JPA settings (`ddl-auto=validate`, Open Session in View off, UTC) and the Flyway location; since Phase 05 `catalog.writes-enabled: false`; since Phase 09 the cache settings (60-second TTL) and Redis left out of health; since Phase 10 the cache on and 200 ms Redis timeouts | — | Spring Boot at startup | Before changing any setting |
| 02.07 | `backend/src/main/resources/application-dev.yml` | Dev profile: listens on `127.0.0.1:24081` (since Phase 03), full health details, DEBUG logs for our package; since Phase 04 the MySQL connection, reading the password from `infra/.env`; since Phase 05 writes enabled; since Phase 09 the Redis address and `/actuator/metrics` | `application.yml`, `infra/.env` | Local runs with the `dev` profile | When running locally |
| 02.08 | `backend/src/main/resources/application-prod.yml` | Prod profile: locked-down Actuator settings pinned, banner off; since Phase 05 writes explicitly disabled (read-only) | `application.yml` | Docker and EC2 (Phases 14–15) | Before deploying |
| 02.09 | `backend/src/test/java/com/example/catalog/HealthEndpointTest.java` | Real-HTTP tests of health, probes, and hidden endpoints under the base, dev, and prod configurations (each with the shared MySQL test container since Phase 04) | 02.05–02.08, 04.07, JUnit 6, AssertJ, Spring Boot test support | Maven Surefire (`mvnw verify`) | To see how profiles change behavior |
| 04.01 | `backend/src/main/resources/db/migration/V1__create_product_table.sql` | Flyway migration creating the `product` table: columns, primary key, unique SKU, six named CHECK constraints, UTC timestamp defaults, version column. Never edited; the search indexes came later, in V2 (08.11) | MySQL 8.4 | Flyway at startup and in tests | To understand the data model and its rules |
| 04.02 | `backend/src/main/java/com/example/catalog/product/Category.java` | The six allowed categories as a Java enum (mirrors `ck_product_category`) | — | `Product`; the API from Phase 05 | When adding a category (enum plus a new migration) |
| 04.03 | `backend/src/main/java/com/example/catalog/product/Product.java` | JPA entity for the `product` table: mappings, `@Version` optimistic locking, UTC timestamps set by lifecycle callbacks | 04.01, 04.02 | `ProductRepository`; the service from Phase 05 | Before touching persistence code |
| 04.04 | `backend/src/main/java/com/example/catalog/product/ProductRepository.java` | Spring Data repository: CRUD methods generated by Spring, plus `findBySku`; since Phase 06 `JpaSpecificationExecutor` for search queries assembled at runtime | 04.03 | `ProductService`, tests | To see how the app talks to MySQL |
| 04.05 | `backend/src/main/resources/db/demo/demo-products.sql` | 24 demo products (four per category, two out of stock); not a migration. Since Phase 12 seven of them point at licensed photos under `/images/products/` | 04.01 | `DemoDataLoader` | When changing the demo catalog |
| 04.06 | `backend/src/main/java/com/example/catalog/demo/DemoDataLoader.java` | Dev-only bean that loads the demo SQL once, into an empty table; first use of constructor injection | 04.05, DataSource, JdbcTemplate | Spring Boot at startup with the `dev` profile | To learn dependency injection and profiles |
| 04.07 | `backend/src/test/java/com/example/catalog/TestcontainersConfiguration.java` | One shared MySQL 8.4.11 container for the whole test run, connected through `@ServiceConnection`; since Phase 09 also Redis 8.10.2 and small idle connection pools | Docker, Testcontainers 2 | Every test that starts Spring with a database | When a test needs a database |
| 04.08 | `backend/src/test/java/com/example/catalog/product/ProductRepositoryTest.java` | Persistence tests on real MySQL: round trip, UTC storage, duplicate SKU, six CHECK constraints, JPA error type, optimistic locking | 04.01–04.04, 04.07 | Maven Surefire | To see what the database guarantees |
| 04.09 | `backend/src/test/java/com/example/catalog/demo/DemoDataLoaderTest.java` | Demo data loads only with `dev`, only into an empty table, never twice | 04.05–04.07 | Maven Surefire | To see how profile-specific beans are tested |
| 05.01 | `backend/src/main/java/com/example/catalog/config/CatalogProperties.java` | Typed `catalog.*` settings; the `writesEnabled` switch | The YAML files | `ProductService` | To see how settings become Java values |
| 05.02 | `backend/src/main/java/com/example/catalog/product/ProductCreateRequest.java` | POST body record with Bean Validation rules mirroring the database constraints; since Phase 12 `imageUrl` may also be a path to one of the storefront's own photos | 04.02 | `ProductController`, `ProductMapper` | Before changing what clients may send |
| 05.03 | `backend/src/main/java/com/example/catalog/product/ProductUpdateRequest.java` | PUT body record: the complete product plus the version last read; since Phase 12 the same `imageUrl` rule as 05.02 | 04.02 | `ProductController`, `ProductService`, `ProductMapper` | To understand optimistic locking in the API |
| 05.04 | `backend/src/main/java/com/example/catalog/product/ProductResponse.java` | Response record: the public shape of a product, including `inStock` and `version` | 04.02 | `ProductMapper`, `ProductController`; the Redis cache from Phase 09 | Before changing what clients receive |
| 05.05 | `backend/src/main/java/com/example/catalog/product/ProductMapper.java` | Explicit DTO ⇄ entity mapping (trims text, prices to two decimals) | 04.03, 05.02–05.04 | `ProductService` | When a field is added |
| 05.06 | `backend/src/main/java/com/example/catalog/product/ProductNotFoundException.java` | Domain error: no product with that id (404) | — | `ProductService`, `ApiExceptionHandler` | With the error handler |
| 05.07 | `backend/src/main/java/com/example/catalog/product/DuplicateSkuException.java` | Domain error: SKU already used (409), translated from MySQL's unique key | — | `ProductService`, `ApiExceptionHandler` | With the error handler |
| 05.08 | `backend/src/main/java/com/example/catalog/product/VersionConflictException.java` | Domain error: outdated version (409), from the version check or Hibernate's `@Version` | — | `ProductService`, `ApiExceptionHandler` | With the error handler |
| 05.09 | `backend/src/main/java/com/example/catalog/error/WritesDisabledException.java` | Domain error: read-only deployment (403) | — | `ProductService`, `ApiExceptionHandler` | With the error handler |
| 05.10 | `backend/src/main/java/com/example/catalog/product/ProductService.java` | Business rules and the transaction boundary for get, create, update, delete; since Phase 06 search (one read-only transaction for the page and count queries); since Phase 09 cache-aside for reading one product; since Phase 10 writes announce `ProductChangedEvent` | 04.04, 05.01, 05.05–05.09, 06.04–06.06 | `ProductController` | To understand transactions |
| 05.11 | `backend/src/main/java/com/example/catalog/product/ProductController.java` | HTTP endpoints for single products (GET, POST, PUT, DELETE); since Phase 06 search, `GET /api/v1/products` | 05.10, 05.02–05.04, 06.04, 06.05 | Spring MVC | First file to read for the API |
| 13.01 | `backend/src/main/java/com/example/catalog/api/SettingsController.java` | `GET /api/v1/settings`: tells the storefront whether writes are allowed, so it can hide the admin; the server still enforces the switch | 05.01 | The storefront (13.04) | Before adding a client-visible setting |
| 05.12 | `backend/src/main/java/com/example/catalog/error/ApiExceptionHandler.java` | Every error as RFC 9457 Problem Details, with field errors; since Phase 06 also for query parameters, with neutral messages for values that cannot be converted | 05.06–05.09, Spring MVC, Jackson 3 | Spring MVC, for every failed request | To understand the error format |
| 05.13 | `backend/src/test/java/com/example/catalog/product/ProductControllerTest.java` | Web-slice tests with a Mockito test double: statuses, JSON, validation, error types; since Phase 06 the search parameters (defaults, binding, every kind of invalid value); since Phase 12 the `imageUrl` rule | 05.11, 05.12, 06.01–06.05 | Maven Surefire | To see how the HTTP layer is tested in isolation |
| 05.14 | `backend/src/test/java/com/example/catalog/product/ProductServiceTest.java` | Real-transaction tests on MySQL: commit, rollback, version and SKU conflicts, read-only `prod` | 05.10, 04.07 | Maven Surefire | To see transactions proven |
| 06.01 | `backend/src/main/java/com/example/catalog/product/ProductSort.java` | The sort allowlist (`newest`, `price_asc`, `price_desc`, `name_asc`): each an order defined in code that ends with `id` | 04.03, Spring Data `Sort` | `ProductSortConverter`, `ProductSearchRequest`, `ProductService` | Before adding a sort option (and its index, Phase 08) |
| 06.02 | `backend/src/main/java/com/example/catalog/product/ProductSortConverter.java` | Converts `?sort=` text into a `ProductSort`; blank means not sent, an unknown value becomes a 400 | 06.01 | Spring MVC data binding | To see how query text becomes Java types |
| 06.03 | `backend/src/main/java/com/example/catalog/product/ValidPriceRange.java` | Custom cross-field validation rule: `maxPrice` ≥ `minPrice`, reported on `maxPrice` | 06.04, Bean Validation API | Hibernate Validator during `@Valid` | To learn how a rule over two fields is written |
| 06.04 | `backend/src/main/java/com/example/catalog/product/ProductSearchRequest.java` | Search query parameters as a validated record; defaults (page 0, size 20, `newest`) set in a compact constructor | 04.02, 06.01, 06.03 | `ProductController`, `ProductService`, `ProductSpecifications` | Before changing a search parameter |
| 06.05 | `backend/src/main/java/com/example/catalog/api/PageResponse.java` | The page envelope `{content, page, size, totalElements, totalPages}`, reusable by any list endpoint | Spring Data `Page` | `ProductService`, `ProductController` | Before changing the shape of list responses |
| 06.06 | `backend/src/main/java/com/example/catalog/product/ProductSpecifications.java` | Filters as JPA predicates with bound parameters, joined with AND; LIKE escaping | 04.02, 04.03, 06.04 | `ProductService.searchProducts` | To see how the WHERE clause is built safely |
| 06.07 | `backend/src/test/java/com/example/catalog/product/ProductSearchTest.java` | Search on real MySQL: each filter and their combination, case and accents, literal wildcards, inclusive bounds, sorts and ties, paging | 05.10, 06.01–06.06, 04.07 | Maven Surefire | To see what search guarantees |
| 07.01 | `backend/src/test/java/com/example/catalog/product/ProductApiIntegrationTest.java` | Full-stack tests over real HTTP on a random port: the write lifecycle, the JSON contract, every error type, a rollback, Unicode, two races of ten simultaneous requests, and the read-only `prod` profile; since Phase 13 the settings endpoint in both modes | The whole application, 04.07, Spring Boot's `RestTestClient` | Maven Surefire | To see the API proven from the outside |
| 07.02 | `backend/src/test/java/com/example/catalog/product/ProductSearchApiIntegrationTest.java` | Search over real HTTP: filters from a query string, the page envelope, ties across pages, percent-encoded text, and the documented binding behavior | The whole application, 04.07, `RestTestClient` | Maven Surefire | To see search proven from the outside |
| 07.03 | `backend/api-examples.http` | Example requests (search, then create, read, update, conflict, delete) for VS Code's REST Client extension | A running dev backend on `127.0.0.1:24081` | Developers trying the API | Before calling the API by hand |
| 08.01 | `backend/src/main/resources/application-bench.yml` | `bench` profile: the application against the `catalog_bench` database on `127.0.0.1:24083`, read-only, with 100,000 generated products and batched inserts; since Phase 10 the local Redis address | `application.yml`, `infra/.env`, 08.02 | Measurements (Phase 08) and benchmarks (Phase 11) | Before measuring anything |
| 08.03 | `backend/src/main/java/com/example/catalog/bench/BenchmarkProperties.java` | Typed `catalog.bench.products` and `catalog.bench.seed`, with defaults and limits | 08.01 | `BenchmarkDataSeeder` | To change the benchmark size or seed |
| 08.04 | `backend/src/main/java/com/example/catalog/bench/BenchmarkProductGenerator.java` | Deterministic, realistic product generator (fixed seed, integer maths, fixed reference date) | 04.02 | `BenchmarkDataSeeder`, its test | To see how reproducible test data is made |
| 08.05 | `backend/src/main/java/com/example/catalog/bench/BenchmarkDataSeeder.java` | `bench`-only: loads the generated products into an empty table in one transaction, then refreshes the statistics | 08.03, 08.04, JdbcTemplate, TransactionTemplate | Spring Boot at startup with the `bench` profile | With DemoDataLoader (04.06), its dev-only sibling |
| 08.06 | `backend/src/test/java/com/example/catalog/bench/BenchmarkProductGeneratorTest.java` | Same seed gives the same products; a pinned dataset fingerprint; every product passes the API's validation rules; realistic shares | 08.04, 05.02, Hibernate Validator | Maven Surefire (no Docker needed) | To see how a dataset is pinned |
| 08.07 | `backend/src/test/java/com/example/catalog/bench/BenchmarkDataSeederTest.java` | Benchmark data loads only with `bench`, only into an empty table, exactly as generated, never twice | 08.03–08.05, 04.07 | Maven Surefire | With DemoDataLoaderTest (04.09) |
| 09.01 | `backend/src/main/java/com/example/catalog/config/ProductCacheProperties.java` | Typed `catalog.cache.enabled` and `catalog.cache.product-ttl`; refuses a TTL that is not positive | `application.yml` | `ProductDetailCache` | To change the cache switch or TTL |
| 09.02 | `backend/src/main/java/com/example/catalog/product/ProductDetailCache.java` | Redis side of the detail cache: key `catalog:v1:product:{id}`, JSON values, TTL, hit/miss/error counters, Redis failures treated as misses; since Phase 10 `evict` with its own counters | `StringRedisTemplate`, Jackson `JsonMapper`, 09.01, Micrometer | `ProductService.getProduct`, `ProductCacheEvictionListener` | With `docs/CACHING.md` |
| 10.01 | `backend/src/main/java/com/example/catalog/product/ProductChangedEvent.java` | The event "product N was created, updated, or deleted", published inside the write's transaction | — | `ProductService`, `ProductCacheEvictionListener` | With the listener |
| 10.02 | `backend/src/main/java/com/example/catalog/product/ProductCacheEvictionListener.java` | `@TransactionalEventListener(AFTER_COMMIT)`: evicts the product's key only after a commit, never after a rollback | 10.01, 09.02 | Spring, after each committed write | To understand when the cache is cleared |
| 09.03 | `backend/src/test/java/com/example/catalog/product/ProductDetailCacheTest.java` | Against real Redis: miss stores JSON with the TTL, hit skips MySQL, round trip, no caching of missing products, bad values repaired, expiry, switch off | 05.10, 09.02, 04.07 | Maven Surefire | To see the cache proven |
| 10.03 | `backend/src/test/java/com/example/catalog/product/ProductCacheInvalidationTest.java` | Committed update, delete, and create evict; a rolled-back update does not; the stale-read race played step by step | 05.10, 09.02, 10.02, 04.07 | Maven Surefire | To see when the cache is cleared |
| 10.04 | `backend/src/test/java/com/example/catalog/product/ProductCacheOutageTest.java` | Redis paused mid-test: reads fall back within the timeouts, a write commits while its eviction fails (counted), the cache recovers | 05.10, 09.02, 04.07 (pauses its Redis) | Maven Surefire | To see the outage behavior proven |
| 09.04 | `backend/src/test/java/com/example/catalog/TestcontainersConfigurationTest.java` | Checks the shared test setup: one idle MySQL connection per test application; Redis from the test container | 04.07 | Maven Surefire | When tests run out of connections |
| 08.11 | `backend/src/main/resources/db/migration/V2__add_search_indexes.sql` | Flyway migration adding the four search indexes in one atomic, online `ALTER TABLE` | 04.01, measurements in `benchmarks/` | Flyway at startup and in tests; MySQL's optimizer | With `docs/QUERY_OPTIMIZATION.md` |

Files the generator produced that we did **not** keep: `HELP.md` (links only), `backend/.gitignore` and
`backend/.gitattributes` (the root files cover them; the useful Java-IDE patterns moved into the root `.gitignore`),
`application.properties` (replaced by the YAML files), and `CatalogApplicationTests` (`HealthEndpointTest` also
starts the application).

<!-- BUILD FLOW 01.06.05 — infra/ and scripts/ (added in Phase 03) -->
## infra/ and scripts/

| BUILD FLOW | Path | Purpose | Depends on | Caller / consumer | When to read |
|---|---|---|---|---|---|
| 03.01 | `infra/compose.dev.yaml` | Local MySQL 8.4.11 and Redis 8.10.2 containers: pinned images, health checks, `127.0.0.1`-only ports 24306/24379, named MySQL volume, Redis as a pure cache | Docker Desktop; `infra/.env` | `scripts/dev-services.ps1`; the backend's dev profile from Phase 04 | Before changing local services or ports |
| 03.02 | `infra/.env.example` | Template for the private `infra/.env`: database name, user, password placeholders, host ports | — | `scripts/dev-services.ps1` (copies it on the first `up`) | When adding a setting that Compose needs |
| 03.03 | `scripts/dev-services.ps1` | One command for `up`, `status`, `logs`, `down`, and `reset -Force`; creates `infra/.env` with random passwords on the first `up` | `infra/compose.dev.yaml`, `infra/.env.example`, Docker | The developer on Windows | Whenever you start or stop local services |
| 08.02 | `scripts/bench-db.ps1` | Creates, inspects (rows, migrations, indexes and their sizes), empties, or drops `catalog_bench`; switches MySQL's general query log on and off to capture the application's SQL | `infra/compose.dev.yaml`, `infra/.env`, Docker | The developer, before and during measurements | Before measuring queries |
| 11.03 | `scripts/run-benchmarks.ps1` | Runs the whole benchmark protocol (fresh backend per part, warm-up, trials, cache counters) with k6 from Docker, keeping the computer awake; repeats trials disturbed outside the backend and stops on backend errors, sleep, or standby (D-049); then writes the merged raw results and the report | Docker, 08.02, the jar, 11.01, 11.02 | The developer | Before running or questioning a benchmark |
| 08.09 | `scripts/measure-queries.ps1` | Runs each query of a query file with `EXPLAIN ANALYZE` (warm, three times, median) and fingerprints its result; compares with a baseline and fails if any result differs | `benchmarks/queries/`, a filled `catalog_bench` | The developer | To produce or check measurements |

<!-- BUILD FLOW 01.06.06 — benchmarks/ (added in Phase 08) -->
## benchmarks/

| BUILD FLOW | Path | Purpose | Depends on | Caller / consumer | When to read |
|---|---|---|---|---|---|
| 08.08 | `benchmarks/queries/search-queries.sql` | The 12 representative search statements, exactly as MySQL received them, each with the API request that produced it | The captured query log | `scripts/measure-queries.ps1` | To see the SQL the API really sends |
| 08.10 | `benchmarks/query-plans/before-v2.txt` | **(generated)** Measurements before V2: context, summary, and every query's times, result fingerprint, and plan | 08.08, 08.09, `catalog_bench` at V1 | `docs/QUERY_OPTIMIZATION.md` | With the after file |
| 08.12 | `benchmarks/query-plans/after-v2.txt` | **(generated)** Measurements after V2, with the comparison: every result identical | 08.08, 08.09, 08.10, `catalog_bench` at V2 | `docs/QUERY_OPTIMIZATION.md` | To see what the indexes changed |
| 08.13 | `benchmarks/queries/index-alternatives.sql` | "What if" variants with index hints: what one index contributes, and why the optimizer chose as it did | 08.08, the V2 indexes | `scripts/measure-queries.ps1` | When questioning an index |
| 11.01 | `benchmarks/k6/product-detail.js` | k6 load for `GET /api/v1/products/{id}` over 1,000 fixed ids: constant virtual users for a time, or one pass per id (cold cache) | The bench backend | `scripts/run-benchmarks.ps1` | To see exactly what was measured |
| 11.02 | `benchmarks/k6/search.js` | k6 load for four storefront searches in rotation, each tagged and reported separately | The bench backend | `scripts/run-benchmarks.ps1` | To see exactly what was measured |
| 11.04 | `benchmarks/results/k6-summaries.json` | **(generated)** The setup, the attempts set aside, and every counted trial's complete k6 summary, plus the cache counters around each product-detail run | 11.01–11.03 | `k6-report.md`, `docs/PERFORMANCE.md` | To trace a number back to its source |
| 11.05 | `benchmarks/results/k6-report.md` | **(generated)** Tables computed from `k6-summaries.json`: medians and ranges per scenario | 11.04 | `docs/PERFORMANCE.md`, the README | To see the results at a glance |
| 08.14 | `benchmarks/query-plans/index-alternatives.txt` | **(generated)** Measurements of the "what if" variants | 08.13, 08.09 | `docs/QUERY_OPTIMIZATION.md` | With section "The four indexes" |

<!-- BUILD FLOW 01.06.07 — frontend/ (added in Phase 12) -->
## frontend/

| BUILD FLOW | Path | Purpose | Depends on | Caller / consumer | When to read |
|---|---|---|---|---|---|
| 12.01 | `frontend/package.json` | **(JSON)** The storefront's npm manifest: scripts (`dev`, `build`, `preview`, `lint`, `test`), React 19 as the only runtime dependency, and the build and test tools as development dependencies, all at exact versions | npm registry | npm, `package-lock.json` | When adding a package or a script |
| 12.02 | `frontend/package-lock.json` | **(generated)** The exact version and checksum of every package, including indirect ones, written by `npm install`; makes installs reproducible | 12.01 | `npm ci` (Phase 14) | Never edited by hand |
| 12.03 | `frontend/vite.config.js` | Vite settings: the React plugin, the dev and preview servers on `127.0.0.1:24173`, the `/api` proxy to the backend on 24081, and the Vitest settings | 12.01 | `npm run dev`, `build`, `preview`, `test` | When the dev server, proxy, or tests misbehave |
| 12.04 | `frontend/.oxlintrc.json` | **(JSON)** Linter rules (oxlint, the linter the Vite template uses): React hooks rules, accessibility (`jsx-a11y`), test rules | oxlint | `npm run lint` | When a lint rule is questioned |
| 12.05 | `frontend/index.html` | The one HTML page: metadata, the `#root` element, and the script that starts the app | 12.06, 12.07 | Vite, the browser | Rarely |
| 12.06 | `frontend/public/favicon.svg` | The browser-tab icon, an "F" drawn from straight lines | — | 12.05 | Rarely |
| 12.07 | `frontend/src/main.jsx` | Entry point: loads the styles and renders `<App />` in StrictMode | 12.08, 12.09, 12.29 | 12.05 | To see how React starts |
| 12.08 | `frontend/src/styles/tokens.css` | Design tokens: colours (contrast-checked), type, spacing, motion | — | 12.09 | Before changing the look |
| 12.09 | `frontend/src/styles/app.css` | All component styles, mobile first, with the 48rem and 64rem layouts and reduced-motion support; since Phase 13 the product page, admin list, form, error summary, and confirmation dialog | 12.08 | Every component | Before changing a layout |
| 12.10 | `frontend/src/lib/catalog.js` | Pure catalog rules: categories, sorts, search state ↔ address bar, the API query, price checks, page numbers, INR formatting; since Phase 13 the placeholder monogram | The API contract (04.02, 06.01, 06.04) | App, hooks, components, 12.31 | To understand what the URL and the API receive |
| 12.11 | `frontend/src/lib/photoCredits.js` | The credit for each product photo, as the licenses require | 12.33–12.39 | 12.28 | When adding a photo |
| 12.12 | `frontend/src/api/products.js` | The only network code: search, and since Phase 13 one product, create, update, delete, and settings; every failure becomes an `ApiError` with the problem type and field errors | 05.11, 05.12, 13.01, 12.03 | 12.15, 13.04, 13.05, 13.09, 13.10 | To see how errors reach the screen |
| 12.13 | `frontend/src/hooks/useUrlState.js` | The address bar as an external store (`useSyncExternalStore`); since Phase 13 the router's core: `navigate()`, and scroll and focus after each move | — | 12.29, 13.03, 13.10 | To understand shareable URLs and in-app navigation |
| 12.14 | `frontend/src/hooks/useDebouncedCallback.js` | Runs a function only after calls pause; `.cancel()` drops a pending call | React | 12.18, 12.20 | To understand debouncing |
| 12.15 | `frontend/src/hooks/useProductSearch.js` | Fetches one page per query, aborts the previous request, derives loading, offers retry; since Phase 13 remembers recent answers for Back | 12.12 | 13.07, 13.09 | To understand stale-request cancellation |
| 12.16 | `frontend/src/components/SiteHeader.jsx` | The sticky header with the wordmark; the search box goes in as children; since Phase 13 the Admin link when writes are on | 12.09, 13.02, 13.03 | 12.29 | Rarely |
| 12.17 | `frontend/src/components/Hero.jsx` | The compact editorial introduction (the page's h1) | 12.09 | 12.29 | When changing the shop's copy |
| 12.18 | `frontend/src/components/SearchBox.jsx` | The search field: debounced, Enter to search now, clear button, follows outside changes; since Phase 13 its own id and label for a second search on a page | 12.14 | 12.29, 13.09 | To see controlled inputs with a draft |
| 12.19 | `frontend/src/components/CategoryTabs.jsx` | "All" plus the six categories as toggle buttons | 12.10 | 12.29 | Rarely |
| 12.20 | `frontend/src/components/FilterPanel.jsx` | Price range (checked before sending) and "In stock only" | 12.10, 12.14 | 12.29, 12.21 | When changing a filter |
| 12.21 | `frontend/src/components/FilterDrawer.jsx` | The filters for narrow screens, in a native modal `<dialog>` | 12.09 | 12.29 | To see an accessible dialog without a library |
| 12.22 | `frontend/src/components/ActiveFilters.jsx` | Removable chips for active filters, and "Clear all" | 12.10 | 12.29 | Rarely |
| 12.23 | `frontend/src/components/ResultsToolbar.jsx` | The live result count, the Filters button, and the sort menu | 12.10 | 12.29 | Rarely |
| 12.24 | `frontend/src/components/ProductGrid.jsx` | The responsive list of cards, keyed by product id; passes the results address to each card (Phase 13) | 12.25 | 13.07 | Rarely |
| 12.25 | `frontend/src/components/ProductCard.jsx` | One product: photo on a mat or a monogram placeholder, category, name, INR price, "Sold out"; since Phase 13 the whole card links to the product page | 12.10, 13.02, 13.03 | 12.24 | When changing what a card shows |
| 12.26 | `frontend/src/components/Pagination.jsx` | Previous, Next, and a window of page numbers, capped at the API's last page | 12.10 | 12.29 | Rarely |
| 12.27 | `frontend/src/components/StatusViews.jsx` | The loading skeleton, three kinds of empty state, and the error with "Try again" | 12.09 | 12.29 | When changing what shoppers see when things go wrong |
| 12.28 | `frontend/src/components/SiteFooter.jsx` | The footer: "nothing here is for sale" and the photo credits | 12.11 | 12.29 | When adding a photo |
| 12.29 | `frontend/src/App.jsx` | The frame and router: header, the page for the current address (since Phase 13; the catalog moved to 13.07), footer; the search state and `update` | 12.10, 12.13, 13.02, 13.04, 13.07–13.11 | 12.07 | First, to see how the storefront fits together |
| 12.30 | `frontend/src/test/setup.js` | Test setup: DOM matchers, a clean page, address, and app memory after each test; since Phase 13 stand-ins for `showModal()` and `scrollTo()`, which jsdom lacks | Testing Library, 12.15, 13.04 | 12.03 | When tests leak into each other |
| 12.31 | `frontend/src/lib/catalog.test.js` | Unit tests of the catalog rules | 12.10 | `npm test` | To see the URL and formatting rules in action |
| 12.32 | `frontend/src/App.test.jsx` | Behaviour tests of the catalog against a fake API: results, a restored URL, debounced typing, cancelled requests, page reset, chips, errors, image fallback | 12.29, 13.17 | `npm test` | To see what the storefront promises |
| 13.02 | `frontend/src/lib/routes.js` | The pages and their addresses: `paths` builds links, `matchRoute` turns a path into a page | — | 12.29 and every link | Before adding a page |
| 13.03 | `frontend/src/components/Link.jsx` | A real `<a href>` whose plain clicks navigate inside the app; modified clicks stay with the browser | 12.13 | Header, cards, pages | To see client-side navigation done accessibly |
| 13.04 | `frontend/src/hooks/useSettings.js` | The server's settings, fetched once and shared; read-only if they can't be read | 12.12, 13.01 | 12.29 | To see how the UI follows the write switch |
| 13.05 | `frontend/src/hooks/useProduct.js` | Loads one product with cancellation and derived loading | 12.12 | 13.08, 13.10 | Rarely |
| 13.06 | `frontend/src/hooks/usePageTitle.js` | Sets the browser tab's title per page | — | 13.07–13.11 | Rarely |
| 13.07 | `frontend/src/pages/CatalogPage.jsx` | The catalog (moved out of App): hero, categories, filters, results, pagination, drawer | 12.15, 12.17–12.27 | 12.29 | To change the shop's front page |
| 13.08 | `frontend/src/pages/ProductPage.jsx` | One product: photo, facts from the API, the way back to the same results, edit link when allowed | 13.05, 13.03 | 12.29 | To change the product page |
| 13.09 | `frontend/src/pages/AdminPage.jsx` | The admin list: find, add, edit, delete after confirming, messages; the read-only notice | 12.15, 13.12, 12.18, 12.26 | 12.29 | To change the admin list |
| 13.10 | `frontend/src/pages/ProductFormPage.jsx` | Add or edit a product: browser-side checks, the server's answers by field, error summary, version conflicts | 13.13, 13.14, 13.05 | 12.29 | To see form validation and error handling end to end |
| 13.11 | `frontend/src/pages/NotFoundPage.jsx` | The page for unknown addresses, with a way back | 13.03 | 12.29 | Rarely |
| 13.12 | `frontend/src/components/ConfirmDialog.jsx` | A native modal question with Cancel focused first; shows progress and errors | — | 13.09 | To see an accessible confirmation |
| 13.13 | `frontend/src/components/FormField.jsx` | Label, hint, error, and control wired with `aria-describedby` and `aria-invalid` | — | 13.10 | To see accessible form fields |
| 13.14 | `frontend/src/lib/productForm.js` | The form's rules: fields, checks mirroring the API, conversions to and from the JSON body | 12.10 | 13.10, 13.15 | Before changing what the form accepts |
| 13.15 | `frontend/src/lib/productForm.test.js` | Unit tests of the form's rules | 13.14 | `npm test` | With 13.14 |
| 13.16 | `frontend/src/lib/routes.test.jsx` | Unit tests of the routes and of which clicks `Link` takes | 13.02, 13.03 | `npm test` | With 13.02 |
| 13.17 | `frontend/src/test/fakeApi.js` | The fake backend the page tests share: records calls, answers like the real API | — | 12.32, 13.18, 13.19 | When writing a page test |
| 13.18 | `frontend/src/pages/ProductPage.test.jsx` | Behaviour tests of the product page: facts, title, 404, back to the same results, edit link | 13.08, 13.17 | `npm test` | With 13.08 |
| 13.19 | `frontend/src/pages/AdminPages.test.jsx` | Behaviour tests of the admin: read-only, list, checks before sending, server answers, version conflict, confirmed delete | 13.09, 13.10, 13.17 | `npm test` | With 13.09 and 13.10 |
| 12.33 | `frontend/public/images/products/frm-app-003.jpg` | **(binary)** Photo of the Relaxed Twill Chinos (CC BY-SA 3.0; credit in 12.40) | — | The API's `imageUrl`, 12.25 | With `docs/IMAGE_CREDITS.md` |
| 12.34 | `frontend/public/images/products/frm-app-004.jpg` | **(binary)** Photo of the Merino Crewneck Sweater (CC BY 2.0) | — | The API's `imageUrl`, 12.25 | With `docs/IMAGE_CREDITS.md` |
| 12.35 | `frontend/public/images/products/frm-ftw-003.jpg` | **(binary)** Photo of the Suede Desert Boot (CC BY-SA 4.0) | — | The API's `imageUrl`, 12.25 | With `docs/IMAGE_CREDITS.md` |
| 12.36 | `frontend/public/images/products/frm-hom-002.jpg` | **(binary)** Photo of the Ceramic Bud Vase (CC BY 4.0) | — | The API's `imageUrl`, 12.25 | With `docs/IMAGE_CREDITS.md` |
| 12.37 | `frontend/public/images/products/frm-hom-004.jpg` | **(binary)** Photo of the Waffle Cotton Bath Towel (public domain) | — | The API's `imageUrl`, 12.25 | With `docs/IMAGE_CREDITS.md` |
| 12.38 | `frontend/public/images/products/frm-kit-002.jpg` | **(binary)** Photo of the Cast Iron Skillet 26 cm (CC BY-SA 4.0) | — | The API's `imageUrl`, 12.25 | With `docs/IMAGE_CREDITS.md` |
| 12.39 | `frontend/public/images/products/frm-sta-004.jpg` | **(binary)** Photo of the Undated Weekly Planner (CC BY 2.0) | — | The API's `imageUrl`, 12.25 | With `docs/IMAGE_CREDITS.md` |

<!-- BUILD FLOW 01.06.03 — Generated files -->
## Generated files

Generated files are never edited to add comments. Their explanation lives in the rows above.
- Maven Wrapper (Phase 02): `backend/mvnw`, `backend/mvnw.cmd`, `backend/.mvn/wrapper/maven-wrapper.properties`.
- Measurements (Phase 08): the three files in `benchmarks/query-plans/`, written by `scripts/measure-queries.ps1`.
  Each starts with a header saying so; they are regenerated, never edited.
- Benchmarks (Phase 11): `benchmarks/results/k6-summaries.json` and `k6-report.md`, written by
  `scripts/run-benchmarks.ps1`; regenerated by each run, never edited.
- npm lockfile (Phase 12): `frontend/package-lock.json`, written by `npm install`; changed only by npm.

Files without a header that are not generated: the two JSON files in `frontend/` (JSON has no comments) and the
product photos in `frontend/public/images/products/`. Their rows above explain them.
