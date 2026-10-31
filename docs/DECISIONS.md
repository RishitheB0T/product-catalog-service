<!--
BUILD FLOW 01.08 — docs/DECISIONS.md
First introduced: Phase 01
Purpose: Record important choices: the context, the decision, the alternatives, and the trade-offs.
Depends on: docs/PROJECT_PLAN.md (the requirements each decision serves).
Read by: Anyone asking "why X and not Y?", which also makes it good interview preparation.
Runtime role: None. Documentation only.
-->

# Decisions

Each entry has a status: **Accepted**, **Planned** (to be confirmed in a later phase), or **Superseded** (kept for
history, with a pointer to its replacement).

<!-- BUILD FLOW 01.08.01 — Decisions made in Phase 01 -->
## D-001 · One repository for backend, frontend, infrastructure, and docs — Accepted (Phase 01)

- **Context:** one developer, one product, and one history to show on GitHub.
- **Decision:** a single repository with `backend/`, `frontend/`, `infra/`, `scripts/`, `benchmarks/`, and `docs/`.
- **Alternative:** a separate repository for each part.
- **Trade-offs:** one commit can change the API and the UI together, and CI sees everything. In exchange, CI must
  check both stacks, and the history mixes Java and JavaScript work. Commit scopes such as `feat(api)` and
  `feat(frontend)` keep the history readable.

## D-002 · Spring Boot 4.1.1 on Java 21 — Accepted (Phase 01)

- **Context:** the requirements call for Java 21 and a current stable Spring Boot release.
- **Decision:** Spring Boot 4.1.1. It is the current GA release and start.spring.io's default on 2026-10-20, and it
  supports Java 17 through 26.
- **Alternatives:** 4.0.8 (the older GA line); 4.2.0-M2 (a milestone, which the version rules exclude); Spring Boot 3.x
  (the previous generation).
- **Trade-offs:** many online tutorials still show Spring Boot 3 names, such as `spring-boot-starter-web`,
  Jackson 2 classes, and Testcontainers 1.x artifact names. We point out these differences when they appear. For
  example, start.spring.io now generates `spring-boot-starter-webmvc` and one test starter per technology.

## D-003 · Version baseline and pinning — Accepted (Phase 01)

- **Decision:** use the exact versions in plan §4, verified on 2026-10-20. Floating tags are excluded, lockfiles
  are committed, direct npm dependencies are saved with exact versions, and each pin is re-checked in the phase
  that adds it.
- **Notable picks:**
  - MySQL **8.4.11**: the newest patch of the 8.4 LTS line that the requirements specify.
  - Redis **8.10.2**: the newest stable line. Older lines (8.8, 8.6, 8.4) still receive patches, but nothing here needs them.
  - Node.js **24.22.0 LTS** for container and CI builds. Node 26 becomes LTS on 2026-10-28 according to the
    official schedule, so this is re-checked in Phase 12. The local Node 26.8.1 meets every planned package's
    `engines` range.
  - k6 **2.3.0**: GitHub marks it as the latest release, while 1.8.x is a maintenance line.
  - Maven **3.10.0** through the wrapper: Apache's recommended release. Maven 4 is still a release candidate.
- **Trade-offs:** newer lines have fewer community examples, and pinned versions need deliberate updates. That is
  the point: no surprise upgrades.

## D-004 · Add dependencies only in the phase that uses them — Accepted (Phase 01)

- **Decision:** Phase 02 starts with Web MVC, Validation, and Actuator only. Database dependencies arrive in
  Phase 04 and Redis in Phase 09.
- **Why:** with JPA on the classpath, the application refuses to start without a database. Adding each piece when
  it is needed keeps every phase runnable and every dependency explainable.
- **Trade-off:** `pom.xml` changes in several phases. Each change is recorded in `BUILD_GUIDE.md`.

## D-005 · One canonical plan document — Accepted (Phase 01)

- **Decision:** the plan lives only in `docs/PROJECT_PLAN.md`; the README and the other guides link to it instead
  of repeating it.
- **Alternative:** spreading plan details across the README and each guide, where the copies drift apart.
- **Trade-off:** one more file to open, in exchange for a single source of truth.

## D-006 · LF line endings in the repository — Accepted (Phase 01)

- **Context:** we develop on Windows (CRLF) but run scripts in Linux containers, CI, and on EC2 (LF). A shell script
  saved with CRLF fails on Linux. This machine's Git has `core.autocrlf=true` set system-wide.
- **Decision:** `.gitattributes` stores text files with LF and checks them out with LF. `.cmd`, `.bat`, and `.ps1`
  files keep CRLF. Images, fonts, and archives are binary.
- **Trade-off:** Windows editors must handle LF files. VS Code does.

<!-- BUILD FLOW 01.08.02 — Planned decisions -->
## D-007 · The public deployment is read-only, enforced by the server — Accepted (implemented in Phase 05)

- **Decision:** a server-side write switch, on for local development and off in production configuration. Hiding
  buttons is not security.
- **Implementation:** see D-026 (403 Forbidden, checked in the service, off by default). Phases 14–15 deploy it.

## D-008 · Cache only product details first; search stays on MySQL — Accepted (implemented in Phase 09)

- **Why:** a detail key maps to exactly one row, so invalidation is precise. Search results depend on many rows and
  on the filters, so caching them would need broad invalidation.

<!-- BUILD FLOW 01.08.04 — Decisions made after Phase 01 -->
## D-009 · Public GitHub repository, pushed after every commit — Accepted (2026-10-20, before Phase 02)

- **Context:** the history should be visible on GitHub as it grows, and CI (Phase 14) needs a remote.
- **Decision:** the public repository `RishitheB0T/product-catalog-service`, created with GitHub CLI 2.102.0
  (installed with winget, signed in through the browser device flow). After every commit, `origin/main` is pushed
  and verified.
- **Alternatives:** a private repository, which could be made public later; creating the repository on a
  pre-filled github.com page instead of installing the CLI.
- **Trade-offs:** everything committed is public immediately, so the secret scan before each commit matters even
  more. In return, the step-by-step history is visible, GitHub Actions is free for public repositories, and `gh`
  can check CI runs from the terminal in Phase 14.

## D-010 · Java package `com.example.catalog` — Accepted (Phase 02)

- **Context:** Spring Initializr needs a group and package name when it generates the project.
- **Decision:** groupId `com.example`, base package `com.example.catalog`.
- **Why:** `example.com` is reserved for documentation (RFC 2606), so the name never claims someone else's domain.
- **Alternative:** a namespace tied to the GitHub account, such as `io.github.rishitheb0t.catalog`.
- **Trade-off:** it looks generic. Renaming later is a mechanical refactor that gets bigger as the code grows.

## D-011 · Safe defaults in the base configuration; profiles relax or pin them — Accepted (Phase 02)

- **Decision:** `application.yml` holds production-safe settings. The `dev` profile relaxes them (health details,
  DEBUG logs for our package, port 8081). The `prod` profile restates the locked-down values, so an edit to the
  base file cannot loosen production.
- **Alternative:** a developer-friendly base file plus a strict `prod` profile.
- **Trade-off:** local runs must remember `dev`. The alternative fails worse: forgetting to activate `prod` on a
  server would expose development settings publicly.

## D-012 · Pin the Maven download with a verified SHA-256 — Accepted (Phase 02)

- **Decision:** `distributionSha256Sum` in `maven-wrapper.properties`. The value was computed only after the
  downloaded zip's SHA-512 matched both Apache's and Maven Central's published values.
- **Why:** the wrapper scripts refuse to run a tampered or corrupted Maven download.
- **Trade-off:** upgrading Maven means updating the URL and the checksum together.

## D-013 · Keep Spring Boot 4's liveness/readiness probes; test the intent, not exact JSON — Accepted (Phase 02)

- **Context:** Spring Boot 4 enables the liveness and readiness probe groups automatically, so the health body is
  `{"groups":["liveness","readiness"],"status":"UP"}`. Our first test expected `{"status":"UP"}` and failed.
- **Decision:** keep the probes; Phase 14 container health checks can use `/actuator/health/readiness`. Tests now
  assert "UP, and no component details", and a separate test checks both probe URLs.
- **Alternative:** `management.endpoint.health.probes.enabled: false` to get exactly `{"status":"UP"}`.
- **Trade-off:** the public body reveals two group names, which is harmless; component details stay hidden.

## D-014 · Health tests call the real server with the JDK's HttpClient — Accepted (Phase 02)

- **Alternatives:** MockMvc (no real server or port), Spring's `RestTestClient` (needs
  `@AutoConfigureRestTestClient`), `TestRestTemplate` (needs the extra `spring-boot-restclient` dependency in
  Spring Boot 4).
- **Why:** no extra dependency or annotation, the HTTP call is plain to read, and it exercises Tomcat, Spring MVC,
  Actuator, and the configuration files together.
- **Revisit:** the API tests in Phases 05–07 need JSON assertions and may use `RestTestClient` or MockMvc.

## D-015 · Local development port 8081 — Superseded by D-016 (Phase 03)

- **Context:** another program listens on port 8080 on the development machine.
- **Decision (Phase 02):** the `dev` profile sets `server.port: 8081`. The base and `prod` settings keep Spring
  Boot's default 8080, which is safe inside containers because each container has its own network namespace.
- **Why superseded:** 8081 is itself a common default in other projects; see D-016.

## D-016 · A project-specific block of host ports (24xxx), loopback only — Accepted (Phase 03)

- **Context:** several projects run on the development machine. At inspection, 8080, 5432, and 9090 were already
  taken, and another project's MySQL container was configured for 3306.
- **Decision:** every host port this project uses comes from the 24xxx block, `24` followed by the last three digits of
  the usual port: backend 24081, MySQL 24306, Redis 24379, Vite 24173 (Phase 12), production-style site 24080
  (Phase 14). Local services listen on `127.0.0.1` only, including the dev backend (`server.address`).
- **How the block was checked:** none of the five ports was in use, none falls inside Windows' reserved port ranges
  (`netsh interface ipv4 show excludedportrange protocol=tcp`), and none appears in the other projects' configuration files.
- **Trade-off:** non-standard ports must be documented (README "Ports") and typed explicitly. Inside containers the
  usual ports stay (3306, 6379, 8080), because containers cannot collide there.

## D-017 · Local services through Docker Compose — Accepted (Phase 03)

- **Decision:** `infra/compose.dev.yaml` runs MySQL and Redis with exact image tags, health checks, a named MySQL
  volume, and an explicit Compose project name (`product-catalog-dev`). `scripts/dev-services.ps1` wraps the
  commands for Windows.
- **Why the project name:** Compose prefixes container, network, and volume names with it. The default would be the
  folder name, `infra`, which other projects could share, and then they would manage each other's containers.
- **Alternatives:** installing MySQL and Redis directly on Windows (version drift, harder to reset); Spring Boot's
  Docker Compose integration, which starts containers automatically (convenient, but hides the steps this project
  wants to show).

## D-018 · Random local passwords, never committed — Accepted (Phase 03)

- **Decision:** the first `dev-services.ps1 up` copies `infra/.env.example` to the git-ignored `infra/.env` and
  replaces each placeholder with a 24-character password from .NET's cryptographic random generator, without modulo
  bias. Inside the container, health checks pass the password through the `MYSQL_PWD` variable, never the command line.
- **Alternative:** a fixed default such as `catalog/catalog` in the template. That is simpler, but identical on every
  machine and easy to copy into a real deployment by mistake.
- **Trade-off:** the passwords exist only in `infra/.env`. Deleting that file while keeping the MySQL volume leaves
  a database whose passwords are unknown; `dev-services.ps1 reset -Force` then starts over.

## D-019 · Redis in development is a pure cache — Accepted (Phase 03)

- **Decision:** no RDB snapshots and no append-only file (`--save ""`, `--appendonly no`), a 128 MB memory cap with
  `allkeys-lru` eviction, and no password locally, because the port is loopback-only and holds only public catalog data.
- **Why:** everything in the cache can be rebuilt from MySQL, so persistence would only cost disk writes, and a cap
  keeps memory predictable on a busy laptop.
- **Revisit:** Phase 14 decides authentication and memory settings for the production stack, where Redis is not
  published on any host port.

## D-020 · Product schema: rules in the database, too — Accepted (Phase 04)

- **Decision:** one `product` table created by Flyway (`V1`). Categories are `VARCHAR` plus a `CHECK` allowlist
  rather than MySQL's `ENUM` type. Prices are `DECIMAL(10,2)`; a `BIGINT version` column supports optimistic locking.
  Six named `CHECK` constraints (SKU format, non-blank name and description, category, non-negative price and stock)
  plus a named unique key on `sku`. No search indexes yet.
- **Why:** validation in the API (Phase 05) can be bypassed by any other client or a bug; database constraints
  cannot. Named constraints make errors readable. `VARCHAR` + `CHECK` is portable and easy to change with a migration,
  whereas `ENUM` sorts by position and needs a table rebuild for some changes. Leaving indexes out keeps a genuine
  "before" baseline for the measured optimization in Phase 08.
- **Trade-off:** the category list lives in two places (the Java enum and the constraint), which must change together.

## D-021 · Timestamps: `Instant` stored as UTC in `DATETIME(6)` — Accepted (Phase 04)

- **Decision:** `Instant` fields, `DATETIME(6)` columns, `hibernate.jdbc.time_zone=UTC`, and `connectionTimeZone=UTC`
  in the development connection URL. The entity sets the values in `@PrePersist`/`@PreUpdate`, truncated to
  microseconds; column defaults (`UTC_TIMESTAMP(6)`) serve only SQL seed scripts.
- **Why:** the development JVM runs in IST, and servers often run in UTC. Storing UTC makes values comparable
  everywhere. `TIMESTAMP` would also store UTC but ends on 2038-01-19; `DATETIME(6)` does not.
- **Evidence:** a test reads the raw column and compares it with the `Instant` converted to UTC. In development the
  stored value was exactly 5 h 30 min behind the IST wall clock.

## D-022 · Demo data through a dev-only loader, not a Flyway migration — Accepted (Phase 04)

- **Context:** the plan proposed a Flyway location enabled only in `dev`.
- **Problem:** a database that has once run with `dev` would then contain a migration that other configurations do
  not know about, and Flyway's validation refuses to start in that situation. Flyway should track the schema, not
  optional sample data.
- **Decision:** `DemoDataLoader`, a `@Profile("dev")` bean, runs `db/demo/demo-products.sql` after startup, and only
  when the table is empty.
- **Trade-off:** changes to the demo file reach a database only after it has been emptied (for example with
  `dev-services.ps1 reset -Force`).

## D-023 · One shared MySQL test container for the whole test run — Accepted (Phase 04)

- **Decision:** `TestcontainersConfiguration` holds one static `mysql:8.4.11` container that every Spring test context
  uses through `@ServiceConnection`.
- **Why:** each MySQL container needs several seconds and about 400 MB of memory; this machine is short on both. The
  first full run used one container for six test contexts.
- **Consequence:** tests must not depend on table contents. Each test creates uniquely named rows; `@DataJpaTest`
  rolls back; the demo-data test empties the table first.
- **Alternatives:** a new container per test class (slower, more memory); an in-memory database (not MySQL, so
  constraints, SQL dialect, and time handling would differ).

## D-024 · Open Session in View disabled — Accepted (Phase 04)

- **Decision:** `spring.jpa.open-in-view: false`.
- **Why:** with it on, a database session stays open until the HTTP response is written, so data can be loaded
  lazily while JSON is rendered. That hides queries, holds database connections longer, and blurs the line between
  layers. With it off, the service loads what a response needs inside its own transaction (Phase 05).
- **Trade-off:** touching an unloaded association outside a transaction fails loudly (`LazyInitializationException`)
  instead of silently running extra queries, which is the point.

## D-025 · Errors as RFC 9457 Problem Details — Accepted (Phase 05)

- **Decision:** every error response is a `ProblemDetail` (`application/problem+json`). Our own problem types are
  named `https://catalog.example/problems/<name>`; Spring's generic errors keep `about:blank`. Validation failures add
  `errors: [{field, message}]`, sorted by field.
- **Why:** a standard shape that clients and tools understand, supported natively by Spring 7, whose
  `ResponseEntityExceptionHandler` renders Spring MVC's own errors in the same format. `type` is a stable
  machine-readable identifier; `detail` is for people.
- **Safety:** Jackson's raw parse messages, which name Java classes, are replaced by neutral text; stack traces are
  logged but never returned. Since Phase 06 the same applies to query parameters that cannot be converted
  (`?minPrice=abc`, `?sort=popular`); for enums the message lists the accepted values.
- **Alternatives:** a custom error record (simpler, but non-standard); Spring Boot's default error JSON (a different
  shape from our own errors).

## D-026 · Read-only mode: 403 from the service layer, off by default — Accepted (Phase 05)

- **Decision:** `catalog.writes-enabled` is `false` by default and in `prod`, `true` only in `dev`. `ProductService`
  checks it at the start of every write and throws `WritesDisabledException`, answered with **403 Forbidden**
  (`writes-disabled`).
- **Why the service:** every write path goes through it, so no controller or future endpoint can forget the check.
- **Why 403:** the server understood the request but refuses to carry it out here. 405 would claim the resource never
  supports the method; 503 would suggest a temporary outage.
- **Why off by default:** a deployment that is missing its settings must stay read-only, not become writable.
- **Trade-off:** validation runs before the check, so an invalid request gets 400 even in read-only mode.

## D-027 · Optimistic locking in the API: version in the PUT body, 409 on conflict — Accepted (Phase 05)

- **Decision:** responses include `version`; `PUT` must send it back. The service compares it with the stored version
  (409 naming both numbers), and Hibernate's `@Version` catches a change committed between our read and our write (409).
- **Alternative:** HTTP conditional requests (`ETag` and `If-Match`, answered with 412 Precondition Failed). That is
  the standard HTTP mechanism, but the requirements ask for 409 conflicts, and a body field is simpler for the admin
  UI. `If-Match` could be added later.
- **Trade-off:** `DELETE` does not take a version, so deleting a product that someone has just edited still succeeds,
  unless the edit commits between the delete's read and its write.

## D-028 · Separate request records for create and update — Accepted (Phase 05)

- **Decision:** `ProductCreateRequest`, `ProductUpdateRequest` (the same rules plus `version`), and
  `ProductResponse`. No entity crosses the controller boundary.
- **Alternatives:** one record with validation groups, or a shared interface carrying the annotations. Both remove
  duplication but are harder to read and explain.
- **Trade-off:** the validation annotations are written twice and must be kept in step.

## D-029 · The service turns database errors into domain errors — Accepted (Phase 05)

- **Decision:** `ProductService` catches a `DataIntegrityViolationException` that names `uk_product_sku` and throws
  `DuplicateSkuException`; it turns `ObjectOptimisticLockingFailureException` into `VersionConflictException`. The web
  layer only knows domain exceptions.
- **Why not check first?** "Does this SKU exist?" followed by an insert is a check-then-act race: two requests can both
  see "free". The unique key cannot be fooled, so the database decides and the service translates its answer.
- **Trade-off:** matching the constraint name in MySQL's message ties the code to that name; `ProductServiceTest`
  fails if the constraint is ever renamed.

## D-030 · Mockito as a Java agent in tests — Accepted (Phase 05)

- **Context:** since Phase 02 the JDK warned that Mockito attached its agent while tests were running, and that future
  JDKs will block this.
- **Decision:** `maven-dependency-plugin`'s `properties` goal plus Surefire's
  `argLine -javaagent:${org.mockito:mockito-core:jar}` start the test JVM with Mockito as a regular agent.
- **Result:** the dynamic-agent warnings are gone. One harmless JVM notice remains ("Sharing is only supported for boot
  loader classes…"): the agent extends the boot class path, which switches off part of Class Data Sharing, a startup
  optimization, in the test JVM.

## D-031 · Search with Spring Data JPA Specifications — Accepted (Phase 06)

- **Decision:** `ProductRepository` extends `JpaSpecificationExecutor`. `ProductSpecifications` holds one small
  predicate per filter; `matching(…)` joins the filters that were sent with `Specification.allOf` (AND), and
  `findAll(specification, pageable)` runs the page query plus, when needed, the count query.
- **Why:** five optional filters make 32 combinations. Predicates composed at runtime cover all of them, and every
  value travels as a bound parameter, so no SQL is ever assembled from request text.
- **Alternatives:** derived query methods (one per combination); one `@Query` with `(:q IS NULL OR …)` for every
  filter (one statement for all 32 combinations, so every execution carries conditions for filters that were not sent;
  whether the optimizer removes them depends on how the driver sends the values, and on databases that reuse cached
  plans such "catch-all" queries are a known performance trap; the tri-state `inStock` and the escaped LIKE pattern
  also fit badly); JPQL built by string concatenation (easy to get wrong, open to injection); Querydsl (type-safe, but
  an extra dependency with code generation); Query by Example (cannot express ranges). *Wording corrected in Phase 07:
  it first claimed the catch-all query makes index use harder for MySQL, which overstates it.*
- **Trade-off:** attribute names are strings (`"price"`), checked only when a query runs. `ProductSearchTest` runs every
  predicate against MySQL. The JPA static metamodel (`Product_.price`) would check them when compiling, at the cost of
  an annotation processor in the build.

## D-032 · A sort allowlist that always ends with `id` — Accepted (Phase 06)

- **Decision:** `sort` accepts exactly `newest` (the default), `price_asc`, `price_desc`, and `name_asc`. Each maps to an
  order written in `ProductSort`, ending with `id` in the same direction as the main column.
- **Why:** clients never name a column, so no request text reaches `ORDER BY`, and only orders that Phase 08 can
  support with indexes exist. The unique `id` gives every row exactly one position, so offset pages never repeat or
  skip a row when values tie. The same direction lets one index (InnoDB appends the primary key) serve the whole order.
- **Alternative:** Spring Data's `Pageable` controller parameter (`?sort=price,desc`). It accepts any entity attribute
  as a sort key and quietly corrects out-of-range page numbers and sizes instead of reporting them.
- **Trade-off:** a new sort option is a code change, which is also the right moment to check its index.

## D-033 · Offset pagination with validated bounds — Accepted (Phase 06)

- **Decision:** zero-based `page` from 0 to 10,000 and `size` from 1 to 100 (default 20). Values outside these ranges
  get a 400 naming them. A page after the last one is a normal 200 with empty `content` and the real totals.
- **Why the page limit:** Spring Data refuses offsets above `Integer.MAX_VALUE` with an exception (seen in its
  `PageableUtils`), which would surface as a 500; the limit turns that into a clear 400. It also bounds the cost of
  `OFFSET`, which makes MySQL read and discard every skipped row.
- **Alternative:** keyset ("seek") pagination, `WHERE (price, id) > (?, ?)` from the last row seen. Its cost does not
  grow with depth and it is stable while rows are inserted, but it cannot jump to page N and needs cursors in the API.
  Numbered pages over a catalog of this size are well served by offsets.
- **Trade-off:** offset pages can shift when products are added or deleted between two page requests.

## D-034 · Search semantics — Accepted (Phase 06)

- **Decision:**
  - `q`: a contains-match on the name, case- and accent-insensitive through the column's collation
    (`utf8mb4_0900_ai_ci`), with `%`, `_`, and `!` matched literally (`ESCAPE '!'`); at most 100 characters.
  - `category`: exact match. `minPrice` and `maxPrice`: inclusive, non-negative, and min ≤ max (`@ValidPriceRange`).
  - `inStock=true`: stock above 0; `inStock=false`: sold out (stock 0); not sent: both.
  - Blank values count as not sent, and parameters with other names are ignored.
- **Observed binding behavior (kept as is):** a repeated parameter uses its first value (`?category=BAGS&category=HOME`
  searched BAGS). If a value cannot be converted (`?sort=popular`), only conversion errors are reported; the
  validation rules run once every value converts.
- **Why:** predictable rules that both the storefront and the admin view can use, each one tested on MySQL.
- **Trade-off:** `LIKE '%…%'` cannot use an ordinary B-tree index to narrow the rows. Phase 08 measures the cost before
  choosing a remedy (a FULLTEXT index, for example, would also change the matching rules).

## D-035 · Our own page envelope — Accepted (Phase 06)

- **Decision:** search returns `PageResponse<T>` with exactly `content`, `page`, `size`, `totalElements`, and
  `totalPages`.
- **Why:** Spring Data's `PageImpl` serialized as-is has no stable JSON shape; Spring Data 4.1 logs a warning when that
  happens. A record of our own is a contract that changes only when we change it.
- **Alternative:** Spring Data's `PagedModel` (`@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`). It is
  stable too, but it nests the numbers in a `page` object, which differs from the required shape.

## D-036 · Full-stack API tests over real HTTP with `RestTestClient` — Accepted (Phase 07)

- **Decision:** `ProductApiIntegrationTest` and `ProductSearchApiIntegrationTest` start the application on a random
  port (`@SpringBootTest(webEnvironment = RANDOM_PORT)`) and call it through the `RestTestClient` that
  `@AutoConfigureRestTestClient` points at that port. The writable test classes share one configuration, so Spring
  starts that application once. Their rows are committed, so every test uses its own SKUs and names.
- **Why real HTTP:** only a real server shows what lies outside Spring MVC's handler methods: the Location header a
  client follows, 404 for unknown paths, 405 with `Allow`, 415, percent-decoding of query strings, and requests that
  are truly simultaneous on separate server threads (the two race tests). MockMvc (05.13) stays for fast checks of the
  web layer in isolation.
- **Why `RestTestClient`:** it belongs to Spring Framework 7's `spring-test`, and its Spring Boot integration comes
  with the Web MVC test starter, so the build gains no dependency. It checks status, headers, and JSON paths fluently.
  D-014 expected this when it chose the JDK `HttpClient` for the simpler health tests. One trap, seen in the
  auto-configuration: if a `MockMvc` bean exists, the client calls MockMvc instead of the server, so a test asserts
  that requests reach the random port.
- **Alternatives:** `TestRestTemplate` (in the same Boot module, built on the older `RestTemplate`, with every check
  written by hand), REST Assured (an extra dependency), the JDK `HttpClient` (no JSON assertions), and a separate
  Maven Failsafe run for integration tests (it would allow a Docker-free `mvn test`, but nearly every test class
  already needs MySQL).
- **Trade-off:** each distinct test configuration starts another application with its own connection pool against the
  one test MySQL. The run now holds 10 pools of up to 10 connections each; MySQL allows 151 connections by default.

## D-037 · A separate benchmark database and a `bench` profile — Accepted (Phase 08)

- **Decision:** benchmark data lives in `catalog_bench`, a second database in the development MySQL container, created
  by `scripts/bench-db.ps1 create`. The `bench` profile (`application-bench.yml`, port 24083, read-only) points the
  application at it; Flyway owns its schema exactly as in the other environments.
- **Why:** the 24-product demo catalog stays small and readable for development (plan §8), and measurements never
  touch it. One MySQL server means nothing new to install, and the same image as everywhere else.
- **Alternatives:** loading 100,000 products into `catalog` (the storefront would show them in development); a second
  MySQL container (more memory and one more port for no extra insight).
- **Trade-off:** creating the database and granting the application user rights on it needs the MySQL root account,
  which the script uses inside the container only.

## D-038 · Generated, fingerprinted benchmark data — Accepted (Phase 08)

- **Decision:** `BenchmarkProductGenerator` builds products from `java.util.Random` with a fixed seed, integer maths
  only, and a fixed reference date. `BenchmarkDataSeeder` (`bench` profile only) loads them into an empty table in one
  transaction. A test pins the fingerprint of the default dataset (SHA-256 over every field of all 100,000 products)
  and checks every product against the API's own validation rules.
- **Why:** measurements can only be compared on identical data. A seed makes the data reproducible on any machine;
  the fingerprint makes an accidental change to the generator visible.
- **Alternatives:** a recursive SQL query generating rows inside MySQL (fast, but harder to read and to test); a
  downloaded public dataset (licensing and size questions, and it would not match our categories).
- **Trade-off:** the generator ships in the application jar. Like the demo loader, it is inactive unless its profile
  is chosen explicitly.

## D-039 · How queries are measured — Accepted (Phase 08)

- **Decision:** capture the SQL the application really sends (MySQL's general query log), then measure each statement
  with `EXPLAIN ANALYZE`: one warm-up run, then three timed runs whose median counts. Fingerprint every result with
  SHA-256, and fail the comparison if any result differs. Keep "what if" questions as index-hinted variants in a
  separate file.
- **Why:** the plan explains the time; the fingerprints prove an optimization did not change behavior; the median of
  warm runs keeps single noisy runs from deciding anything.
- **Limits:** times are server-side only, on one laptop, one query at a time, with data in memory. Phase 11 measures
  whole HTTP requests.

## D-040 · Four search indexes in V2, chosen from measured plans — Accepted (Phase 08)

- **Decision:** `V2__add_search_indexes.sql` adds `(created_at)`, `(category, created_at)`, `(category, price)`, and
  `(price)` in one atomic, online `ALTER TABLE` (`ALGORITHM = INPLACE, LOCK = NONE`).
- **Why:** before V2, every page query read all 100,000 rows and sorted them (37–49 ms). Each index lets one storefront
  request read its rows already in order and stop after 20 (0.08–0.66 ms, 73–483× faster), with identical results
  (`docs/QUERY_OPTIMIZATION.md`).
- **Not added:** an index on `name` (a contains search cannot use one), on `stock_quantity` (93 % of products are in
  stock), a separate `(category)` index (the composite indexes cover it by the leftmost-prefix rule), and
  `(price, stock_quantity)` (it would break the `price, id` order the page query needs).
- **Trade-off:** 12–18 MB of index space on 100,000 rows, and more work for every insert and every update of an
  indexed column. A catalog is read far more often than written.

## D-041 · Known slow cases, accepted for now — Accepted (Phase 08)

- **Decision:** three measured weaknesses stay as they are:
  - **Counts** still visit every matching row (8–33 ms, more than the pages they accompany).
  - **The deepest pages** cost about 0.1 s, bounded by the page limit (D-033).
  - **A name search for a word that matches nothing** got slower: 44.5 → 193 ms. The optimizer walks the
    `created_at` index hoping to find matches early.
- **Why:** each remedy changes behavior or adds machinery, and its value should first be measured end to end (Phase 11):
  - capped or cached counts, or "next page" without a total;
  - keyset pagination;
  - a FULLTEXT index or a search engine, which change the matching rules.

  A per-statement optimizer setting was measured and does not help.
- **Revisit:** when Phase 11 shows these requests matter, or the catalog grows beyond about 100,000 products.

## D-042 · Redis is not part of the health check — Accepted (Phase 09)

- **Decision:** `management.health.redis.enabled: false`. Health and readiness report MySQL (and the JVM), not Redis.
- **Why:** the cache only speeds up reads; without it, product details come from MySQL. If Redis counted towards
  health, a cache outage would mark the service DOWN, and a load balancer or container platform would stop sending it
  traffic, turning a slowdown into an outage.
- **Trade-off:** a broken Redis does not show up in health. It shows up in the `error` count of the cache metric and
  in the logs instead.

## D-043 · Explicit cache-aside code, JSON written by the application's mapper — Accepted (Phase 09)

- **Decision:** `ProductDetailCache` (a small component using `StringRedisTemplate`) is called directly by
  `ProductService.getProduct`; no `@Cacheable` annotations. Values are `ProductResponse` JSON written by the
  application's own Jackson 3 `JsonMapper`. The read-only database transaction covers only the MySQL read.
- **Why:** every step stays visible and testable: what is looked up, when it is stored, which failures are caught and
  counted. Annotations hide those steps in a proxy, where self-invocation silently disables them and failure handling
  is harder to control. The application's mapper makes the cached JSON identical to the API's.
- **Alternatives:** Spring's cache abstraction (`@Cacheable` with `RedisCacheManager`): less code, but implicit;
  Java serialization (binary, fragile across class changes, unreadable in `redis-cli`).
- **Trade-off:** more lines of code than one annotation.

## D-044 · The cache ships switched off until writes clear it — Accepted (Phase 09); fulfilled in Phase 10

- **Decision:** `catalog.cache.enabled` defaults to `false` in Phase 09; tests and manual checks enable it explicitly.
  Phase 10 adds after-commit eviction and turns it on.
- **Why:** without eviction, an updated or deleted product could be served from Redis for up to 60 seconds. A feature
  that is correct only after the next phase should not be on in between.
- **Phase 10:** after-commit eviction (D-046) and short timeouts (D-047) landed, and the default became `true`.

## D-045 · Small idle connection pools in tests — Accepted (Phase 09)

- **Decision:** `TestcontainersConfiguration` sets `spring.datasource.hikari.minimum-idle=1` for every test
  application (pools still grow to 10 when needed). `TestcontainersConfigurationTest` checks that it applies.
- **Why:** Spring keeps each distinct test configuration's application, and with it its connection pool, for reuse.
  Thirteen configurations with 10 idle connections each would approach MySQL's limit of 151 (D-036).

## D-046 · Evict after commit, through a transactional event — Accepted (Phase 10)

- **Decision:** create, update, and delete publish `ProductChangedEvent(id)` inside their transaction;
  `ProductCacheEvictionListener` deletes the key in `@TransactionalEventListener(phase = AFTER_COMMIT)`. Writes
  delete the key rather than store the new value.
- **Why after the commit:** evicting inside the transaction leaves a gap in which another request re-caches the old,
  still-committed row. After a rollback nothing changed, so nothing is evicted.
- **Why delete, not update:** two concurrent writers could store their values in the opposite order from their
  commits; a delete is idempotent, and the next read stores whatever is committed.
- **Why an event:** the service states what happened; the cache reacts. Spring checks the transaction's outcome, and
  runs the listener in its `afterCompletion` callback, which logs exceptions instead of rethrowing them, so a
  committed write is never reported as failed (checked in Spring Framework 7.0's bytecode).
- **Alternatives:** `@CacheEvict` (runs when the method returns, which can be before the commit);
  `TransactionSynchronizationManager.registerSynchronization` in the service (the same mechanism, less readable);
  database change-data-capture (robust against every write path, but a large extra system).
- **Trade-off:** the stale-read race in `docs/CACHING.md` remains possible; the TTL bounds it.

## D-047 · 200 ms Redis timeouts, and MySQL as the fallback — Accepted (Phase 10)

- **Decision:** `spring.data.redis.timeout` and `connect-timeout` are 200 ms. A failed cache step is logged, counted,
  and replaced by MySQL; a failed eviction is logged and counted.
- **Why:** the Redis client waits 60 seconds by default, so a hung Redis would hold every product request for a minute.
  A local Redis answers in well under a millisecond, so 200 ms only fires when Redis is in trouble.
- **Measured:** with Redis stopped, product reads still answered correctly in about 430 ms (two timeouts plus MySQL);
  with Redis paused in a test, each step gave up after about 200–230 ms; writes succeeded throughout.
- **Trade-off:** a slow but working Redis (for example during a long pause of its own) is treated as failed; and a
  stopped Redis still costs two timeouts per read. Rejecting commands at once while disconnected, or a circuit
  breaker, would remove that cost for a stopped Redis; documented as extensions.

## D-048 · How the benchmarks are run — Accepted (Phase 11)

- **Decision:** k6 2.3.0 from its official Docker image, driven by `scripts/run-benchmarks.ps1`, against the `bench`
  profile on 100,000 generated products. A closed model (10 virtual users, each sending its next request as soon as
  the previous one answered) for 30-second trials, three trials per scenario, after a 20-second warm-up on a freshly
  started backend. Product detail runs with the cache off, cold (each of 1,000 ids once, after deleting exactly those
  keys), and warm; search runs without and with the Phase 08 indexes. Raw k6 summaries and a computed report are
  committed.
- **Why:** the same endpoint and request mix compared under identical conditions, with the switch as the only
  difference; warm-ups remove JVM start-up effects; several trials show the run-to-run spread; the cold runs keep
  "first request" effects out of the warm numbers instead of mixing them in.
- **Alternatives:** an open model (a fixed arrival rate), which separates latency from throughput better but needs a
  rate chosen below saturation for every scenario; JMeter or Gatling (heavier); k6 installed on Windows, which would
  avoid Docker's network forwarding (the source of the rare connection hangs handled in D-049) but gives up the
  pinned, identical-everywhere image.
- **Limits:** everything (k6, the backend, MySQL, Redis, Docker's network forwarding) shares one laptop with 4 cores,
  so absolute numbers depend on the machine and on what else runs; comparisons between scenarios of the same run are
  what the numbers support. The metrics endpoint is switched on only for these local runs, on 127.0.0.1.

## D-049 · When a benchmark run counts — Accepted (Phase 11)

- **Decision:** `scripts/run-benchmarks.ps1` keeps the computer and its display awake while it runs, refuses to start
  on battery (unless `-AllowBattery`), and judges every measured trial by facts about the run, never by its latency:
  - **Set aside and repeated** (up to three attempts, each listed in the report with its reason): a timed run that
    lasted more than 5 s past its planned length; a request that could not open a connection (`dial` in k6's
    "Request Failed" warning); a connection that took more than 5 s to open.
  - **Stops everything:** any other failed request (an error status, a reset, a timeout after connecting), because
    that is the backend's own result; and any sleep or Modern Standby that Windows logged during the run.

  Nothing is written to `benchmarks/results/` unless the whole run passes. The benchmark backend also runs with
  `server.tomcat.max-keep-alive-requests=-1`, so each virtual user keeps one connection for the whole run.
- **Why:** the first full run (2026-10-29) took three hours instead of fifteen minutes. The laptop slipped into Modern
  Standby whenever its display went off, and two 30-second trials lasted 54 minutes and two hours. Later runs on
  mains power found a second problem. Tomcat closes a connection after 100 requests by default, so at several hundred
  requests per second k6 opened several new connections a second through Docker Desktop's forwarding to the host.
  Some of those connections took 19.5 s to open or timed out after 30 s, three attempts in a row, while the measured
  request durations stayed normal. Neither problem shows in the latency numbers themselves. With connections kept
  open, a two-minute probe sent 111,541 requests with no failures, no reconnects, and no connection slower than 3 ms.
  Rules based only on the run's own facts can't be bent toward a preferred result, and repeating a disturbed trial is
  cheaper than repeating the whole protocol.
- **Alternatives:** rerunning everything until one run is clean (the same rules, much slower); keeping disturbed
  trials and explaining them (their throughput is wrong by an unknown amount); dropping the failed requests (hides
  them); a shorter k6 request timeout (shortens a hang but still loses the requests); k6 on Windows (see D-048),
  which keeps Tomcat's default but changes the load generator.
- **Limits:** a disturbance shorter than the thresholds still passes, for example a two-second pause. The trial-to-trial
  ranges in the report are the remaining guard, and `docs/PERFORMANCE.md` reads the numbers with that in mind. Keeping
  connections open removes the cost of a reconnect every 100 requests from every scenario alike. It doesn't change how
  the scenarios compare, but absolute numbers are slightly better than with Tomcat's default.

## D-050 · The storefront's shape: URL state, plain hooks, no router yet — Accepted (Phase 12)

- **Decision:** React 19 with Vite, in plain JavaScript.
  - **State:** the search lives in the address bar's query string, read and written by a small hook (`useUrlState`).
  - **Data:** one hook fetches the results (`useProductSearch`), and one module holds all calls to the API.
  - **Styling:** plain CSS with design tokens, and system font stacks.
  - **Lint:** oxlint, as the current Vite template ships it.
  - **Versions:** exact in `package.json`, plus the lockfile.
  - **No router:** Phase 12 has a single page.
- **Why:**
  - Search state in the URL makes refresh, sharing, and Back/Forward work with no extra code.
  - A router, a state library, or a data-fetching library would add dependencies for what about 60 lines of hooks do
    here.
  - System fonts mean no web-font download and no third-party request.
- **Alternatives:**
  - React Router, which Phase 13 decides with the product detail and admin views.
  - TanStack Query, for caching and retries.
  - A CSS framework.
  - ESLint, which the Vite template no longer ships.
- **Limits:** results are not cached in the browser: going back to a search fetches it again. Each page is a fresh
  request, because the API orders and counts by itself.

## D-051 · Product photos: free-licensed, self-hosted, credited — Accepted (Phase 12)

- **Decision:** seven demo products use photos from Wikimedia Commons, and the other seventeen show a designed
  placeholder.
  - **The rules:** a free license (CC0, public domain, CC BY, CC BY-SA), no visible brand, matching the product.
  - **Storage:** resized copies in `frontend/public/images/products/`, referenced by path from the demo data.
  - **Credits:** in the footer and in `docs/IMAGE_CREDITS.md`.
- **Why:**
  - Openverse did not respond, and most Commons results were museum pieces, people, or other brands' products. A
    fictional shop must not present another company's logo as its own.
  - Hosting the files ourselves avoids hotlinking, a third-party request on every page, and broken images if a remote
    file moves.
  - Showing each photo whole on a shared mat keeps a mixed set looking consistent.
- **Alternatives:** photos supplied by the developer; original illustrations; remote URLs; placeholders only.
- **Limits:** only seven photos, and they don't share a style. CC BY and CC BY-SA require the credits to stay
  visible.

## D-052 · `imageUrl` may be a path to the storefront's own photos — Accepted (Phase 12)

- **Decision:** the create and update requests accept an `imageUrl` that is either an http(s) URL or
  `/images/products/<name>.(jpg|png|webp)`, where the name has only lower-case letters, digits, and hyphens.
- **Why:** the demo data now stores such paths. If the write API rejected them, the admin form of Phase 13 could not
  save a demo product unchanged. The narrow pattern still blocks `javascript:` and `data:` URLs, and paths that climb
  out of the folder.
- **Alternatives:** absolute URLs in the demo data, which tie it to one host and port; or leaving the API stricter
  than the data it serves.
- **Limits:** the API does not check that the file exists; a wrong path shows the placeholder.

## D-053 · Frontend versions re-checked — Accepted (Phase 12)

- **Decision:** on 2026-10-30 the frontend baseline was checked again against the npm registry and the nodejs.org
  release index.
  - Vite moved from 8.3.4 to the patch release 8.3.5, and `create-vite` from 9.3.1 to 9.3.3; both are taken.
  - The Vite template now lints with oxlint (1.90.0) instead of ESLint (D-050).
  - Node 26 became the active LTS line on 2026-10-28, so builds and CI (Phase 14) target Node 26 LTS (26.13.0).
  - Everything else matched the baseline.
- **Why:** the plan's rule (§4): re-check each version in the phase that pins it, and take newer patch releases.
- **Alternatives:** Node 24 LTS, which is supported for longer as a maintenance line but is the older one.
- **Limits:** the development machine runs Node 26.8.1. That is the same major line, but older than the LTS
  release; updating it is recommended, not required.

## D-054 · A small hand-written router — Accepted (Phase 13)

- **Decision:** routing is done with about 60 lines of the project's own code:
  - a hook that reads the address bar through `useSyncExternalStore`;
  - `navigate()`;
  - a `<Link>` that turns plain clicks into in-app navigation;
  - one `matchRoute()` function for the five pages (catalog, product, admin list, new, edit).

  After a move it restores the scroll position (Back and Forward) or starts at the top and moves focus to `<main>`.
  Recent search answers are kept in memory, so Back shows the results at once, at the old position.
- **Why:**
  - Five flat routes need no nesting, loaders, or route-level code splitting.
  - Every line can be read and explained, and the bundle grows by nothing.
  - The parts a library would hide are now explicit: history entries, focus after navigation, and scroll restoration.
- **Alternatives:**
  - React Router 8.5.0, the planned candidate: nested routes, data loaders, and a well-tested scroll restoration. It is
    worth adopting if the site grows nested layouts.
  - Hash-based URLs (`/#/products/42`), which avoid server fallbacks but give worse URLs.
- **Limits:**
  - The server must answer every page address with `index.html`: Vite does this in development, and Nginx needs a
    fallback in Phase 14.
  - Scroll positions are restored only within one visit.

## D-055 · The server tells the storefront whether writes are on — Accepted (Phase 13)

- **Decision:** `GET /api/v1/settings` returns `{"writesEnabled": …}`. The storefront shows the Admin link, the edit
  links, and the admin pages only when it is `true`. If the settings can't be read, it behaves as read-only.
- **Why:**
  - Offering buttons that always fail is poor design.
  - Hiding them is not security: the service refuses every write while the switch is off (403 `writes-disabled`, D-007
    and D-026),
    whatever a client does. The read-only browser check sent a DELETE directly and got exactly that.
  - The flag reveals nothing that a failed write wouldn't.
- **Alternatives:**
  - Building the frontend with a different flag per deployment, which would put configuration in the bundle and
    allow it to disagree with the server.
  - Trying a write and reacting to 403, which is too late for a good interface.
- **Limits:** one request per page load; the value is read once and isn't refreshed while the page stays open.

## D-056 · How the admin handles input and conflicts — Accepted (Phase 13)

- **Decision:**
  - **Checks:** the form checks its input with the same rules as the API before sending. The server's answer always
    wins, and is shown next to the field it names.
  - **Error summary:** after a failed submit, focus moves to a list of every problem, each a link to its field.
  - **Conflicts:**
    - A version conflict (409) explains that someone else saved first, and offers to load the latest version,
      replacing what was typed.
    - A duplicate SKU (409) is shown on the SKU field.
  - **Deleting:** it asks first. Cancel is focused, so Enter can't delete by reflex. The confirmation message then
    receives focus.
- **Why:**
  - Checking in the browser saves a round trip for most mistakes. The server checks again, because a browser can be
    bypassed.
  - The error-summary pattern (from the UK government's design system) makes problems findable by keyboard and screen
    reader.
  - Overwriting someone else's change silently is the failure optimistic locking exists to prevent (D-027). The form
    makes the conflict visible instead of retrying blindly.
- **Alternatives:** merging the two versions field by field (complex, and rarely needed for a local admin); a last-write-
  wins PUT without a version (loses changes); undo instead of confirming deletes (needs soft deletion).
- **Limits:** no warning when leaving a form with unsaved changes; no bulk actions; one person's admin, not an
  authenticated multi-user tool (plan §1: no full auth platform).

<!-- BUILD FLOW 01.08.03 — Open questions -->
## Open questions (each is decided in its phase)

| Question | Phase |
|---|---|
| ~~Routing: React Router or a small hand-written router~~ Decided: hand-written (D-054) | 13 |
| ~~Product images: self-hosted licensed files or licensed remote URLs~~ Decided: self-hosted (D-051) | 12 |
| Nginx image: official or unprivileged variant | 14 |
| How EC2 receives the exact same image: a registry, or `docker save` and `docker load` | 14–15 |
| EC2 instance type, region, and cost (approved before anything is launched) | 15 |
